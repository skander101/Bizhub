package com.bizhub.controller.marketplace;

import com.bizhub.model.marketplace.CommandeJoinProduit;
import com.bizhub.model.services.common.service.AppSession;
import com.bizhub.model.services.common.service.NavigationService;
import com.bizhub.model.services.marketplace.CommandeService;
import com.bizhub.model.services.marketplace.FactureService;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import java.awt.Desktop;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CommandeTrackingController {

    private static final Logger LOGGER = Logger.getLogger(CommandeTrackingController.class.getName());
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    // Liste & filtres
    @FXML private VBox              boxCommandes;
    @FXML private ProgressIndicator spinner;
    @FXML private Label             lblEmpty;
    @FXML private TextField         tfSearch;
    @FXML private ComboBox<String>  cbFilterStatut;

    // ✅ KPIs (top cards)
    @FXML private Text kpiTotal;
    @FXML private Text kpiAttente;
    @FXML private Text kpiConfirmee;
    @FXML private Text kpiPayee;
    @FXML private Text kpiLivree;

    private final CommandeService commandeService = new CommandeService();
    private final FactureService  factureService  = new FactureService();
    private List<CommandeJoinProduit> allCommandes = new ArrayList<>();

    // =========================================================================
    // INIT
    // =========================================================================
    @FXML
    public void initialize() {

        if (cbFilterStatut != null) {
            cbFilterStatut.getItems().addAll("Tous","en_attente","confirmee","livree","annule");
            cbFilterStatut.setValue("Tous");
            cbFilterStatut.valueProperty().addListener((obs,o,n) -> applyFilter());
        }
        if (tfSearch != null) {
            tfSearch.textProperty().addListener((obs,o,n) -> applyFilter());
        }

        // init KPIs (0)
        updateKpis(List.of());

        setVM(spinner, true);
        setVM(lblEmpty, false);
        loadCommandes();
    }

    // =========================================================================
    // ACTIONS
    // =========================================================================
    @FXML
    private void onRefresh(ActionEvent e) {
        if (cbFilterStatut != null) cbFilterStatut.setValue("Tous");
        if (tfSearch != null) tfSearch.clear();
        setVM(spinner, true);
        setVM(lblEmpty, false);
        loadCommandes();
    }

    @FXML
    public void goToMarketplace() {
        try {
            Stage stage = (Stage) boxCommandes.getScene().getWindow();
            new NavigationService(stage).goToCommande();
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Navigation erreur", e);
        }
    }

    // =========================================================================
    // LOAD
    // =========================================================================
    private void loadCommandes() {
        Task<List<CommandeJoinProduit>> task = new Task<>() {
            @Override protected List<CommandeJoinProduit> call() throws Exception {
                var user = AppSession.getCurrentUser();
                if (user == null) return List.of();

                return isInvestisseur()
                        ? commandeService.getByOwnerJoinProduit(user.getUserId())
                        : commandeService.getByClientJoinProduit(user.getUserId());
            }

            @Override protected void succeeded() {
                allCommandes = (getValue() != null) ? getValue() : new ArrayList<>();

                // ✅ KPIs = sur TOUTES les commandes (pas le filtre)
                updateKpis(allCommandes);

                setVM(spinner, false);
                applyFilter(); // rend la liste filtrée (ou complète)
            }

            @Override protected void failed() {
                setVM(spinner, false);
                setVM(lblEmpty, true);
                if (lblEmpty != null)
                    lblEmpty.setText("❌ Erreur : " + getException().getMessage());
                LOGGER.log(Level.SEVERE, "Erreur chargement tracking", getException());
            }
        };
        new Thread(task, "tracking-loader").start();
    }

    // =========================================================================
    // FILTER
    // =========================================================================
    private void applyFilter() {
        if (allCommandes == null) return;

        String search = tfSearch != null ? tfSearch.getText().trim().toLowerCase(Locale.ROOT) : "";
        String statut = cbFilterStatut != null ? cbFilterStatut.getValue() : "Tous";

        List<CommandeJoinProduit> filtered = allCommandes.stream().filter(c -> {
            boolean ms = "Tous".equals(statut) || statut.equalsIgnoreCase(safe(c.getStatut()));
            boolean mq = search.isBlank()
                    || safe(c.getProduitNom()).toLowerCase(Locale.ROOT).contains(search)
                    || String.valueOf(c.getIdCommande()).contains(search);
            return ms && mq;
        }).toList();

        renderCommandes(filtered);
    }

    // =========================================================================
    // KPIs
    // =========================================================================
    private void updateKpis(List<CommandeJoinProduit> list) {
        int total = list.size();

        int attente = 0, conf = 0, liv = 0, payee = 0;

        for (CommandeJoinProduit c : list) {
            String s = safe(c.getStatut()).toLowerCase(Locale.ROOT);

            if ("en_attente".equals(s)) attente++;
            if ("confirmee".equals(s))  conf++;
            if ("livree".equals(s))     liv++;

            // ✅ Payées: priorité à est_payee (plus fiable que statut)
            if (c.isEstPayee()) payee++;
        }

        setText(kpiTotal,   total);
        setText(kpiAttente, attente);
        setText(kpiConfirmee, conf);
        setText(kpiPayee,   payee);
        setText(kpiLivree,  liv);
    }

    private void setText(Text t, int v) {
        if (t != null) t.setText(String.valueOf(v));
    }

    // =========================================================================
    // RENDER
    // =========================================================================
    private void renderCommandes(List<CommandeJoinProduit> list) {
        if (boxCommandes == null) return;

        boxCommandes.getChildren().clear();

        if (list == null || list.isEmpty()) {
            setVM(lblEmpty, true);
            if (lblEmpty != null) lblEmpty.setText("Aucune commande trouvée.");
            return;
        }

        setVM(lblEmpty, false);

        for (CommandeJoinProduit cmd : list) {
            boxCommandes.getChildren().add(buildCommandeCard(cmd));
        }
    }

    // =========================================================================
    // CARD (style = même que stat-card)
    // =========================================================================
    private VBox buildCommandeCard(CommandeJoinProduit cmd) {

        VBox card = new VBox(10);

        // ✅ style identique aux autres box (stat-card) + petite classe dédiée
        card.getStyleClass().addAll("stat-card", "commande-card");

        // ── Header ─────────────────────────────────────────
        HBox title = new HBox(8);
        title.setAlignment(Pos.CENTER_LEFT);

        Label numCmd = new Label("Commande #" + cmd.getIdCommande());
        numCmd.setStyle("-fx-font-weight:900;-fx-font-size:15px;-fx-text-fill:#FFFFFF;");

        String nomProduit = safe(cmd.getProduitNom());
        if (nomProduit.isEmpty()) nomProduit = "(multi-articles)";

        Label produit = new Label("• " + nomProduit);
        produit.setStyle("-fx-font-size:12px;-fx-text-fill:rgba(255,255,255,0.70);");
        produit.setMaxWidth(520);
        produit.setWrapText(true);

        String qteText = cmd.getQuantiteCommande() > 0 ? "x" + cmd.getQuantiteCommande() : "—";
        Label qte = new Label(qteText);
        qte.setStyle("-fx-background-color:rgba(232,169,58,0.15);-fx-text-fill:#E8A93A;" +
                "-fx-padding:3 8;-fx-background-radius:999;-fx-font-size:11px;-fx-font-weight:800;");

        Label badgeStatut = buildStatutBadge(cmd.getStatut());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label dateLabel = new Label(cmd.getDateCommande() != null ? cmd.getDateCommande().format(FMT) : "—");
        dateLabel.setStyle("-fx-font-size:10px;-fx-text-fill:rgba(255,255,255,0.45);");

        title.getChildren().addAll(numCmd, produit, qte, badgeStatut, spacer, dateLabel);
        card.getChildren().add(title);

        // ── Totaux ─────────────────────────────────────────
        if (cmd.getTotalTtc() != null && cmd.getTotalTtc().compareTo(BigDecimal.ZERO) > 0) {
            HBox totaux = new HBox(16);
            totaux.setAlignment(Pos.CENTER_LEFT);
            totaux.getChildren().addAll(
                    totLabel("HT", cmd.getTotalHt()),
                    totLabel("TVA 19%", cmd.getTotalTva()),
                    totLabelTTC(cmd.getTotalTtc())
            );
            card.getChildren().add(totaux);
        }

        // ── Timeline ──────────────────────────────────────
        card.getChildren().add(buildTimeline(cmd));

        // ── Bottom ────────────────────────────────────────
        HBox bottom = new HBox(10);
        bottom.setAlignment(Pos.CENTER_LEFT);

        if (cmd.isEstPayee() && cmd.getPaidAt() != null) {
            Label paidAt = new Label("💳 Payée le " + cmd.getPaidAt().format(FMT));
            paidAt.setStyle("-fx-font-size:11px;-fx-text-fill:#E8A93A;-fx-font-weight:800;");
            bottom.getChildren().add(paidAt);
        }

        Region sp2 = new Region();
        HBox.setHgrow(sp2, Priority.ALWAYS);
        bottom.getChildren().add(sp2);

        if (cmd.isEstPayee()) {

            Button btnFacture = new Button("🧾 Facture PDF");
            btnFacture.getStyleClass().add("btn-primary");
            btnFacture.setOnAction(e -> onTelechargerFacture(cmd, btnFacture));

            Button btnPrint = new Button("🖨 Imprimer");
            btnPrint.getStyleClass().add("btn-secondary");
            btnPrint.setOnAction(e -> onImprimerFacture(cmd, btnPrint));

            bottom.getChildren().addAll(btnFacture, btnPrint);

        } else {
            Label lockLbl = new Label("🔒 Facture & impression disponibles après paiement");
            lockLbl.setStyle("-fx-font-size:11px;-fx-text-fill:rgba(255,255,255,0.45);-fx-font-style:italic;");
            bottom.getChildren().add(lockLbl);
        }

        card.getChildren().add(bottom);

        return card;
    }

    // ── Totaux helpers ───────────────────────────────────
    private Label totLabel(String lbl, BigDecimal val) {
        String txt = (val != null) ? String.format("%s : %.3f TND", lbl, val) : lbl + " : —";
        Label l = new Label(txt);
        l.setStyle("-fx-font-size:11px;-fx-text-fill:rgba(255,255,255,0.70);");
        return l;
    }

    private Label totLabelTTC(BigDecimal val) {
        Label l = new Label(val != null ? String.format("TTC : %.3f TND", val) : "TTC : —");
        l.setStyle("-fx-font-size:12px;-fx-font-weight:900;-fx-text-fill:#E8A93A;");
        return l;
    }

    // ── Badge statut ─────────────────────────────────────
    private Label buildStatutBadge(String statut) {
        String s = safe(statut).toLowerCase(Locale.ROOT);
        String bg;
        String text;

        switch (s) {
            case "confirmee" -> { bg = "rgba(16,185,129,0.20)"; text = "Confirmée"; }
            case "livree"    -> { bg = "rgba(99,102,241,0.20)"; text = "Livrée"; }
            case "annule"    -> { bg = "rgba(239,68,68,0.20)"; text = "Annulée"; }
            default          -> { bg = "rgba(255,255,255,0.10)"; text = "En attente"; }
        }

        Label l = new Label(text);
        l.setStyle("-fx-background-color:" + bg + ";" +
                "-fx-text-fill:#FFFFFF;-fx-font-size:10px;-fx-font-weight:800;" +
                "-fx-padding:3 10;-fx-background-radius:999;");
        return l;
    }

    // ── Timeline ─────────────────────────────────────────
    private HBox buildTimeline(CommandeJoinProduit cmd) {
        String[] labels = {"Créée","Confirmée","Payée","Livrée"};
        boolean[] done  = {
                true,
                isAtLeast(cmd.getStatut(), "confirmee"),
                cmd.isEstPayee(),
                "livree".equalsIgnoreCase(safe(cmd.getStatut()))
        };

        HBox tl = new HBox();
        tl.setAlignment(Pos.CENTER_LEFT);
        tl.setPadding(new Insets(8,0,4,0));

        for (int i = 0; i < labels.length; i++) {
            tl.getChildren().add(buildStep(labels[i], done[i]));
            if (i < labels.length - 1) tl.getChildren().add(buildConnector(done[i] && done[i+1]));
        }
        return tl;
    }

    private VBox buildStep(String label, boolean done) {
        VBox step = new VBox(4);
        step.setAlignment(Pos.CENTER);
        step.setMinWidth(75);

        Circle c = new Circle(13);
        c.setFill(done ? Color.web("#E8A93A") : Color.web("rgba(255,255,255,0.08)"));
        c.setStroke(done ? Color.web("rgba(232,169,58,0.70)") : Color.web("rgba(255,255,255,0.15)"));
        c.setStrokeWidth(2);

        Text icon = new Text(done ? "✓" : "○");
        icon.setFill(done ? Color.web("#0A192F") : Color.web("rgba(255,255,255,0.55)"));
        icon.setStyle("-fx-font-size:12px;-fx-font-weight:900;");

        StackPane node = new StackPane(c, icon);

        Label lbl = new Label(label);
        lbl.setStyle("-fx-font-size:10px;-fx-font-weight:800;-fx-text-fill:" +
                (done ? "#E8A93A" : "rgba(255,255,255,0.45)") + ";");

        step.getChildren().addAll(node, lbl);
        return step;
    }

    private Region buildConnector(boolean done) {
        Region line = new Region();
        line.setPrefWidth(35);
        line.setPrefHeight(2);
        line.setMaxHeight(2);
        line.setStyle("-fx-background-color:" + (done ? "rgba(232,169,58,0.75)" : "rgba(255,255,255,0.12)") + ";");
        VBox.setMargin(line, new Insets(11,0,0,0));
        return line;
    }

    // ── Facture / impression ─────────────────────────────
    private void onTelechargerFacture(CommandeJoinProduit cmd, Button btn) {
        btn.setDisable(true);
        btn.setText("⏳...");
        Task<Path> task = new Task<>() {
            @Override protected Path call() throws Exception {
                return factureService.genererFacture(cmd);
            }
            @Override protected void succeeded() {
                btn.setText("✅ Téléchargée");
                Path pdf = getValue();
                new Thread(() -> {
                    try { if (Desktop.isDesktopSupported()) Desktop.getDesktop().open(pdf.toFile()); }
                    catch (Exception ignored) {}
                }).start();
                new Thread(() -> {
                    try { Thread.sleep(2000); } catch (InterruptedException ignored) {}
                    Platform.runLater(() -> { btn.setText("🧾 Facture PDF"); btn.setDisable(false); });
                }).start();
            }
            @Override protected void failed() {
                btn.setText("❌ Erreur");
                btn.setDisable(false);
                LOGGER.log(Level.SEVERE, "Erreur facture", getException());
            }
        };
        new Thread(task, "facture-gen").start();
    }

    private void onImprimerFacture(CommandeJoinProduit cmd, Button btn) {
        if (!cmd.isEstPayee()) return;

        btn.setDisable(true);
        btn.setText("⏳...");
        Task<Path> task = new Task<>() {
            @Override protected Path call() throws Exception {
                return factureService.genererFacture(cmd);
            }
            @Override protected void succeeded() {
                try {
                    // Plus fiable: ouvrir PDF, l’utilisateur fait Ctrl+P
                    if (Desktop.isDesktopSupported()) Desktop.getDesktop().open(getValue().toFile());
                    btn.setText("📄 Ouverte (Ctrl+P)");
                } catch (Exception ex) {
                    btn.setText("❌ Erreur");
                }
                new Thread(() -> {
                    try { Thread.sleep(2000); } catch (InterruptedException ignored) {}
                    Platform.runLater(() -> { btn.setDisable(false); btn.setText("🖨 Imprimer"); });
                }).start();
            }
            @Override protected void failed() {
                btn.setDisable(false);
                btn.setText("🖨 Imprimer");
            }
        };
        new Thread(task, "print-facture").start();
    }

    // ── Helpers ──────────────────────────────────────────
    private boolean isAtLeast(String statut, String target) {
        if (statut == null) return false;
        List<String> order = List.of("en_attente","confirmee","livree");
        int sIdx = order.indexOf(statut.toLowerCase(Locale.ROOT));
        int tIdx = order.indexOf(target.toLowerCase(Locale.ROOT));
        return sIdx >= tIdx && sIdx >= 0;
    }

    private boolean isInvestisseur() {
        var u = AppSession.getCurrentUser();
        if (u == null || u.getUserType() == null) return false;
        return u.getUserType().trim().toLowerCase(Locale.ROOT).contains("invest");
    }

    private void setVM(javafx.scene.Node node, boolean v) {
        if (node != null) { node.setVisible(v); node.setManaged(v); }
    }

    private String safe(String s) { return s == null ? "" : s.trim(); }
}