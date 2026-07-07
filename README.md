# cashback-settlement-system

## 카드 서비스 심사 및 캐시백 정산 시스템

Spring Boot + Kafka + Redis + Spring Batch를 활용한 대용량 결제 후행 처리 시스템입니다.
동시성 제어, 메시지 순서 보장, 배치 안정성에 집중했습니다.

## 기술 스택

| 기술 | 선택 이유 | 대안 검토 |
|------|---------|---------|
| Spring Batch | skip 정책 + 실패 지점 재시작 + 청크 처리 | @Scheduled(skip 없음), Quartz(재시작 없음) |
| Kafka | offset 기반 재처리 + 파티션 순서 보장 + Consumer Group 병렬처리 | RabbitMQ(소비 후 삭제), DB Polling(DB 부하) |
| Redis | 분산 환경 중앙 캐시 + 정책 변경 시 단일 무효화 | Local Cache(서버별 불일치), DB 직접 조회(부하) |
| JPA | 도메인 모델 설계 + 변경 감지 + 연관관계 관리 | - |



## 시스템 아키텍처

```
[결제 API]
결제 발생 → 카드 상태·한도 체크(비관적 락) → Sales 생성 → Kafka 발행(sales.created)

[Consumer: 서비스 심사]
매출건 수신 → Redis 정책 조회(miss → DB 조회 후 캐싱)
→ merchantCode + merchantCategory 조건 필터링 → ServiceApplication 생성

[취소 API]
취소건 Sales 추가 → 연관 ServiceApplication isApplied=false → Kafka 발행(sales.cancelled)

[Consumer: 취소 처리]
originalSalesId로 원래 Sales 조회
→ Sales 없으면: 순서 역전 판단 → Retry(3회, 1초 간격) → DLT 이동
→ Sales 있고 SA 없으면: 서비스 미적용 건 → 정상 종료
→ SA 있으면: isApplied=false 처리

[Spring Batch: 월별 캐시백 지급]
CASHBACK + isApplied=true 청크 조회(1000건)
→ Sales.isCancelled 이중 검증 → 금액 분할(999만원 초과) → skip(주소 미등록) → PAID 처리
```


## ERD

```
Member ──< Card ──< Sales ──< ServiceApplication
                    Sales <── Sales (자기참조: 취소건)
ServicePolicy (독립 테이블: cardProductCode + merchantCode + merchantCategory 조건)
```

| 테이블 | 주요 컬럼 | 설명 |
|--------|---------|------|
| Sales | merchantCode, merchantCategory, originalSalesId | 취소건을 별도 row로 관리 (원본 불변) |
| ServiceApplication | isApplied, paymentStatus, benefitAmount | 심사 결과 및 배치 처리 상태 |
| ServicePolicy | merchantCode(nullable), merchantCategory(nullable), priority | null이면 전체 적용, priority로 우선순위 결정 |


## 핵심 설계 결정

### 1. 취소건 별도 row 관리

원본 매출내역을 수정하지 않고 취소건을 별도 행으로 추가합니다. 원장 불변 원칙을 지켜 감사 추적과 데이터 일관성을 확보합니다.

```java
// isCancelled는 DB 컬럼 대신 메서드로 표현 — originalSales 존재 여부로 판단
public boolean isCancelled() {
    return this.originalSales != null;
}
```

### 2. merchantCode + merchantCategory 독립 조건 분리

특정 가맹점만 적용하거나 특정 업종 전체에 적용하는 등 두 조건이 독립적으로 동작합니다. null이면 전체 적용으로 처리해 유연한 정책 설정이 가능합니다.

```java
private boolean matchesMerchantCode(ServicePolicy policy, String merchantCode) {
    return policy.getMerchantCode() == null || policy.getMerchantCode().equals(merchantCode);
}
```

### 3. 비관적 락으로 한도 동시성 처리

같은 카드 동시 결제 시 한도 체크와 usedAmount 업데이트에서 Race Condition이 발생합니다. Card 조회 시 비관적 락을 적용해 순차 처리를 보장합니다.

```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("SELECT c FROM Card c WHERE c.id = :id")
Optional<Card> findByIdWithLock(@Param("id") Long id);
```

### 4. TransactionalEventListener로 Kafka 발행 시점 제어

DB 저장 실패 시 Kafka 메시지가 발행되는 문제를 방지합니다. 트랜잭션 커밋 이후에만 발행해 데이터 정합성을 보장합니다.

```java
@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
public void handleSalesCreated(SalesCreatedEvent event) {
    kafkaTemplate.send(TOPIC_SALES_CREATED, key, salesEvent);
}
```

### 5. ServicePolicyCacheService 분리 (@Cacheable self-invocation 해결)

같은 클래스 내부에서 `@Cacheable` 메서드를 호출하면 Spring AOP 프록시를 우회해 캐시가 동작하지 않습니다. 캐시 로직을 별도 빈으로 분리해 해결했습니다.

```java
// Before: 내부 호출 → AOP 프록시 우회 → 캐시 미작동
List<ServicePolicy> policies = getCachedPolicies(cardProductCode);

// After: 외부 빈 호출 → AOP 프록시 통과 → 캐시 정상 작동
List<ServicePolicy> policies = servicePolicyCacheService.getPolicies(cardProductCode);
```

### 6. 배치 이중 검증

Kafka 취소 처리 실패 건을 배치에서 최종 보완합니다. ItemProcessor에서 Sales.isCancelled를 재확인해 과지급을 차단하고 paymentStatus=SKIPPED로 기록해 운영 추적이 가능합니다.

```java
if (item.getSales().isCancelled()) {
    item.skip();  // paymentStatus=SKIPPED로 DB 기록
    return null;
}
```

<br>

## 트러블슈팅

### 1. 캐시백 금액 자릿수 초과

**문제** benefitAmount가 특정 금액을 초과하는 경우 배치 오류 발생.

**원인** 캐시백 금액 컬럼 최대값 설계 오류로 초과 금액 처리 불가.

**해결** ItemProcessor에서 초과 시 자동 분할. 분할된 건은 각각 별도 ServiceApplication으로 생성합니다.

```java
if (item.getBenefitAmount() > MAX_BENEFIT_AMOUNT) {
    return splitBenefit(item);
}
```

**결과** 배치 오류 제거. 수동 보정 불필요.

---

### 2. 취소건 캐시백 과지급

**문제** 취소된 결제건에 캐시백이 지급되는 문제.

**원인** 배치가 isApplied=true만 확인하고 연관 Sales의 취소 여부를 재확인하지 않음.

**해결** ItemProcessor에서 Sales.isCancelled 이중 검증 추가.

**결과** 캐시백 과지급 차단. paymentStatus=SKIPPED 기록으로 운영 추적 가능.

---

### 3. 후행 처리 중 취소건 순서 역전

**문제** 병렬 처리 중 취소건이 정상건보다 먼저 처리되어 취소됐는데 캐시백이 적용된 상태로 남음.

**원인** 병렬처리 순서 미보장으로 취소건 처리 시점에 ServiceApplication이 아직 없음.

**해결** originalSalesId로 원래 Sales를 조회해 없으면 순서 역전으로 판단하고 Retry. 3회 실패 시 DLT 이동 후 배치 이중 검증으로 최종 보완합니다.

```java
if (originalSales.isEmpty()) {
    throw new IllegalStateException("Original sales not found: " + event.getOriginalSalesId());
}
```

**결과** 순서 역전 문제를 재처리 의존 없이 구조적으로 해결.




## 로컬 실행

```bash
# 1. 인프라 실행
docker-compose up -d

# 2. 애플리케이션 실행
./gradlew bootRun

# 3. API 문서 확인
http://localhost:8080/swagger-ui.html
```

**Docker Compose 구성**
- MySQL 8.0 (port 3307)
- Kafka + Zookeeper (port 9092)
- Redis 7 (port 6380)
- Kafka 토픽 자동 생성 (sales.created, sales.cancelled, DLT 포함)