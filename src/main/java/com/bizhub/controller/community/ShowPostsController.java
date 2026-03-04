package com.bizhub.controller.community;

import com.bizhub.model.community.comment.Comment;
import com.bizhub.model.community.post.Post;
import com.bizhub.model.services.community.comment.CommentService;
import com.bizhub.model.services.community.post.PostService;
import com.bizhub.model.services.community.ai.GeminiService;
import com.bizhub.model.services.community.reaction.ReactionService;
import com.bizhub.model.services.common.service.AppSession;
import com.bizhub.controller.users_avis.user.SidebarController;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

public class ShowPostsController {

    @FXML private SidebarController sidebarController;

    // Topbar
    @FXML private HBox topbar;

    // Main layout
    @FXML private StackPane mainStack;

    // Feed
    @FXML private ListView<Post> postsList;
    @FXML private ComboBox<String> sortCombo;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> filterCategoryCombo;

    // All posts cache for filtering
    private List<Post> allPosts = new java.util.ArrayList<>();

    // Inline post form
    @FXML private VBox formPanel;
    @FXML private Text formTitle;
    @FXML private TextField titleField;
    @FXML private ComboBox<String> categoryCombo;
    @FXML private TextArea contentArea;
    @FXML private Label formError;
    @FXML private Label mediaLabel;
    @FXML private Button clearMediaBtn;
    @FXML private TextField locationField;

    // Location state — set when user picks from dropdown
    private String selectedLocation = null;
    private double selectedLat = 0;
    private double selectedLon = 0;
    private final ContextMenu locationMenu = new ContextMenu();

    // Comments overlay
    @FXML private StackPane commentsOverlay;
    @FXML private Label commentPostTitle;
    @FXML private ListView<Comment> commentsList;
    @FXML private TextArea newCommentArea;
    @FXML private Label commentError;

    // Edit comment overlay
    @FXML private StackPane editCommentOverlay;
    @FXML private TextArea editCommentArea;
    @FXML private Label editCommentError;

    private final PostService postService = new PostService();
    private final CommentService commentService = new CommentService();
    private final ReactionService reactionService = new ReactionService();
    private final com.bizhub.model.services.community.geo.GeoLocationService geoService =
            new com.bizhub.model.services.community.geo.GeoLocationService();
    private final GeminiService geminiService = new GeminiService();

    private Post editingPost = null;
    private Post currentPost = null;
    private Comment editingComment = null;
    private String selectedMediaUrl = null;
    private String selectedMediaType = null;

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm");

    /** Returns the absolute path to the media directory, creating it if needed */
    private java.io.File getMediaDir() {
        // Save next to the running jar / target/classes, always absolute
        java.io.File dir = new java.io.File("src/main/resources/com/bizhub/images/community");
        if (!dir.exists()) dir.mkdirs();
        return dir;
    }

    /** Returns true if current user owns this resource OR is admin */
    private boolean canEdit(int resourceUserId) {
        var me = AppSession.getCurrentUser();
        if (me == null) return false;
        return me.getUserId() == resourceUserId || "admin".equalsIgnoreCase(me.getUserType());
    }

    @FXML
    public void initialize() {
        if (sidebarController != null) sidebarController.setActivePage("community");

        // Post form category combo
        categoryCombo.getItems().addAll("General", "Business", "Tech", "Investment", "Events", "Other");
        categoryCombo.setValue("General");

        // Sort options
        sortCombo.getItems().addAll(
                "Newest First",
                "Oldest First",
                "Title A → Z",
                "Title Z → A",
                "Most Commented",
                "Most Popular"
        );
        sortCombo.setValue("Newest First");
        sortCombo.setOnAction(e -> applyFilters());

        // Filter by category
        filterCategoryCombo.getItems().addAll(
                "All Categories", "General", "Business", "Tech", "Investment", "Events", "Other"
        );
        filterCategoryCombo.setValue("All Categories");
        filterCategoryCombo.setOnAction(e -> applyFilters());

        // Live search — filters as user types
        searchField.textProperty().addListener((obs, oldVal, newVal) -> applyFilters());

        // Location autocomplete using ContextMenu — floats above all other nodes
        locationMenu.setStyle("-fx-background-color:#1A3352;");
        locationField.textProperty().addListener((obs, oldVal, newVal) -> {
            locationMenu.hide();
            selectedLocation = null;
            selectedLat = 0;
            selectedLon = 0;
            if (newVal != null && newVal.trim().length() >= 2) {
                new Thread(() -> {
                    java.util.List<com.bizhub.model.services.community.geo.GeoLocationService.LocationResult>
                            suggestions = geoService.searchLocations(newVal);
                    javafx.application.Platform.runLater(() -> {
                        locationMenu.getItems().clear();
                        if (!suggestions.isEmpty()) {
                            for (var result : suggestions) {
                                MenuItem item = new MenuItem("📍 " + result.displayName);
                                item.setStyle("-fx-text-fill:white;-fx-background-color:#1A3352;");
                                item.setOnAction(ev -> {
                                    selectedLocation = result.displayName;
                                    selectedLat = result.lat;
                                    selectedLon = result.lon;
                                    locationField.setText(result.displayName);
                                    locationMenu.hide();
                                });
                                locationMenu.getItems().add(item);
                            }
                            locationMenu.show(locationField,
                                    javafx.geometry.Side.BOTTOM, 0, 0);
                        }
                    });
                }).start();
            }
        });

        // Cell factories
        postsList.setCellFactory(lv -> new PostCard());
        commentsList.setCellFactory(lv -> new CommentCard());

        refreshPosts();
    }

    // ==================== POST FORM ====================

    @FXML
    public void onShowAddForm() {
        editingPost = null;
        formTitle.setText("✏  New Post");
        titleField.clear();
        contentArea.clear();
        categoryCombo.setValue("General");
        formError.setText("");
        locationField.clear();
        locationMenu.hide();
        selectedLocation = null;
        selectedLat = 0;
        selectedLon = 0;
        onClearMedia();
        showForm(true);
    }

    @FXML
    public void onCancelForm() {
        showForm(false);
    }

    private void showEditForm(Post post) {
        editingPost = post;
        formTitle.setText("✏  Edit Post");
        titleField.setText(post.getTitle());
        contentArea.setText(post.getContent());
        categoryCombo.setValue(post.getCategory());
        formError.setText("");
        // Load existing media info
        selectedMediaUrl = post.getMediaUrl();
        selectedMediaType = post.getMediaType();
        if (selectedMediaUrl != null) {
            mediaLabel.setText("📎 Current media attached");
            mediaLabel.setStyle("-fx-text-fill:#4CAF50;-fx-font-size:12px;");
            clearMediaBtn.setVisible(true);
            clearMediaBtn.setManaged(true);
        } else {
            onClearMedia();
        }
        // Load existing location
        selectedLocation = post.getLocation();
        selectedLat = post.getLocationLat();
        selectedLon = post.getLocationLon();
        locationField.setText(post.getLocation() != null ? post.getLocation() : "");
        showForm(true);
    }

    private void showForm(boolean show) {
        formPanel.setVisible(show);
        formPanel.setManaged(show);
    }

    @FXML
    public void onPickMedia() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Select Image or Video");
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.webp"),
                new FileChooser.ExtensionFilter("Videos", "*.mp4", "*.avi", "*.mkv", "*.mov")
        );
        Stage stage = (Stage) titleField.getScene().getWindow();
        File file = chooser.showOpenDialog(stage);
        if (file == null) return;

        try {
            File dir = getMediaDir();
            String ext = file.getName().substring(file.getName().lastIndexOf('.'));
            String uniqueName = java.util.UUID.randomUUID().toString() + ext;
            File target = new File(dir, uniqueName);

            // Copy file
            java.nio.file.Files.copy(file.toPath(), target.toPath(),
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING);

            // Store the ABSOLUTE path so we can load it at runtime
            selectedMediaUrl = target.getAbsolutePath();

            // Determine type
            String nameLower = file.getName().toLowerCase();
            if (nameLower.endsWith(".mp4") || nameLower.endsWith(".avi") ||
                    nameLower.endsWith(".mkv") || nameLower.endsWith(".mov")) {
                selectedMediaType = "video";
            } else {
                selectedMediaType = "image";
            }

            mediaLabel.setText("📎 " + file.getName());
            mediaLabel.setStyle("-fx-text-fill:#4CAF50;-fx-font-size:12px;");
            clearMediaBtn.setVisible(true);
            clearMediaBtn.setManaged(true);

        } catch (Exception e) {
            mediaLabel.setText("Error: " + e.getMessage());
            mediaLabel.setStyle("-fx-text-fill:#EF4444;");
        }
    }

    @FXML
    public void onClearMedia() {
        selectedMediaUrl = null;
        selectedMediaType = null;
        mediaLabel.setText("No file selected");
        mediaLabel.setStyle("-fx-text-fill:#607A93;-fx-font-size:12px;");
        clearMediaBtn.setVisible(false);
        clearMediaBtn.setManaged(false);
    }

    @FXML
    public void onSavePost() {
        formError.setText("");
        String title    = titleField.getText().trim();
        String content  = contentArea.getText().trim();
        String category = categoryCombo.getValue();

        if (title.isEmpty())          { formError.setText("Title is required.");                    return; }
        if (title.length() < 3)       { formError.setText("Title must be at least 3 characters."); return; }
        if (title.length() > 200)     { formError.setText("Title must be under 200 characters.");  return; }
        if (content.isEmpty())        { formError.setText("Content is required.");                  return; }
        if (content.length() < 10)    { formError.setText("Content must be at least 10 characters."); return; }

        try {
            if (editingPost == null) {
                int userId = AppSession.getCurrentUser() != null ? AppSession.getCurrentUser().getUserId() : 1;
                Post p = new Post(userId, title, content, category);
                p.setMediaUrl(selectedMediaUrl);
                p.setMediaType(selectedMediaType);
                p.setLocation(selectedLocation != null ? selectedLocation : locationField.getText().trim());
                p.setLocationLat(selectedLat);
                p.setLocationLon(selectedLon);
                postService.create(p);
            } else {
                editingPost.setTitle(title);
                editingPost.setContent(content);
                editingPost.setCategory(category);
                editingPost.setMediaUrl(selectedMediaUrl);
                editingPost.setMediaType(selectedMediaType);
                editingPost.setLocation(selectedLocation != null ? selectedLocation : locationField.getText().trim());
                editingPost.setLocationLat(selectedLat);
                editingPost.setLocationLon(selectedLon);
                postService.update(editingPost);
            }
            showForm(false);
            refreshPosts();
        } catch (SQLException e) {
            formError.setText("Error: " + e.getMessage());
        }
    }

    // ==================== COMMENTS OVERLAY ====================

    private void openComments(Post post) {
        currentPost = post;
        commentPostTitle.setText(post.getTitle());
        commentError.setText("");
        newCommentArea.clear();
        refreshComments();
        commentsOverlay.setVisible(true);
        commentsOverlay.setManaged(true);
    }

    @FXML
    public void onCloseComments() {
        commentsOverlay.setVisible(false);
        commentsOverlay.setManaged(false);
        currentPost = null;
    }

    @FXML
    public void onAddComment() {
        commentError.setText("");
        String content = newCommentArea.getText().trim();
        if (content.isEmpty())     { commentError.setText("Comment cannot be empty."); return; }
        if (content.length() < 3)  { commentError.setText("Min 3 characters.");        return; }

        int userId = AppSession.getCurrentUser() != null ? AppSession.getCurrentUser().getUserId() : 1;
        Comment c = new Comment(currentPost.getPostId(), userId, content);
        try {
            commentService.create(c);
            newCommentArea.clear();
            refreshComments();
        } catch (SQLException e) {
            commentError.setText("Error: " + e.getMessage());
        }
    }

    private void refreshComments() {
        if (currentPost == null) return;
        try {
            List<Comment> comments = commentService.findByPostId(currentPost.getPostId());
            commentsList.setItems(FXCollections.observableArrayList(comments));
        } catch (SQLException e) {
            commentError.setText("Error loading comments: " + e.getMessage());
        }
    }

    // ==================== EDIT COMMENT OVERLAY ====================

    private void openEditComment(Comment comment) {
        editingComment = comment;
        editCommentArea.setText(comment.getContent());
        editCommentError.setText("");
        editCommentOverlay.setVisible(true);
        editCommentOverlay.setManaged(true);
    }

    @FXML
    public void onSaveEditComment() {
        String content = editCommentArea.getText().trim();
        if (content.isEmpty()) { editCommentError.setText("Cannot be empty."); return; }
        editingComment.setContent(content);
        try {
            commentService.update(editingComment);
            onCancelEditComment();
            refreshComments();
        } catch (SQLException e) {
            editCommentError.setText("Error: " + e.getMessage());
        }
    }

    @FXML
    public void onCancelEditComment() {
        editCommentOverlay.setVisible(false);
        editCommentOverlay.setManaged(false);
        editingComment = null;
    }

    // ==================== REFRESH ====================


    /** Opens location in Google Maps in the system browser */
    private void showMapPopup(String locationName, double lat, double lon, javafx.scene.Node anchor) {
        try {
            String url;
            if (lat != 0 && lon != 0) {
                url = "https://www.google.com/maps/search/?api=1&query=" + lat + "," + lon;
            } else {
                url = "https://www.google.com/maps/search/?api=1&query=" +
                        java.net.URLEncoder.encode(locationName, java.nio.charset.StandardCharsets.UTF_8);
            }
            // Use xdg-open on Linux, open on Mac, Desktop.browse on Windows
            String os = System.getProperty("os.name", "").toLowerCase();
            if (os.contains("linux")) {
                new ProcessBuilder("xdg-open", url).inheritIO().start();
            } else if (os.contains("mac")) {
                new ProcessBuilder("open", url).inheritIO().start();
            } else {
                java.awt.Desktop.getDesktop().browse(new java.net.URI(url));
            }
        } catch (Exception e) {
            System.err.println("Could not open map: " + e.getMessage());
        }
    }

    public void refreshPosts() {
        try {
            allPosts = postService.findAll();
            applyFilters();
        } catch (SQLException e) {
            showInlineError("Error loading posts: " + e.getMessage());
        }
    }

    /** Applies search text + category filter + sort to allPosts and updates the ListView */
    private void applyFilters() {
        String search = searchField != null && searchField.getText() != null
                ? searchField.getText().trim().toLowerCase() : "";
        String category = filterCategoryCombo != null
                ? filterCategoryCombo.getValue() : "All Categories";
        String sort = sortCombo != null ? sortCombo.getValue() : "Newest First";

        List<Post> filtered = allPosts.stream()
                .filter(p -> {
                    // Search filter — checks title, content and author
                    if (!search.isEmpty()) {
                        boolean matchTitle   = p.getTitle()   != null && p.getTitle().toLowerCase().contains(search);
                        boolean matchContent = p.getContent() != null && p.getContent().toLowerCase().contains(search);
                        boolean matchAuthor  = p.getAuthorName() != null && p.getAuthorName().toLowerCase().contains(search);
                        if (!matchTitle && !matchContent && !matchAuthor) return false;
                    }
                    // Category filter
                    if (category != null && !"All Categories".equals(category)) {
                        if (!category.equals(p.getCategory())) return false;
                    }
                    return true;
                })
                .collect(java.util.stream.Collectors.toList());

        // Sort
        if ("Oldest First".equals(sort)) {
            filtered.sort(java.util.Comparator.comparing(
                    Post::getCreatedAt, java.util.Comparator.nullsLast(java.util.Comparator.naturalOrder())));
        } else if ("Title A → Z".equals(sort)) {
            filtered.sort(java.util.Comparator.comparing(
                    p -> p.getTitle() != null ? p.getTitle().toLowerCase() : ""));
        } else if ("Title Z → A".equals(sort)) {
            filtered.sort((a, b) -> {
                String ta = a.getTitle() != null ? a.getTitle().toLowerCase() : "";
                String tb = b.getTitle() != null ? b.getTitle().toLowerCase() : "";
                return tb.compareTo(ta);
            });
        } else if ("Most Commented".equals(sort)) {
            filtered.sort((a, b) -> {
                try {
                    int ca = commentService.findByPostId(a.getPostId()).size();
                    int cb = commentService.findByPostId(b.getPostId()).size();
                    return Integer.compare(cb, ca);
                } catch (SQLException ex) { return 0; }
            });
        } else if ("Most Popular".equals(sort)) {
            filtered.sort((a, b) -> {
                try {
                    int ra = reactionService.getTotalCount(a.getPostId())
                            + commentService.findByPostId(a.getPostId()).size();
                    int rb = reactionService.getTotalCount(b.getPostId())
                            + commentService.findByPostId(b.getPostId()).size();
                    return Integer.compare(rb, ra);
                } catch (SQLException ex) { return 0; }
            });
        } else {
            // Default: Newest First
            filtered.sort(java.util.Comparator.comparing(
                    Post::getCreatedAt, java.util.Comparator.nullsLast(java.util.Comparator.reverseOrder())));
        }

        postsList.setItems(FXCollections.observableArrayList(filtered));
    }

    @FXML
    public void onClearFilters() {
        searchField.clear();
        filterCategoryCombo.setValue("All Categories");
        sortCombo.setValue("Newest First");
        applyFilters();
    }

    // ==================== DELETE CONFIRMATION OVERLAY ====================

    @FXML private StackPane confirmOverlay;
    @FXML private Label confirmMessage;
    private Runnable confirmAction;

    private void showConfirm(String message, Runnable onConfirm) {
        confirmMessage.setText(message);
        confirmAction = onConfirm;
        confirmOverlay.setVisible(true);
        confirmOverlay.setManaged(true);
    }

    @FXML
    public void onConfirmYes() {
        confirmOverlay.setVisible(false);
        confirmOverlay.setManaged(false);
        if (confirmAction != null) confirmAction.run();
        confirmAction = null;
    }

    @FXML
    public void onConfirmNo() {
        confirmOverlay.setVisible(false);
        confirmOverlay.setManaged(false);
        confirmAction = null;
    }

    private void deletePost(Post post) {
        showConfirm("Delete post \"" + post.getTitle() + "\"? This cannot be undone.", () -> {
            try {
                postService.delete(post.getPostId());
                refreshPosts();
            } catch (SQLException e) {
                showInlineError("Error: " + e.getMessage());
            }
        });
    }

    private void deleteComment(Comment comment) {
        showConfirm("Delete this comment? This cannot be undone.", () -> {
            try {
                commentService.delete(comment.getCommentId());
                refreshComments();
            } catch (SQLException e) {
                showInlineError("Error: " + e.getMessage());
            }
        });
    }

    private void showInlineError(String msg) {
        // show in whichever error label is visible
        if (commentsOverlay.isVisible()) {
            commentError.setText(msg);
        } else {
            formError.setText(msg);
        }
    }

    // ==================== CELL FACTORIES ====================

    /** Social-media style post card */
    // No active players needed - video removed due to JavaFX module conflict

    private class PostCard extends ListCell<Post> {

        @Override
        protected void updateItem(Post post, boolean empty) {
            super.updateItem(post, empty);

            if (empty || post == null) { setGraphic(null); setText(null); return; }

            // Avatar circle with first letter of author
            String author = post.getAuthorName() != null ? post.getAuthorName() : "?";
            Label avatar = new Label(author.substring(0, 1).toUpperCase());
            avatar.setStyle(
                    "-fx-background-color:#FFB84D;" +
                            "-fx-text-fill:#0F2035;" +
                            "-fx-font-weight:bold;" +
                            "-fx-font-size:18px;" +
                            "-fx-min-width:44px; -fx-min-height:44px;" +
                            "-fx-max-width:44px; -fx-max-height:44px;" +
                            "-fx-background-radius:999;" +
                            "-fx-alignment:center;"
            );

            // Author name
            Label authorLabel = new Label(author);
            authorLabel.setStyle("-fx-text-fill:white;-fx-font-weight:bold;-fx-font-size:13px;");

            // Category chip
            Label cat = new Label(post.getCategory() != null ? post.getCategory() : "General");
            cat.setStyle(
                    "-fx-background-color:#1A3352;" +
                            "-fx-text-fill:#FFB84D;" +
                            "-fx-font-size:11px;" +
                            "-fx-background-radius:999;" +
                            "-fx-padding:3 10;"
            );

            // Date
            String dateStr = post.getCreatedAt() != null ? post.getCreatedAt().format(FMT) : "";
            Label date = new Label("🕐 " + dateStr);
            date.setStyle("-fx-text-fill:#607A93;-fx-font-size:11px;");

            // Title
            Label title = new Label(post.getTitle());
            title.setStyle("-fx-text-fill:white;-fx-font-weight:bold;-fx-font-size:16px;");
            title.setWrapText(true);

            // Content preview
            String preview = post.getContent();
            if (preview != null && preview.length() > 180) preview = preview.substring(0, 177) + "…";
            Label content = new Label(preview);
            content.setStyle("-fx-text-fill:#A0B4C8;-fx-font-size:13px;");
            content.setWrapText(true);

            // Action buttons — comments always visible, edit/delete only if owner or admin
            Button btnComment = new Button("💬 Comments");
            btnComment.setStyle("-fx-background-color:#1A3352;-fx-text-fill:#FFB84D;-fx-background-radius:6;-fx-cursor:hand;-fx-font-size:12px;-fx-padding:5 12;");
            btnComment.setOnAction(e -> openComments(post));

            // --- Reaction button + counts ---
            // LinkedIn reactions: emoji + label
            String[][] REACTIONS = {
                    {"LIKE",       "👍", "Like"},
                    {"LOVE",       "❤️", "Love"},
                    {"CELEBRATE",  "🎉", "Celebrate"},
                    {"SUPPORT",    "🤝", "Support"},
                    {"INSIGHTFUL", "💡", "Insightful"},
                    {"CURIOUS",    "🔥", "Curious"}
            };

            // Load current user reaction + counts
            String myReaction = null;
            java.util.Map<String, Integer> reactionCounts = new java.util.HashMap<>();
            try {
                int uid = AppSession.getCurrentUser() != null ? AppSession.getCurrentUser().getUserId() : -1;
                myReaction = reactionService.getUserReaction(post.getPostId(), uid);
                reactionCounts.putAll(reactionService.getCounts(post.getPostId()));
            } catch (Exception ignored) {}

            final String[] currentReaction = {myReaction};

            // Reaction button — shows user's current reaction or default 👍 Like
            String btnEmoji = "👍";
            String btnLabel = "Like";
            if (currentReaction[0] != null) {
                for (String[] r : REACTIONS) {
                    if (r[0].equals(currentReaction[0])) { btnEmoji = r[1]; btnLabel = r[2]; break; }
                }
            }
            Button reactionBtn = new Button(btnEmoji + " " + btnLabel);
            reactionBtn.setStyle(currentReaction[0] != null
                    ? "-fx-background-color:#1A2A4A;-fx-text-fill:#A78BFA;-fx-background-radius:6;-fx-cursor:hand;-fx-font-size:12px;-fx-padding:5 12;-fx-font-weight:bold;"
                    : "-fx-background-color:#1A3352;-fx-text-fill:#A0B4C8;-fx-background-radius:6;-fx-cursor:hand;-fx-font-size:12px;-fx-padding:5 12;"
            );

            // Reaction counts label — e.g. "👍 8  ❤️ 3  💡 2"
            StringBuilder countStr = new StringBuilder();
            for (String[] r : REACTIONS) {
                Integer cnt = reactionCounts.get(r[0]);
                if (cnt != null && cnt > 0) countStr.append(r[1]).append(" ").append(cnt).append("  ");
            }
            Label countsLabel = new Label(countStr.toString().trim());
            countsLabel.setStyle("-fx-text-fill:#607A93;-fx-font-size:11px;");

            // Hover picker popup — horizontal pill of 6 reaction buttons
            javafx.stage.Popup reactionPicker = new javafx.stage.Popup();
            HBox pickerBox = new HBox(4);
            pickerBox.setStyle(
                    "-fx-background-color:#0F2035;" +
                            "-fx-border-color:#2A4A6F;" +
                            "-fx-border-width:1.5;" +
                            "-fx-border-radius:999;" +
                            "-fx-background-radius:999;" +
                            "-fx-padding:8 12;" +
                            "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.5),12,0,0,3);"
            );

            for (String[] r : REACTIONS) {
                Button rb = new Button(r[1]);
                rb.setStyle(
                        "-fx-background-color:transparent;" +
                                "-fx-font-size:22px;" +
                                "-fx-cursor:hand;" +
                                "-fx-padding:2 4;"
                );
                Tooltip tip = new Tooltip(r[2]);
                rb.setTooltip(tip);

                // Scale up on hover
                rb.setOnMouseEntered(ev -> rb.setStyle(
                        "-fx-background-color:transparent;-fx-font-size:28px;-fx-cursor:hand;-fx-padding:2 4;"
                ));
                rb.setOnMouseExited(ev -> rb.setStyle(
                        "-fx-background-color:transparent;-fx-font-size:22px;-fx-cursor:hand;-fx-padding:2 4;"
                ));

                final String rType  = r[0];
                final String rEmoji = r[1];
                final String rLabel = r[2];

                rb.setOnAction(ev -> {
                    reactionPicker.hide();
                    try {
                        int uid = AppSession.getCurrentUser().getUserId();
                        if (rType.equals(currentReaction[0])) {
                            // Same reaction — toggle off (un-react)
                            reactionService.delete(post.getPostId(), uid);
                            currentReaction[0] = null;
                            reactionBtn.setText("👍 Like");
                            reactionBtn.setStyle("-fx-background-color:#1A3352;-fx-text-fill:#A0B4C8;-fx-background-radius:6;-fx-cursor:hand;-fx-font-size:12px;-fx-padding:5 12;");
                        } else {
                            // New or changed reaction
                            reactionService.upsert(post.getPostId(), uid, rType);
                            currentReaction[0] = rType;
                            reactionBtn.setText(rEmoji + " " + rLabel);
                            reactionBtn.setStyle("-fx-background-color:#1A2A4A;-fx-text-fill:#A78BFA;-fx-background-radius:6;-fx-cursor:hand;-fx-font-size:12px;-fx-padding:5 12;-fx-font-weight:bold;");
                        }
                        // Refresh counts label
                        java.util.Map<String, Integer> newCounts = reactionService.getCounts(post.getPostId());
                        StringBuilder sb = new StringBuilder();
                        for (String[] rx : REACTIONS) {
                            Integer cnt = newCounts.get(rx[0]);
                            if (cnt != null && cnt > 0) sb.append(rx[1]).append(" ").append(cnt).append("  ");
                        }
                        countsLabel.setText(sb.toString().trim());
                    } catch (Exception ex) {
                        System.err.println("Reaction error: " + ex.getMessage());
                    }
                });
                pickerBox.getChildren().add(rb);
            }
            reactionPicker.getContent().add(pickerBox);
            reactionPicker.setAutoHide(true);

            // Show picker on hover over reaction button
            final boolean[] pickerShowing = {false};
            reactionBtn.setOnMouseEntered(ev -> {
                if (!pickerShowing[0]) {
                    javafx.geometry.Bounds b = reactionBtn.localToScreen(reactionBtn.getBoundsInLocal());
                    reactionPicker.show(reactionBtn.getScene().getWindow(),
                            b.getMinX() - 10,
                            b.getMinY() - 68);
                    pickerShowing[0] = true;
                }
            });
            reactionPicker.setOnHidden(ev -> pickerShowing[0] = false);

            // Click on reaction button itself = quick Like toggle
            reactionBtn.setOnAction(ev -> {
                if (reactionPicker.isShowing()) { reactionPicker.hide(); return; }
                try {
                    int uid = AppSession.getCurrentUser().getUserId();
                    if (currentReaction[0] != null) {
                        reactionService.delete(post.getPostId(), uid);
                        currentReaction[0] = null;
                        reactionBtn.setText("👍 Like");
                        reactionBtn.setStyle("-fx-background-color:#1A3352;-fx-text-fill:#A0B4C8;-fx-background-radius:6;-fx-cursor:hand;-fx-font-size:12px;-fx-padding:5 12;");
                    } else {
                        reactionService.upsert(post.getPostId(), uid, "LIKE");
                        currentReaction[0] = "LIKE";
                        reactionBtn.setText("👍 Like");
                        reactionBtn.setStyle("-fx-background-color:#1A2A4A;-fx-text-fill:#A78BFA;-fx-background-radius:6;-fx-cursor:hand;-fx-font-size:12px;-fx-padding:5 12;-fx-font-weight:bold;");
                    }
                    java.util.Map<String, Integer> newCounts = reactionService.getCounts(post.getPostId());
                    StringBuilder sb = new StringBuilder();
                    for (String[] rx : REACTIONS) {
                        Integer cnt = newCounts.get(rx[0]);
                        if (cnt != null && cnt > 0) sb.append(rx[1]).append(" ").append(cnt).append("  ");
                    }
                    countsLabel.setText(sb.toString().trim());
                } catch (Exception ex) { System.err.println("Reaction error: " + ex.getMessage()); }
            });

            // ✨ Ask AI button
            Button btnAI = new Button("✨ Ask AI");
            btnAI.setStyle("-fx-background-color:#1A2A4A;-fx-text-fill:#A78BFA;-fx-background-radius:6;-fx-cursor:hand;-fx-font-size:12px;-fx-padding:5 12;-fx-border-color:#4C3A8A;-fx-border-radius:6;-fx-border-width:1;");

            HBox actions = new HBox(8, reactionBtn, btnComment, btnAI);
            // Counts label on the right
            Pane spacer = new Pane();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            actions.getChildren().addAll(spacer, countsLabel);
            actions.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

            if (canEdit(post.getUserId())) {
                Button btnEdit   = new Button("✏ Edit");
                Button btnDelete = new Button("🗑 Delete");
                btnEdit.setStyle(  "-fx-background-color:#2A4A6F;-fx-text-fill:white;-fx-background-radius:6;-fx-cursor:hand;-fx-font-size:12px;-fx-padding:5 12;");
                btnDelete.setStyle("-fx-background-color:#7f1d1d;-fx-text-fill:white;-fx-background-radius:6;-fx-cursor:hand;-fx-font-size:12px;-fx-padding:5 12;");
                btnEdit.setOnAction(  e -> showEditForm(post));
                btnDelete.setOnAction(e -> deletePost(post));
                actions.getChildren().addAll(btnEdit, btnDelete);
            }

            // --- AI Panel (hidden by default, toggles on btnAI click) ---
            VBox aiPanel = new VBox(8);
            aiPanel.setVisible(false);
            aiPanel.setManaged(false);
            aiPanel.setStyle(
                    "-fx-background-color:#0D1B2E;" +
                            "-fx-border-color:#4C3A8A;" +
                            "-fx-border-width:1;" +
                            "-fx-border-radius:8;" +
                            "-fx-background-radius:8;" +
                            "-fx-padding:12;"
            );

            // Quick action buttons row
            Button btnFactCheck  = new Button("🔍 Fact-check");
            Button btnSummarize  = new Button("📝 Summarize");
            Button btnTopics     = new Button("🏷 Suggest Topics");
            String quickBtnStyle = "-fx-background-color:#1A2A4A;-fx-text-fill:#A78BFA;-fx-background-radius:6;-fx-cursor:hand;-fx-font-size:11px;-fx-padding:4 10;-fx-border-color:#4C3A8A;-fx-border-radius:6;-fx-border-width:1;";
            btnFactCheck.setStyle(quickBtnStyle);
            btnSummarize.setStyle(quickBtnStyle);
            btnTopics.setStyle(quickBtnStyle);

            HBox quickActions = new HBox(6, btnFactCheck, btnSummarize, btnTopics);

            // Custom question row
            TextField questionField = new TextField();
            questionField.setPromptText("Ask anything about this post...");
            questionField.setStyle("-fx-background-color:#1A3352;-fx-text-fill:white;-fx-prompt-text-fill:#607A93;-fx-background-radius:6;-fx-border-radius:6;-fx-border-color:#4C3A8A;-fx-padding:6 10;");
            HBox.setHgrow(questionField, Priority.ALWAYS);

            Button btnAsk = new Button("Ask");
            btnAsk.setStyle("-fx-background-color:#4C3A8A;-fx-text-fill:white;-fx-background-radius:6;-fx-cursor:hand;-fx-font-size:12px;-fx-padding:6 14;");

            HBox askRow = new HBox(8, questionField, btnAsk);
            askRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

            // AI response area
            Label aiResponse = new Label("Choose an action or ask a question above.");
            aiResponse.setStyle("-fx-text-fill:#C4B5FD;-fx-font-size:12px;-fx-wrap-text:true;");
            aiResponse.setWrapText(true);
            aiResponse.setMaxWidth(460);

            // Loading indicator
            Label loadingLabel = new Label("⏳ Thinking...");
            loadingLabel.setStyle("-fx-text-fill:#607A93;-fx-font-size:12px;");
            loadingLabel.setVisible(false);
            loadingLabel.setManaged(false);

            aiPanel.getChildren().addAll(quickActions, askRow, loadingLabel, aiResponse);

            // "Post as Comment" button — shown after AI responds
            Button btnPostComment = new Button("💬 Post as Comment");
            btnPostComment.setStyle(
                    "-fx-background-color:#1A3352;-fx-text-fill:#FFB84D;" +
                            "-fx-background-radius:6;-fx-cursor:hand;-fx-font-size:11px;-fx-padding:4 12;"
            );
            btnPostComment.setVisible(false);
            btnPostComment.setManaged(false);
            aiPanel.getChildren().add(btnPostComment);

            // Tracks what was asked so we can format the comment
            String[] lastActionLabel = {""};
            String[] lastQuestion    = {""};

            // Helper to call Groq on background thread
            Runnable[] pendingTask = {null};
            java.util.function.Consumer<java.util.concurrent.Callable<String>> runAI = task -> {
                aiResponse.setText("");
                btnPostComment.setVisible(false);
                btnPostComment.setManaged(false);
                loadingLabel.setVisible(true);
                loadingLabel.setManaged(true);
                btnFactCheck.setDisable(true);
                btnSummarize.setDisable(true);
                btnTopics.setDisable(true);
                btnAsk.setDisable(true);
                new Thread(() -> {
                    String result;
                    try { result = task.call(); }
                    catch (Exception ex) { result = "⚠ Error: " + ex.getMessage(); }
                    final String finalResult = result;
                    javafx.application.Platform.runLater(() -> {
                        loadingLabel.setVisible(false);
                        loadingLabel.setManaged(false);
                        aiResponse.setText(finalResult);
                        btnFactCheck.setDisable(false);
                        btnSummarize.setDisable(false);
                        btnTopics.setDisable(false);
                        btnAsk.setDisable(false);
                        // Show post-as-comment button after response arrives
                        btnPostComment.setVisible(true);
                        btnPostComment.setManaged(true);

                        // Format comment: question (or action) + AI answer
                        btnPostComment.setOnAction(ev -> {
                            String commentText;
                            if (!lastQuestion[0].isEmpty()) {
                                commentText = "🤖 AI Q&A\n\n" +
                                        "❓ " + lastQuestion[0] + "\n\n" +
                                        "✨ " + finalResult;
                            } else {
                                commentText = "🤖 AI " + lastActionLabel[0] + "\n\n" +
                                        "✨ " + finalResult;
                            }
                            try {
                                var me = AppSession.getCurrentUser();
                                if (me == null) return;
                                Comment c = new Comment();
                                c.setPostId(post.getPostId());
                                c.setUserId(me.getUserId());
                                c.setContent(commentText);
                                commentService.create(c);
                                btnPostComment.setText("✓ Posted!");
                                btnPostComment.setStyle(
                                        "-fx-background-color:#1A4A2E;-fx-text-fill:#4CAF50;" +
                                                "-fx-background-radius:6;-fx-font-size:11px;-fx-padding:4 12;"
                                );
                                btnPostComment.setDisable(true);
                            } catch (Exception ex) {
                                btnPostComment.setText("⚠ Failed");
                            }
                        });
                    });
                }).start();
            };

            btnFactCheck.setOnAction(e -> {
                lastActionLabel[0] = "Fact-check";
                lastQuestion[0] = "";
                runAI.accept(() -> geminiService.factCheck(post.getTitle(), post.getContent(), post.getMediaUrl()));
            });
            btnSummarize.setOnAction(e -> {
                lastActionLabel[0] = "Summary";
                lastQuestion[0] = "";
                runAI.accept(() -> geminiService.summarize(post.getTitle(), post.getContent(), post.getMediaUrl()));
            });
            btnTopics.setOnAction(e -> {
                lastActionLabel[0] = "Topic Suggestions";
                lastQuestion[0] = "";
                runAI.accept(() -> geminiService.suggestTopics(post.getTitle(), post.getContent(), post.getMediaUrl()));
            });
            btnAsk.setOnAction(e -> {
                String q = questionField.getText().trim();
                if (!q.isEmpty()) {
                    lastActionLabel[0] = "";
                    lastQuestion[0] = q;
                    runAI.accept(() -> geminiService.askAboutPost(post.getTitle(), post.getContent(), post.getMediaUrl(), q));
                }
            });
            questionField.setOnAction(e -> btnAsk.fire());

            // Toggle AI panel visibility
            btnAI.setOnAction(e -> {
                boolean show = !aiPanel.isVisible();
                aiPanel.setVisible(show);
                aiPanel.setManaged(show);
                btnAI.setText(show ? "✨ Hide AI" : "✨ Ask AI");
                btnAI.setStyle(show
                        ? "-fx-background-color:#4C3A8A;-fx-text-fill:white;-fx-background-radius:6;-fx-cursor:hand;-fx-font-size:12px;-fx-padding:5 12;"
                        : "-fx-background-color:#1A2A4A;-fx-text-fill:#A78BFA;-fx-background-radius:6;-fx-cursor:hand;-fx-font-size:12px;-fx-padding:5 12;-fx-border-color:#4C3A8A;-fx-border-radius:6;-fx-border-width:1;"
                );
            });

            HBox meta = new HBox(8, authorLabel, cat, date);
            if (post.getLocation() != null && !post.getLocation().isBlank()) {
                Label loc = new Label("📍 " + post.getLocation());
                loc.setStyle(
                        "-fx-text-fill:#4CAF50;-fx-font-size:11px;" +
                                "-fx-cursor:hand;" +
                                "-fx-underline:true;"
                );
                loc.setTooltip(new Tooltip("Click to view on map"));

                loc.setOnMouseClicked(e -> showMapPopup(
                        post.getLocation(),
                        post.getLocationLat(),
                        post.getLocationLon(),
                        loc
                ));
                meta.getChildren().add(loc);
            }
            meta.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

            HBox header = new HBox(12, avatar, new VBox(4, meta, title));
            header.setAlignment(javafx.geometry.Pos.TOP_LEFT);
            HBox.setHgrow(header.getChildren().get(1), Priority.ALWAYS);

            VBox card = new VBox(10, header, content);

            // Show image or video if present
            if (post.getMediaUrl() != null && !post.getMediaUrl().isBlank()) {
                if ("image".equals(post.getMediaType())) {
                    try {
                        File imgFile = new File(post.getMediaUrl());
                        if (imgFile.exists()) {
                            ImageView imageView = new ImageView(
                                    new Image(imgFile.toURI().toString()));
                            imageView.setFitWidth(400);
                            imageView.setPreserveRatio(true);
                            imageView.setStyle("-fx-background-radius:8;");
                            card.getChildren().add(imageView);
                        }
                    } catch (Exception ignored) {}
                } else if ("video".equals(post.getMediaType())) {
                    // Show a placeholder label — video playback requires full modular JavaFX setup
                    Label videoLabel = new Label("🎬 Video attached (open file to play)");
                    videoLabel.setStyle("-fx-text-fill:#FFB84D;-fx-font-size:12px;-fx-background-color:#1A3352;-fx-padding:8 12;-fx-background-radius:6;");
                    card.getChildren().add(videoLabel);
                }
            }

            card.getChildren().addAll(actions, aiPanel);
            card.setStyle(
                    "-fx-background-color:#112236;" +
                            "-fx-background-radius:14;" +
                            "-fx-border-radius:14;" +
                            "-fx-border-color:#2A4A6F;" +
                            "-fx-border-width:1.5;" +
                            "-fx-padding:16;"
            );
            card.setMaxWidth(Double.MAX_VALUE);

            setGraphic(card);
            setStyle("-fx-background-color:transparent;-fx-padding:4 0;");
        }
    }

    /** Comment card inside the overlay */
    private class CommentCard extends ListCell<Comment> {
        @Override
        protected void updateItem(Comment comment, boolean empty) {
            super.updateItem(comment, empty);
            if (empty || comment == null) { setGraphic(null); setText(null); return; }

            String author = comment.getAuthorName() != null ? comment.getAuthorName() : "User";
            Label avatar = new Label(author.substring(0, 1).toUpperCase());
            avatar.setStyle(
                    "-fx-background-color:#2A4A6F;" +
                            "-fx-text-fill:#FFB84D;" +
                            "-fx-font-weight:bold;" +
                            "-fx-font-size:13px;" +
                            "-fx-min-width:34px; -fx-min-height:34px;" +
                            "-fx-max-width:34px; -fx-max-height:34px;" +
                            "-fx-background-radius:999;" +
                            "-fx-alignment:center;"
            );

            Label authorLabel = new Label(author);
            authorLabel.setStyle("-fx-text-fill:#FFB84D;-fx-font-weight:bold;-fx-font-size:12px;");

            Label content = new Label(comment.getContent());
            content.setStyle("-fx-text-fill:white;-fx-font-size:13px;");
            content.setWrapText(true);

            String dateStr = comment.getCreatedAt() != null ? comment.getCreatedAt().format(FMT) : "";
            Label date = new Label("🕐 " + dateStr);
            date.setStyle("-fx-text-fill:#607A93;-fx-font-size:11px;");

            HBox footer = new HBox(8, date);
            footer.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

            if (canEdit(comment.getUserId())) {
                Button btnEdit = new Button("✏");
                Button btnDel  = new Button("🗑");
                btnEdit.setStyle("-fx-background-color:#2A4A6F;-fx-text-fill:white;-fx-background-radius:4;-fx-cursor:hand;-fx-padding:3 8;");
                btnDel.setStyle( "-fx-background-color:#7f1d1d;-fx-text-fill:white;-fx-background-radius:4;-fx-cursor:hand;-fx-padding:3 8;");
                btnEdit.setOnAction(e -> openEditComment(comment));
                btnDel.setOnAction( e -> deleteComment(comment));
                Pane spacer = new Pane(); HBox.setHgrow(spacer, Priority.ALWAYS);
                footer.getChildren().addAll(spacer, btnEdit, btnDel);
            }

            VBox body = new VBox(3, authorLabel, content, footer);
            HBox.setHgrow(body, Priority.ALWAYS);

            HBox row = new HBox(10, avatar, body);
            row.setStyle(
                    "-fx-background-color:#1A3352;" +
                            "-fx-background-radius:10;" +
                            "-fx-border-radius:10;" +
                            "-fx-border-color:#2A4A6F;" +
                            "-fx-border-width:1;" +
                            "-fx-padding:10;"
            );
            row.setMaxWidth(Double.MAX_VALUE);

            setGraphic(row);
            setStyle("-fx-background-color:transparent;-fx-padding:3 0;");
        }
    }
}