-- src/main/resources/schema.sql

-- 서비스 정책 조회
CREATE INDEX IF NOT EXISTS idx_service_policy_lookup
    ON service_policy (card_product_code, merchant_code, merchant_category, is_active);

--CASHBACK + isApplied=true 조회
CREATE INDEX IF NOT EXISTS idx_service_application_batch
    ON service_application (service_type, is_applied);

-- 취소 시 연관 ServiceApplication 조회
CREATE INDEX IF NOT EXISTS idx_service_application_sales
    ON service_application (sales_id);

-- Sales 취소건 조회
CREATE INDEX IF NOT EXISTS idx_sales_card_cancelled
    ON sales (card_id, is_cancelled);