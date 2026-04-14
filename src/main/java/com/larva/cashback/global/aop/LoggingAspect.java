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

        String methodName = joinPoint.getSignature().getName();

        log.info("[START] {}", methodName);
        long start = System.currentTimeMillis();

        try {
            Object result = joinPoint.proceed();
            log.info("[END] {} ({}ms)", methodName, System.currentTimeMillis() - start);
            return result;
        } catch (Exception e) {
            log.error("[ERROR] {} - {}", methodName, e.getMessage());
            throw e;
        }
    }
}
