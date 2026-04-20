INSERT INTO service_policy (card_product_code, service_type, merchant_category, condition_amount, benefit_rate, max_benefit_amount, priority, is_active, valid_from, valid_to, created_at, updated_at)
VALUES
-- PROD_001: 음식점 캐시백 (5%)
('PROD_001', 'CASHBACK', 'FOOD', 10000, 0.05, 50000, 1, true, '2024-01-01 00:00:00', '2099-12-31 23:59:59', NOW(), NOW()),
-- PROD_001: 전체 가맹점 포인트 (1%)
('PROD_001', 'POINT', NULL, 0, 0.01, 10000, 2, true, '2024-01-01 00:00:00', '2099-12-31 23:59:59', NOW(), NOW()),
-- PROD_001: 할인 (청구할인 3%)
('PROD_001', 'BILLING_DISCOUNT', 'SHOPPING', 50000, 0.03, 30000, 1, true, '2024-01-01 00:00:00', '2099-12-31 23:59:59', NOW(), NOW()),
-- PROD_002: 주유 캐시백 (7%)
('PROD_002', 'CASHBACK', 'GAS', 30000, 0.07, 70000, 1, true, '2024-01-01 00:00:00', '2099-12-31 23:59:59', NOW(), NOW()),
-- PROD_002: 전체 가맹점 캐시백 (2%)
('PROD_002', 'CASHBACK', NULL, 0, 0.02, 20000, 2, true, '2024-01-01 00:00:00', '2099-12-31 23:59:59', NOW(), NOW()),
-- PROD_002: 할부 할인
('PROD_002', 'INSTALLMENT_DISCOUNT', NULL, 100000, 0.01, 5000, 3, true, '2024-01-01 00:00:00', '2099-12-31 23:59:59', NOW(), NOW()),
-- PROD_003: 비활성 정책 (테스트용)
('PROD_003', 'CASHBACK', 'FOOD', 0, 0.10, 100000, 1, false, '2024-01-01 00:00:00', '2099-12-31 23:59:59', NOW(), NOW()),
-- PROD_003: 기간 만료 정책 (테스트용)
('PROD_003', 'POINT', NULL, 0, 0.05, 50000, 1, true, '2024-01-01 00:00:00', '2024-12-31 23:59:59', NOW(), NOW()),
-- PROD_001: 카페 캐시백
('PROD_001', 'CASHBACK', 'CAFE', 5000, 0.10, 30000, 1, true, '2024-01-01 00:00:00', '2099-12-31 23:59:59', NOW(), NOW()),
-- PROD_002: 쇼핑 포인트
('PROD_002', 'POINT', 'SHOPPING', 20000, 0.03, 15000, 1, true, '2024-01-01 00:00:00', '2099-12-31 23:59:59', NOW(), NOW());