package com.bizhub.controller.community;

import com.bizhub.model.community.comment.Comment;
import com.bizhub.model.community.post.Post;
import com.bizhub.model.services.community.comment.CommentService;
import com.bizhub.model.services.community.post.PostService;
import com.bizhub.model.services.common.service.AppSession;
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

public class ShowPostsController {

    // Topbar
    @FXML private HBox topbar;

    // Main layout
    @FXML private StackPane mainStack;

    // Feed
    @FXML private ListView<Post> postsList;
    @FXML private ComboBox<String> sortCombo;

    // Inline post form
    @FXML private VBox formPanel;
    @FXML private Text formTitle;
    @FXML private TextField titleField;
    @FXML private ComboBox<String> categoryCombo;
    @FXML private TextArea contentArea;
    @FXML private Label formError;
    @FXML private Label mediaLabel;
    @FXML private Button clearMediaBtn;

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
        // Setup combos
        categoryCombo.getItems().addAll("General", "Business", "Tech", "Investment", "Events", "Other");
        categoryCombo.setValue("General");

        sortCombo.getItems().addAll("Newest First", "Oldest First");
        sortCombo.setValue("Newest First");
        sortCombo.setOnAction(e -> refreshPosts());

        // Posts list cell factory — social card style
        postsList.setCellFactory(lv -> new PostCard());

        // Comments list cell factory
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
                postService.create(p);
            } else {
                editingPost.setTitle(title);
                editingPost.setContent(content);
                editingPost.setCategory(category);
                editingPost.setMediaUrl(selectedMediaUrl);
                editingPost.setMediaType(selectedMediaType);
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

    public void refreshPosts() {
        try {
            List<Post> posts = postService.findAll();
            if ("Oldest First".equals(sortCombo.getValue())) {
                java.util.Collections.reverse(posts);
            }
            postsList.setItems(FXCollections.observableArrayList(posts));
        } catch (SQLException e) {
            showInlineError("Error loading posts: " + e.getMessage());
        }
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

            HBox actions = new HBox(8, btnComment);

            if (canEdit(post.getUserId())) {
                Button btnEdit   = new Button("✏ Edit");
                Button btnDelete = new Button("🗑 Delete");
                btnEdit.setStyle(  "-fx-background-color:#2A4A6F;-fx-text-fill:white;-fx-background-radius:6;-fx-cursor:hand;-fx-font-size:12px;-fx-padding:5 12;");
                btnDelete.setStyle("-fx-background-color:#7f1d1d;-fx-text-fill:white;-fx-background-radius:6;-fx-cursor:hand;-fx-font-size:12px;-fx-padding:5 12;");
                btnEdit.setOnAction(  e -> showEditForm(post));
                btnDelete.setOnAction(e -> deletePost(post));
                actions.getChildren().addAll(btnEdit, btnDelete);
            }

            HBox meta = new HBox(8, authorLabel, cat, date);
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

            card.getChildren().add(actions);
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