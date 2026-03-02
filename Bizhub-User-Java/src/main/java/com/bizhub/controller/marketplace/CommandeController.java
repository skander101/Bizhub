package com.bizhub.controller.marketplace;

import com.bizhub.model.marketplace.Commande;
import com.bizhub.model.marketplace.CommandeJoinProduit;
import com.bizhub.model.marketplace.ProduitService;

import com.bizhub.model.services.common.service.AppSession;
import com.bizhub.model.services.marketplace.CommandeService;
import com.bizhub.model.services.marketplace.ProduitServiceService;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;

public class CommandeController {

    // ===== UI : blocs role-based =====
    @FXML private VBox boxAdd;
    @FXML private VBox boxManage;
    @FXML private TextField tfQuantite;
    @FXML private ComboBox<ProduitService> cbProduit;
    @FXML private Button btnAjouter;

    @FXML private ComboBox<String> cbStatut;
    @FXML private Button btnChangerStatut;

    @FXML private TextField tfFilterClient;
    @FXML private ComboBox<String> cbFilterStatut;

    @FXML private TableView<CommandeJoinProduit> tableCommandes;
    @FXML private TableColumn<CommandeJoinProduit, Integer> colIdCommande;
    @FXML private TableColumn<CommandeJoinProduit, Integer> colIdClient;
    @FXML private TableColumn<CommandeJoinProduit, String> colProduitNom;
    @FXML private TableColumn<CommandeJoinProduit, Integer> colQte;
    @FXML private TableColumn<CommandeJoinProduit, String> colStatut;

    @FXML private javafx.scene.text.Text titleForm;

    // ===== Services =====
    private final CommandeService commandeService = new CommandeService();
    private final ProduitServiceService produitService = new ProduitServiceService();

    private final ObservableList<CommandeJoinProduit> masterData = FXCollections.observableArrayList();
    private FilteredList<CommandeJoinProduit> filteredData;

    private boolean isStartup;
    private boolean isInvestor;

    @FXML
    public void initialize() {
        // ===== columns =====
        colIdCommande.setCellValueFactory(new PropertyValueFactory<>("idCommande"));
        colIdClient.setCellValueFactory(new PropertyValueFactory<>("idClient"));
        colProduitNom.setCellValueFactory(new PropertyValueFactory<>("produitNom"));
        colQte.setCellValueFactory(new PropertyValueFactory<>("quantiteCommande"));
        colStatut.setCellValueFactory(new PropertyValueFactory<>("statut"));

        // ===== combos =====
        cbStatut.setItems(FXCollections.observableArrayList("en_attente", "confirmee", "livree", "annule"));
        cbStatut.getSelectionModel().select("en_attente");

        cbFilterStatut.setItems(FXCollections.observableArrayList("Tous", "en_attente", "confirmee", "livree", "annule"));
        cbFilterStatut.getSelectionModel().select("Tous");

        // ===== table data =====
        filteredData = new FilteredList<>(masterData, x -> true);
        SortedList<CommandeJoinProduit> sorted = new SortedList<>(filteredData);
        sorted.comparatorProperty().bind(tableCommandes.comparatorProperty());
        tableCommandes.setItems(sorted);

        // listeners filters
        if (tfFilterClient != null) tfFilterClient.textProperty().addListener((obs, o, n) -> applyFilters());
        if (cbFilterStatut != null) cbFilterStatut.valueProperty().addListener((obs, o, n) -> applyFilters());

        // ===== role =====
        resolveRoleAndApplyUI();

        // load
        loadProduits();
        refreshJoin();
        applyFilters();
    }

    private void resolveRoleAndApplyUI() {
        var me = AppSession.getCurrentUser();
        String role = (me == null || me.getUserType() == null) ? "" : me.getUserType().toLowerCase().trim();

        // ✅ adapte si chez toi c’est "investisseur" / "startup" exactement
        isStartup = role.contains("startup");
        isInvestor = role.contains("invest") || role.contains("investisseur");

        // Si aucun rôle reconnu => fallback startup
        if (!isStartup && !isInvestor) isStartup = true;

        // ===== UI rules =====
        if (isStartup) {
            // startup: show add, hide manage
            showNode(boxAdd, true);
            showNode(boxManage, false);

            // filters: startup ne doit pas filtrer par client
            if (tfFilterClient != null) {
                tfFilterClient.clear();
                tfFilterClient.setDisable(true);
                tfFilterClient.setVisible(false);
                tfFilterClient.setManaged(false);
            }

            titleForm.setText("Passer une commande");
        } else {
            // investor: show manage, hide add
            showNode(boxAdd, false);
            showNode(boxManage, true);

            // investor: peut filtrer par client
            if (tfFilterClient != null) {
                tfFilterClient.setDisable(false);
                tfFilterClient.setVisible(true);
                tfFilterClient.setManaged(true);
            }

            titleForm.setText("Gestion des statuts");
        }
    }

    private void showNode(javafx.scene.Node n, boolean show) {
        if (n == null) return;
        n.setVisible(show);
        n.setManaged(show);
    }

    // ===== Refresh =====
    @FXML
    public void refreshJoin() {
        try {
            var me = AppSession.getCurrentUser();
            if (me == null) {
                masterData.clear();
                return;
            }

            int idClient = me.getUserId();

            if (isStartup) {
                // ✅ startup voit uniquement ses commandes
                masterData.setAll(commandeService.getByClientJoinProduit(idClient));
            } else {
                // ✅ investisseur voit tout
                masterData.setAll(commandeService.getAllJoinProduit());
            }

            applyFilters();
        } catch (Exception e) {
            error("Erreur DB", e.getMessage());
        }
    }

    // ===== Startup: add order =====
    @FXML
    private void onAjouter() {
        try {
            if (!isStartup) {
                error("Accès refusé", "Seul le startup peut passer une commande.");
                return;
            }

            var me = AppSession.getCurrentUser();
            if (me == null) throw new IllegalStateException("Session vide (utilisateur non connecté).");

            ProduitService p = cbProduit.getValue();
            if (p == null) throw new IllegalArgumentException("Sélectionnez un produit.");

            int qte = parseInt(tfQuantite.getText(), "quantite");
            if (qte <= 0) throw new IllegalArgumentException("Quantité doit être > 0");

            Commande c = new Commande();
            c.setIdClient(me.getUserId());
            c.setIdProduit(p.getIdProduit());
            c.setQuantite(qte);
            c.setStatut("en_attente"); // ✅ startup ne choisit pas le statut

            commandeService.ajouter(c);

            tfQuantite.clear();
            cbProduit.getSelectionModel().clearSelection();
            refreshJoin();
            info("Succès", "Commande ajoutée.");
        } catch (Exception e) {
            error("Erreur", e.getMessage());
        }
    }

    // ===== Investisseur: change status =====
    @FXML
    private void onChangerStatut() {
        try {
            if (!isInvestor) {
                error("Accès refusé", "Seul l’investisseur peut changer le statut.");
                return;
            }

            CommandeJoinProduit selected = tableCommandes.getSelectionModel().getSelectedItem();
            if (selected == null) {
                error("Erreur", "Sélectionnez une commande.");
                return;
            }

            String newStatut = cbStatut.getValue();
            if (newStatut == null || newStatut.isBlank()) {
                error("Erreur", "Choisissez un statut.");
                return;
            }

            commandeService.changerStatut(selected.getIdCommande(), newStatut);

            refreshJoin();
            info("Succès", "Statut modifié.");
        } catch (Exception e) {
            error("Erreur", e.getMessage());
        }
    }

    // ===== products =====
    private void loadProduits() {
        try {
            cbProduit.setItems(FXCollections.observableArrayList(produitService.getAll()));

            cbProduit.setCellFactory(list -> new ListCell<>() {
                @Override protected void updateItem(ProduitService item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty || item == null ? null : item.getNom() + " | " + item.getPrix());
                }
            });
            cbProduit.setButtonCell(new ListCell<>() {
                @Override protected void updateItem(ProduitService item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty || item == null ? null : item.getNom() + " | " + item.getPrix());
                }
            });
        } catch (Exception e) {
            error("Erreur DB", e.getMessage());
        }
    }

    // ===== filters =====
    private void applyFilters() {
        if (filteredData == null) return;

        String clientTxt = (tfFilterClient == null || tfFilterClient.getText() == null) ? "" : tfFilterClient.getText().trim();
        String statut = (cbFilterStatut == null) ? "Tous" : cbFilterStatut.getValue();

        filteredData.setPredicate(c -> {
            boolean okClient = true;

            // ✅ filtre client seulement si investisseur
            if (isInvestor && !clientTxt.isEmpty()) {
                try {
                    int idClient = Integer.parseInt(clientTxt);
                    okClient = c.getIdClient() == idClient;
                } catch (Exception ignored) {
                    okClient = true;
                }
            }

            boolean okStatut = (statut == null || "Tous".equals(statut)) ||
                    (c.getStatut() != null && c.getStatut().equalsIgnoreCase(statut));

            return okClient && okStatut;
        });
    }

    private int parseInt(String v, String field) {
        try { return Integer.parseInt(v.trim()); }
        catch (Exception e) { throw new IllegalArgumentException("Champ " + field + " invalide"); }
    }

    private void info(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(title); a.setHeaderText(null); a.setContentText(msg); a.showAndWait();
    }

    private void error(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle(title); a.setHeaderText(null); a.setContentText(msg); a.showAndWait();
    }
}
