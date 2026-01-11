package tk.jaooo.osmanager.aspect;

import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import tk.jaooo.osmanager.model.Tecnico;

import java.util.Arrays;

@Aspect
@Component
public class AuditLoggerAspect {

    private static final Logger logger = LoggerFactory.getLogger("AUDIT_LOG");

    @Around("execution(* tk.jaooo.osmanager.controller..*(..))")
    public Object logUserAction(ProceedingJoinPoint joinPoint) throws Throwable {
        long start = System.currentTimeMillis();

        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
        String method = request.getMethod();
        String uri = request.getRequestURI();
        String remoteAddr = request.getRemoteAddr();

        String username = "ANONYMOUS";
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof Tecnico tecnico) {
            username = tecnico.getUsername() + " (ID:" + tecnico.getId() + ")";
            MDC.put("user", tecnico.getUsername());
        }

        String args = Arrays.toString(joinPoint.getArgs());

        logger.info("REQ_START | User: {} | IP: {} | {} {}", username, remoteAddr, method, uri);

        Object proceed;
        try {
            proceed = joinPoint.proceed();
        } catch (Throwable e) {
            long executionTime = System.currentTimeMillis() - start;
            logger.error("REQ_FAIL  | User: {} | {} {} | Time: {}ms | Error: {}",
                    username, method, uri, executionTime, e.getMessage());
            throw e;
        }

        long executionTime = System.currentTimeMillis() - start;
        logger.info("REQ_END   | User: {} | {} {} | Time: {}ms | Status: SUCCESS",
                username, method, uri, executionTime);

        MDC.clear();
        return proceed;
    }
}
