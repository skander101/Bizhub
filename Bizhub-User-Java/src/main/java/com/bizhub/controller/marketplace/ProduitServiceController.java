package com.bizhub.controller.marketplace;

import com.bizhub.model.marketplace.CommandeJoinProduit;
import com.bizhub.model.marketplace.CommandeRepository;
import com.bizhub.model.marketplace.ProduitService;
import com.bizhub.model.marketplace.ProduitServiceRepository;
import com.bizhub.model.services.common.service.AppSession;
import com.bizhub.model.services.common.ui.toastUtil;
import com.bizhub.model.services.marketplace.CommandeNotificationService;
import com.bizhub.model.services.marketplace.CommandePriorityEngine;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ProduitServiceController {

    private static final Logger LOGGER = Logger.getLogger(ProduitServiceController.class.getName());

    // ── Statuts ───────────────────────────────────────────────
    private static final String STATUT_ATTENTE   = "en_attente";
    private static final String STATUT_CONFIRMEE = "confirmee";
    private static final String STATUT_LIVREE    = "livree";
    private static final String STATUT_ANNULEE   = "annulee";

    // ── Bordures inline ───────────────────────────────────────
    private static final String BORDER_OK  = "-fx-border-color:#10B981;-fx-border-width:2;-fx-border-radius:12;";
    private static final String BORDER_ERR = "-fx-border-color:#EF4444;-fx-border-width:2;-fx-border-radius:12;";
    private static final String BORDER_DEF = "";

    // ── Webhook + Auto-confirm ────────────────────────────────
    private static final String DISCORD_WEBHOOK_URL = "https://discord.com/api/webhooks/XXXX/YYY";
    private static final boolean AUTO_CONFIRM_ENABLED = true;
    private static final int AUTO_CONFIRM_THRESHOLD = 350;
    private static final int MAX_AUTO_CONFIRM_PER_REFRESH = 3;

    // ── Services / Repos ──────────────────────────────────────
    private final ProduitServiceRepository repo = new ProduitServiceRepository();
    private final CommandeRepository commandeRepo = new CommandeRepository();

    private final CommandePriorityEngine priorityEngine = new CommandePriorityEngine();
    private final CommandeNotificationService notifService =
            new CommandeNotificationService(DISCORD_WEBHOOK_URL);

    // ── Data ──────────────────────────────────────────────────
    private final ObservableList<ProduitService> produitsData = FXCollections.observableArrayList();
    private final ObservableList<CommandeJoinProduit> commandesData = FXCollections.observableArrayList();

    private ProduitService selected;

    // ── FXML Filtres ──────────────────────────────────────────
    @FXML private TextField tfSearchNom;
    @FXML private ComboBox<String> cbFilterCategorie;

    // ── FXML KPI ──────────────────────────────────────────────
    @FXML private javafx.scene.text.Text kpiTotalProduits;
    @FXML private javafx.scene.text.Text kpiDisponibles;
    @FXML private javafx.scene.text.Text kpiCategories;

    // ── FXML Formulaire produit ───────────────────────────────
    @FXML private TextField tfIdProfile;
    @FXML private TextField tfNom;
    @FXML private TextField tfPrix;
    @FXML private TextField tfQuantite;
    @FXML private TextField tfCategorie;
    @FXML private CheckBox cbDisponible;
    @FXML private TextArea taDescription;

    @FXML private Button btnAjouter;
    @FXML private Button btnModifier;
    @FXML private Button btnSupprimer;
    @FXML private Button btnVider;

    // ── FXML Messages validation inline (produit) ─────────────
    @FXML private HBox hboxValIdProfile;
    @FXML private Label iconValIdProfile;
    @FXML private Label lblValIdProfile;

    @FXML private HBox hboxValNom;
    @FXML private Label iconValNom;
    @FXML private Label lblValNom;

    @FXML private HBox hboxValPrix;
    @FXML private Label iconValPrix;
    @FXML private Label lblValPrix;

    @FXML private HBox hboxValQuantite;
    @FXML private Label iconValQuantite;
    @FXML private Label lblValQuantite;

    @FXML private HBox hboxValCategorie;
    @FXML private Label iconValCategorie;
    @FXML private Label lblValCategorie;

    @FXML private HBox hboxMsgCrud;
    @FXML private Label iconMsgCrud;
    @FXML private Label lblMsgCrud;

    // ── FXML Table produits ───────────────────────────────────
    @FXML private TableView<ProduitService> tableProduits;
    @FXML private TableColumn<ProduitService, Integer> colId;
    @FXML private TableColumn<ProduitService, String> colNom;
    @FXML private TableColumn<ProduitService, String> colCategorie;
    @FXML private TableColumn<ProduitService, Integer> colQuantite;
    @FXML private TableColumn<ProduitService, BigDecimal> colPrix;
    @FXML private TableColumn<ProduitService, Boolean> colDisponible;

    // ── FXML Section commandes investisseur ───────────────────
    @FXML private VBox boxCommandesInvestor;

    @FXML private HBox hboxCmdInfo;
    @FXML private Label lblCmdInfo;

    @FXML private TableView<CommandeJoinProduit> tableCommandes;
    @FXML private TableColumn<CommandeJoinProduit, Integer> colCmdId;
    @FXML private TableColumn<CommandeJoinProduit, Integer> colCmdClient;
    @FXML private TableColumn<CommandeJoinProduit, String> colCmdProduit;
    @FXML private TableColumn<CommandeJoinProduit, Integer> colCmdQte;
    @FXML private TableColumn<CommandeJoinProduit, String> colCmdStatut;

    // ✅ Colonnes priorité
    @FXML private TableColumn<CommandeJoinProduit, String> colCmdPriorite;
    @FXML private TableColumn<CommandeJoinProduit, Integer> colCmdScore;

    @FXML private Button btnConfirmerCmd;
    @FXML private Button btnAnnulerCmd;

    @FXML private HBox hboxMsgCmd;
    @FXML private Label iconMsgCmd;
    @FXML private Label lblMsgCmd;

    // ====== FUN UI popover priorité ======
    private javafx.stage.Popup priorityPopup;

    // =====================================================
    // INITIALISATION
    // =====================================================
    @FXML
    public void initialize() {

        // ✅ Table produits
        if (colId != null)         colId.setCellValueFactory(new PropertyValueFactory<>("idProduit"));
        if (colNom != null)        colNom.setCellValueFactory(new PropertyValueFactory<>("nom"));
        if (colCategorie != null)  colCategorie.setCellValueFactory(new PropertyValueFactory<>("categorie"));
        if (colQuantite != null)   colQuantite.setCellValueFactory(new PropertyValueFactory<>("quantite"));
        if (colPrix != null)       colPrix.setCellValueFactory(new PropertyValueFactory<>("prix"));
        if (colDisponible != null) colDisponible.setCellValueFactory(new PropertyValueFactory<>("disponible"));
        if (tableProduits != null) tableProduits.setItems(produitsData);

        if (tableProduits != null) {
            tableProduits.getSelectionModel().selectedItemProperty()
                    .addListener((obs, o, n) -> { selected = n; if (n != null) fillForm(n); });
        }

        // ✅ Table commandes (investisseur)
        if (tableCommandes != null) {
            if (colCmdId != null)      colCmdId.setCellValueFactory(new PropertyValueFactory<>("idCommande"));
            if (colCmdClient != null)  colCmdClient.setCellValueFactory(new PropertyValueFactory<>("idClient"));
            if (colCmdProduit != null) colCmdProduit.setCellValueFactory(new PropertyValueFactory<>("produitNom"));
            if (colCmdQte != null)     colCmdQte.setCellValueFactory(new PropertyValueFactory<>("quantiteCommande"));
            if (colCmdStatut != null)  colCmdStatut.setCellValueFactory(new PropertyValueFactory<>("statut"));

            if (colCmdPriorite != null) colCmdPriorite.setCellValueFactory(new PropertyValueFactory<>("priorityLabel"));
            if (colCmdScore != null)    colCmdScore.setCellValueFactory(new PropertyValueFactory<>("priorityScore"));

            hideColumn(colCmdId);
            hideColumn(colCmdClient);

            tableCommandes.setItems(commandesData);

            setupCmdStatutBadgesInvestor();
            setupCmdRowHoverGlowInvestor();
            setupPriorityColumnEffects();
            setupRowPulseForHighPriority();

            tableCommandes.getSelectionModel().selectedItemProperty()
                    .addListener((obs, o, sel) -> onCmdSelectionChanged(sel));
        }

        // ✅ Rôle
        String role = getCurrentRole();
        boolean isInvestisseur = role.contains("invest");

        setVM(boxCommandesInvestor, isInvestisseur);

        // CRUD produits : investisseur ou fournisseur
        boolean canCrud = isInvestisseur || role.contains("fournisseur");
        setProductsCrudEnabled(canCrud);

        // Validation temps réel produit
        attachRealTimeValidation();

        // Filtres produits
        if (tfSearchNom != null) tfSearchNom.textProperty().addListener((obs, o, n) -> applyFilters());
        if (cbFilterCategorie != null) cbFilterCategorie.valueProperty().addListener((obs, o, n) -> applyFilters());

        refreshProduits();
        if (isInvestisseur) refreshCommandes();
    }

    // =====================================================
    // COMMANDES INVESTISSEUR
    // =====================================================
    @FXML
    public void refreshCommandes() {
        try {
            var me = AppSession.getCurrentUser();
            if (me == null) { commandesData.clear(); return; }

            int ownerId = me.getUserId();

            // 1) Charger commandes reçues
            commandesData.setAll(commandeRepo.findByOwnerJoinProduit(ownerId));

            // 2) Appliquer moteur IA (score + label + recommandation + raison)
            for (CommandeJoinProduit c : commandesData) {
                priorityEngine.apply(c);
            }

            // 3) AUTO-CONFIRM
            if (AUTO_CONFIRM_ENABLED) {
                int done = 0;

                for (CommandeJoinProduit c : commandesData) {
                    if (done >= MAX_AUTO_CONFIRM_PER_REFRESH) break;

                    boolean enAttente = STATUT_ATTENTE.equalsIgnoreCase(safe(c.getStatut()));
                    boolean rec = c.isAutoConfirmRecommended();
                    int score = c.getPriorityScore();

                    if (enAttente && rec && score >= AUTO_CONFIRM_THRESHOLD) {
                        int updated = commandeRepo.updateStatutIfEnAttente(c.getIdCommande(), STATUT_CONFIRMEE);

                        if (updated == 1) {
                            done++;

                            // Toast fun
                            try { toastUtil.ai("🤖 Auto-confirm ✅\nCommande #" + c.getIdCommande() + "\nScore: " + score); }
                            catch (Exception ignore) {}

                            // Webhook Discord
                            try {
                                notifService.sendDiscord(
                                        "🤖 **BizHub Auto-confirm**\n"
                                                + "• Commande #" + c.getIdCommande() + "\n"
                                                + "• Produit : " + safe(c.getProduitNom()) + "\n"
                                                + "• Qté : " + c.getQuantiteCommande() + "\n"
                                                + "• Score : " + score + "\n"
                                                + "• Raison : " + safe(c.getAutoReason())
                                );
                            } catch (Exception ignore) {}
                        }
                    }
                }

                if (done > 0) {
                    commandesData.setAll(commandeRepo.findByOwnerJoinProduit(ownerId));
                    for (CommandeJoinProduit c : commandesData) priorityEngine.apply(c);

                    showOk(hboxMsgCmd, iconMsgCmd, lblMsgCmd, null,
                            "🤖 Auto-confirmation : " + done + " commande(s) confirmée(s).");

                    try { toastUtil.success("✅ " + done + " commande(s) confirmée(s) automatiquement."); }
                    catch (Exception ignore) {}
                }
            }

            // 4) Tri score desc
            if (tableCommandes != null && colCmdScore != null) {
                colCmdScore.setSortType(TableColumn.SortType.DESCENDING);
                tableCommandes.getSortOrder().setAll(colCmdScore);
                tableCommandes.sort();
            }

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "refreshCommandes", e);
            try { toastUtil.error("Erreur DB : " + e.getMessage()); } catch (Exception ignore) {}
            showErr(hboxMsgCmd, iconMsgCmd, lblMsgCmd, null, "Erreur DB : " + e.getMessage());
        }
    }

    private void onCmdSelectionChanged(CommandeJoinProduit sel) {
        resetMsg(hboxMsgCmd, iconMsgCmd, lblMsgCmd);

        // reset buttons
        if (btnConfirmerCmd != null) {
            btnConfirmerCmd.setDisable(true);
            btnConfirmerCmd.setText("✔ Confirmer");
            btnConfirmerCmd.setStyle("");
            btnConfirmerCmd.setTooltip(null);
        }
        if (btnAnnulerCmd != null) {
            btnAnnulerCmd.setDisable(true);
            btnAnnulerCmd.setText("✖ Annuler");
            btnAnnulerCmd.setStyle("");
            btnAnnulerCmd.setTooltip(null);
        }

        if (sel == null) { setVM(hboxCmdInfo, false); return; }

        String statut = safe(sel.getStatut()).toLowerCase(Locale.ROOT);
        String reason = safe(sel.getAutoReason());

        if (lblCmdInfo != null) {
            lblCmdInfo.setText("Commande #" + sel.getIdCommande()
                    + " | " + safe(sel.getProduitNom())
                    + " | Qté: " + sel.getQuantiteCommande()
                    + " | Statut: " + safe(sel.getStatut())
                    + " | Priorité: " + safe(sel.getPriorityLabel()) + " (" + sel.getPriorityScore() + ")"
                    + (reason.isEmpty() ? "" : "\n🤖 " + reason));
        }
        setVM(hboxCmdInfo, true);

        boolean editable = STATUT_ATTENTE.equalsIgnoreCase(statut);
        if (btnConfirmerCmd != null) btnConfirmerCmd.setDisable(!editable);
        if (btnAnnulerCmd != null)   btnAnnulerCmd.setDisable(!editable);

        if (editable && btnConfirmerCmd != null) {
            boolean recommended = sel.isAutoConfirmRecommended();
            if (recommended) {
                btnConfirmerCmd.setText("🤖 Confirmer (Recommandé)");
                btnConfirmerCmd.setStyle(
                        "-fx-background-radius:14; -fx-font-weight:900;"
                                + "-fx-background-color: linear-gradient(#F59E0B,#FBBF24);"
                                + "-fx-text-fill:#111827;"
                                + "-fx-effect: dropshadow(gaussian, rgba(251,191,36,0.35), 18, 0.2, 0, 6);"
                );
            }
            if (!reason.isEmpty()) btnConfirmerCmd.setTooltip(new Tooltip(reason));
        }
    }

    @FXML
    public void onConfirmerCommande() {
        resetMsg(hboxMsgCmd, iconMsgCmd, lblMsgCmd);

        CommandeJoinProduit sel = tableCommandes != null
                ? tableCommandes.getSelectionModel().getSelectedItem() : null;

        if (sel == null) {
            showErr(hboxMsgCmd, iconMsgCmd, lblMsgCmd, null, "Sélectionnez une commande.");
            return;
        }
        if (!STATUT_ATTENTE.equalsIgnoreCase(safe(sel.getStatut()))) {
            showErr(hboxMsgCmd, iconMsgCmd, lblMsgCmd, null,
                    "Impossible — statut actuel : " + safe(sel.getStatut())
                            + "\nSeules les commandes en attente peuvent être confirmées.");
            return;
        }

        try {
            commandeRepo.updateStatut(sel.getIdCommande(), STATUT_CONFIRMEE);
            toastUtil.success("✅ Confirmée : #" + sel.getIdCommande());
            refreshCommandes();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "onConfirmerCommande", e);
            toastUtil.error("Erreur : " + e.getMessage());
            showErr(hboxMsgCmd, iconMsgCmd, lblMsgCmd, null, "Erreur : " + e.getMessage());
        }
    }

    @FXML
    public void onAnnulerCommande() {
        resetMsg(hboxMsgCmd, iconMsgCmd, lblMsgCmd);

        CommandeJoinProduit sel = tableCommandes != null
                ? tableCommandes.getSelectionModel().getSelectedItem() : null;

        if (sel == null) {
            showErr(hboxMsgCmd, iconMsgCmd, lblMsgCmd, null, "Sélectionnez une commande.");
            return;
        }
        if (!STATUT_ATTENTE.equalsIgnoreCase(safe(sel.getStatut()))) {
            showErr(hboxMsgCmd, iconMsgCmd, lblMsgCmd, null,
                    "Impossible — statut actuel : " + safe(sel.getStatut())
                            + "\nSeules les commandes en attente peuvent être annulées.");
            return;
        }

        try {
            commandeRepo.updateStatut(sel.getIdCommande(), STATUT_ANNULEE);
            toastUtil.warning("⛔ Annulée : #" + sel.getIdCommande());
            refreshCommandes();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "onAnnulerCommande", e);
            toastUtil.error("Erreur : " + e.getMessage());
            showErr(hboxMsgCmd, iconMsgCmd, lblMsgCmd, null, "Erreur : " + e.getMessage());
        }
    }

    // =====================================================
    // FUN UI — Statut badges + Hover
    // =====================================================
    private void setupCmdStatutBadgesInvestor() {
        if (colCmdStatut == null) return;

        colCmdStatut.setCellFactory(col -> new TableCell<>() {
            private final Label badge = new Label();
            { badge.setStyle("-fx-padding:4 10; -fx-font-weight:800;"
                    + "-fx-background-radius:999; -fx-border-radius:999; -fx-border-width:1;"
                    + "-fx-font-size:11px;"); }

            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setGraphic(null); return; }

                String s = item.trim().toLowerCase(Locale.ROOT);
                badge.setStyle("-fx-padding:4 10; -fx-font-weight:800;"
                        + "-fx-background-radius:999; -fx-border-radius:999; -fx-border-width:1;"
                        + "-fx-font-size:11px;");

                switch (s) {
                    case STATUT_ATTENTE -> { badge.setText("⏳ En attente");
                        badge.setStyle(badge.getStyle()
                                + " -fx-background-color: rgba(245,158,11,0.18);"
                                + " -fx-text-fill:#FCD34D;"
                                + " -fx-border-color: rgba(245,158,11,0.55);"); }
                    case STATUT_CONFIRMEE -> { badge.setText("✅ Confirmée");
                        badge.setStyle(badge.getStyle()
                                + " -fx-background-color: rgba(16,185,129,0.18);"
                                + " -fx-text-fill:#6EE7B7;"
                                + " -fx-border-color: rgba(16,185,129,0.55);"); }
                    case STATUT_LIVREE -> { badge.setText("📦 Livrée");
                        badge.setStyle(badge.getStyle()
                                + " -fx-background-color: rgba(59,130,246,0.18);"
                                + " -fx-text-fill:#93C5FD;"
                                + " -fx-border-color: rgba(59,130,246,0.55);"); }
                    case STATUT_ANNULEE -> { badge.setText("⛔ Annulée");
                        badge.setStyle(badge.getStyle()
                                + " -fx-background-color: rgba(239,68,68,0.18);"
                                + " -fx-text-fill:#FCA5A5;"
                                + " -fx-border-color: rgba(239,68,68,0.55);"); }
                    default -> badge.setText(item);
                }
                setGraphic(badge);
            }
        });
    }

    private void setupCmdRowHoverGlowInvestor() {
        if (tableCommandes == null) return;

        tableCommandes.setRowFactory(tv -> {
            TableRow<CommandeJoinProduit> row = new TableRow<>();
            row.hoverProperty().addListener((obs, oldV, hover) -> {
                if (row.isEmpty()) { row.setStyle(""); return; }
                row.setStyle(hover ? "-fx-background-color: rgba(251,191,36,0.07);" : "");
            });
            return row;
        });
    }

    private void setupPriorityColumnEffects() {
        if (tableCommandes == null || colCmdPriorite == null) return;

        colCmdPriorite.setCellFactory(col -> new TableCell<>() {
            private final Label badge = new Label();
            private final Tooltip tip = new Tooltip();

            {
                badge.setStyle("-fx-padding:4 10; -fx-font-weight:800; -fx-background-radius:999;"
                        + "-fx-border-radius:999; -fx-border-width:1; -fx-font-size:11px;");
                badge.setMinWidth(90);
                badge.setPrefWidth(110);

                Tooltip.install(badge, tip);

                badge.setOnMouseClicked(e -> {
                    CommandeJoinProduit c = getRowItem();
                    if (c == null) return;
                    showPriorityPopover(badge, c);
                });
            }

            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setGraphic(null); setText(null); return; }

                CommandeJoinProduit row = getRowItem();
                int score = row != null ? row.getPriorityScore() : 0;

                // reset base
                badge.setStyle("-fx-padding:4 10; -fx-font-weight:800; -fx-background-radius:999;"
                        + "-fx-border-radius:999; -fx-border-width:1; -fx-font-size:11px;");

                String p = item.toUpperCase(Locale.ROOT);
                switch (p) {
                    case "HAUTE" -> {
                        badge.setText("🔥 HAUTE");
                        badge.setStyle(badge.getStyle()
                                + " -fx-background-color: rgba(239,68,68,0.18);"
                                + " -fx-text-fill: #FCA5A5;"
                                + " -fx-border-color: rgba(239,68,68,0.55);");
                    }
                    case "MOYENNE" -> {
                        badge.setText("⚡ MOYENNE");
                        badge.setStyle(badge.getStyle()
                                + " -fx-background-color: rgba(245,158,11,0.18);"
                                + " -fx-text-fill: #FCD34D;"
                                + " -fx-border-color: rgba(245,158,11,0.55);");
                    }
                    default -> {
                        badge.setText("✅ BASSE");
                        badge.setStyle(badge.getStyle()
                                + " -fx-background-color: rgba(16,185,129,0.18);"
                                + " -fx-text-fill: #6EE7B7;"
                                + " -fx-border-color: rgba(16,185,129,0.55);");
                    }
                }

                String produit = row != null ? safe(row.getProduitNom()) : "";
                String statut  = row != null ? safe(row.getStatut()) : "";
                int qte        = row != null ? row.getQuantiteCommande() : 0;

                tip.setText("Produit : " + produit
                        + "\nQté : " + qte
                        + "\nStatut : " + statut
                        + "\nScore : " + score
                        + "\n\nClique pour voir le détail 🙂");

                setGraphic(badge);
                setText(null);
            }

            private CommandeJoinProduit getRowItem() {
                return getTableRow() == null ? null : (CommandeJoinProduit) getTableRow().getItem();
            }
        });

        if (colCmdScore != null) {
            colCmdScore.setCellFactory(col -> new TableCell<>() {
                private final ProgressBar bar = new ProgressBar();
                private final Label text = new Label();

                {
                    bar.setMaxWidth(Double.MAX_VALUE);
                    bar.setPrefHeight(10);
                    text.setStyle("-fx-font-size:11px; -fx-font-weight:800; -fx-text-fill:#E5E7EB;");
                }

                @Override protected void updateItem(Integer score, boolean empty) {
                    super.updateItem(score, empty);
                    if (empty || score == null) { setGraphic(null); setText(null); return; }

                    double v = Math.max(0, Math.min(score, 500)) / 500.0;
                    bar.setProgress(v);

                    CommandeJoinProduit row = getTableRow() == null ? null : (CommandeJoinProduit) getTableRow().getItem();
                    String p = row == null ? "" : safe(row.getPriorityLabel()).toUpperCase(Locale.ROOT);
                    if ("HAUTE".equals(p)) bar.setStyle("-fx-accent: #EF4444;");
                    else if ("MOYENNE".equals(p)) bar.setStyle("-fx-accent: #F59E0B;");
                    else bar.setStyle("-fx-accent: #10B981;");

                    text.setText(String.valueOf(score));
                    HBox box = new HBox(8, bar, text);
                    box.setAlignment(Pos.CENTER_LEFT);
                    setGraphic(box);
                    setText(null);
                }
            });
        }
    }

    private void showPriorityPopover(Node anchor, CommandeJoinProduit c) {
        if (priorityPopup != null && priorityPopup.isShowing()) priorityPopup.hide();
        priorityPopup = new javafx.stage.Popup();
        priorityPopup.setAutoHide(true);

        String p = safe(c.getPriorityLabel()).toUpperCase(Locale.ROOT);
        int score = c.getPriorityScore();

        Label title = new Label("Détail Priorité");
        title.setStyle("-fx-font-size:13px; -fx-font-weight:900; -fx-text-fill:#FBBF24;");

        Label l1 = new Label("• Priorité : " + p);
        Label l2 = new Label("• Score : " + score);
        Label l3 = new Label("• Raison : " + reasonText(c));

        l1.setStyle("-fx-text-fill:#E5E7EB; -fx-font-weight:800;");
        l2.setStyle("-fx-text-fill:#E5E7EB; -fx-font-weight:800;");
        l3.setStyle("-fx-text-fill:#93C5FD; -fx-font-weight:800;");

        VBox box = new VBox(6, title, l1, l2, l3);
        box.setStyle("""
            -fx-background-color: rgba(17,24,39,0.98);
            -fx-padding: 12;
            -fx-background-radius: 14;
            -fx-border-radius: 14;
            -fx-border-color: rgba(251,191,36,0.35);
            -fx-border-width: 1;
            -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.45), 18, 0.2, 0, 6);
        """);

        priorityPopup.getContent().add(box);

        var p2d = anchor.localToScreen(anchor.getBoundsInLocal().getMinX(), anchor.getBoundsInLocal().getMaxY());
        priorityPopup.show(anchor, p2d.getX(), p2d.getY() + 6);
    }

    private String reasonText(CommandeJoinProduit c) {
        String st = safe(c.getStatut());
        int q = c.getQuantiteCommande();

        if (!STATUT_ATTENTE.equalsIgnoreCase(st)) return "Commande non-actionnable (info).";
        if (q >= 200) return "Grosse quantité → priorité max 🔥";
        if (q >= 80)  return "Quantité moyenne → important ⚡";
        return "Petite quantité → moins urgent ✅";
    }

    private void setupRowPulseForHighPriority() {
        if (tableCommandes == null) return;

        tableCommandes.setRowFactory(tv -> new TableRow<>() {
            private Timeline pulse;

            @Override
            protected void updateItem(CommandeJoinProduit item, boolean empty) {
                super.updateItem(item, empty);

                if (pulse != null) pulse.stop();
                setStyle("");

                if (empty || item == null) return;

                if ("HAUTE".equalsIgnoreCase(safe(item.getPriorityLabel()))) {
                    setStyle("-fx-background-color: rgba(239,68,68,0.05);");
                    pulse = new Timeline(
                            new KeyFrame(javafx.util.Duration.ZERO, new KeyValue(opacityProperty(), 1.0)),
                            new KeyFrame(javafx.util.Duration.seconds(0.9), new KeyValue(opacityProperty(), 0.85)),
                            new KeyFrame(javafx.util.Duration.seconds(1.8), new KeyValue(opacityProperty(), 1.0))
                    );
                    pulse.setCycleCount(Animation.INDEFINITE);
                    pulse.play();
                }
            }
        });
    }

    // =====================================================
    // PRODUITS CRUD + VALIDATION
    // =====================================================
    @FXML
    public void onAjouter() {
        resetAllCrudValidation();
        try {
            var me = AppSession.getCurrentUser();
            if (me == null) throw new IllegalStateException("Utilisateur non connecté.");

            ProduitService p = readForm();
            p.setOwnerUserId(me.getUserId());

            repo.add(p);
            refreshProduits();
            onVider();

            showOk(hboxMsgCrud, iconMsgCrud, lblMsgCrud, null, "✓ Produit ajouté avec succès.");
            toastUtil.success("✅ Produit ajouté");
        } catch (IllegalArgumentException e) {
            showErr(hboxMsgCrud, iconMsgCrud, lblMsgCrud, null, e.getMessage());
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "onAjouter", e);
            showErr(hboxMsgCrud, iconMsgCrud, lblMsgCrud, null, "Erreur : " + e.getMessage());
        }
    }

    @FXML
    public void onModifier() {
        resetAllCrudValidation();
        try {
            if (selected == null) throw new IllegalArgumentException("Sélectionnez un produit dans la liste.");
            var me = AppSession.getCurrentUser();
            if (me == null) throw new IllegalStateException("Utilisateur non connecté.");

            ProduitService p = readForm();
            p.setIdProduit(selected.getIdProduit());
            p.setOwnerUserId(me.getUserId());

            repo.update(p);
            refreshProduits();
            showOk(hboxMsgCrud, iconMsgCrud, lblMsgCrud, null, "✓ Produit modifié avec succès.");
            toastUtil.success("✏️ Produit modifié");
        } catch (IllegalArgumentException e) {
            showErr(hboxMsgCrud, iconMsgCrud, lblMsgCrud, null, e.getMessage());
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "onModifier", e);
            showErr(hboxMsgCrud, iconMsgCrud, lblMsgCrud, null, "Erreur : " + e.getMessage());
        }
    }

    @FXML
    public void onSupprimer() {
        resetAllCrudValidation();
        try {
            if (selected == null) throw new IllegalArgumentException("Sélectionnez un produit dans la liste.");
            repo.delete(selected.getIdProduit());
            refreshProduits();
            onVider();
            showOk(hboxMsgCrud, iconMsgCrud, lblMsgCrud, null, "✓ Produit supprimé.");
            toastUtil.warning("🗑️ Produit supprimé");
        } catch (IllegalArgumentException e) {
            showErr(hboxMsgCrud, iconMsgCrud, lblMsgCrud, null, e.getMessage());
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "onSupprimer", e);
            showErr(hboxMsgCrud, iconMsgCrud, lblMsgCrud, null, "Erreur : " + e.getMessage());
        }
    }

    @FXML
    public void onVider() {
        selected = null;
        resetAllCrudValidation();
        if (tableProduits != null) tableProduits.getSelectionModel().clearSelection();
        if (tfIdProfile != null) { tfIdProfile.clear(); tfIdProfile.setStyle(BORDER_DEF); }
        if (tfNom != null)       { tfNom.clear();       tfNom.setStyle(BORDER_DEF); }
        if (tfPrix != null)      { tfPrix.clear();      tfPrix.setStyle(BORDER_DEF); }
        if (tfQuantite != null)  { tfQuantite.clear();  tfQuantite.setStyle(BORDER_DEF); }
        if (tfCategorie != null) { tfCategorie.clear(); tfCategorie.setStyle(BORDER_DEF); }
        if (taDescription != null) taDescription.clear();
        if (cbDisponible != null) cbDisponible.setSelected(true);
    }

    private ProduitService readForm() {
        boolean valid = true;

        int idProfile = 0;
        String rawId = nz(tfIdProfile != null ? tfIdProfile.getText() : "");
        if (rawId.isEmpty() || !rawId.matches("\\d+") || (idProfile = Integer.parseInt(rawId)) <= 0) {
            showErr(hboxValIdProfile, iconValIdProfile, lblValIdProfile, tfIdProfile, "Entier positif requis.");
            valid = false;
        } else showOk(hboxValIdProfile, iconValIdProfile, lblValIdProfile, tfIdProfile, "Valide.");

        String nom = nz(tfNom != null ? tfNom.getText() : "");
        if (nom.isEmpty()) {
            showErr(hboxValNom, iconValNom, lblValNom, tfNom, "Le nom est obligatoire.");
            valid = false;
        } else showOk(hboxValNom, iconValNom, lblValNom, tfNom, "Valide.");

        BigDecimal prix;
        try {
            prix = new BigDecimal(nz(tfPrix != null ? tfPrix.getText() : ""));
            if (prix.compareTo(BigDecimal.ZERO) <= 0) throw new NumberFormatException();
            showOk(hboxValPrix, iconValPrix, lblValPrix, tfPrix, "Valide.");
        } catch (Exception e) {
            showErr(hboxValPrix, iconValPrix, lblValPrix, tfPrix, "Nombre décimal > 0 requis (ex: 49.90).");
            valid = false;
            prix = BigDecimal.ZERO;
        }

        int qte = 0;
        String rawQte = nz(tfQuantite != null ? tfQuantite.getText() : "");
        if (rawQte.isEmpty() || !rawQte.matches("\\d+")) {
            showErr(hboxValQuantite, iconValQuantite, lblValQuantite, tfQuantite, "Entier >= 0 requis.");
            valid = false;
        } else {
            qte = Integer.parseInt(rawQte);
            showOk(hboxValQuantite, iconValQuantite, lblValQuantite, tfQuantite, "Valide.");
        }

        String cat = nz(tfCategorie != null ? tfCategorie.getText() : "");
        if (cat.isEmpty()) {
            showErr(hboxValCategorie, iconValCategorie, lblValCategorie, tfCategorie, "Catégorie obligatoire.");
            valid = false;
        } else showOk(hboxValCategorie, iconValCategorie, lblValCategorie, tfCategorie, "Valide.");

        if (!valid) throw new IllegalArgumentException("Corrigez les champs en rouge avant de continuer.");

        ProduitService p = new ProduitService();
        p.setIdProfile(idProfile);
        p.setNom(nom);
        p.setPrix(prix);
        p.setQuantite(qte);
        p.setCategorie(cat);
        p.setDescription(nz(taDescription != null ? taDescription.getText() : ""));
        p.setDisponible(cbDisponible != null && cbDisponible.isSelected());
        return p;
    }

    private void fillForm(ProduitService p) {
        resetAllCrudValidation();
        if (tfIdProfile != null) tfIdProfile.setText(String.valueOf(p.getIdProfile()));
        if (tfNom != null) tfNom.setText(nz(p.getNom()));
        if (tfPrix != null) tfPrix.setText(p.getPrix() == null ? "" : p.getPrix().toPlainString());
        if (tfQuantite != null) tfQuantite.setText(String.valueOf(p.getQuantite()));
        if (tfCategorie != null) tfCategorie.setText(nz(p.getCategorie()));
        if (taDescription != null) taDescription.setText(nz(p.getDescription()));
        if (cbDisponible != null) cbDisponible.setSelected(p.isDisponible());
    }

    // =====================================================
    // REFRESH PRODUITS + KPI
    // =====================================================
    public void refreshProduits() {
        try {
            produitsData.setAll(repo.findAll());
            loadCategories();
            applyFilters();
            updateKpi();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "refreshProduits", e);
            showErr(hboxMsgCrud, iconMsgCrud, lblMsgCrud, null, "Erreur DB : " + e.getMessage());
        }
    }

    private void loadCategories() throws SQLException {
        if (cbFilterCategorie == null) return;
        List<String> cats = repo.findAllCategories();
        ObservableList<String> items = FXCollections.observableArrayList("Toutes");
        items.addAll(cats);
        cbFilterCategorie.setItems(items);
        if (cbFilterCategorie.getValue() == null) cbFilterCategorie.setValue("Toutes");
    }

    private void applyFilters() {
        String q = tfSearchNom == null ? "" : nz(tfSearchNom.getText()).toLowerCase(Locale.ROOT);
        String cat = cbFilterCategorie == null ? "Toutes" : cbFilterCategorie.getValue();
        try {
            produitsData.setAll(repo.findAll().stream().filter(p -> {
                boolean okNom = q.isEmpty() || nz(p.getNom()).toLowerCase(Locale.ROOT).contains(q);
                boolean okCat = (cat == null || cat.equals("Toutes")) || nz(p.getCategorie()).equalsIgnoreCase(cat);
                return okNom && okCat;
            }).toList());
            updateKpi();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "applyFilters", e);
        }
    }

    private void updateKpi() {
        int total = produitsData.size();
        int dispo = (int) produitsData.stream().filter(ProduitService::isDisponible).count();
        Set<String> cats = new HashSet<>();
        produitsData.forEach(p -> {
            String c = nz(p.getCategorie());
            if (!c.isBlank()) cats.add(c.toLowerCase(Locale.ROOT));
        });

        if (kpiTotalProduits != null) kpiTotalProduits.setText(String.valueOf(total));
        if (kpiDisponibles != null) kpiDisponibles.setText(String.valueOf(dispo));
        if (kpiCategories != null) kpiCategories.setText(String.valueOf(cats.size()));
    }

    private void setProductsCrudEnabled(boolean enabled) {
        setVM(btnAjouter, enabled);
        setVM(btnModifier, enabled);
        setVM(btnSupprimer, enabled);
        setVM(btnVider, enabled);
    }

    // =====================================================
    // VALIDATION TEMPS RÉEL (produit)
    // =====================================================
    private void attachRealTimeValidation() {
        if (tfIdProfile != null)
            tfIdProfile.textProperty().addListener((obs, o, n) -> {
                if (n == null || n.isBlank()) { resetMsg(hboxValIdProfile, iconValIdProfile, lblValIdProfile); tfIdProfile.setStyle(BORDER_DEF); return; }
                if (!n.trim().matches("\\d+") || Integer.parseInt(n.trim()) <= 0)
                    showErr(hboxValIdProfile, iconValIdProfile, lblValIdProfile, tfIdProfile, "Entier positif requis.");
                else showOk(hboxValIdProfile, iconValIdProfile, lblValIdProfile, tfIdProfile, "Valide.");
            });

        if (tfNom != null)
            tfNom.textProperty().addListener((obs, o, n) -> {
                if (n == null || n.isBlank()) { resetMsg(hboxValNom, iconValNom, lblValNom); tfNom.setStyle(BORDER_DEF); return; }
                if (n.trim().isEmpty())
                    showErr(hboxValNom, iconValNom, lblValNom, tfNom, "Le nom est obligatoire.");
                else showOk(hboxValNom, iconValNom, lblValNom, tfNom, "Valide.");
            });

        if (tfPrix != null)
            tfPrix.textProperty().addListener((obs, o, n) -> {
                if (n == null || n.isBlank()) { resetMsg(hboxValPrix, iconValPrix, lblValPrix); tfPrix.setStyle(BORDER_DEF); return; }
                try {
                    BigDecimal v = new BigDecimal(n.trim());
                    if (v.compareTo(BigDecimal.ZERO) <= 0)
                        showErr(hboxValPrix, iconValPrix, lblValPrix, tfPrix, "Le prix doit être > 0.");
                    else showOk(hboxValPrix, iconValPrix, lblValPrix, tfPrix, "Valide.");
                } catch (NumberFormatException e) {
                    showErr(hboxValPrix, iconValPrix, lblValPrix, tfPrix, "Format invalide (ex: 49.90).");
                }
            });

        if (tfQuantite != null)
            tfQuantite.textProperty().addListener((obs, o, n) -> {
                if (n == null || n.isBlank()) { resetMsg(hboxValQuantite, iconValQuantite, lblValQuantite); tfQuantite.setStyle(BORDER_DEF); return; }
                if (!n.trim().matches("\\d+"))
                    showErr(hboxValQuantite, iconValQuantite, lblValQuantite, tfQuantite, "Entier >= 0 requis.");
                else showOk(hboxValQuantite, iconValQuantite, lblValQuantite, tfQuantite, "Valide.");
            });

        if (tfCategorie != null)
            tfCategorie.textProperty().addListener((obs, o, n) -> {
                if (n == null || n.isBlank()) { resetMsg(hboxValCategorie, iconValCategorie, lblValCategorie); tfCategorie.setStyle(BORDER_DEF); return; }
                if (n.trim().isEmpty())
                    showErr(hboxValCategorie, iconValCategorie, lblValCategorie, tfCategorie, "Catégorie obligatoire.");
                else showOk(hboxValCategorie, iconValCategorie, lblValCategorie, tfCategorie, "Valide.");
            });
    }

    // =====================================================
    // HELPERS UI
    // =====================================================
    private void showErr(HBox hbox, Label icon, Label lbl, Control field, String msg) {
        if (field != null) field.setStyle(BORDER_ERR);
        applyLabelStyle(icon, lbl, "✗", msg, "#EF4444");
        setVM(hbox, true);
    }

    private void showOk(HBox hbox, Label icon, Label lbl, Control field, String msg) {
        if (field != null) field.setStyle(BORDER_OK);
        applyLabelStyle(icon, lbl, "✓", msg, "#10B981");
        setVM(hbox, true);
    }

    private void applyLabelStyle(Label icon, Label lbl, String icone, String msg, String color) {
        String base = "-fx-text-fill:" + color + ";-fx-font-weight:700;";
        if (icon != null) { icon.setText(icone); icon.setStyle(base + "-fx-font-size:13px;"); }
        if (lbl != null)  { lbl.setText(msg);   lbl.setStyle(base + "-fx-font-size:11px;"); }
    }

    private void resetMsg(HBox hbox, Label icon, Label lbl) {
        setVM(hbox, false);
        if (icon != null) { icon.setText(""); icon.setStyle(""); }
        if (lbl != null)  { lbl.setText("");  lbl.setStyle(""); }
    }

    private void resetAllCrudValidation() {
        resetMsg(hboxValIdProfile, iconValIdProfile, lblValIdProfile);
        resetMsg(hboxValNom, iconValNom, lblValNom);
        resetMsg(hboxValPrix, iconValPrix, lblValPrix);
        resetMsg(hboxValQuantite, iconValQuantite, lblValQuantite);
        resetMsg(hboxValCategorie, iconValCategorie, lblValCategorie);
        resetMsg(hboxMsgCrud, iconMsgCrud, lblMsgCrud);

        if (tfIdProfile != null) tfIdProfile.setStyle(BORDER_DEF);
        if (tfNom != null) tfNom.setStyle(BORDER_DEF);
        if (tfPrix != null) tfPrix.setStyle(BORDER_DEF);
        if (tfQuantite != null) tfQuantite.setStyle(BORDER_DEF);
        if (tfCategorie != null) tfCategorie.setStyle(BORDER_DEF);
    }

    private void setVM(Node node, boolean v) {
        if (node != null) { node.setVisible(v); node.setManaged(v); }
    }

    private void hideColumn(TableColumn<?, ?> col) {
        if (col == null) return;
        col.setVisible(false);
        col.setMinWidth(0);
        col.setPrefWidth(0);
        col.setMaxWidth(0);
        col.setResizable(false);
    }

    private String getCurrentRole() {
        var u = AppSession.getCurrentUser();
        if (u == null || u.getUserType() == null) return "";
        return u.getUserType().trim().toLowerCase(Locale.ROOT);
    }

    private static String nz(String s) { return s == null ? "" : s.trim(); }
    private static String safe(String s) { return s == null ? "" : s.trim(); }
}