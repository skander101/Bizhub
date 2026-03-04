package com.bizhub.controller.users_avis.user;
import com.bizhub.model.services.common.service.AppSession;
import com.bizhub.model.services.common.service.NavigationService;
import com.bizhub.model.services.common.service.Services;
import com.bizhub.model.users_avis.user.User;
import javafx.fxml.FXML;
import javafx.scene.control.Accordion;
import javafx.scene.control.Button;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.Stage;
public class SidebarController {
    @FXML private Text subtitleText;
    @FXML private Button goDashboardBtn;
    @FXML private Button goUsersBtn;
    @FXML private Button goProfileBtn;
    @FXML private Button goMarketplaceBtn;
    @FXML private Button goFormationsBtn;
    @FXML private Button goReviewsBtn;
    @FXML private Accordion investAccordion;
    @FXML private TitledPane investPane;
    private Button[] topButtons;
    @FXML
    public void initialize() {
        User me = AppSession.getCurrentUser();
        boolean admin = AppSession.isAdmin();
        String role = (me != null && me.getUserType() != null) ? me.getUserType().trim().toLowerCase() : "";
        boolean isInvestor = role.contains("investisseur");
        show(goDashboardBtn, admin);
        show(goUsersBtn, admin);
        show(goProfileBtn, !admin);
        show(goMarketplaceBtn, !admin);
        if (admin) {
            subtitleText.setText("Admin Panel");
            goReviewsBtn.setText("Reviews (Moderation)");
        }
        show(investAccordion, admin || isInvestor);
        topButtons = new Button[]{ goDashboardBtn, goUsersBtn, goProfileBtn, goFormationsBtn, goReviewsBtn, goMarketplaceBtn };
    }
    public void setActivePage(String pageId) {
        if (topButtons == null) return;
        for (Button b : topButtons) if (b != null) b.getStyleClass().remove("active");
        clearSubButtons();
        boolean isInvestPage = pageId != null && switch (pageId) {
            case "investments", "add-investment", "projects", "add-project",
                 "negotiations", "portfolio", "analytics", "smart-search",
                 "ai-demo", "add-payment", "deal-pipeline", "negotiation-view" -> true;
            default -> false;
        };
        if (isInvestPage) {
            if (investAccordion.isVisible()) investAccordion.setExpandedPane(investPane);
            markSubButton(pageId);
        } else {
            investAccordion.setExpandedPane(null);
            switch (pageId == null ? "" : pageId) {
                case "dashboard"   -> markActive(goDashboardBtn);
                case "users"       -> markActive(goUsersBtn);
                case "profile"     -> markActive(goProfileBtn);
                case "formations"  -> markActive(goFormationsBtn);
                case "reviews"     -> markActive(goReviewsBtn);
                case "marketplace" -> markActive(goMarketplaceBtn);
            }
        }
    }
    @FXML public void goDashboard()    { nav().goToAdminDashboard(); }
    @FXML public void goUsers()        { nav().goToUserManagement(); }
    @FXML public void goProfile()      { nav().goToProfile(); }
    @FXML public void goFormations()   { nav().goToFormations(); }
    @FXML public void goReviews()      { nav().goToReviews(); }
    @FXML public void goAiChat()       { nav().goToAiChat(); }
    @FXML public void goToMarketplaceHome() {
        User me = AppSession.getCurrentUser();
        if (me == null) { nav().goToLogin(); return; }
        String r = me.getUserType() == null ? "" : me.getUserType().trim().toLowerCase();
        if (r.contains("startup")) nav().goToCommande();
        else if (r.contains("investisseur") || r.contains("fournisseur")) nav().goToProduitService();
        else nav().goToCommande();
    }
    @FXML public void goInvestissements()    { nav().goToInvestissements(); }
    @FXML public void goAddInvestissement()  { nav().goToAddInvestissement(); }
    @FXML public void goProjects()           { nav().goToProjects(); }
    @FXML public void goAddProject()         { nav().goToAddProject(); }
    @FXML public void goNegotiations()       { nav().goToNegotiations(); }
    @FXML public void goPortfolioAdvisor()   { nav().goToPortfolioAdvisor(); }
    @FXML public void goAnalyticsDashboard() { nav().goToAnalyticsDashboard(); }
    @FXML public void goSmartSearch()        { nav().goToSmartSearch(); }
    @FXML public void goAiDemo()             { nav().loadInvestFxml("/com/bizhub/fxml/AIDemoView.fxml", "AI Demo"); }
    @FXML public void logout() {
        AppSession.clear();
        Services.auth().logout();
        nav().goToLogin();
    }
    private NavigationService nav() {
        return new NavigationService((Stage) goFormationsBtn.getScene().getWindow());
    }
    private void show(javafx.scene.Node node, boolean visible) {
        if (node != null) { node.setVisible(visible); node.setManaged(visible); }
    }
    private void markActive(Button b) {
        if (b != null && !b.getStyleClass().contains("active")) b.getStyleClass().add("active");
    }
    private void clearSubButtons() {
        if (investPane == null) return;
        javafx.scene.Node content = investPane.getContent();
        if (content instanceof VBox vbox)
            vbox.getChildren().forEach(n -> { if (n instanceof Button b) b.getStyleClass().remove("active"); });
    }
    private void markSubButton(String pageId) {
        if (investPane == null) return;
        javafx.scene.Node content = investPane.getContent();
        if (!(content instanceof VBox vbox)) return;
        String prefix = switch (pageId) {
            case "investments" -> "\uD83D\uDCCB"; case "add-investment" -> "\u2795 Add Inv";
            case "projects" -> "\uD83D\uDCC1"; case "add-project" -> "\u2795 Add Pro";
            case "negotiations" -> "\uD83E\uDD1D"; case "portfolio" -> "\uD83D\uDD2E";
            case "analytics" -> "\uD83D\uDCCA"; case "smart-search" -> "\uD83D\uDD0D";
            case "ai-demo" -> "\uD83E\uDD16"; default -> "";
        };
        vbox.getChildren().forEach(n -> {
            if (n instanceof Button b && b.getText() != null && b.getText().startsWith(prefix))
                if (!b.getStyleClass().contains("active")) b.getStyleClass().add("active");
        });
    }
}
