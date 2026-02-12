-- Migration: remplacer formateur_id par user_id dans participation
-- Exécuter si la table participation existe déjà avec formateur_id

ALTER TABLE participation CHANGE formateur_id user_id int(11) NOT NULL;
ALTER TABLE participation DROP FOREIGN KEY participation_ibfk_2;
ALTER TABLE participation ADD CONSTRAINT participation_ibfk_2 FOREIGN KEY (user_id) REFERENCES user(user_id) ON DELETE CASCADE;
