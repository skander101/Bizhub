-- Seed data for BizHub mini-project (Users + Formation + Avis)
-- Tested against BizHub schema from BizHub.sql

-- (Optional) Clean existing rows (be careful in real environments)
-- DELETE FROM avis;
-- DELETE FROM formation;
-- DELETE FROM `user`;

-- USERS
-- Password plaintext for all these users: admin123
-- Hash generated with BCrypt cost=12 (compatible with jBCrypt)
SET @pwd = '$2a$12$C8YcS38Lh0hgJvPJECEZxOIGKsn9tI8c1Q6MC6w8eVZ3uG6r1yB7y';

INSERT INTO `user` (email, password_hash, user_type, full_name, phone, company_name, sector, is_active)
VALUES
 ('admin@bizhub.tn', @pwd, 'admin', 'Admin BizHub', '22110000', 'BizHub', 'Tech', 1),
 ('trainer@bizhub.tn', @pwd, 'formateur', 'Trainer One', '22110001', NULL, NULL, 1),
 ('user1@bizhub.tn', @pwd, 'startup', 'Startup User', '22110002', 'StartupCo', 'SaaS', 1),
 ('inactive@bizhub.tn', @pwd, 'startup', 'Inactive User', NULL, NULL, NULL, 0);

-- FORMATIONS (minimal columns used by the mini-project)
-- Note: BizHub.sql defines AUTO_INCREMENT formation_id; your excerpt used INT PRIMARY KEY.
-- This insert works for both if AUTO_INCREMENT is present.
INSERT INTO formation (title, trainer_id)
SELECT 'Java JDBC Basics', u.user_id
FROM `user` u WHERE u.email='trainer@bizhub.tn'
LIMIT 1;

INSERT INTO formation (title, trainer_id)
SELECT 'Advanced JavaFX', u.user_id
FROM `user` u WHERE u.email='trainer@bizhub.tn'
LIMIT 1;

-- AVIS
INSERT INTO avis (reviewer_id, formation_id, rating, comment)
SELECT ru.user_id, f.formation_id, 5, 'Excellent formation!'
FROM `user` ru, formation f
WHERE ru.email='user1@bizhub.tn' AND f.title='Java JDBC Basics'
LIMIT 1;

INSERT INTO avis (reviewer_id, formation_id, rating, comment)
SELECT ru.user_id, f.formation_id, 4, 'Très bon contenu, un peu rapide.'
FROM `user` ru, formation f
WHERE ru.email='user1@bizhub.tn' AND f.title='Advanced JavaFX'
LIMIT 1;

