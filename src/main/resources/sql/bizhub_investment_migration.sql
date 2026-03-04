-- =============================================================================
-- BizHub Investment Module – Migration (run on existing BizHub database)
-- =============================================================================
-- Use: Run this script AFTER loading bizhub.sql. It adds only investment-related
--      tables and one compatibility column so the investment module works with
--      the same user/project/investment schema. Compatible with Bizhub-User-Java.
-- =============================================================================

SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
SET NAMES utf8mb4;

-- -----------------------------------------------------------------------------
-- 0. User table columns required by Bizhub-User-Java (may be missing in older
--    bizhub.sql exports). Safe to re-run – uses IF NOT EXISTS / IGNORE.
-- -----------------------------------------------------------------------------
ALTER TABLE `user` ADD COLUMN IF NOT EXISTS `totp_secret` VARCHAR(255) DEFAULT NULL;
ALTER TABLE `user` ADD COLUMN IF NOT EXISTS `face_token` VARCHAR(255) DEFAULT NULL;
ALTER TABLE `user` ADD COLUMN IF NOT EXISTS `admin_role` VARCHAR(50) DEFAULT NULL;
ALTER TABLE `user` ADD COLUMN IF NOT EXISTS `role_start_date` DATE DEFAULT NULL;

-- -----------------------------------------------------------------------------
-- 1. Compatibility: investment.created_at (used by fraud/analytics queries)
--    Skip this block if your BizHub.investment already has created_at.
-- -----------------------------------------------------------------------------
ALTER TABLE `investment`
  ADD COLUMN IF NOT EXISTS `created_at` datetime DEFAULT current_timestamp() AFTER `contract_url`;

-- (If your MySQL version does not support ADD COLUMN IF NOT EXISTS, run once:
--  ALTER TABLE `investment` ADD COLUMN `created_at` datetime DEFAULT current_timestamp() AFTER `contract_url`;
--  and comment out the line above on re-runs.)

-- -----------------------------------------------------------------------------
-- 2. Negotiation (investor <-> startup)
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `negotiation` (
  `negotiation_id` int(11) NOT NULL AUTO_INCREMENT,
  `project_id` int(11) NOT NULL,
  `investor_id` int(11) NOT NULL,
  `startup_id` int(11) NOT NULL,
  `status` enum('open','accepted','rejected','expired') DEFAULT 'open',
  `proposed_amount` decimal(15,2) DEFAULT NULL,
  `final_amount` decimal(15,2) DEFAULT NULL,
  `created_at` datetime DEFAULT current_timestamp(),
  `updated_at` datetime DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  PRIMARY KEY (`negotiation_id`),
  KEY `idx_negotiation_project` (`project_id`),
  KEY `idx_negotiation_investor` (`investor_id`),
  KEY `idx_negotiation_status` (`status`),
  CONSTRAINT `negotiation_project_fk` FOREIGN KEY (`project_id`) REFERENCES `project` (`project_id`) ON DELETE CASCADE,
  CONSTRAINT `negotiation_investor_fk` FOREIGN KEY (`investor_id`) REFERENCES `user` (`user_id`) ON DELETE CASCADE,
  CONSTRAINT `negotiation_startup_fk` FOREIGN KEY (`startup_id`) REFERENCES `user` (`user_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- -----------------------------------------------------------------------------
-- 3. Negotiation messages (chat + AI suggestions)
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `negotiation_message` (
  `message_id` int(11) NOT NULL AUTO_INCREMENT,
  `negotiation_id` int(11) NOT NULL,
  `sender_id` int(11) NOT NULL,
  `message` text NOT NULL,
  `message_type` enum('text','offer','counter_offer','ai_suggestion') DEFAULT 'text',
  `proposed_amount` decimal(15,2) DEFAULT NULL,
  `sentiment` varchar(20) DEFAULT NULL,
  `created_at` datetime DEFAULT current_timestamp(),
  PRIMARY KEY (`message_id`),
  KEY `idx_neg_msg_negotiation` (`negotiation_id`),
  CONSTRAINT `neg_msg_negotiation_fk` FOREIGN KEY (`negotiation_id`) REFERENCES `negotiation` (`negotiation_id`) ON DELETE CASCADE,
  CONSTRAINT `neg_msg_sender_fk` FOREIGN KEY (`sender_id`) REFERENCES `user` (`user_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- -----------------------------------------------------------------------------
-- 4. Deal (payment, contract, signature, email)
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `deal` (
  `deal_id` int(11) NOT NULL AUTO_INCREMENT,
  `negotiation_id` int(11) DEFAULT NULL,
  `project_id` int(11) NOT NULL,
  `buyer_id` int(11) NOT NULL,
  `seller_id` int(11) NOT NULL,
  `amount` decimal(15,2) NOT NULL,
  `stripe_payment_intent_id` varchar(255) DEFAULT NULL,
  `stripe_payment_status` varchar(50) DEFAULT 'pending',
  `stripe_checkout_session_id` varchar(255) DEFAULT NULL,
  `contract_pdf_path` varchar(500) DEFAULT NULL,
  `yousign_signature_request_id` varchar(255) DEFAULT NULL,
  `yousign_status` varchar(50) DEFAULT 'pending',
  `email_sent` tinyint(1) DEFAULT 0,
  `status` enum('pending_payment','paid','pending_signature','signed','completed','cancelled') DEFAULT 'pending_payment',
  `created_at` datetime DEFAULT current_timestamp(),
  `completed_at` datetime DEFAULT NULL,
  PRIMARY KEY (`deal_id`),
  KEY `idx_deal_project` (`project_id`),
  KEY `idx_deal_status` (`status`),
  CONSTRAINT `deal_negotiation_fk` FOREIGN KEY (`negotiation_id`) REFERENCES `negotiation` (`negotiation_id`) ON DELETE SET NULL,
  CONSTRAINT `deal_project_fk` FOREIGN KEY (`project_id`) REFERENCES `project` (`project_id`) ON DELETE CASCADE,
  CONSTRAINT `deal_buyer_fk` FOREIGN KEY (`buyer_id`) REFERENCES `user` (`user_id`) ON DELETE CASCADE,
  CONSTRAINT `deal_seller_fk` FOREIGN KEY (`seller_id`) REFERENCES `user` (`user_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- -----------------------------------------------------------------------------
-- 5. AI analysis (pitch, sentiment, valuation, search)
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `ai_analysis` (
  `analysis_id` int(11) NOT NULL AUTO_INCREMENT,
  `project_id` int(11) DEFAULT NULL,
  `analysis_type` enum('pitch','sentiment','valuation','search') NOT NULL,
  `input_text` text DEFAULT NULL,
  `result_json` text DEFAULT NULL,
  `risk_score` decimal(3,1) DEFAULT NULL,
  `created_at` datetime DEFAULT current_timestamp(),
  PRIMARY KEY (`analysis_id`),
  KEY `idx_ai_analysis_project` (`project_id`),
  KEY `idx_ai_analysis_type` (`analysis_type`),
  CONSTRAINT `ai_analysis_project_fk` FOREIGN KEY (`project_id`) REFERENCES `project` (`project_id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- -----------------------------------------------------------------------------
-- 6. Chatbot conversation (AI assistant history)
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `chatbot_conversation` (
  `conversation_id` int(11) NOT NULL AUTO_INCREMENT,
  `user_id` int(11) NOT NULL,
  `message` text NOT NULL,
  `message_type` enum('user','bot') NOT NULL,
  `intent` varchar(50) DEFAULT NULL,
  `created_at` datetime DEFAULT current_timestamp(),
  PRIMARY KEY (`conversation_id`),
  KEY `idx_chatbot_user` (`user_id`),
  CONSTRAINT `chatbot_user_fk` FOREIGN KEY (`user_id`) REFERENCES `user` (`user_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- -----------------------------------------------------------------------------
-- 7. Fraud alert (AI fraud detection)
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `fraud_alert` (
  `alert_id` int(11) NOT NULL AUTO_INCREMENT,
  `investment_id` int(11) NOT NULL,
  `investor_id` int(11) NOT NULL,
  `alert_level` enum('LOW','MEDIUM','HIGH','CRITICAL') NOT NULL,
  `alert_message` text NOT NULL,
  `created_at` datetime DEFAULT current_timestamp(),
  `status` enum('PENDING','REVIEWED','RESOLVED','FALSE_POSITIVE') DEFAULT 'PENDING',
  PRIMARY KEY (`alert_id`),
  KEY `idx_fraud_alert_investment` (`investment_id`),
  KEY `idx_fraud_alert_investor` (`investor_id`),
  CONSTRAINT `fraud_alert_investment_fk` FOREIGN KEY (`investment_id`) REFERENCES `investment` (`investment_id`) ON DELETE CASCADE,
  CONSTRAINT `fraud_alert_investor_fk` FOREIGN KEY (`investor_id`) REFERENCES `user` (`user_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- -----------------------------------------------------------------------------
-- 8. Fraud analysis (risk score and factors)
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `fraud_analysis` (
  `analysis_id` int(11) NOT NULL AUTO_INCREMENT,
  `investment_id` int(11) NOT NULL,
  `investor_id` int(11) NOT NULL,
  `risk_score` decimal(5,2) NOT NULL,
  `risk_level` enum('LOW','MEDIUM','HIGH','CRITICAL') NOT NULL,
  `analysis_date` datetime DEFAULT current_timestamp(),
  `risk_factors` text DEFAULT NULL,
  PRIMARY KEY (`analysis_id`),
  KEY `idx_fraud_analysis_investment` (`investment_id`),
  KEY `idx_fraud_analysis_investor` (`investor_id`),
  CONSTRAINT `fraud_analysis_investment_fk` FOREIGN KEY (`investment_id`) REFERENCES `investment` (`investment_id`) ON DELETE CASCADE,
  CONSTRAINT `fraud_analysis_investor_fk` FOREIGN KEY (`investor_id`) REFERENCES `user` (`user_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- -----------------------------------------------------------------------------
-- 9. Recommendation analytics (AI recommendations)
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `recommendation_analytics` (
  `analytics_id` int(11) NOT NULL AUTO_INCREMENT,
  `user_id` int(11) NOT NULL,
  `project_id` int(11) NOT NULL,
  `recommendation_score` decimal(5,4) NOT NULL,
  `recommended_at` datetime DEFAULT current_timestamp(),
  `clicked` tinyint(1) DEFAULT 0,
  `invested` tinyint(1) DEFAULT 0,
  `investment_amount` decimal(15,2) DEFAULT NULL,
  `clicked_at` datetime DEFAULT NULL,
  `invested_at` datetime DEFAULT NULL,
  PRIMARY KEY (`analytics_id`),
  KEY `idx_rec_analytics_user` (`user_id`),
  KEY `idx_rec_analytics_project` (`project_id`),
  CONSTRAINT `rec_analytics_user_fk` FOREIGN KEY (`user_id`) REFERENCES `user` (`user_id`) ON DELETE CASCADE,
  CONSTRAINT `rec_analytics_project_fk` FOREIGN KEY (`project_id`) REFERENCES `project` (`project_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- -----------------------------------------------------------------------------
-- 10. Optional seed: sample projects for investment module (only if empty)
--     Uses startup_id from existing BizHub users (e.g. 8 = TestStartup).
--     Uncomment to add 2 sample projects when project table is empty.
-- -----------------------------------------------------------------------------
-- INSERT INTO `project` (`startup_id`, `title`, `description`, `required_budget`, `status`)
-- SELECT 8, 'Sample Startup Project', 'Description for investment demo.', 50000.00, 'pending'
-- FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `project` LIMIT 1);
-- INSERT INTO `project` (`startup_id`, `title`, `description`, `required_budget`, `status`)
-- SELECT 8, 'Second Project', 'Another project for testing.', 100000.00, 'pending'
-- FROM DUAL WHERE (SELECT COUNT(*) FROM `project`) = 1;

-- =============================================================================
-- 8. Enrichment columns for project table
-- =============================================================================
ALTER TABLE `project` ADD COLUMN IF NOT EXISTS `website_url` VARCHAR(500) DEFAULT NULL;
ALTER TABLE `project` ADD COLUMN IF NOT EXISTS `logo_url` VARCHAR(500) DEFAULT NULL;

-- =============================================================================
-- End of BizHub Investment migration
-- =============================================================================
