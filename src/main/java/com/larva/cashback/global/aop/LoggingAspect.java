package com.larva.cashback.global.aop;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect
@Component
@Slf4j
public class LoggingAspect {
    @Around("execution(* com.larva.cashback.domain..*Service.*(..))")
    public Object logService(ProceedingJoinPoint joinPoint) throws Throwable {

        String methodName = joinPoint.getSignature().getName(); // 실행되는 메서드명 가져옴

        log.info("[START] {}", methodName);
        long start = System.currentTimeMillis();  // 시작 시간 기록

        try {
            Object result = joinPoint.proceed();  // 실제 Service 메서드 실행
            log.info("[END] {} ({}ms)", methodName, System.currentTimeMillis() - start); // 실행시간 로그
            return result;  // 실제 메서드 결과 그대로 반환
        } catch (Exception e) {
            log.error("[ERROR] {} - {}", methodName, e.getMessage()); // 예외 발생 시 ERROR 로그
            throw e;  // 예외 다시 던짐 → GlobalExceptionHandler가 처리
        }
    }
}
