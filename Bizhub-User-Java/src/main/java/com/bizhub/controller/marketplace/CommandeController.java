package com.bizhub.controller.marketplace;

import com.bizhub.model.marketplace.Commande;
import com.bizhub.model.marketplace.CommandeJoinProduit;
import com.bizhub.model.marketplace.CommandeRepository;
import com.bizhub.model.marketplace.ProduitService;
import com.bizhub.model.services.common.service.AppSession;
import com.bizhub.model.services.common.ui.toastUtil;
import com.bizhub.model.services.marketplace.CommandeService;
import com.bizhub.model.services.marketplace.ProduitServiceService;
import com.bizhub.model.services.marketplace.payment.PaymentResult;
import com.bizhub.model.services.marketplace.payment.PaymentService;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.util.Duration;
import javafx.util.StringConverter;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;

import java.sql.SQLException;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;
import com.bizhub.model.services.marketplace.TwilioSmsService;
public class CommandeController {

    private static final Logger LOGGER = Logger.getLogger(CommandeController.class.getName());

    private static final int QTE_MAX = 10_000;

    private static final String STATUT_ATTENTE   = "en_attente";
    private static final String STATUT_CONFIRMEE = "confirmee";
    private static final String STATUT_LIVREE    = "livree";
    private static final String STATUT_ANNULEE   = "annule"; // ✅ cohérent

    private static final String BORDER_OK  = "-fx-border-color:#10B981;-fx-border-width:2;-fx-border-radius:12;";
    private static final String BORDER_ERR = "-fx-border-color:#EF4444;-fx-border-width:2;-fx-border-radius:12;";
    private static final String BORDER_DEF = "";

    // ── FXML Layout ──────────────────────────────────────
    @FXML private VBox boxAdd;
    @FXML private VBox boxManage;
    @FXML private Text titleForm;

    // ── Startup ─────────────────────────────────────────
    @FXML private ComboBox<ProduitService> cbProduit;
    @FXML private TextField tfQuantite;
    @FXML private Button btnAjouter;
    @FXML private Button btnSupprimer;

    @FXML private HBox  hboxValProduit;
    @FXML private Label iconValProduit;
    @FXML private Label lblValProduit;

    @FXML private HBox  hboxValQte;
    @FXML private Label iconValQte;
    @FXML private Label lblValQte;

    @FXML private HBox  hboxMsgAdd;
    @FXML private Label iconMsgAdd;
    @FXML private Label lblMsgAdd;

    @FXML private HBox  hboxMsgDel;
    @FXML private Label iconMsgDel;
    @FXML private Label lblMsgDel;

    // ── Paiement ─────────────────────────────────────────
    @FXML private VBox boxPaiement;
    @FXML private Label lblInfoPaiement;
    @FXML private Button btnPayer;
    @FXML private ProgressBar progressPaiement;
    @FXML private HBox  hboxMsgPaiement;
    @FXML private Label iconMsgPaiement;
    @FXML private Label lblMsgPaiement;

    // ── Investisseur ─────────────────────────────────────
    @FXML private Button btnConfirmer;
    @FXML private Button btnAnnuler;

    @FXML private HBox  hboxSelectedInfo;
    @FXML private Label lblSelectedInfo;

    @FXML private HBox  hboxMsgStatut;
    @FXML private Label iconMsgStatut;
    @FXML private Label lblMsgStatut;

    // ── Filtres ──────────────────────────────────────────
    @FXML private Label lblFilterClient;
    @FXML private TextField tfFilterClient;
    @FXML private ComboBox<String> cbFilterStatut;

    // ── KPIs ─────────────────────────────────────────────
    @FXML private Text kpiTotal;
    @FXML private Text kpiAttente;
    @FXML private Text kpiConfirmee;
    @FXML private Text kpiLivree;
    @FXML private Text kpiAnnulee;

    // ── Table ────────────────────────────────────────────
    @FXML private TableView<CommandeJoinProduit> tableCommandes;
    @FXML private TableColumn<CommandeJoinProduit, Integer> colIdCommande;
    @FXML private TableColumn<CommandeJoinProduit, Integer> colIdClient;
    @FXML private TableColumn<CommandeJoinProduit, String>  colProduitNom;
    @FXML private TableColumn<CommandeJoinProduit, Integer> colQte;
    @FXML private TableColumn<CommandeJoinProduit, String>  colStatut;

    // ✅ colonnes paiement
    @FXML private TableColumn<CommandeJoinProduit, String>  colPaymentStatus;
    @FXML private TableColumn<CommandeJoinProduit, Boolean> colPayee;

    // ── Services ─────────────────────────────────────────
    private final CommandeService commandeService = new CommandeService();
    private final ProduitServiceService produitService = new ProduitServiceService();
    private final PaymentService paymentService = new PaymentService();
    private final CommandeRepository commandeRepo = new CommandeRepository();

    private final ObservableList<CommandeJoinProduit> masterData = FXCollections.observableArrayList();
    private FilteredList<CommandeJoinProduit> filteredData;
    private volatile boolean paying = false;

    // =====================================================
    // INITIALISATION
    // =====================================================
    @FXML
    public void initialize() {

        // Table bindings
        if (colIdCommande != null) colIdCommande.setCellValueFactory(new PropertyValueFactory<>("idCommande"));
        if (colIdClient   != null) colIdClient.setCellValueFactory(new PropertyValueFactory<>("idClient"));
        if (colProduitNom != null) colProduitNom.setCellValueFactory(new PropertyValueFactory<>("produitNom"));
        if (colQte        != null) colQte.setCellValueFactory(new PropertyValueFactory<>("quantiteCommande"));
        if (colStatut     != null) colStatut.setCellValueFactory(new PropertyValueFactory<>("statut"));

        if (colPaymentStatus != null)
            colPaymentStatus.setCellValueFactory(new PropertyValueFactory<>("paymentStatus"));

        if (colPayee != null)
            colPayee.setCellValueFactory(new PropertyValueFactory<>("estPayee"));
        setupStatutBadges();
        setupRowHoverGlow();
        setupPayeeBadgeColumn();
        setupPaymentStatusBadge();

        hideColumn(colIdCommande);
        hideColumn(colIdClient);

        // Filters
        if (cbFilterStatut != null) {
            cbFilterStatut.setItems(FXCollections.observableArrayList(
                    "Tous", STATUT_ATTENTE, STATUT_CONFIRMEE, STATUT_LIVREE, STATUT_ANNULEE
            ));
            if (cbFilterStatut.getValue() == null) cbFilterStatut.setValue("Tous");
            cbFilterStatut.valueProperty().addListener((obs, o, n) -> applyFilters());
        }
        if (tfFilterClient != null) tfFilterClient.textProperty().addListener((obs, o, n) -> applyFilters());

        // Data pipeline
        filteredData = new FilteredList<>(masterData, x -> true);
        SortedList<CommandeJoinProduit> sorted = new SortedList<>(filteredData);
        if (tableCommandes != null) {
            sorted.comparatorProperty().bind(tableCommandes.comparatorProperty());
            tableCommandes.setItems(sorted);

            tableCommandes.getSelectionModel().selectedItemProperty()
                    .addListener((obs, o, sel) -> onSelectionChanged(sel));
        }

        // realtime validation
        if (tfQuantite != null)
            tfQuantite.textProperty().addListener((obs, o, n) -> validerQuantiteRealTime(n));

        // default
        if (btnConfirmer != null) btnConfirmer.setDisable(true);
        if (btnAnnuler   != null) btnAnnuler.setDisable(true);

        setVM(boxPaiement, false);
        setVM(progressPaiement, false);
        setVM(hboxMsgPaiement, false);

        loadProduits();
        applyRoleUI();
        refreshJoin();
    }

    // =====================================================
    // RÔLES
    // =====================================================
    private String getCurrentRole() {
        var u = AppSession.getCurrentUser();
        if (u == null || u.getUserType() == null) return "";
        return u.getUserType().trim().toLowerCase(Locale.ROOT);
    }

    private boolean isStartup()      { return getCurrentRole().contains("startup"); }
    private boolean isInvestisseur() { return getCurrentRole().contains("invest"); }

    private void applyRoleUI() {
        boolean startup = isStartup();
        boolean invest  = isInvestisseur();

        setVM(boxAdd, startup);
        setVM(boxManage, invest);

        setVM(lblFilterClient, invest);
        setVM(tfFilterClient, invest);

        if (titleForm != null) titleForm.setText(startup ? "Passer une commande" : "Gestion des commandes");
    }

    // =====================================================
    // SÉLECTION
    // =====================================================
    private void onSelectionChanged(CommandeJoinProduit sel) {

        resetMsg(hboxMsgStatut, iconMsgStatut, lblMsgStatut);

        // reset paiement
        setVM(boxPaiement, false);
        setVM(progressPaiement, false);
        resetMsg(hboxMsgPaiement, iconMsgPaiement, lblMsgPaiement);

        if (btnPayer != null) {
            btnPayer.setDisable(false);
            btnPayer.setText("💳  Payer cette commande");
        }

        if (btnConfirmer != null) btnConfirmer.setDisable(true);
        if (btnAnnuler   != null) btnAnnuler.setDisable(true);

        if (sel == null) {
            setVM(hboxSelectedInfo, false);
            return;
        }

        // info selection invest
        if (lblSelectedInfo != null) {
            lblSelectedInfo.setText("Commande #" + sel.getIdCommande()
                    + " | " + safe(sel.getProduitNom())
                    + " | Qté: " + sel.getQuantiteCommande()
                    + " | Statut: " + safe(sel.getStatut()));
        }
        setVM(hboxSelectedInfo, isInvestisseur());

        // ===== STARTUP : afficher boxPaiement si confirmée =====
        if (isStartup()) {
            boolean conf = STATUT_CONFIRMEE.equalsIgnoreCase(safe(sel.getStatut()));
            if (conf) {
                setVM(boxPaiement, true);
                animerApparition(boxPaiement);

                if (lblInfoPaiement != null) {
                    lblInfoPaiement.setText("Commande #" + sel.getIdCommande()
                            + " confirmée ✅ — " + safe(sel.getProduitNom())
                            + " (x" + sel.getQuantiteCommande() + ")");
                }

                if (sel.isEstPayee()) {
                    if (btnPayer != null) {
                        btnPayer.setDisable(true);
                        btnPayer.setText("✅ Déjà payée");
                    }
                    showOk(hboxMsgPaiement, iconMsgPaiement, lblMsgPaiement, null,
                            "Commande déjà payée ✅");
                } else {
                    animerPulse(btnPayer);
                }
                return;
            }
        }

        // ===== INVEST : activer boutons si en attente =====
        if (isInvestisseur()) {
            boolean editable = STATUT_ATTENTE.equalsIgnoreCase(safe(sel.getStatut()));
            if (btnConfirmer != null) btnConfirmer.setDisable(!editable);
            if (btnAnnuler   != null) btnAnnuler.setDisable(!editable);
        }
    }

    // =====================================================
    // PAIEMENT (startup)
    // =====================================================
    @FXML
    public void onPayerCommande() {

        resetMsg(hboxMsgPaiement, iconMsgPaiement, lblMsgPaiement);

        if (paying) return;
        paying = true;

        CommandeJoinProduit sel = tableCommandes != null
                ? tableCommandes.getSelectionModel().getSelectedItem()
                : null;

        if (sel == null)          { safeToastError("Sélectionnez une commande."); paying = false; return; }
        if (!isStartup())         { safeToastWarning("Action réservée au Startup."); paying = false; return; }
        if (!STATUT_CONFIRMEE.equalsIgnoreCase(safe(sel.getStatut()))) {
            safeToastWarning("Paiement disponible uniquement après confirmation ✅");
            paying = false;
            return;
        }
        if (sel.isEstPayee())     { safeToastWarning("Commande déjà payée ✅"); paying = false; return; }

        // UI loading
        if (btnPayer != null) {
            btnPayer.setDisable(true);
            btnPayer.setText("⏳  Connexion à Stripe...");
        }
        setVM(progressPaiement, true);
        if (progressPaiement != null) progressPaiement.setProgress(-1);

        CommandeJoinProduit cmdFinal = sel;

        Task<PaymentResult> task = new Task<>() {
            @Override
            protected PaymentResult call() throws Exception {

                PaymentResult pay = paymentService.initiateStripeCheckout(cmdFinal);
                if (pay == null || !pay.isSuccess()) return pay;

                LOGGER.info("PAY INIT | orderId=" + cmdFinal.getIdCommande()
                        + " | ref=" + pay.getRef()
                        + " | url=" + pay.getPaymentUrl());

                try {
                    commandeRepo.updatePaymentRef(
                            cmdFinal.getIdCommande(),
                            pay.getRef(),
                            pay.getPaymentUrl()
                    );
                } catch (Exception ignore) { }

                paymentService.openInBrowser(pay.getPaymentUrl());
                return pay;
            }
        };

        task.setOnSucceeded(e -> Platform.runLater(() -> {
            paying = false;
            stopProgress();

            PaymentResult res = task.getValue();
            if (res == null || !res.isSuccess()) {
                if (btnPayer != null) {
                    btnPayer.setDisable(false);
                    btnPayer.setText("💳  Payer cette commande");
                }
                String msg = (res == null) ? "Erreur Stripe" : safe(res.getErrorMessage());
                safeToastError("Stripe : " + msg);
                showErr(hboxMsgPaiement, iconMsgPaiement, lblMsgPaiement, null,
                        "Erreur Stripe : " + msg);
                return;
            }

            if (btnPayer != null) btnPayer.setText("✅  Paiement lancé !");

            showOk(hboxMsgPaiement, iconMsgPaiement, lblMsgPaiement, null,
                    "✓ Page Stripe ouverte dans votre navigateur.\n"
                            + "Carte test : 4242 4242 4242 4242 | Date: 12/26 | CVC: 123");

            refreshJoin();
        }));

        task.setOnFailed(e -> Platform.runLater(() -> {
            paying = false;
            stopProgress();
            if (btnPayer != null) {
                btnPayer.setDisable(false);
                btnPayer.setText("💳  Payer cette commande");
            }
            String err = task.getException() != null
                    ? task.getException().getMessage() : "Erreur inconnue";
            safeToastError("Erreur : " + err);
            showErr(hboxMsgPaiement, iconMsgPaiement, lblMsgPaiement, null, "Erreur : " + err);
        }));

        new Thread(task, "stripe-thread").start();
    }
    private void stopProgress() {
        if (progressPaiement != null) progressPaiement.setProgress(1);
        setVM(progressPaiement, false);
    }

    // =====================================================
    // REFRESH
    // =====================================================
    @FXML
    public void refreshJoin() {
        try {
            var me = AppSession.getCurrentUser();
            if (me == null) {
                masterData.clear();
                updateKpis();
                return;
            }

            if (isStartup()) {
                masterData.setAll(commandeService.getByClientJoinProduit(me.getUserId()));
            } else if (isInvestisseur()) {
                masterData.setAll(commandeService.getAllJoinProduit());
            } else {
                masterData.clear();
            }

            applyFilters();

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "refreshJoin", e);
            if (isStartup()) showErr(hboxMsgAdd, iconMsgAdd, lblMsgAdd, null, "Erreur : " + e.getMessage());
            else showErr(hboxMsgStatut, iconMsgStatut, lblMsgStatut, null, "Erreur : " + e.getMessage());
        }
    }

    // =====================================================
    // STARTUP : AJOUTER
    // =====================================================
    @FXML
    private void onAjouter() {
        resetAllValidation();

        ProduitService produit = cbProduit != null ? cbProduit.getValue() : null;
        if (produit == null) {
            showErr(hboxValProduit, iconValProduit, lblValProduit, cbProduit, "Sélectionnez un produit.");
            return;
        } else {
            showOk(hboxValProduit, iconValProduit, lblValProduit, cbProduit, "Produit sélectionné.");
        }

        int qte;
        try {
            qte = parseQuantite(tfQuantite != null ? tfQuantite.getText() : "");
            showOk(hboxValQte, iconValQte, lblValQte, tfQuantite, "Quantité valide.");
        } catch (IllegalArgumentException e) {
            showErr(hboxValQte, iconValQte, lblValQte, tfQuantite, e.getMessage());
            return;
        }

        try {
            var me = AppSession.getCurrentUser();
            if (me == null) throw new IllegalStateException("Session expirée.");
            if (!isStartup()) throw new IllegalStateException("Action réservée aux Startup.");

            Commande c = new Commande();
            c.setIdClient(me.getUserId());
            c.setIdProduit(produit.getIdProduit());
            c.setQuantite(qte);
            c.setStatut(STATUT_ATTENTE);

            commandeService.ajouter(c);

            clearStartupForm();
            refreshJoin();

            showOk(hboxMsgAdd, iconMsgAdd, lblMsgAdd, null, "✓ Commande ajoutée avec succès !");

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "onAjouter", e);
            showErr(hboxMsgAdd, iconMsgAdd, lblMsgAdd, null, e.getMessage());
        }
    }

    // =====================================================
    // STARTUP : SUPPRIMER
    // =====================================================
    @FXML
    private void onSupprimer() {
        resetMsg(hboxMsgDel, iconMsgDel, lblMsgDel);

        if (tableCommandes == null) return;
        CommandeJoinProduit sel = tableCommandes.getSelectionModel().getSelectedItem();

        if (sel == null) {
            showErr(hboxMsgDel, iconMsgDel, lblMsgDel, null, "Sélectionnez une commande.");
            return;
        }

        var me = AppSession.getCurrentUser();
        if (me == null) {
            showErr(hboxMsgDel, iconMsgDel, lblMsgDel, null, "Session expirée.");
            return;
        }

        if (sel.getIdClient() != me.getUserId()) {
            showErr(hboxMsgDel, iconMsgDel, lblMsgDel, null, "Vous ne pouvez supprimer que vos propres commandes.");
            return;
        }

        if (!STATUT_ATTENTE.equalsIgnoreCase(safe(sel.getStatut()))) {
            showErr(hboxMsgDel, iconMsgDel, lblMsgDel, null,
                    "Statut « " + sel.getStatut() + " » — suppression impossible.\n"
                            + "Seules les commandes en attente sont supprimables.");
            return;
        }

        try {
            commandeService.supprimer(sel.getIdCommande());
            refreshJoin();
            showOk(hboxMsgDel, iconMsgDel, lblMsgDel, null,
                    "✓ Commande #" + sel.getIdCommande() + " supprimée.");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "onSupprimer", e);
            showErr(hboxMsgDel, iconMsgDel, lblMsgDel, null, e.getMessage());
        }
    }

    // =====================================================
    // INVESTISSEUR : CONFIRMER / ANNULER
    // =====================================================
    @FXML
    private void onConfirmer() {
        resetMsg(hboxMsgStatut, iconMsgStatut, lblMsgStatut);

        if (!isInvestisseur()) {
            showErr(hboxMsgStatut, iconMsgStatut, lblMsgStatut, null,
                    "Action réservée à l'investisseur.");
            return;
        }
        if (tableCommandes == null) return;

        CommandeJoinProduit sel = tableCommandes.getSelectionModel().getSelectedItem();
        if (sel == null) {
            showErr(hboxMsgStatut, iconMsgStatut, lblMsgStatut, null,
                    "Sélectionnez une commande.");
            return;
        }

        if (!STATUT_ATTENTE.equalsIgnoreCase(safe(sel.getStatut()))) {
            showErr(hboxMsgStatut, iconMsgStatut, lblMsgStatut, null,
                    "Impossible — statut actuel : « " + sel.getStatut() + " ».\n"
                            + "Seules les commandes en attente peuvent être confirmées.");
            return;
        }

        try {
            // ✅ Change le statut + (dans le Service) envoie le SMS au startup si Twilio est configuré
            commandeService.changerStatut(sel.getIdCommande(), STATUT_CONFIRMEE);

            refreshJoin();

            showOk(hboxMsgStatut, iconMsgStatut, lblMsgStatut, null,
                    "✓ Commande #" + sel.getIdCommande() + " confirmée.\n"
                            + "Le startup peut maintenant la payer.");

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "onConfirmer", e);
            showErr(hboxMsgStatut, iconMsgStatut, lblMsgStatut, null,
                    (e.getMessage() == null ? "Erreur confirmation" : e.getMessage()));
        }
    }
    @FXML
    private void onAnnuler() {
        resetMsg(hboxMsgStatut, iconMsgStatut, lblMsgStatut);

        if (!isInvestisseur()) {
            showErr(hboxMsgStatut, iconMsgStatut, lblMsgStatut, null, "Action réservée à l'investisseur.");
            return;
        }
        if (tableCommandes == null) return;

        CommandeJoinProduit sel = tableCommandes.getSelectionModel().getSelectedItem();
        if (sel == null) {
            showErr(hboxMsgStatut, iconMsgStatut, lblMsgStatut, null, "Sélectionnez une commande.");
            return;
        }
        if (!STATUT_ATTENTE.equalsIgnoreCase(safe(sel.getStatut()))) {
            showErr(hboxMsgStatut, iconMsgStatut, lblMsgStatut, null,
                    "Impossible — statut actuel : « " + sel.getStatut() + " ».\n"
                            + "Seules les commandes en attente peuvent être annulées.");
            return;
        }

        try {
            commandeService.changerStatut(sel.getIdCommande(), STATUT_ANNULEE);
            refreshJoin();
            showOk(hboxMsgStatut, iconMsgStatut, lblMsgStatut, null,
                    "✓ Commande #" + sel.getIdCommande() + " annulée.");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "onAnnuler", e);
            showErr(hboxMsgStatut, iconMsgStatut, lblMsgStatut, null, e.getMessage());
        }
    }

    // =====================================================
    // PRODUITS
    // =====================================================
    private void loadProduits() {
        try {
            if (cbProduit == null) return;

            cbProduit.setItems(FXCollections.observableArrayList(produitService.getAllDisponibles()));
            cbProduit.setConverter(new StringConverter<>() {
                @Override public String toString(ProduitService p) {
                    return p == null ? "" : safe(p.getNom()) + " — " + p.getPrix() + " TND";
                }
                @Override public ProduitService fromString(String s) { return null; }
            });

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "loadProduits", e);
            showErr(hboxMsgAdd, iconMsgAdd, lblMsgAdd, null,
                    "Impossible de charger les produits : " + e.getMessage());
        }
    }

    // =====================================================
    // FILTRES + KPIs
    // =====================================================
    private void applyFilters() {
        if (filteredData == null) return;

        String statut = cbFilterStatut == null ? "Tous" : safe(cbFilterStatut.getValue());
        String clientTxt = tfFilterClient == null ? "" : safe(tfFilterClient.getText()).trim();

        if (tfFilterClient != null) tfFilterClient.setStyle(BORDER_DEF);

        filteredData.setPredicate(c -> {
            boolean okStatut = "Tous".equalsIgnoreCase(statut)
                    || (c.getStatut() != null && c.getStatut().equalsIgnoreCase(statut));

            boolean okClient = true;
            if (isInvestisseur() && !clientTxt.isEmpty()) {
                if (!clientTxt.matches("\\d+")) {
                    if (tfFilterClient != null) tfFilterClient.setStyle(BORDER_ERR);
                    okClient = false;
                } else okClient = (c.getIdClient() == Integer.parseInt(clientTxt));
            }
            return okStatut && okClient;
        });

        updateKpis();
    }

    private void updateKpis() {
        int total = 0, att = 0, conf = 0, liv = 0, ann = 0;

        for (CommandeJoinProduit c : masterData) {
            total++;
            switch (safe(c.getStatut()).toLowerCase(Locale.ROOT)) {
                case STATUT_ATTENTE -> att++;
                case STATUT_CONFIRMEE -> conf++;
                case STATUT_LIVREE -> liv++;
                case STATUT_ANNULEE -> ann++;
            }
        }

        setText(kpiTotal, total);
        setText(kpiAttente, att);
        setText(kpiConfirmee, conf);
        setText(kpiLivree, liv);
        setText(kpiAnnulee, ann);
    }

    private void validerQuantiteRealTime(String val) {
        if (val == null || val.isBlank()) {
            resetMsg(hboxValQte, iconValQte, lblValQte);
            if (tfQuantite != null) tfQuantite.setStyle(BORDER_DEF);
            return;
        }
        try {
            parseQuantite(val);
            showOk(hboxValQte, iconValQte, lblValQte, tfQuantite, "Quantité valide ✓");
        } catch (IllegalArgumentException e) {
            showErr(hboxValQte, iconValQte, lblValQte, tfQuantite, e.getMessage());
        }
    }

    // =====================================================
    // UI: BADGES
    // =====================================================
    private void setupPayeeBadgeColumn() {
        if (colPayee == null) return;
        colPayee.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Boolean v, boolean empty) {
                super.updateItem(v, empty);
                if (empty || v == null) { setText(null); return; }
                setText(v ? "✅" : "⏳");
            }
        });
    }

    private void setupPaymentStatusBadge() {
        if (colPaymentStatus == null) return;

        colPaymentStatus.setCellFactory(col -> new TableCell<>() {
            private final Label badge = new Label();
            {
                badge.setStyle("-fx-padding:4 10; -fx-font-weight:800;"
                        + "-fx-background-radius:999; -fx-border-radius:999; -fx-border-width:1;"
                        + "-fx-font-size:11px;");
            }

            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) { setGraphic(null); return; }

                String s = safe(item).toLowerCase(Locale.ROOT);
                String base = "-fx-padding:4 10; -fx-font-weight:800;"
                        + "-fx-background-radius:999; -fx-border-radius:999; -fx-border-width:1;"
                        + "-fx-font-size:11px;";

                if (s.isBlank() || "non_initie".equals(s)) {
                    badge.setText("⏳ non initié");
                    badge.setStyle(base + "-fx-background-color: rgba(245,158,11,0.18);"
                            + "-fx-text-fill:#FCD34D; -fx-border-color: rgba(245,158,11,0.55);");
                } else if ("en_cours".equals(s)) {
                    badge.setText("💳 en cours");
                    badge.setStyle(base + "-fx-background-color: rgba(59,130,246,0.18);"
                            + "-fx-text-fill:#93C5FD; -fx-border-color: rgba(59,130,246,0.55);");
                } else if ("paid".equals(s) || "paye".equals(s)) {
                    badge.setText("✅ payé");
                    badge.setStyle(base + "-fx-background-color: rgba(16,185,129,0.18);"
                            + "-fx-text-fill:#6EE7B7; -fx-border-color: rgba(16,185,129,0.55);");
                } else if ("failed".equals(s) || "echec".equals(s)) {
                    badge.setText("⚠ échec");
                    badge.setStyle(base + "-fx-background-color: rgba(239,68,68,0.18);"
                            + "-fx-text-fill:#FCA5A5; -fx-border-color: rgba(239,68,68,0.55);");
                } else {
                    badge.setText(item);
                    badge.setStyle(base);
                }

                setGraphic(badge);
            }
        });
    }

    private void setupStatutBadges() {
        if (colStatut == null) return;

        colStatut.setCellFactory(col -> new TableCell<>() {
            private final Label badge = new Label();
            { badge.setStyle("-fx-padding:4 10;-fx-font-weight:800;"
                    + "-fx-background-radius:999;-fx-border-radius:999;-fx-border-width:1;"
                    + "-fx-font-size:11px;"); }

            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setGraphic(null); return; }

                String base = "-fx-padding:4 10;-fx-font-weight:800;"
                        + "-fx-background-radius:999;-fx-border-radius:999;-fx-border-width:1;"
                        + "-fx-font-size:11px;";

                switch (item.trim().toLowerCase(Locale.ROOT)) {
                    case STATUT_ATTENTE -> {
                        badge.setText("⏳ En attente");
                        badge.setStyle(base + "-fx-background-color:rgba(245,158,11,0.18);"
                                + "-fx-text-fill:#FCD34D;-fx-border-color:rgba(245,158,11,0.55);");
                    }
                    case STATUT_CONFIRMEE -> {
                        badge.setText("✅ Confirmée");
                        badge.setStyle(base + "-fx-background-color:rgba(16,185,129,0.18);"
                                + "-fx-text-fill:#6EE7B7;-fx-border-color:rgba(16,185,129,0.55);");
                    }
                    case STATUT_LIVREE -> {
                        badge.setText("📦 Livrée");
                        badge.setStyle(base + "-fx-background-color:rgba(59,130,246,0.18);"
                                + "-fx-text-fill:#93C5FD;-fx-border-color:rgba(59,130,246,0.55);");
                    }
                    case STATUT_ANNULEE -> {
                        badge.setText("⛔ Annulée");
                        badge.setStyle(base + "-fx-background-color:rgba(239,68,68,0.18);"
                                + "-fx-text-fill:#FCA5A5;-fx-border-color:rgba(239,68,68,0.55);");
                    }
                    default -> {
                        badge.setText(item);
                        badge.setStyle(base);
                    }
                }
                setGraphic(badge);
            }
        });
    }

    private void setupRowHoverGlow() {
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

    // =====================================================
    // ANIMATIONS
    // =====================================================
    private void animerApparition(Node node) {
        if (node == null) return;
        node.setOpacity(0);
        new Timeline(
                new KeyFrame(Duration.ZERO,        new KeyValue(node.opacityProperty(), 0)),
                new KeyFrame(Duration.millis(400), new KeyValue(node.opacityProperty(), 1))
        ).play();
    }

    private void animerPulse(Button btn) {
        if (btn == null) return;
        Timeline pulse = new Timeline(
                new KeyFrame(Duration.ZERO,
                        new KeyValue(btn.scaleXProperty(), 1.0),
                        new KeyValue(btn.scaleYProperty(), 1.0)),
                new KeyFrame(Duration.millis(280),
                        new KeyValue(btn.scaleXProperty(), 1.06),
                        new KeyValue(btn.scaleYProperty(), 1.06)),
                new KeyFrame(Duration.millis(560),
                        new KeyValue(btn.scaleXProperty(), 1.0),
                        new KeyValue(btn.scaleYProperty(), 1.0))
        );
        pulse.setCycleCount(3);
        pulse.play();
    }

    // =====================================================
    // HELPERS UI
    // =====================================================
    private void showErr(HBox hbox, Label icon, Label lbl, Control field, String msg) {
        if (field != null) field.setStyle(BORDER_ERR);
        applyStyle(icon, lbl, "✗", msg, "#EF4444");
        setVM(hbox, true);
    }

    private void showOk(HBox hbox, Label icon, Label lbl, Control field, String msg) {
        if (field != null) field.setStyle(BORDER_OK);
        applyStyle(icon, lbl, "✓", msg, "#10B981");
        setVM(hbox, true);
    }

    private void applyStyle(Label icon, Label lbl, String icone, String msg, String color) {
        String style = "-fx-text-fill:" + color + ";-fx-font-weight:700;-fx-font-size:12px;";
        if (icon != null) { icon.setText(icone); icon.setStyle(style + "-fx-font-size:13px;"); }
        if (lbl  != null) { lbl.setText(msg);    lbl.setStyle(style); }
    }

    private void resetMsg(HBox hbox, Label icon, Label lbl) {
        setVM(hbox, false);
        if (icon != null) { icon.setText(""); icon.setStyle(""); }
        if (lbl  != null) { lbl.setText("");  lbl.setStyle(""); }
    }

    private void resetAllValidation() {
        resetMsg(hboxValProduit, iconValProduit, lblValProduit);
        resetMsg(hboxValQte,     iconValQte,     lblValQte);
        resetMsg(hboxMsgAdd,     iconMsgAdd,     lblMsgAdd);
        resetMsg(hboxMsgDel,     iconMsgDel,     lblMsgDel);
        resetMsg(hboxMsgStatut,  iconMsgStatut,  lblMsgStatut);
        resetMsg(hboxMsgPaiement, iconMsgPaiement, lblMsgPaiement);

        if (cbProduit  != null) cbProduit.setStyle(BORDER_DEF);
        if (tfQuantite != null) tfQuantite.setStyle(BORDER_DEF);
    }

    private void clearStartupForm() {
        if (tfQuantite != null) tfQuantite.clear();
        if (cbProduit != null) cbProduit.getSelectionModel().clearSelection();
        if (tableCommandes != null) tableCommandes.getSelectionModel().clearSelection();

        resetAllValidation();
        setVM(boxPaiement, false);
    }

    private int parseQuantite(String value) {
        String v = safe(value).trim();
        if (v.isEmpty()) throw new IllegalArgumentException("La quantité est obligatoire.");
        if (!v.matches("\\d+")) throw new IllegalArgumentException("Entrez un nombre entier (ex: 3).");
        int qte = Integer.parseInt(v);
        if (qte <= 0) throw new IllegalArgumentException("La quantité doit être supérieure à 0.");
        if (qte > QTE_MAX) throw new IllegalArgumentException("Maximum : " + QTE_MAX + " unités.");
        return qte;
    }

    private void setVM(Node node, boolean v) {
        if (node != null) { node.setVisible(v); node.setManaged(v); }
    }

    private void setText(Text t, int v) {
        if (t != null) t.setText(String.valueOf(v));
    }

    private void hideColumn(TableColumn<?, ?> col) {
        if (col == null) return;
        col.setVisible(false);
        col.setMinWidth(0);
        col.setPrefWidth(0);
        col.setMaxWidth(0);
        col.setResizable(false);
    }

    private String safe(String s) { return s == null ? "" : s.trim(); }

    private void safeToastError(String msg) { try { toastUtil.error(msg); } catch (Exception ignore) {} }
    private void safeToastWarning(String msg) { try { toastUtil.warning(msg); } catch (Exception ignore) {} }
}