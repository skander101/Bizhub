package com.bizhub.user.service;

import com.bizhub.common.dao.MyDatabase;
import com.bizhub.user.dao.UserDAO;
import com.bizhub.user.model.User;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class UserService {

    private final UserDAO userDAO;
    private final Connection cnx;

    public UserService() {
        this.cnx = MyDatabase.getInstance().getCnx();
        this.userDAO = new UserDAO(cnx);
    }

    public void create(User user) throws SQLException {
        userDAO.create(user);
    }

    public Optional<User> findById(int userId) throws SQLException {
        return userDAO.findById(userId);
    }

    public Optional<User> findByEmail(String email) throws SQLException {
        return userDAO.findByEmail(email);
    }

    public List<User> findAll() throws SQLException {
        return userDAO.findAll();
    }

    public List<User> findLatest(int limit) throws SQLException {
        return userDAO.findLatest(limit);
    }

    public void update(User user) throws SQLException {
        userDAO.update(user);
    }

    public void setActive(int userId, boolean active) throws SQLException {
        userDAO.setActive(userId, active);
    }

    public void updatePasswordHash(int userId, String newPasswordHash) throws SQLException {
        userDAO.updatePasswordHash(userId, newPasswordHash);
    }

    public String updateProfilePicture(int userId, File imageFile) throws SQLException, IOException {
        // Define the target directory
        Path targetDirectory = Paths.get("src/main/resources/com/bizhub/images/avatars");
        if (!Files.exists(targetDirectory)) {
            Files.createDirectories(targetDirectory);
        }

        // Generate a unique file name
        String extension = getFileExtension(imageFile.getName());
        String uniqueFileName = UUID.randomUUID().toString() + (extension.isBlank() ? "" : ("." + extension));
        Path targetPath = targetDirectory.resolve(uniqueFileName);

        // Copy the file
        Files.copy(imageFile.toPath(), targetPath, StandardCopyOption.REPLACE_EXISTING);

        // Path stored in DB is relative to resources root, so it can be used for classpath/file fallbacks.
        String relativePath = "com/bizhub/images/avatars/" + uniqueFileName;
        userDAO.updateAvatarUrl(userId, relativePath);

        return relativePath;
    }

    private String getFileExtension(String fileName) {
        int lastIndexOf = fileName == null ? -1 : fileName.lastIndexOf('.');
        if (lastIndexOf == -1 || lastIndexOf == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(lastIndexOf + 1);
    }
}
