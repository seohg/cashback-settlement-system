-- ============================================================
-- 인덱스 설계
-- ============================================================

-- 서비스 심사 정책 조회 최적화
-- Consumer가 매출건마다 정책 조회 → 가장 빈번한 쿼리
-- Redis 캐시 미스 시 DB fallback에서 인덱스 스캔으로 처리
CREATE INDEX IF NOT EXISTS idx_service_policy_lookup
    ON service_policy (card_product_code, merchant_code, merchant_category, is_active);

-- 배치 조회 최적화
-- 월별 캐시백 지급: serviceType=CASHBACK + isApplied=true 조회
CREATE INDEX IF NOT EXISTS idx_service_application_batch
    ON service_application (service_type, is_applied);

-- 취소 시 연관 ServiceApplication 조회
CREATE INDEX IF NOT EXISTS idx_service_application_sales
    ON service_application (sales_id);

-- Sales 카드별 취소건 조회
CREATE INDEX IF NOT EXISTS idx_sales_card_cancelled
    ON sales (card_id, is_cancelled);
