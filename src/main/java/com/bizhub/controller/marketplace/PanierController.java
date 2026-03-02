package com.bizhub.controller.marketplace;

import com.bizhub.model.marketplace.PanierItem;
import com.bizhub.model.services.common.service.AppSession;

import com.bizhub.model.services.common.service.NavigationService;
import com.bizhub.model.services.marketplace.PanierService;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import javafx.util.Callback;

import java.math.BigDecimal;
import java.net.URL;
import java.sql.SQLException;
import java.util.List;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

public class PanierController implements Initializable {

    private static final Logger LOGGER = Logger.getLogger(PanierController.class.getName());

    // ── Table ──
    @FXML private TableView<PanierItem>         tablePanier;
    @FXML private TableColumn<PanierItem,String>     colNom;
    @FXML private TableColumn<PanierItem,String>     colCategorie;
    @FXML private TableColumn<PanierItem,BigDecimal> colPrixUnit;
    @FXML private TableColumn<PanierItem,Integer>    colQte;
    @FXML private TableColumn<PanierItem,BigDecimal> colPrixHT;
    @FXML private TableColumn<PanierItem,Void>       colAction;

    // ── Résumé ──
    @FXML private Label lblNbArticles;
    @FXML private Label lblTotalHT;
    @FXML private Label lblTVA;
    @FXML private Label lblTotalTTC;

    // ── Modifier quantité ──
    @FXML private TextField tfQteEdit;
    @FXML private HBox  hboxMsgQte;
    @FXML private Label iconMsgQte;
    @FXML private Label lblMsgQte;

    // ── Commander ──
    @FXML private Button btnCommander;
    @FXML private Button btnVider;
    @FXML private HBox  hboxMsgCmd;
    @FXML private Label iconMsgCmd;
    @FXML private Label lblMsgCmd;

    private final PanierService panierService = new PanierService();
    private final ObservableList<PanierItem> items = FXCollections.observableArrayList();

    private int idClient() {
        return AppSession.getCurrentUser().getUserId();
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        setupColumns();
        tablePanier.setItems(items);
        refreshPanier();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // COLONNES
    // ─────────────────────────────────────────────────────────────────────────

    private void setupColumns() {
        colNom.setCellValueFactory(new PropertyValueFactory<>("produitNom"));
        colCategorie.setCellValueFactory(new PropertyValueFactory<>("categorie"));
        colPrixUnit.setCellValueFactory(new PropertyValueFactory<>("prixUnitaire"));
        colQte.setCellValueFactory(new PropertyValueFactory<>("quantite"));

        // Colonne Total HT calculée
        colPrixHT.setCellValueFactory(data ->
                new javafx.beans.property.SimpleObjectProperty<>(data.getValue().getPrixHT()));

        // Formater les prix
        colPrixUnit.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(BigDecimal v, boolean empty) {
                super.updateItem(v, empty);
                setText(empty || v == null ? null : String.format("%.3f TND", v));
            }
        });
        colPrixHT.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(BigDecimal v, boolean empty) {
                super.updateItem(v, empty);
                setText(empty || v == null ? null : String.format("%.3f TND", v));
            }
        });

        // Bouton supprimer dans chaque ligne
        colAction.setCellFactory(buildDeleteButtonFactory());
    }

    private Callback<TableColumn<PanierItem, Void>, TableCell<PanierItem, Void>> buildDeleteButtonFactory() {
        return col -> new TableCell<>() {
            private final Button btn = new Button("🗑");
            {
                btn.setStyle("-fx-background-color:transparent;-fx-text-fill:#EF4444;"
                        + "-fx-font-size:14;-fx-cursor:hand;");
                btn.setOnAction(e -> {
                    PanierItem item = getTableView().getItems().get(getIndex());
                    try {
                        panierService.retirer(idClient(), item.getIdProduit());
                        refreshPanier();
                    } catch (SQLException ex) {
                        showErr(hboxMsgCmd, iconMsgCmd, lblMsgCmd,
                                "Erreur suppression : " + ex.getMessage());
                    }
                });
            }
            @Override protected void updateItem(Void v, boolean empty) {
                super.updateItem(v, empty);
                setGraphic(empty ? null : btn);
            }
        };
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ACTIONS
    // ─────────────────────────────────────────────────────────────────────────

    @FXML
    public void onVider() {
        try {
            panierService.vider(idClient());
            refreshPanier();
            showOk(hboxMsgCmd, iconMsgCmd, lblMsgCmd, "Panier vidé.");
        } catch (SQLException e) {
            showErr(hboxMsgCmd, iconMsgCmd, lblMsgCmd, "Erreur : " + e.getMessage());
        }
    }

    @FXML
    public void onAppliquerQte() {
        resetMsg(hboxMsgQte, iconMsgQte, lblMsgQte);
        PanierItem sel = tablePanier.getSelectionModel().getSelectedItem();
        if (sel == null) {
            showErr(hboxMsgQte, iconMsgQte, lblMsgQte, "Sélectionnez un article.");
            return;
        }
        String txt = tfQteEdit.getText() == null ? "" : tfQteEdit.getText().trim();
        try {
            int qte = Integer.parseInt(txt);
            if (qte < 0) throw new NumberFormatException();
            panierService.setQuantite(idClient(), sel.getIdProduit(), qte);
            refreshPanier();
            showOk(hboxMsgQte, iconMsgQte, lblMsgQte,
                    qte == 0 ? "Article retiré." : "Quantité mise à jour.");
        } catch (NumberFormatException e) {
            showErr(hboxMsgQte, iconMsgQte, lblMsgQte, "Entrez un nombre entier ≥ 0.");
        } catch (SQLException e) {
            showErr(hboxMsgQte, iconMsgQte, lblMsgQte, "Erreur : " + e.getMessage());
        }
    }

    @FXML
    public void onCommander() {
        resetMsg(hboxMsgCmd, iconMsgCmd, lblMsgCmd);
        if (items.isEmpty()) {
            showErr(hboxMsgCmd, iconMsgCmd, lblMsgCmd, "Votre panier est vide.");
            return;
        }

        btnCommander.setDisable(true);
        btnCommander.setText("⏳ En cours...");

        new Thread(() -> {
            try {
                int nb = panierService.commanderTout(idClient());
                Platform.runLater(() -> {
                    refreshPanier();
                    btnCommander.setDisable(false);
                    btnCommander.setText("✅  Commander tout le panier");
                    showOk(hboxMsgCmd, iconMsgCmd, lblMsgCmd,
                            "✅ " + nb + " commande(s) créée(s) avec succès !\n"
                                    + "Retrouvez-les dans la page Marketplace.");
                });
            } catch (SQLException e) {
                Platform.runLater(() -> {
                    btnCommander.setDisable(false);
                    btnCommander.setText("✅  Commander tout le panier");
                    showErr(hboxMsgCmd, iconMsgCmd, lblMsgCmd,
                            "Erreur commande : " + e.getMessage());
                });
            }
        }, "panier-commander").start();
    }

    @FXML
    public void goToMarketplace() {
        try {
            Stage stage = (Stage) tablePanier.getScene().getWindow();
            new NavigationService(stage).goToCommande();
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Navigation erreur", e);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // REFRESH
    // ─────────────────────────────────────────────────────────────────────────

    private void refreshPanier() {
        try {
            List<PanierItem> list = panierService.getPanier(idClient());
            items.setAll(list);
            updateTotaux(list);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Erreur chargement panier", e);
        }
    }

    private void updateTotaux(List<PanierItem> list) {
        int nb = list.stream().mapToInt(PanierItem::getQuantite).sum();
        BigDecimal ht  = panierService.getTotalHT(list);
        BigDecimal tva = panierService.getTotalTVA(list);
        BigDecimal ttc = panierService.getTotalTTC(list);

        if (lblNbArticles != null) lblNbArticles.setText(String.valueOf(nb));
        if (lblTotalHT    != null) lblTotalHT.setText(String.format("%.3f TND", ht));
        if (lblTVA        != null) lblTVA.setText(String.format("%.3f TND", tva));
        if (lblTotalTTC   != null) lblTotalTTC.setText(String.format("%.3f TND", ttc));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // HELPERS UI
    // ─────────────────────────────────────────────────────────────────────────

    private void resetMsg(HBox box, Label icon, Label lbl) {
        if (box != null) { box.setVisible(false); box.setManaged(false); }
    }

    private void showOk(HBox box, Label icon, Label lbl, String msg) {
        if (icon != null) { icon.setText("✔"); icon.setStyle("-fx-text-fill:#10B981;"); }
        if (lbl  != null) { lbl.setText(msg); lbl.setStyle("-fx-text-fill:#10B981;"); }
        if (box  != null) { box.setVisible(true); box.setManaged(true); }
    }

    private void showErr(HBox box, Label icon, Label lbl, String msg) {
        if (icon != null) { icon.setText("✖"); icon.setStyle("-fx-text-fill:#EF4444;"); }
        if (lbl  != null) { lbl.setText(msg); lbl.setStyle("-fx-text-fill:#EF4444;"); }
        if (box  != null) { box.setVisible(true); box.setManaged(true); }
    }
}