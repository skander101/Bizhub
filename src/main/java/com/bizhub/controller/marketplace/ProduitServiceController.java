package com.bizhub.controller.marketplace;

import com.bizhub.controller.users_avis.user.SidebarController;
import com.bizhub.model.marketplace.CommandeJoinProduit;
import com.bizhub.model.marketplace.CommandeRepository;
import com.bizhub.model.marketplace.ProduitService;
import com.bizhub.model.marketplace.ProduitServiceRepository;
import com.bizhub.model.services.common.service.AppSession;
import com.bizhub.model.services.common.ui.toastUtil;
import com.bizhub.model.services.marketplace.CommandeNotificationService;
import com.bizhub.model.services.marketplace.CommandePriorityEngine;
import com.bizhub.model.services.marketplace.CommandeService;
import com.bizhub.model.services.marketplace.payment.PaymentResult;
import com.bizhub.model.services.marketplace.payment.PaymentService;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.bizhub.controller.marketplace.InvestorStatsApiServer;
import com.bizhub.model.marketplace.StatsPoint;
import com.google.gson.Gson;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
/**
 * ProduitServiceController
 *
 * ✅ Corrections apportées :
 * - colCmdPaymentStatus et colCmdPayee branchés dans initialize()
 * - colCmdPaidAt (date paiement) branché dans initialize()
 * - Stripe NE S'OUVRE PAS côté investisseur
 * - onConfirmerCommande() : confirme + génère URL Stripe sans ouvrir navigateur
 * - Auto-confirm : même comportement
 */
public class ProduitServiceController {

    private static final Logger LOGGER = Logger.getLogger(ProduitServiceController.class.getName());

    // ── Sidebar ───────────────────────────────────────────────
    @FXML private SidebarController sidebarController;

    // ── Statuts ───────────────────────────────────────────────
    private static final String STATUT_ATTENTE   = "en_attente";
    private static final String STATUT_CONFIRMEE = "confirmee";
    private static final String STATUT_LIVREE    = "livree";
    private static final String STATUT_ANNULEE   = "annule";

    // ── Bordures inline ───────────────────────────────────────
    private static final String BORDER_OK  = "-fx-border-color:#10B981;-fx-border-width:2;-fx-border-radius:12;";
    private static final String BORDER_ERR = "-fx-border-color:#EF4444;-fx-border-width:2;-fx-border-radius:12;";
    private static final String BORDER_DEF = "";

    // ── Webhook + Auto-confirm ────────────────────────────────
    private static final String DISCORD_WEBHOOK_URL        = "https://discord.com/api/webhooks/XXXX/YYY";
    private static final boolean AUTO_CONFIRM_ENABLED      = true;
    private static final int AUTO_CONFIRM_THRESHOLD        = 350;
    private static final int MAX_AUTO_CONFIRM_PER_REFRESH  = 3;

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    // ── Services / Repos ──────────────────────────────────────
    private final ProduitServiceRepository repo         = new ProduitServiceRepository();
    private final CommandeRepository       commandeRepo = new CommandeRepository();
    private final CommandeService          commandeService = new CommandeService(); // ✅ SMS

    private final CommandePriorityEngine       priorityEngine = new CommandePriorityEngine();
    private final CommandeNotificationService  notifService   =
            new CommandeNotificationService(DISCORD_WEBHOOK_URL);
    private final PaymentService               paymentService = new PaymentService();

    // ── Data ──────────────────────────────────────────────────
    private final ObservableList<ProduitService>      produitsData  = FXCollections.observableArrayList();
    private final ObservableList<CommandeJoinProduit> commandesData = FXCollections.observableArrayList();

    private ProduitService selected;

    // ── FXML Filtres ──────────────────────────────────────────
    @FXML private TextField       tfSearchNom;
    @FXML private ComboBox<String> cbFilterCategorie;

    // ── Stats chart (API) ───────────────────────────────────
    @FXML
    private LineChart<String, Number> statsChart;

    @FXML private Label kpiVentesVal;   // ✅ KPI total ventes
    @FXML private Label kpiAchatsVal;   // ✅ KPI total achats

    // ── AI Insights Section ───────────────────────────────────
    @FXML private VBox  boxAiInsights;
    @FXML private VBox  hboxAiContent;
    @FXML private HBox  hboxAiLoading;
    @FXML private Label lblAiSummary;
    @FXML private Label lblAiAnomalies;
    @FXML private Label lblAiRecommendations;
    @FXML private Label lblAiError;
    @FXML private Button btnAnalyzeAi;

    @FXML
    private ComboBox<String> periodeCombo;

    // ── FXML KPI ──────────────────────────────────────────────
    @FXML private javafx.scene.text.Text kpiTotalProduits;
    @FXML private javafx.scene.text.Text kpiDisponibles;
    @FXML private javafx.scene.text.Text kpiCategories;

    // ── FXML Formulaire produit ───────────────────────────────
    @FXML private TextField  tfIdProfile;
    @FXML private TextField  tfNom;
    @FXML private TextField  tfPrix;
    @FXML private TextField  tfQuantite;
    @FXML private TextField  tfCategorie;
    @FXML private CheckBox   cbDisponible;
    @FXML private TextArea   taDescription;

    @FXML private Button btnAjouter;
    @FXML private Button btnModifier;
    @FXML private Button btnSupprimer;
    @FXML private Button btnVider;

    // ── FXML Messages validation inline (produit) ─────────────
    @FXML private HBox  hboxValIdProfile;
    @FXML private Label iconValIdProfile;
    @FXML private Label lblValIdProfile;

    @FXML private HBox  hboxValNom;
    @FXML private Label iconValNom;
    @FXML private Label lblValNom;

    @FXML private HBox  hboxValPrix;
    @FXML private Label iconValPrix;
    @FXML private Label lblValPrix;

    @FXML private HBox  hboxValQuantite;
    @FXML private Label iconValQuantite;
    @FXML private Label lblValQuantite;

    @FXML private HBox  hboxValCategorie;
    @FXML private Label iconValCategorie;
    @FXML private Label lblValCategorie;

    @FXML private HBox  hboxMsgCrud;
    @FXML private Label iconMsgCrud;
    @FXML private Label lblMsgCrud;

    // ── FXML Table produits ───────────────────────────────────
    @FXML private TableView<ProduitService>          tableProduits;
    @FXML private TableColumn<ProduitService, Integer>    colId;
    @FXML private TableColumn<ProduitService, String>     colNom;
    @FXML private TableColumn<ProduitService, String>     colCategorie;
    @FXML private TableColumn<ProduitService, Integer>    colQuantite;
    @FXML private TableColumn<ProduitService, BigDecimal> colPrix;
    @FXML private TableColumn<ProduitService, Boolean>    colDisponible;

    // ── FXML Section commandes investisseur ───────────────────
    @FXML private VBox  boxCommandesInvestor;

    @FXML private HBox  hboxCmdInfo;
    @FXML private Label lblCmdInfo;

    @FXML private TableView<CommandeJoinProduit>              tableCommandes;
    @FXML private TableColumn<CommandeJoinProduit, Integer>   colCmdId;
    @FXML private TableColumn<CommandeJoinProduit, Integer>   colCmdClient;
    @FXML private TableColumn<CommandeJoinProduit, String>    colCmdProduit;
    @FXML private TableColumn<CommandeJoinProduit, Integer>   colCmdQte;
    @FXML private TableColumn<CommandeJoinProduit, String>    colCmdStatut;

    // ✅ Colonnes priorité
    @FXML private TableColumn<CommandeJoinProduit, String>  colCmdPriorite;
    @FXML private TableColumn<CommandeJoinProduit, Integer> colCmdScore;

    // ✅ Colonnes paiement (investisseur)
    @FXML private TableColumn<CommandeJoinProduit, String>        colCmdPaymentStatus;
    @FXML private TableColumn<CommandeJoinProduit, Boolean>       colCmdPayee;
    @FXML private TableColumn<CommandeJoinProduit, LocalDateTime> colCmdPaidAt;

    @FXML private Button btnConfirmerCmd;
    @FXML private Button btnAnnulerCmd;

    @FXML private HBox  hboxMsgCmd;
    @FXML private Label iconMsgCmd;
    @FXML private Label lblMsgCmd;

    // ── Popover priorité ──────────────────────────────────────
    private javafx.stage.Popup priorityPopup;

    // =====================================================
    // INITIALISATION
    // =====================================================
    @FXML
    public void initialize() {
        if (sidebarController != null) sidebarController.setActivePage("marketplace");

        // ── Table produits ────────────────────────────────────
        if (colId        != null) colId.setCellValueFactory(new PropertyValueFactory<>("idProduit"));
        if (colNom       != null) colNom.setCellValueFactory(new PropertyValueFactory<>("nom"));
        if (colCategorie != null) colCategorie.setCellValueFactory(new PropertyValueFactory<>("categorie"));
        if (colQuantite  != null) colQuantite.setCellValueFactory(new PropertyValueFactory<>("quantite"));
        if (colPrix      != null) colPrix.setCellValueFactory(new PropertyValueFactory<>("prix"));
        if (colDisponible!= null) colDisponible.setCellValueFactory(new PropertyValueFactory<>("disponible"));
        if (tableProduits!= null) tableProduits.setItems(produitsData);

        if (tableProduits != null) {
            tableProduits.getSelectionModel().selectedItemProperty()
                    .addListener((obs, o, n) -> { selected = n; if (n != null) fillForm(n); });
        }

        // ── Table commandes investisseur ──────────────────────
        if (tableCommandes != null) {
            if (colCmdId     != null) colCmdId.setCellValueFactory(new PropertyValueFactory<>("idCommande"));
            if (colCmdClient != null) colCmdClient.setCellValueFactory(new PropertyValueFactory<>("idClient"));
            if (colCmdProduit!= null) colCmdProduit.setCellValueFactory(new PropertyValueFactory<>("produitNom"));
            if (colCmdQte    != null) colCmdQte.setCellValueFactory(new PropertyValueFactory<>("quantiteCommande"));
            if (colCmdStatut != null) colCmdStatut.setCellValueFactory(new PropertyValueFactory<>("statut"));
            if (colCmdPriorite!= null) colCmdPriorite.setCellValueFactory(new PropertyValueFactory<>("priorityLabel"));
            if (colCmdScore  != null) colCmdScore.setCellValueFactory(new PropertyValueFactory<>("priorityScore"));

            // ✅ Colonnes paiement — branchées ici
            if (colCmdPaymentStatus != null) {
                colCmdPaymentStatus.setCellValueFactory(new PropertyValueFactory<>("paymentStatus"));
                colCmdPaymentStatus.setCellFactory(col -> new TableCell<>() {
                    private final Label badge = new Label();
                    {
                        badge.setStyle("-fx-padding:4 10; -fx-font-weight:800;"
                                + "-fx-background-radius:999; -fx-border-radius:999;"
                                + "-fx-border-width:1; -fx-font-size:11px;");
                    }
                    @Override protected void updateItem(String item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty) { setGraphic(null); return; }
                        String s = (item == null ? "" : item.trim().toLowerCase(Locale.ROOT));
                        String base = "-fx-padding:4 10; -fx-font-weight:800;"
                                + "-fx-background-radius:999; -fx-border-radius:999;"
                                + "-fx-border-width:1; -fx-font-size:11px;";
                        if (s.isBlank() || "non_initie".equals(s)) {
                            badge.setText("⏳ non initié");
                            badge.setStyle(base
                                    + "-fx-background-color:rgba(245,158,11,0.18);"
                                    + "-fx-text-fill:#FCD34D;"
                                    + "-fx-border-color:rgba(245,158,11,0.55);");
                        } else if ("en_cours".equals(s)) {
                            badge.setText("💳 en cours");
                            badge.setStyle(base
                                    + "-fx-background-color:rgba(59,130,246,0.18);"
                                    + "-fx-text-fill:#93C5FD;"
                                    + "-fx-border-color:rgba(59,130,246,0.55);");
                        } else if ("paid".equals(s) || "paye".equals(s)) {
                            badge.setText("✅ payé");
                            badge.setStyle(base
                                    + "-fx-background-color:rgba(16,185,129,0.18);"
                                    + "-fx-text-fill:#6EE7B7;"
                                    + "-fx-border-color:rgba(16,185,129,0.55);");
                        } else if ("failed".equals(s) || "echec".equals(s)) {
                            badge.setText("⚠ échec");
                            badge.setStyle(base
                                    + "-fx-background-color:rgba(239,68,68,0.18);"
                                    + "-fx-text-fill:#FCA5A5;"
                                    + "-fx-border-color:rgba(239,68,68,0.55);");
                        } else {
                            badge.setText(item == null ? "" : item);
                            badge.setStyle(base);
                        }
                        setGraphic(badge);
                    }
                });
            }

            // ✅ Colonne est_payee
            if (colCmdPayee != null) {
                colCmdPayee.setCellValueFactory(new PropertyValueFactory<>("estPayee"));
                colCmdPayee.setCellFactory(col -> new TableCell<>() {
                    @Override protected void updateItem(Boolean v, boolean empty) {
                        super.updateItem(v, empty);
                        if (empty || v == null) { setText(null); setStyle(""); return; }
                        if (v) {
                            setText("✅ Payée");
                            setStyle("-fx-text-fill:#6EE7B7; -fx-font-weight:800;");
                        } else {
                            setText("⏳ Impayée");
                            setStyle("-fx-text-fill:#FCD34D; -fx-font-weight:800;");
                        }
                    }
                });
            }

            // ✅ Colonne paid_at (date/heure du paiement)
            if (colCmdPaidAt != null) {
                colCmdPaidAt.setCellValueFactory(new PropertyValueFactory<>("paidAt"));
                colCmdPaidAt.setCellFactory(col -> new TableCell<>() {
                    @Override protected void updateItem(LocalDateTime dt, boolean empty) {
                        super.updateItem(dt, empty);
                        if (empty || dt == null) {
                            setText("—");
                            setStyle("-fx-text-fill:#6B7280;");
                        } else {
                            setText(dt.format(DATE_FMT));
                            setStyle("-fx-text-fill:#6EE7B7; -fx-font-weight:700;");
                        }
                    }
                });
            }

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

        // ── Rôle ─────────────────────────────────────────────
        String role          = getCurrentRole();
        boolean isInvestisseur = role.contains("invest");

        setVM(boxCommandesInvestor, isInvestisseur);
        setVM(boxAiInsights, isInvestisseur);
        boolean canCrud = isInvestisseur || role.contains("fournisseur");
        setProductsCrudEnabled(canCrud);

        // Validation temps réel produit
        attachRealTimeValidation();

        // Filtres produits
        if (tfSearchNom       != null) tfSearchNom.textProperty().addListener((obs, o, n) -> applyFilters());
        if (cbFilterCategorie != null) cbFilterCategorie.valueProperty().addListener((obs, o, n) -> applyFilters());

        refreshProduits();
        if (isInvestisseur) refreshCommandes();

        // ── Stats chart init ──────────────────────────────────
        if (periodeCombo != null) {
            periodeCombo.getItems().setAll("30 jours", "90 jours", "1 an");
            if (periodeCombo.getValue() == null) periodeCombo.setValue("30 jours");
            periodeCombo.valueProperty().addListener((obs, o, n) -> loadStatsChart());
        }

        // ✅ FIX 1 : Démarrer le serveur API stats si pas encore démarré
        if (isInvestisseur) {
            try {
                int port = InvestorStatsApiServer.start();
                LOGGER.info("✅ InvestorStatsApiServer démarré sur port " + port);
            } catch (Exception e) {
                LOGGER.warning("⚠ InvestorStatsApiServer échec démarrage : " + e.getMessage());
            }
            loadStatsChart();
        }
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

            // 1) Charger commandes reçues sur SES produits
            commandesData.setAll(commandeRepo.findByOwnerJoinProduit(ownerId));

            // 2) Moteur IA priorité
            for (CommandeJoinProduit c : commandesData) {
                priorityEngine.apply(c);
            }

            // 3) AUTO-CONFIRM (sans ouvrir Stripe)
            if (AUTO_CONFIRM_ENABLED) {
                int done = 0;

                for (CommandeJoinProduit c : commandesData) {
                    if (done >= MAX_AUTO_CONFIRM_PER_REFRESH) break;

                    boolean enAttente = STATUT_ATTENTE.equalsIgnoreCase(safe(c.getStatut()));
                    boolean rec       = c.isAutoConfirmRecommended();
                    int     score     = c.getPriorityScore();

                    if (enAttente && rec && score >= AUTO_CONFIRM_THRESHOLD) {
                        // ✅ FIX : passe par commandeService → déclenche le SMS Twilio
                        int updated = commandeService.changerStatutSiEnAttente(
                                c.getIdCommande(), STATUT_CONFIRMEE);

                        if (updated == 1) {
                            // Génère URL Stripe SANS ouvrir le navigateur
                            PaymentResult pay = paymentService.initiateStripeCheckout(c);

                            if (pay.isSuccess()) {
                                commandeRepo.setPaymentInitiatedIfNull(
                                        c.getIdCommande(),
                                        pay.getRef(),
                                        pay.getPaymentUrl()
                                );
                            } else {
                                LOGGER.warning("Auto-confirm Stripe FAIL #"
                                        + c.getIdCommande() + " : " + safe(pay.getErrorMessage()));
                            }

                            done++;

                            try {
                                toastUtil.ai("🤖 Auto-confirm ✅\nCommande #"
                                        + c.getIdCommande() + "\nScore: " + score);
                            } catch (Exception ignore) {}

                            try {
                                notifService.sendDiscord(
                                        "🤖 **BizHub Auto-confirm**\n"
                                                + "• Commande #" + c.getIdCommande() + "\n"
                                                + "• Produit : " + safe(c.getProduitNom()) + "\n"
                                                + "• Qté : " + c.getQuantiteCommande() + "\n"
                                                + "• Score : " + score + "\n"
                                                + "• Raison : " + safe(c.getAutoReason())
                                                + (pay.isSuccess()
                                                ? "\n• Paiement : lien généré ✅"
                                                : "\n• Paiement : échec Stripe ⚠ ("
                                                + safe(pay.getErrorMessage()) + ")")
                                );
                            } catch (Exception ignore) {}
                        }
                    }
                }

                if (done > 0) {
                    // Recharger après auto-confirmations
                    commandesData.setAll(commandeRepo.findByOwnerJoinProduit(ownerId));
                    for (CommandeJoinProduit c : commandesData) priorityEngine.apply(c);

                    showOk(hboxMsgCmd, iconMsgCmd, lblMsgCmd, null,
                            "🤖 Auto-confirmation : " + done + " commande(s) confirmée(s).");
                    try { toastUtil.success("✅ " + done + " commande(s) auto-confirmée(s)."); }
                    catch (Exception ignore) {}
                }
            }

            // 4) Tri score décroissant
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
            String paidInfo = sel.isEstPayee()
                    ? " | 💳 Payée le " + (sel.getPaidAt() != null
                    ? sel.getPaidAt().format(DATE_FMT) : "?")
                    : " | ⏳ Non payée";

            lblCmdInfo.setText("Commande #" + sel.getIdCommande()
                    + " | " + safe(sel.getProduitNom())
                    + " | Qté: " + sel.getQuantiteCommande()
                    + " | Statut: " + safe(sel.getStatut())
                    + " | Priorité: " + safe(sel.getPriorityLabel())
                    + " (" + sel.getPriorityScore() + ")"
                    + paidInfo
                    + (reason.isEmpty() ? "" : "\n🤖 " + reason));
        }
        setVM(hboxCmdInfo, true);

        boolean editable = STATUT_ATTENTE.equalsIgnoreCase(statut);
        if (btnConfirmerCmd != null) btnConfirmerCmd.setDisable(!editable);
        if (btnAnnulerCmd   != null) btnAnnulerCmd.setDisable(!editable);

        if (editable && btnConfirmerCmd != null && sel.isAutoConfirmRecommended()) {
            btnConfirmerCmd.setText("🤖 Confirmer (Recommandé)");
            btnConfirmerCmd.setStyle(
                    "-fx-background-radius:14; -fx-font-weight:900;"
                            + "-fx-background-color: linear-gradient(#F59E0B,#FBBF24);"
                            + "-fx-text-fill:#111827;"
                            + "-fx-effect: dropshadow(gaussian,rgba(251,191,36,0.35),18,0.2,0,6);");
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
            // 1) Confirmer en DB + ✅ déclenche SMS Twilio via CommandeService
            commandeService.changerStatut(sel.getIdCommande(), STATUT_CONFIRMEE);

            // 2) Générer lien Stripe SANS ouvrir le navigateur
            PaymentResult pay = paymentService.initiateStripeCheckout(sel);

            if (pay.isSuccess()) {
                commandeRepo.setPaymentInitiatedIfNull(
                        sel.getIdCommande(),
                        pay.getRef(),
                        pay.getPaymentUrl()
                );
                showOk(hboxMsgCmd, iconMsgCmd, lblMsgCmd, null,
                        "✓ Commande #" + sel.getIdCommande() + " confirmée.\n"
                                + "💳 Lien de paiement généré — le client peut maintenant payer.");
                try { toastUtil.success("✅ Confirmée #" + sel.getIdCommande()); }
                catch (Exception ignore) {}
            } else {
                // Confirmer quand même, mais signaler l'échec Stripe
                showErr(hboxMsgCmd, iconMsgCmd, lblMsgCmd, null,
                        "✓ Commande #" + sel.getIdCommande() + " confirmée.\n"
                                + "⚠ Stripe : " + safe(pay.getErrorMessage())
                                + "\n(Le client ne pourra pas payer tant que Stripe échoue)");
                try { toastUtil.error("Stripe : " + safe(pay.getErrorMessage())); }
                catch (Exception ignore) {}
            }

            refreshCommandes();

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "onConfirmerCommande", e);
            try { toastUtil.error("Erreur : " + e.getMessage()); } catch (Exception ignore) {}
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
            try { toastUtil.error("⛔ Annulée #" + sel.getIdCommande()); } catch (Exception ignore) {}
            refreshCommandes();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "onAnnulerCommande", e);
            try { toastUtil.error("Erreur : " + e.getMessage()); } catch (Exception ignore) {}
            showErr(hboxMsgCmd, iconMsgCmd, lblMsgCmd, null, "Erreur : " + e.getMessage());
        }
    }

    // =====================================================
    // BADGES STATUT COMMANDE
    // =====================================================
    private void setupCmdStatutBadgesInvestor() {
        if (colCmdStatut == null) return;
        colCmdStatut.setCellFactory(col -> new TableCell<>() {
            private final Label badge = new Label();
            {
                badge.setStyle("-fx-padding:4 10; -fx-font-weight:800;"
                        + "-fx-background-radius:999; -fx-border-radius:999;"
                        + "-fx-border-width:1; -fx-font-size:11px;");
            }
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setGraphic(null); return; }
                String s = item.trim().toLowerCase(Locale.ROOT);
                String base = "-fx-padding:4 10; -fx-font-weight:800;"
                        + "-fx-background-radius:999; -fx-border-radius:999;"
                        + "-fx-border-width:1; -fx-font-size:11px;";
                badge.setStyle(base);
                switch (s) {
                    case STATUT_ATTENTE -> {
                        badge.setText("⏳ En attente");
                        badge.setStyle(base
                                + "-fx-background-color:rgba(245,158,11,0.18);"
                                + "-fx-text-fill:#FCD34D;"
                                + "-fx-border-color:rgba(245,158,11,0.55);");
                    }
                    case STATUT_CONFIRMEE -> {
                        badge.setText("✅ Confirmée");
                        badge.setStyle(base
                                + "-fx-background-color:rgba(16,185,129,0.18);"
                                + "-fx-text-fill:#6EE7B7;"
                                + "-fx-border-color:rgba(16,185,129,0.55);");
                    }
                    case STATUT_LIVREE -> {
                        badge.setText("📦 Livrée");
                        badge.setStyle(base
                                + "-fx-background-color:rgba(59,130,246,0.18);"
                                + "-fx-text-fill:#93C5FD;"
                                + "-fx-border-color:rgba(59,130,246,0.55);");
                    }
                    case STATUT_ANNULEE -> {
                        badge.setText("⛔ Annulée");
                        badge.setStyle(base
                                + "-fx-background-color:rgba(239,68,68,0.18);"
                                + "-fx-text-fill:#FCA5A5;"
                                + "-fx-border-color:rgba(239,68,68,0.55);");
                    }
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
                row.setStyle(hover ? "-fx-background-color:rgba(251,191,36,0.07);" : "");
            });
            return row;
        });
    }

    private void setupPriorityColumnEffects() {
        if (tableCommandes == null || colCmdPriorite == null) return;

        colCmdPriorite.setCellFactory(col -> new TableCell<>() {
            private final Label   badge = new Label();
            private final Tooltip tip   = new Tooltip();
            {
                badge.setStyle("-fx-padding:4 10; -fx-font-weight:800; -fx-background-radius:999;"
                        + "-fx-border-radius:999; -fx-border-width:1; -fx-font-size:11px;");
                badge.setMinWidth(90);
                badge.setPrefWidth(110);
                Tooltip.install(badge, tip);
                badge.setOnMouseClicked(e -> {
                    CommandeJoinProduit c = getRowItem();
                    if (c != null) showPriorityPopover(badge, c);
                });
            }
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setGraphic(null); setText(null); return; }

                CommandeJoinProduit row = getRowItem();
                int score = row != null ? row.getPriorityScore() : 0;

                String base = "-fx-padding:4 10; -fx-font-weight:800; -fx-background-radius:999;"
                        + "-fx-border-radius:999; -fx-border-width:1; -fx-font-size:11px;";
                badge.setStyle(base);

                switch (item.toUpperCase(Locale.ROOT)) {
                    case "HAUTE" -> {
                        badge.setText("🔥 HAUTE");
                        badge.setStyle(base
                                + "-fx-background-color:rgba(239,68,68,0.18);"
                                + "-fx-text-fill:#FCA5A5;"
                                + "-fx-border-color:rgba(239,68,68,0.55);");
                    }
                    case "MOYENNE" -> {
                        badge.setText("⚡ MOYENNE");
                        badge.setStyle(base
                                + "-fx-background-color:rgba(245,158,11,0.18);"
                                + "-fx-text-fill:#FCD34D;"
                                + "-fx-border-color:rgba(245,158,11,0.55);");
                    }
                    default -> {
                        badge.setText("✅ BASSE");
                        badge.setStyle(base
                                + "-fx-background-color:rgba(16,185,129,0.18);"
                                + "-fx-text-fill:#6EE7B7;"
                                + "-fx-border-color:rgba(16,185,129,0.55);");
                    }
                }

                String produit = row != null ? safe(row.getProduitNom()) : "";
                String statut  = row != null ? safe(row.getStatut()) : "";
                int    qte     = row != null ? row.getQuantiteCommande() : 0;
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
                private final ProgressBar bar  = new ProgressBar();
                private final Label       text = new Label();
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
                    CommandeJoinProduit row = getTableRow() == null
                            ? null : (CommandeJoinProduit) getTableRow().getItem();
                    String p = row == null ? "" : safe(row.getPriorityLabel()).toUpperCase(Locale.ROOT);
                    if ("HAUTE".equals(p))        bar.setStyle("-fx-accent:#EF4444;");
                    else if ("MOYENNE".equals(p)) bar.setStyle("-fx-accent:#F59E0B;");
                    else                          bar.setStyle("-fx-accent:#10B981;");
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

        Label title = new Label("Détail Priorité");
        title.setStyle("-fx-font-size:13px; -fx-font-weight:900; -fx-text-fill:#FBBF24;");

        Label l1 = new Label("• Priorité : " + safe(c.getPriorityLabel()).toUpperCase(Locale.ROOT));
        Label l2 = new Label("• Score : " + c.getPriorityScore());
        Label l3 = new Label("• Raison : " + reasonText(c));
        Label l4 = new Label("• Paiement : " + (c.isEstPayee()
                ? "✅ Payée le " + (c.getPaidAt() != null ? c.getPaidAt().format(DATE_FMT) : "?")
                : "⏳ Non payée"));

        l1.setStyle("-fx-text-fill:#E5E7EB; -fx-font-weight:800;");
        l2.setStyle("-fx-text-fill:#E5E7EB; -fx-font-weight:800;");
        l3.setStyle("-fx-text-fill:#93C5FD; -fx-font-weight:800;");
        l4.setStyle("-fx-text-fill:" + (c.isEstPayee() ? "#6EE7B7" : "#FCD34D") + "; -fx-font-weight:800;");

        VBox box = new VBox(6, title, l1, l2, l3, l4);
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
        var p2d = anchor.localToScreen(
                anchor.getBoundsInLocal().getMinX(),
                anchor.getBoundsInLocal().getMaxY());
        priorityPopup.show(anchor, p2d.getX(), p2d.getY() + 6);
    }

    private String reasonText(CommandeJoinProduit c) {
        String st = safe(c.getStatut());
        int    q  = c.getQuantiteCommande();
        if (!STATUT_ATTENTE.equalsIgnoreCase(st)) return "Commande non-actionnable (info).";
        if (q >= 200) return "Grosse quantité → priorité max 🔥";
        if (q >= 80)  return "Quantité moyenne → important ⚡";
        return "Petite quantité → moins urgent ✅";
    }

    private void setupRowPulseForHighPriority() {
        if (tableCommandes == null) return;
        tableCommandes.setRowFactory(tv -> new TableRow<>() {
            private Timeline pulse;
            @Override protected void updateItem(CommandeJoinProduit item, boolean empty) {
                super.updateItem(item, empty);
                if (pulse != null) pulse.stop();
                setStyle("");
                if (empty || item == null) return;
                if ("HAUTE".equalsIgnoreCase(safe(item.getPriorityLabel()))) {
                    setStyle("-fx-background-color:rgba(239,68,68,0.05);");
                    pulse = new Timeline(
                            new KeyFrame(javafx.util.Duration.ZERO,
                                    new KeyValue(opacityProperty(), 1.0)),
                            new KeyFrame(javafx.util.Duration.seconds(0.9),
                                    new KeyValue(opacityProperty(), 0.85)),
                            new KeyFrame(javafx.util.Duration.seconds(1.8),
                                    new KeyValue(opacityProperty(), 1.0))
                    );
                    pulse.setCycleCount(Animation.INDEFINITE);
                    pulse.play();
                }
            }
        });
    }

    // =====================================================
    // PRODUITS CRUD
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
            try { toastUtil.success("✅ Produit ajouté"); } catch (Exception ignore) {}
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
            try { toastUtil.success("✏️ Produit modifié"); } catch (Exception ignore) {}
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
            try { toastUtil.error("🗑️ Produit supprimé"); } catch (Exception ignore) {}
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
        if (tfIdProfile   != null) { tfIdProfile.clear();   tfIdProfile.setStyle(BORDER_DEF); }
        if (tfNom         != null) { tfNom.clear();         tfNom.setStyle(BORDER_DEF); }
        if (tfPrix        != null) { tfPrix.clear();        tfPrix.setStyle(BORDER_DEF); }
        if (tfQuantite    != null) { tfQuantite.clear();    tfQuantite.setStyle(BORDER_DEF); }
        if (tfCategorie   != null) { tfCategorie.clear();   tfCategorie.setStyle(BORDER_DEF); }
        if (taDescription != null) taDescription.clear();
        if (cbDisponible  != null) cbDisponible.setSelected(true);
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
        if (tfIdProfile   != null) tfIdProfile.setText(String.valueOf(p.getIdProfile()));
        if (tfNom         != null) tfNom.setText(nz(p.getNom()));
        if (tfPrix        != null) tfPrix.setText(p.getPrix() == null ? "" : p.getPrix().toPlainString());
        if (tfQuantite    != null) tfQuantite.setText(String.valueOf(p.getQuantite()));
        if (tfCategorie   != null) tfCategorie.setText(nz(p.getCategorie()));
        if (taDescription != null) taDescription.setText(nz(p.getDescription()));
        if (cbDisponible  != null) cbDisponible.setSelected(p.isDisponible());
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
        String q   = tfSearchNom == null ? "" : nz(tfSearchNom.getText()).toLowerCase(Locale.ROOT);
        String cat = cbFilterCategorie == null ? "Toutes" : cbFilterCategorie.getValue();
        try {
            produitsData.setAll(repo.findAll().stream().filter(p -> {
                boolean okNom = q.isEmpty() || nz(p.getNom()).toLowerCase(Locale.ROOT).contains(q);
                boolean okCat = (cat == null || cat.equals("Toutes"))
                        || nz(p.getCategorie()).equalsIgnoreCase(cat);
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
        if (kpiDisponibles   != null) kpiDisponibles.setText(String.valueOf(dispo));
        if (kpiCategories    != null) kpiCategories.setText(String.valueOf(cats.size()));
    }

    private void setProductsCrudEnabled(boolean enabled) {
        setVM(btnAjouter,  enabled);
        setVM(btnModifier, enabled);
        setVM(btnSupprimer,enabled);
        setVM(btnVider,    enabled);
    }

    // =====================================================
    // VALIDATION TEMPS RÉEL (produit)
    // =====================================================
    private void attachRealTimeValidation() {
        if (tfIdProfile != null)
            tfIdProfile.textProperty().addListener((obs, o, n) -> {
                if (n == null || n.isBlank()) {
                    resetMsg(hboxValIdProfile, iconValIdProfile, lblValIdProfile);
                    tfIdProfile.setStyle(BORDER_DEF); return;
                }
                if (!n.trim().matches("\\d+") || Integer.parseInt(n.trim()) <= 0)
                    showErr(hboxValIdProfile, iconValIdProfile, lblValIdProfile, tfIdProfile, "Entier positif requis.");
                else
                    showOk(hboxValIdProfile, iconValIdProfile, lblValIdProfile, tfIdProfile, "Valide.");
            });

        if (tfNom != null)
            tfNom.textProperty().addListener((obs, o, n) -> {
                if (n == null || n.isBlank()) {
                    resetMsg(hboxValNom, iconValNom, lblValNom);
                    tfNom.setStyle(BORDER_DEF); return;
                }
                if (n.trim().isEmpty())
                    showErr(hboxValNom, iconValNom, lblValNom, tfNom, "Le nom est obligatoire.");
                else
                    showOk(hboxValNom, iconValNom, lblValNom, tfNom, "Valide.");
            });

        if (tfPrix != null)
            tfPrix.textProperty().addListener((obs, o, n) -> {
                if (n == null || n.isBlank()) {
                    resetMsg(hboxValPrix, iconValPrix, lblValPrix);
                    tfPrix.setStyle(BORDER_DEF); return;
                }
                try {
                    BigDecimal v = new BigDecimal(n.trim());
                    if (v.compareTo(BigDecimal.ZERO) <= 0)
                        showErr(hboxValPrix, iconValPrix, lblValPrix, tfPrix, "Le prix doit être > 0.");
                    else
                        showOk(hboxValPrix, iconValPrix, lblValPrix, tfPrix, "Valide.");
                } catch (NumberFormatException e) {
                    showErr(hboxValPrix, iconValPrix, lblValPrix, tfPrix, "Format invalide (ex: 49.90).");
                }
            });

        if (tfQuantite != null)
            tfQuantite.textProperty().addListener((obs, o, n) -> {
                if (n == null || n.isBlank()) {
                    resetMsg(hboxValQuantite, iconValQuantite, lblValQuantite);
                    tfQuantite.setStyle(BORDER_DEF); return;
                }
                if (!n.trim().matches("\\d+"))
                    showErr(hboxValQuantite, iconValQuantite, lblValQuantite, tfQuantite, "Entier >= 0 requis.");
                else
                    showOk(hboxValQuantite, iconValQuantite, lblValQuantite, tfQuantite, "Valide.");
            });

        if (tfCategorie != null)
            tfCategorie.textProperty().addListener((obs, o, n) -> {
                if (n == null || n.isBlank()) {
                    resetMsg(hboxValCategorie, iconValCategorie, lblValCategorie);
                    tfCategorie.setStyle(BORDER_DEF); return;
                }
                if (n.trim().isEmpty())
                    showErr(hboxValCategorie, iconValCategorie, lblValCategorie, tfCategorie, "Catégorie obligatoire.");
                else
                    showOk(hboxValCategorie, iconValCategorie, lblValCategorie, tfCategorie, "Valide.");
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
        if (lbl  != null) { lbl.setText(msg);    lbl.setStyle(base  + "-fx-font-size:11px;"); }
    }

    private void resetMsg(HBox hbox, Label icon, Label lbl) {
        setVM(hbox, false);
        if (icon != null) { icon.setText(""); icon.setStyle(""); }
        if (lbl  != null) { lbl.setText("");  lbl.setStyle(""); }
    }

    private void resetAllCrudValidation() {
        resetMsg(hboxValIdProfile, iconValIdProfile, lblValIdProfile);
        resetMsg(hboxValNom,       iconValNom,       lblValNom);
        resetMsg(hboxValPrix,      iconValPrix,      lblValPrix);
        resetMsg(hboxValQuantite,  iconValQuantite,  lblValQuantite);
        resetMsg(hboxValCategorie, iconValCategorie, lblValCategorie);
        resetMsg(hboxMsgCrud,      iconMsgCrud,      lblMsgCrud);

        if (tfIdProfile != null) tfIdProfile.setStyle(BORDER_DEF);
        if (tfNom       != null) tfNom.setStyle(BORDER_DEF);
        if (tfPrix      != null) tfPrix.setStyle(BORDER_DEF);
        if (tfQuantite  != null) tfQuantite.setStyle(BORDER_DEF);
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

    private static String nz(String s)   { return s == null ? "" : s.trim(); }
    private static String safe(String s) { return s == null ? "" : s.trim(); }

    // =====================================================
// STATS CHART (Ventes / Achats) via API
// =====================================================

    @FXML
    private void onRefreshStats() {
        loadStatsChart();
    }

    private void loadStatsChart() {
        try {
            if (statsChart == null) return;

            var me = AppSession.getCurrentUser();
            if (me == null) { statsChart.getData().clear(); return; }

            int investorId = me.getUserId();

            // ── Période ────────────────────────────────────────────────────
            int days = 30;
            String mode = "daily";
            if (periodeCombo != null) {
                String val = periodeCombo.getValue();
                if ("90 jours".equals(val))  { days = 90;  mode = "weekly"; }
                else if ("1 an".equals(val)) { days = 365; mode = "monthly"; }
                else if (val == null || val.isBlank()) periodeCombo.setValue("30 jours");
            }

            LocalDate from = LocalDate.now().minusDays(days);
            LocalDate to   = LocalDate.now();

            // ── Serveur API ────────────────────────────────────────────────
            int port = InvestorStatsApiServer.getActivePort();
            if (port <= 0) {
                try { port = InvestorStatsApiServer.start(); }
                catch (Exception ex) {
                    LOGGER.warning("InvestorStatsApiServer: " + ex.getMessage());
                    statsChart.getData().clear(); return;
                }
            }

            // ── Appel HTTP async pour ne pas bloquer le thread JavaFX ──────
            final int fPort = port;
            final LocalDate fFrom = from, fTo = to;
            final String fMode = mode;
            final int fId = investorId;

            new Thread(() -> {
                try {
                    String url = "http://localhost:" + fPort + "/api/investor/stats"
                            + "?investorId=" + fId + "&from=" + fFrom + "&to=" + fTo;

                    java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient();
                    java.net.http.HttpResponse<String> resp = client.send(
                            java.net.http.HttpRequest.newBuilder().uri(java.net.URI.create(url)).GET().build(),
                            java.net.http.HttpResponse.BodyHandlers.ofString());

                    String body = resp.body() == null ? "" : resp.body().trim();
                    if (!body.startsWith("[")) {
                        LOGGER.warning("Stats API: " + body);
                        Platform.runLater(() -> statsChart.getData().clear());
                        return;
                    }

                    com.google.gson.Gson gson = new com.google.gson.GsonBuilder()
                            .registerTypeAdapter(LocalDate.class,
                                    (com.google.gson.JsonDeserializer<LocalDate>)
                                            (json, type, ctx) -> LocalDate.parse(json.getAsString()))
                            .create();

                    StatsPoint[] data = gson.fromJson(body, StatsPoint[].class);
                    if (data == null || data.length == 0) {
                        Platform.runLater(() -> statsChart.getData().clear());
                        return;
                    }

                    // ── Agrégation selon la période ────────────────────────
                    java.util.LinkedHashMap<String, double[]> buckets = new java.util.LinkedHashMap<>();
                    // [0]=montantConf [1]=montantAnn [2]=nbConf [3]=nbAnn

                    java.util.function.Function<LocalDate, String> keyFn;
                    if ("monthly".equals(fMode)) {
                        keyFn = d -> d.format(java.time.format.DateTimeFormatter.ofPattern("MMM yy", java.util.Locale.FRENCH));
                    } else if ("weekly".equals(fMode)) {
                        keyFn = d -> d.with(java.time.DayOfWeek.MONDAY)
                                .format(java.time.format.DateTimeFormatter.ofPattern("dd/MM"));
                    } else {
                        keyFn = d -> d.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM"));
                    }

                    for (StatsPoint p : data) {
                        if (p.getDate() == null) continue;
                        String key = keyFn.apply(p.getDate());
                        double[] acc = buckets.computeIfAbsent(key, k -> new double[4]);
                        acc[0] += p.getMontantConf() == null ? 0 : p.getMontantConf().doubleValue();
                        acc[1] += p.getMontantAnn()  == null ? 0 : p.getMontantAnn().doubleValue();
                        acc[2] += p.getConfirmees();
                        acc[3] += p.getAnnulees();
                    }

                    XYChart.Series<String, Number> confSeries = new XYChart.Series<>();
                    confSeries.setName("Confirmées");
                    XYChart.Series<String, Number> annSeries = new XYChart.Series<>();
                    annSeries.setName("Annulées");

                    double[] totals = {0, 0, 0, 0};
                    for (var e : buckets.entrySet()) {
                        double[] v = e.getValue();
                        confSeries.getData().add(new XYChart.Data<>(e.getKey(), v[0]));
                        annSeries.getData().add(new XYChart.Data<>(e.getKey(), v[1]));
                        totals[0] += v[0]; totals[1] += v[1];
                        totals[2] += v[2]; totals[3] += v[3];
                    }

                    final double[] ft = totals;
                    Platform.runLater(() -> {
                        statsChart.getData().clear();
                        statsChart.getData().addAll(confSeries, annSeries);

                        // KPIs
                        if (kpiVentesVal != null)
                            kpiVentesVal.setText(String.format("%,.0f TND%n(%d cmd)", ft[0], (int)ft[2]));
                        if (kpiAchatsVal != null)
                            kpiAchatsVal.setText(String.format("%,.0f TND%n(%d cmd)", ft[1], (int)ft[3]));

                        // Style après rendu
                        javafx.animation.PauseTransition pt = new javafx.animation.PauseTransition(
                                javafx.util.Duration.millis(120));
                        pt.setOnFinished(ev -> styleChart(confSeries, annSeries));
                        pt.play();
                    });

                } catch (Exception ex) {
                    LOGGER.warning("loadStatsChart async: " + ex.getMessage());
                    Platform.runLater(() -> statsChart.getData().clear());
                }
            }, "stats-loader").start();

        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "loadStatsChart: " + e.getMessage(), e);
        }
    }

    private void styleChart(XYChart.Series<String, Number> conf,
                            XYChart.Series<String, Number> ann) {
        try {
            // ── Lignes ────────────────────────────────────────────────────
            if (conf.getNode() != null)
                conf.getNode().setStyle("-fx-stroke:#10B981; -fx-stroke-width:2.5px;");
            if (ann.getNode() != null)
                ann.getNode().setStyle("-fx-stroke:#EF4444; -fx-stroke-width:2px;" +
                        "-fx-stroke-dash-array:6 4;");

            // ── Points confirmées ──────────────────────────────────────────
            for (XYChart.Data<String, Number> d : conf.getData()) {
                if (d.getNode() == null) continue;
                d.getNode().setStyle(
                        "-fx-background-color:#10B981,#0f1824;" +
                                "-fx-background-radius:5; -fx-padding:4;");
                String tip = d.getXValue() + "\n✅ Confirmées : "
                        + String.format("%,.2f TND", d.getYValue().doubleValue());
                javafx.scene.control.Tooltip t = new javafx.scene.control.Tooltip(tip);
                t.setStyle("-fx-background-color:#1a2235; -fx-text-fill:white;" +
                        "-fx-font-size:11px; -fx-background-radius:8; -fx-padding:6 10;");
                javafx.scene.control.Tooltip.install(d.getNode(), t);
            }

            // ── Points annulées ────────────────────────────────────────────
            for (XYChart.Data<String, Number> d : ann.getData()) {
                if (d.getNode() == null) continue;
                d.getNode().setStyle(
                        "-fx-background-color:#EF4444,#0f1824;" +
                                "-fx-background-radius:5; -fx-padding:4;");
                String tip = d.getXValue() + "\n❌ Annulées : "
                        + String.format("%,.2f TND", d.getYValue().doubleValue());
                javafx.scene.control.Tooltip t = new javafx.scene.control.Tooltip(tip);
                t.setStyle("-fx-background-color:#1a2235; -fx-text-fill:white;" +
                        "-fx-font-size:11px; -fx-background-radius:8; -fx-padding:6 10;");
                javafx.scene.control.Tooltip.install(d.getNode(), t);
            }

            // ── Fond + grilles ────────────────────────────────────────────
            javafx.scene.Node plot = statsChart.lookup(".chart-plot-background");
            if (plot != null) plot.setStyle("-fx-background-color:rgba(255,255,255,0.02);" +
                    "-fx-background-radius:8;");

            statsChart.lookupAll(".chart-horizontal-grid-lines").forEach(n ->
                    n.setStyle("-fx-stroke:rgba(255,255,255,0.05); -fx-stroke-dash-array:3 5;"));
            statsChart.lookupAll(".chart-vertical-grid-lines").forEach(n ->
                    n.setStyle("-fx-stroke:transparent;"));

            // ── Légende ────────────────────────────────────────────────────
            statsChart.lookupAll(".chart-legend").forEach(n ->
                    n.setStyle("-fx-background-color:rgba(26,34,53,0.85);" +
                            "-fx-border-color:rgba(255,255,255,0.08);" +
                            "-fx-border-radius:8; -fx-background-radius:8;" +
                            "-fx-padding:4 10;"));

        } catch (Exception ignore) {}
    }

    @FXML
    private void onAnalyzeAi() {
        if (btnAnalyzeAi != null) btnAnalyzeAi.setDisable(true);
        if (hboxAiLoading != null) { hboxAiLoading.setVisible(true); hboxAiLoading.setManaged(true); }
        if (hboxAiContent != null) { hboxAiContent.setVisible(false); hboxAiContent.setManaged(false); }
        if (lblAiError != null)    { lblAiError.setVisible(false);    lblAiError.setManaged(false); }

        var me = AppSession.getCurrentUser();
        if (me == null) return;
        int investorId = me.getUserId();

        int days = 30;
        if (periodeCombo != null) {
            String val = periodeCombo.getValue();
            if ("90 jours".equals(val)) days = 90;
            else if ("1 an".equals(val)) days = 365;
        }
        LocalDate from = LocalDate.now().minusDays(days);
        LocalDate to   = LocalDate.now();

        final int fDays = days;
        new Thread(() -> {
            try {
                // ✅ Démarrer le serveur Insights si pas encore démarré
                int port = com.bizhub.controller.marketplace.InvestorInsightsApiServer.getActivePort();
                if (port <= 0)
                    port = com.bizhub.controller.marketplace.InvestorInsightsApiServer.start();

                String url = "http://localhost:" + port + "/api/investor/insights"
                        + "?investorId=" + investorId
                        + "&from=" + from + "&to=" + to;

                java.net.http.HttpResponse<String> resp =
                        java.net.http.HttpClient.newHttpClient().send(
                                java.net.http.HttpRequest.newBuilder()
                                        .uri(java.net.URI.create(url))
                                        .timeout(java.time.Duration.ofSeconds(60))
                                        .GET().build(),
                                java.net.http.HttpResponse.BodyHandlers.ofString());

                String body = resp.body() == null ? "" : resp.body().trim();

                com.google.gson.JsonObject obj =
                        new com.google.gson.Gson().fromJson(body, com.google.gson.JsonObject.class);

                if (obj.has("error")) throw new Exception(obj.get("error").getAsString());

                String summary = obj.has("summary") ? obj.get("summary").getAsString() : "—";

                // Listes → texte avec bullets
                String anomalies = formatList(obj, "anomalies", "•");
                String recomms   = formatList(obj, "recommendations", "→");

                Platform.runLater(() -> {
                    if (lblAiSummary != null) lblAiSummary.setText(summary);
                    if (lblAiAnomalies != null) lblAiAnomalies.setText(anomalies);
                    if (lblAiRecommendations != null) lblAiRecommendations.setText(recomms);

                    if (hboxAiLoading != null) { hboxAiLoading.setVisible(false); hboxAiLoading.setManaged(false); }
                    if (hboxAiContent != null) { hboxAiContent.setVisible(true); hboxAiContent.setManaged(true); }
                    if (btnAnalyzeAi != null) btnAnalyzeAi.setDisable(false);
                });

            } catch (Exception e) {
                LOGGER.warning("onAnalyzeAi erreur : " + e.getMessage());
                Platform.runLater(() -> {
                    if (hboxAiLoading != null) { hboxAiLoading.setVisible(false); hboxAiLoading.setManaged(false); }
                    if (lblAiError != null) {
                        lblAiError.setText("❌ Erreur IA : " + e.getMessage());
                        lblAiError.setVisible(true); lblAiError.setManaged(true);
                    }
                    if (btnAnalyzeAi != null) btnAnalyzeAi.setDisable(false);
                });
            }
        }, "ai-insights").start();
    }

    private String formatList(com.google.gson.JsonObject obj, String key, String bullet) {
        if (!obj.has(key) || !obj.get(key).isJsonArray()) return "—";
        StringBuilder sb = new StringBuilder();
        obj.getAsJsonArray(key).forEach(el -> {
            if (!sb.isEmpty()) sb.append("\n");
            sb.append(bullet).append(" ").append(el.getAsString());
        });
        return sb.isEmpty() ? "Aucun élément." : sb.toString();
    }
}