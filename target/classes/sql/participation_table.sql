-- Table participation: id_candidature (PK), formation_id (FK), user_id (FK user - participant), date_affectation, remarques
CREATE TABLE IF NOT EXISTS `participation` (
  `id_candidature` int(11) NOT NULL AUTO_INCREMENT,
  `formation_id` int(11) NOT NULL,
  `user_id` int(11) NOT NULL,
  `date_affectation` datetime DEFAULT current_timestamp(),
  `remarques` text DEFAULT NULL,
  PRIMARY KEY (`id_candidature`),
  KEY `idx_participation_formation` (`formation_id`),
  KEY `idx_participation_user` (`user_id`),
  CONSTRAINT `participation_ibfk_1` FOREIGN KEY (`formation_id`) REFERENCES `formation` (`formation_id`) ON DELETE CASCADE,
  CONSTRAINT `participation_ibfk_2` FOREIGN KEY (`user_id`) REFERENCES `user` (`user_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- Si la table existe déjà avec formateur_id, exécuter:
-- ALTER TABLE participation CHANGE formateur_id user_id int(11) NOT NULL;
-- ALTER TABLE participation DROP FOREIGN KEY participation_ibfk_2;
-- ALTER TABLE participation ADD CONSTRAINT participation_ibfk_2 FOREIGN KEY (user_id) REFERENCES user(user_id) ON DELETE CASCADE;
