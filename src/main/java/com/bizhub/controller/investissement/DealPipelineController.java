package com.bizhub.controller.investissement;

import com.bizhub.model.investissement.Deal;
import com.bizhub.model.services.investissement.*;
import com.bizhub.model.services.common.config.ApiConfig;
import javafx.application.Platform;
import com.bizhub.controller.users_avis.user.SidebarController;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.animation.*;
import javafx.util.Duration;

import java.awt.Desktop;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.sql.SQLException;
import java.util.logging.Logger;

import com.stripe.model.checkout.Session;

public class DealPipelineController {

    private static final Logger logger = Logger.getLogger(DealPipelineController.class.getName());

    @FXML private SidebarController sidebarController;
    @FXML private Label dealTitleLabel;
    @FXML private Label dealAmountLabel;
    @FXML private Label dealStatusLabel;

    @FXML private VBox step1Box;
    @FXML private VBox step2Box;
    @FXML private VBox step3Box;
    @FXML private VBox step4Box;

    @FXML private Label step1Status;
    @FXML private Label step2Status;
    @FXML private Label step3Status;
    @FXML private Label step4Status;

    @FXML private ProgressBar pipelineProgress;
    @FXML private Button processPaymentBtn;
    @FXML private Button generateContractBtn;
    @FXML private Button sendSignatureBtn;
    @FXML private Button sendEmailBtn;
    @FXML private Button reviewContractBtn;
    @FXML private ProgressIndicator loadingSpinner;
    @FXML private Label statusMessageLabel;

    private DealService dealService;
    private StripePaymentService stripeService;
    private ContractPDFService pdfService;
    private YousignService yousignService;
    private EmailService emailService;
    private Deal currentDeal;

    @FXML
    public void initialize() {
        if (sidebarController != null) sidebarController.setActivePage("deal-pipeline");
        dealService = new DealService();
        stripeService = new StripePaymentService();
        pdfService = new ContractPDFService();
        yousignService = new YousignService();
        emailService = new EmailService();
        loadingSpinner.setVisible(false);
    }

    public void initDeal(Integer negotiationId, int projectId, int buyerId, int sellerId, BigDecimal amount) {
        try {
            Deal deal = new Deal(negotiationId, projectId, buyerId, sellerId, amount);
            currentDeal = dealService.create(deal);
            currentDeal = dealService.getById(currentDeal.getDealId());
            refreshUI();
        } catch (SQLException e) {
            setStatus("Error creating deal: " + e.getMessage());
        }
    }

    public void loadDeal(int dealId) {
        try {
            currentDeal = dealService.getById(dealId);
            refreshUI();
        } catch (SQLException e) {
            setStatus("Error loading deal: " + e.getMessage());
        }
    }

    private void refreshUI() {
        if (currentDeal == null) return;

        dealTitleLabel.setText(currentDeal.getProjectTitle() != null
                ? currentDeal.getProjectTitle() : "Deal #" + currentDeal.getDealId());
        dealAmountLabel.setText(currentDeal.getAmount().toPlainString() + " EUR");
        dealStatusLabel.setText(currentDeal.getStatus().replace("_", " ").toUpperCase());

        updateStepStatus(step1Status, processPaymentBtn, currentDeal.getStripePaymentStatus(), "pending_payment");
        updateStep1ButtonLabel();
        updateStepStatus(step2Status, generateContractBtn, currentDeal.getContractPdfPath() != null ? "done" : "pending", "paid");
        if (reviewContractBtn != null) {
            reviewContractBtn.setDisable(currentDeal.getContractPdfPath() == null);
        }
        updateStep3Status();
        updateStep4Status();

        double progress = calculateProgress();
        animateProgress(progress);
    }

    private void updateStepStatus(Label statusLabel, Button actionBtn, String status, String requiredDealStatus) {
        boolean isCompleted = "succeeded".equals(status) || "done".equals(status) || "signed".equals(status);
        boolean isCurrentStep = currentDeal.getStatus().equals(requiredDealStatus);

        if (isCompleted) {
            statusLabel.setText("COMPLETED");
            statusLabel.setStyle("-fx-text-fill: #10B981; -fx-font-weight: 800;");
            actionBtn.setDisable(true);
            actionBtn.setText("Done");
        } else if (isCurrentStep) {
            statusLabel.setText("IN PROGRESS");
            statusLabel.setStyle("-fx-text-fill: #FDB813; -fx-font-weight: 800;");
            actionBtn.setDisable(false);
        } else {
            statusLabel.setText("PENDING");
            statusLabel.setStyle("-fx-text-fill: rgba(232,169,58,0.5); -fx-font-weight: 800;");
            actionBtn.setDisable(true);
        }
    }

    /** Step 3 is enabled when payment is done, contract is generated, and signature not yet completed. */
    private void updateStep3Status() {
        boolean paymentDone = "succeeded".equals(currentDeal.getStripePaymentStatus());
        boolean contractDone = currentDeal.getContractPdfPath() != null && !currentDeal.getContractPdfPath().isBlank();
        boolean signatureDone = "done".equals(currentDeal.getYousignStatus()) || "signed".equals(currentDeal.getYousignStatus());

        if (signatureDone) {
            step3Status.setText("COMPLETED");
            step3Status.setStyle("-fx-text-fill: #10B981; -fx-font-weight: 800;");
            sendSignatureBtn.setDisable(true);
            sendSignatureBtn.setText("Done");
        } else if (paymentDone && contractDone) {
            step3Status.setText("IN PROGRESS");
            step3Status.setStyle("-fx-text-fill: #FDB813; -fx-font-weight: 800;");
            sendSignatureBtn.setDisable(false);
            sendSignatureBtn.setText("Send for Signature");
        } else {
            step3Status.setText("PENDING");
            step3Status.setStyle("-fx-text-fill: rgba(232,169,58,0.5); -fx-font-weight: 800;");
            sendSignatureBtn.setDisable(true);
        }
    }

    /** Step 4 (Send Confirmation) is enabled once signature was requested: pending_signature or signed. So after you sign in Yousign you can click Send Confirmation without waiting for webhook. */
    private void updateStep4Status() {
        boolean emailSent = currentDeal.isEmailSent();
        boolean canSendConfirmation = "pending_signature".equals(currentDeal.getStatus()) || "signed".equals(currentDeal.getStatus());

        if (emailSent) {
            step4Status.setText("COMPLETED");
            step4Status.setStyle("-fx-text-fill: #10B981; -fx-font-weight: 800;");
            sendEmailBtn.setDisable(true);
            sendEmailBtn.setText("Done");
        } else if (canSendConfirmation) {
            step4Status.setText("IN PROGRESS");
            step4Status.setStyle("-fx-text-fill: #FDB813; -fx-font-weight: 800;");
            sendEmailBtn.setDisable(false);
            sendEmailBtn.setText("Send Confirmation");
        } else {
            step4Status.setText("PENDING");
            step4Status.setStyle("-fx-text-fill: rgba(232,169,58,0.5); -fx-font-weight: 800;");
            sendEmailBtn.setDisable(true);
        }
    }

    private double calculateProgress() {
        return switch (currentDeal.getStatus()) {
            case "paid" -> 0.25;
            case "pending_signature" -> 0.50;
            case "signed" -> 0.75;
            case "completed" -> 1.0;
            default -> 0.0;
        };
    }

    private void animateProgress(double target) {
        Timeline timeline = new Timeline(
                new KeyFrame(Duration.millis(800),
                        new KeyValue(pipelineProgress.progressProperty(), target, Interpolator.EASE_BOTH)));
        timeline.play();
    }

    private void updateStep1ButtonLabel() {
        if (currentDeal == null || processPaymentBtn == null) return;
        boolean hasCheckoutSession = currentDeal.getStripeCheckoutSessionId() != null && !currentDeal.getStripeCheckoutSessionId().isBlank();
        boolean notPaid = !"succeeded".equals(currentDeal.getStripePaymentStatus());
        if (hasCheckoutSession && notPaid) {
            processPaymentBtn.setText("I've paid");
            processPaymentBtn.setOnAction(ev -> handleConfirmPaymentDone());
        } else if (notPaid) {
            processPaymentBtn.setText("Pay with card");
            processPaymentBtn.setOnAction(ev -> handleProcessPayment());
        }
    }

    @FXML
    public void handleProcessPayment() {
        if (currentDeal == null) return;
        setLoading(true);
        setStatus("Opening Stripe Checkout in browser...");

        String successUrl = "https://stripe.com";
        String cancelUrl = "https://stripe.com";

        javafx.concurrent.Task<Session> task = new javafx.concurrent.Task<>() {
            @Override
            protected Session call() throws Exception {
                Session session = stripeService.createCheckoutSession(
                        currentDeal.getAmount(),
                        currentDeal.getProjectTitle() != null ? currentDeal.getProjectTitle() : "Deal",
                        currentDeal.getBuyerEmail(),
                        successUrl,
                        cancelUrl);
                dealService.updateStripeCheckoutSession(currentDeal.getDealId(), session.getId());
                return session;
            }
        };

        task.setOnSucceeded(e -> Platform.runLater(() -> {
            setLoading(false);
            Session session = task.getValue();
            String url = session.getUrl();
            if (url != null && !url.isBlank()) {
                try {
                    if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                        Desktop.getDesktop().browse(URI.create(url));
                    }
                } catch (Exception ex) {
                    logger.warning("Could not open browser: " + ex.getMessage());
                }
            }
            setStatus("Complete payment in the browser window. When finished, click \"I've paid\" below.");
            reloadDeal();
        }));

        task.setOnFailed(ev -> Platform.runLater(() -> {
            setLoading(false);
            setStatus("Payment setup failed: " + task.getException().getMessage());
        }));

        new Thread(task).start();
    }

    @FXML
    public void handleConfirmPaymentDone() {
        if (currentDeal == null || currentDeal.getStripeCheckoutSessionId() == null) return;
        setLoading(true);
        setStatus("Checking payment status...");

        javafx.concurrent.Task<Void> task = new javafx.concurrent.Task<>() {
            @Override
            protected Void call() throws Exception {
                Session session = stripeService.retrieveCheckoutSession(currentDeal.getStripeCheckoutSessionId());
                if ("paid".equals(session.getPaymentStatus())) {
                    String paymentIntentId = session.getPaymentIntent();
                    if (paymentIntentId != null && !paymentIntentId.isBlank()) {
                        dealService.updateStripePayment(currentDeal.getDealId(), paymentIntentId, "succeeded");
                    }
                }
                return null;
            }
        };

        task.setOnSucceeded(ev -> Platform.runLater(() -> {
            setLoading(false);
            reloadDeal();
            setStatus("Payment confirmed! You can continue with the contract.");
        }));

        task.setOnFailed(ev -> Platform.runLater(() -> {
            setLoading(false);
            setStatus("Could not confirm payment. Make sure you completed the payment in the browser, then try again.");
        }));

        new Thread(task).start();
    }

    @FXML
    public void handleGenerateContract() {
        if (currentDeal == null) return;
        setLoading(true);
        setStatus("Generating PDF contract...");

        javafx.concurrent.Task<String> task = new javafx.concurrent.Task<>() {
            @Override
            protected String call() throws Exception {
                String pdfPath = pdfService.generateContract(currentDeal);
                dealService.updateContractPdf(currentDeal.getDealId(), pdfPath);
                return pdfPath;
            }
        };

        task.setOnSucceeded(e -> Platform.runLater(() -> {
            setLoading(false);
            setStatus("Contract generated: " + task.getValue());
            reloadDeal();
        }));

        task.setOnFailed(e -> Platform.runLater(() -> {
            setLoading(false);
            setStatus("PDF generation failed: " + task.getException().getMessage());
        }));

        new Thread(task).start();
    }

    @FXML
    public void handleReviewContract() {
        if (currentDeal == null || currentDeal.getContractPdfPath() == null) return;

        Stage owner = (Stage) dealTitleLabel.getScene().getWindow();
        Stage reviewStage = new Stage();
        reviewStage.initOwner(owner);
        reviewStage.setTitle("AI Contract Review");

        javafx.scene.layout.VBox root = new javafx.scene.layout.VBox(0);
        root.setStyle("-fx-background-color: #0A192F;");

        Label header = new Label("AI Contract Clause Review");
        header.setStyle("-fx-text-fill: #06B6D4; -fx-font-size: 18px; -fx-font-weight: 900;");
        header.setPadding(new javafx.geometry.Insets(20, 24, 6, 24));

        Label contractInfo = new Label("Contract: " + currentDeal.getContractPdfPath());
        contractInfo.setStyle("-fx-text-fill: rgba(255,255,255,0.5); -fx-font-size: 11px;");
        contractInfo.setPadding(new javafx.geometry.Insets(0, 24, 10, 24));

        javafx.scene.control.ProgressIndicator spinner = new javafx.scene.control.ProgressIndicator();
        spinner.setPrefSize(36, 36);
        Label loadingLabel = new Label("AI is reviewing the contract...");
        loadingLabel.setStyle("-fx-text-fill: rgba(255,255,255,0.5); -fx-font-size: 12px;");
        javafx.scene.layout.VBox loadingBox = new javafx.scene.layout.VBox(10, spinner, loadingLabel);
        loadingBox.setAlignment(javafx.geometry.Pos.CENTER);
        loadingBox.setPadding(new javafx.geometry.Insets(20));

        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent; -fx-border-color: transparent;");
        javafx.scene.layout.VBox.setVgrow(scrollPane, javafx.scene.layout.Priority.ALWAYS);

        javafx.scene.layout.VBox resultContent = new javafx.scene.layout.VBox(14);
        resultContent.setPadding(new javafx.geometry.Insets(8, 24, 20, 24));
        scrollPane.setContent(resultContent);

        Button closeBtn = new Button("Close");
        closeBtn.setStyle("-fx-background-color: rgba(6,182,212,0.15); -fx-text-fill: #06B6D4; -fx-font-weight: 800; " +
                "-fx-background-radius: 14; -fx-padding: 10 28; -fx-cursor: hand; " +
                "-fx-border-color: rgba(6,182,212,0.3); -fx-border-radius: 14; -fx-border-width: 1;");
        closeBtn.setOnAction(ev -> reviewStage.close());
        javafx.scene.layout.HBox bottomBar = new javafx.scene.layout.HBox(closeBtn);
        bottomBar.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);
        bottomBar.setPadding(new javafx.geometry.Insets(10, 24, 16, 24));

        root.getChildren().addAll(header, contractInfo, loadingBox, scrollPane, bottomBar);
        Scene scene = new Scene(root, 660, 700);
        reviewStage.setScene(scene);
        reviewStage.show();

       com.bizhub.model.services.investissement.AI.ContractReviewService reviewService =
                new com.bizhub.model.services.investissement.AI.ContractReviewService();
        javafx.concurrent.Task<com.bizhub.model.services.investissement.AI.ContractReviewService.ClauseReview> reviewTask =
                reviewService.reviewContractAsync(currentDeal.getContractPdfPath());

        reviewTask.setOnSucceeded(e -> Platform.runLater(() -> {
            loadingBox.setVisible(false);
            loadingBox.setManaged(false);
            var result = reviewTask.getValue();
            com.google.gson.JsonObject r = result.parsed();

            // Overall rating badge
            if (r.has("overallRating")) {
                String rating = r.get("overallRating").getAsString();
                int score = r.has("overallScore") ? r.get("overallScore").getAsInt() : 0;
                String rColor = score >= 7 ? "#10B981" : score >= 4 ? "#F59E0B" : "#EF4444";
                javafx.scene.layout.HBox ratingRow = new javafx.scene.layout.HBox(12);
                ratingRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                Label rBadge = new Label(rating);
                rBadge.setStyle("-fx-background-color: " + rColor + "; -fx-text-fill: white; " +
                        "-fx-font-size: 14px; -fx-font-weight: 900; -fx-background-radius: 12; -fx-padding: 5 16;");
                Label scoreLbl = new Label(score + "/10");
                scoreLbl.setStyle("-fx-text-fill: " + rColor + "; -fx-font-size: 22px; -fx-font-weight: 900;");
                ratingRow.getChildren().addAll(scoreLbl, rBadge);
                resultContent.getChildren().add(ratingRow);
            }

            // Summary
            if (r.has("summary")) {
                Label sum = new Label(r.get("summary").getAsString());
                sum.setWrapText(true);
                sum.setStyle("-fx-text-fill: #FFFFFF; -fx-font-size: 13px; -fx-line-spacing: 2;");
                resultContent.getChildren().add(sum);
            }

            // Clauses
            if (r.has("clauses") && r.get("clauses").isJsonArray()) {
                Label clauseTitle = new Label("Clause Analysis");
                clauseTitle.setStyle("-fx-text-fill: #06B6D4; -fx-font-size: 14px; -fx-font-weight: 900;");
                resultContent.getChildren().add(clauseTitle);

                for (com.google.gson.JsonElement el : r.getAsJsonArray("clauses")) {
                    if (!el.isJsonObject()) continue;
                    com.google.gson.JsonObject c = el.getAsJsonObject();
                    String name = c.has("clauseName") ? c.get("clauseName").getAsString() : "";
                    String cRating = c.has("rating") ? c.get("rating").getAsString() : "";
                    int riskLvl = c.has("riskLevel") ? c.get("riskLevel").getAsInt() : 0;
                    String analysis = c.has("analysis") ? c.get("analysis").getAsString() : "";
                    String suggestion = c.has("suggestion") ? c.get("suggestion").getAsString() : "";

                    String cColor = riskLvl <= 2 ? "#10B981" : riskLvl <= 3 ? "#F59E0B" : "#EF4444";
                    javafx.scene.layout.VBox clauseBox = new javafx.scene.layout.VBox(6);
                    clauseBox.setPadding(new javafx.geometry.Insets(12));
                    clauseBox.setStyle("-fx-background-color: rgba(26,51,82,0.96); -fx-background-radius: 14; " +
                            "-fx-border-color: " + cColor + "33; -fx-border-radius: 14; -fx-border-width: 1;");

                    javafx.scene.layout.HBox clauseHeader = new javafx.scene.layout.HBox(10);
                    clauseHeader.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                    Label cName = new Label(name);
                    cName.setStyle("-fx-text-fill: #FFFFFF; -fx-font-size: 13px; -fx-font-weight: 800;");
                    Label cBadge = new Label(cRating + " (" + riskLvl + "/5)");
                    cBadge.setStyle("-fx-background-color: " + cColor + "22; -fx-text-fill: " + cColor + "; " +
                            "-fx-font-size: 10px; -fx-font-weight: 800; -fx-background-radius: 999; -fx-padding: 2 8;");
                    clauseHeader.getChildren().addAll(cName, cBadge);
                    clauseBox.getChildren().add(clauseHeader);

                    if (!analysis.isBlank()) {
                        Label aLabel = new Label(analysis);
                        aLabel.setWrapText(true);
                        aLabel.setStyle("-fx-text-fill: rgba(255,255,255,0.7); -fx-font-size: 11px;");
                        clauseBox.getChildren().add(aLabel);
                    }
                    if (!suggestion.isBlank()) {
                        Label sLabel = new Label("Suggestion: " + suggestion);
                        sLabel.setWrapText(true);
                        sLabel.setStyle("-fx-text-fill: #06B6D4; -fx-font-size: 11px; -fx-font-style: italic;");
                        clauseBox.getChildren().add(sLabel);
                    }
                    resultContent.getChildren().add(clauseBox);
                }
            }

            // Red Flags
            if (r.has("redFlags") && r.get("redFlags").isJsonArray() && r.getAsJsonArray("redFlags").size() > 0) {
                Label rfTitle = new Label("Red Flags");
                rfTitle.setStyle("-fx-text-fill: #EF4444; -fx-font-size: 14px; -fx-font-weight: 900;");
                resultContent.getChildren().add(rfTitle);
                for (com.google.gson.JsonElement el : r.getAsJsonArray("redFlags")) {
                    Label l = new Label("  ⚠  " + el.getAsString());
                    l.setWrapText(true);
                    l.setStyle("-fx-text-fill: #EF4444; -fx-font-size: 12px;");
                    resultContent.getChildren().add(l);
                }
            }

            // Missing Clauses
            if (r.has("missingClauses") && r.get("missingClauses").isJsonArray() && r.getAsJsonArray("missingClauses").size() > 0) {
                Label mcTitle = new Label("Missing Clauses");
                mcTitle.setStyle("-fx-text-fill: #F59E0B; -fx-font-size: 14px; -fx-font-weight: 900;");
                resultContent.getChildren().add(mcTitle);
                for (com.google.gson.JsonElement el : r.getAsJsonArray("missingClauses")) {
                    Label l = new Label("  •  " + el.getAsString());
                    l.setWrapText(true);
                    l.setStyle("-fx-text-fill: #F59E0B; -fx-font-size: 12px;");
                    resultContent.getChildren().add(l);
                }
            }

            // Negotiation Tips
            if (r.has("negotiationTips") && r.get("negotiationTips").isJsonArray()) {
                Label ntTitle = new Label("Negotiation Tips");
                ntTitle.setStyle("-fx-text-fill: #8B5CF6; -fx-font-size: 14px; -fx-font-weight: 900;");
                resultContent.getChildren().add(ntTitle);
                for (com.google.gson.JsonElement el : r.getAsJsonArray("negotiationTips")) {
                    Label l = new Label("  →  " + el.getAsString());
                    l.setWrapText(true);
                    l.setStyle("-fx-text-fill: rgba(255,255,255,0.8); -fx-font-size: 12px;");
                    resultContent.getChildren().add(l);
                }
            }

            // Verdict
            if (r.has("verdict")) {
                Label verdictTitle = new Label("Verdict");
                verdictTitle.setStyle("-fx-text-fill: #06B6D4; -fx-font-size: 14px; -fx-font-weight: 900;");
                Label verdict = new Label(r.get("verdict").getAsString());
                verdict.setWrapText(true);
                verdict.setStyle("-fx-text-fill: #FFFFFF; -fx-font-size: 13px; -fx-font-weight: 700;");
                resultContent.getChildren().addAll(verdictTitle, verdict);
            }
        }));

        reviewTask.setOnFailed(e -> Platform.runLater(() -> {
            loadingBox.setVisible(false);
            loadingBox.setManaged(false);
            Label err = new Label("Review failed: " + reviewTask.getException().getMessage());
            err.setWrapText(true);
            err.setStyle("-fx-text-fill: #EF4444; -fx-font-size: 13px;");
            resultContent.getChildren().add(err);
        }));

        new Thread(reviewTask).start();
    }

    @FXML
    public void handleSendSignature() {
        if (currentDeal == null || currentDeal.getContractPdfPath() == null) return;
        setLoading(true);
        setStatus("Sending for digital signature via Yousign...");

        String override = ApiConfig.getYousignSignerEmailOverride();
        String buyerEmail = override != null && !override.isBlank()
                ? override
                : (currentDeal.getBuyerEmail() != null ? currentDeal.getBuyerEmail() : "buyer@test.com");
        String sellerEmail = override != null && !override.isBlank()
                ? override
                : (currentDeal.getSellerEmail() != null ? currentDeal.getSellerEmail() : "seller@test.com");

        javafx.concurrent.Task<String> task = yousignService.createAndSendForSignatureAsync(
                "BizHub Contract #" + currentDeal.getDealId(),
                currentDeal.getContractPdfPath(),
                buyerEmail,
                currentDeal.getBuyerName() != null ? currentDeal.getBuyerName() : "Buyer",
                sellerEmail,
                currentDeal.getSellerName() != null ? currentDeal.getSellerName() : "Seller");

        task.setOnSucceeded(e -> Platform.runLater(() -> {
            setLoading(false);
            try {
                String requestId = task.getValue();
                dealService.updateYousign(currentDeal.getDealId(), requestId, "ongoing");
                setStatus("Signature request sent! Check your email (and spam). Links are also shown below if needed.");
                reloadDeal();
                fetchAndShowSignatureLinks(requestId);
            } catch (SQLException ex) {
                setStatus("Error updating deal: " + ex.getMessage());
            }
        }));

        task.setOnFailed(e -> Platform.runLater(() -> {
            setLoading(false);
            setStatus("Yousign failed: " + task.getException().getMessage());
        }));

        new Thread(task).start();
    }

    private void fetchAndShowSignatureLinks(String signatureRequestId) {
        javafx.concurrent.Task<java.util.List<java.util.Map.Entry<String, String>>> t = new javafx.concurrent.Task<>() {
            @Override
            protected java.util.List<java.util.Map.Entry<String, String>> call() throws Exception {
                return yousignService.getSignatureLinks(signatureRequestId);
            }
        };
        t.setOnSucceeded(ev -> Platform.runLater(() -> {
            var links = t.getValue();
            javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION);
            alert.setTitle("Yousign Signature");
            alert.setHeaderText("Signature request sent!");

            if (links != null && !links.isEmpty()) {
                StringBuilder sb = new StringBuilder();
                for (var entry : links) {
                    sb.append(entry.getKey()).append(":\n").append(entry.getValue()).append("\n\n");
                }
                alert.setContentText("Signature links / previews found. Check your email inbox and spam folder too.");
                javafx.scene.control.TextArea area = new javafx.scene.control.TextArea(sb.toString());
                area.setEditable(false);
                area.setWrapText(true);
                area.setPrefRowCount(8);
                alert.getDialogPane().setExpandableContent(new javafx.scene.layout.VBox(area));
                alert.getDialogPane().setExpanded(true);
            } else {
                String override = ApiConfig.getYousignSignerEmailOverride();
                String targetEmail = (override != null && !override.isBlank()) ? override : "the signer emails";
                alert.setContentText("Yousign will send the signature email to: " + targetEmail +
                        "\n\nCheck your inbox and spam folder. The email may take 1-2 minutes to arrive." +
                        "\n\nNote: In sandbox mode, signature_link is null — the email is the only delivery method.");
            }
            alert.getDialogPane().setMinWidth(500);
            alert.showAndWait();
        }));
        new Thread(t).start();
    }

    @FXML
    public void handleSendEmail() {
        if (currentDeal == null) return;
        setLoading(true);
        setStatus("Sending confirmation emails...");

        javafx.concurrent.Task<Void> task = new javafx.concurrent.Task<>() {
            @Override
            protected Void call() throws Exception {
                String amount = currentDeal.getAmount().toPlainString();
                String title = currentDeal.getProjectTitle();
                String pdf = currentDeal.getContractPdfPath();

                String emailOverride = com.bizhub.model.services.common.config.ApiConfig.getEmailOverride();
                String buyerEmail = (emailOverride != null && !emailOverride.isBlank())
                        ? emailOverride : currentDeal.getBuyerEmail();
                String sellerEmail = (emailOverride != null && !emailOverride.isBlank())
                        ? emailOverride : currentDeal.getSellerEmail();

                if (buyerEmail != null) {
                    emailService.sendDealConfirmation(
                            buyerEmail, currentDeal.getBuyerName(),
                            title, amount, pdf);
                }
                if (sellerEmail != null && !sellerEmail.equalsIgnoreCase(buyerEmail)) {
                    emailService.sendDealConfirmation(
                            sellerEmail, currentDeal.getSellerName(),
                            title, amount, pdf);
                }
                dealService.markEmailSent(currentDeal.getDealId());
                dealService.completeDeal(currentDeal.getDealId());
                return null;
            }
        };

        task.setOnSucceeded(e -> Platform.runLater(() -> {
            setLoading(false);
            setStatus("Emails sent! Deal completed successfully!");
            reloadDeal();
        }));

        task.setOnFailed(e -> Platform.runLater(() -> {
            setLoading(false);
            setStatus("Email failed: " + task.getException().getMessage());
        }));

        new Thread(task).start();
    }

    private void reloadDeal() {
        if (currentDeal != null) loadDeal(currentDeal.getDealId());
    }

    private void setLoading(boolean loading) {
        loadingSpinner.setVisible(loading);
        processPaymentBtn.setDisable(loading);
        generateContractBtn.setDisable(loading);
        if (reviewContractBtn != null) reviewContractBtn.setDisable(loading);
        sendSignatureBtn.setDisable(loading);
        sendEmailBtn.setDisable(loading);
    }

    private void setStatus(String message) {
        statusMessageLabel.setText(message);
        FadeTransition fade = new FadeTransition(Duration.millis(300), statusMessageLabel);
        fade.setFromValue(0);
        fade.setToValue(1);
        fade.play();
    }

    @FXML
    public void handleBack() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/bizhub/fxml/ProjectsListView.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) dealTitleLabel.getScene().getWindow();
            stage.getScene().setRoot(root);
        } catch (IOException e) { e.printStackTrace(); }
    }
}
