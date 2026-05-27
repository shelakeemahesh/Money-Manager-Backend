package in.maheshshelakee.moneymanager.aspect;

import in.maheshshelakee.moneymanager.service.SystemMonitoringService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
public class RepositoryMonitoringAspect {

    private final SystemMonitoringService monitoringService;

    @Around("execution(* in.maheshshelakee.moneymanager.repository..*.*(..))")
    public Object userRepositoryQuery(ProceedingJoinPoint joinPoint) throws Throwable {
        long start = System.currentTimeMillis();
        try {
            return joinPoint.proceed();
        } finally {
            long duration = System.currentTimeMillis() - start;
            String querySignature = joinPoint.getSignature().toShortString();
            monitoringService.recordQuery(querySignature, duration);
        }
    }
}
