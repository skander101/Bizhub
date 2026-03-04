-- Test accounts for integration testing. Password for all: test123
-- Run on BizHub database after bizhub.sql and bizhub_investment_migration.sql.
-- Uses explicit IDs (900-902) to avoid conflicts with real data.

INSERT IGNORE INTO `user` (`user_id`, `email`, `password_hash`, `user_type`, `full_name`, `is_active`,
    `company_name`, `sector`, `company_description`, `website`)
VALUES (900, 'startup@test.com',
    '$2a$12$kpsFFeMPpwhYdty0ub3eReBCSOXFTsXW6K/8.MjKv9chb.mBB5Xam',
    'startup', 'Test Startup', 1,
    'StartupCo', 'Technology', 'A test startup for integration testing', 'https://startupco.test');

INSERT IGNORE INTO `user` (`user_id`, `email`, `password_hash`, `user_type`, `full_name`, `is_active`,
    `investment_sector`, `max_budget`, `years_experience`, `represented_company`)
VALUES (901, 'invest@test.com',
    '$2a$12$kpsFFeMPpwhYdty0ub3eReBCSOXFTsXW6K/8.MjKv9chb.mBB5Xam',
    'investisseur', 'Test Investor', 1,
    'Technology', 500000.00, 5, 'InvestCorp');

INSERT IGNORE INTO `user` (`user_id`, `email`, `password_hash`, `user_type`, `full_name`, `is_active`)
VALUES (902, 'admin@test.com',
    '$2a$12$kpsFFeMPpwhYdty0ub3eReBCSOXFTsXW6K/8.MjKv9chb.mBB5Xam',
    'admin', 'Test Admin', 1);
