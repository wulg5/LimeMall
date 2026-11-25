package com.limemall.aspect;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.limemall.common.Result;
import com.limemall.entity.SysAdminLog;
import com.limemall.mapper.SysAdminLogMapper;
import com.limemall.security.LoginUser;
import com.limemall.security.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.regex.Pattern;

/**
 * 后台操作审计切面：自动记录 /admin/** 的写操作（POST/PUT/DELETE），
 * 成功与失败均留痕，日志写入失败不影响业务。
 * 敏感字段（密码/私钥）在落库前脱敏，避免凭据泄漏到日志。
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class AdminLogAspect {

    private static final Pattern SENSITIVE_FIELD = Pattern.compile(
            "(\"(?:password|oldPassword|newPassword|privateKey|merchantPrivateKey)\")\\s*:\\s*\"[^\"]*\"");

    private final SysAdminLogMapper adminLogMapper;
    private final ObjectMapper objectMapper;

    @Around("execution(* com.limemall.controller..*.*(..))")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        HttpServletRequest request = currentRequest();
        boolean loggable = request != null
                && request.getRequestURI().startsWith("/admin")
                && !"GET".equalsIgnoreCase(request.getMethod());
        if (!loggable) {
            return joinPoint.proceed();
        }
        long start = System.currentTimeMillis();
        try {
            Object result = joinPoint.proceed();
            record(joinPoint, request, result, null, System.currentTimeMillis() - start);
            return result;
        } catch (Throwable e) {
            record(joinPoint, request, null, e, System.currentTimeMillis() - start);
            throw e;
        }
    }

    private void record(ProceedingJoinPoint joinPoint, HttpServletRequest request,
                        Object result, Throwable error, long costMs) {
        try {
            SysAdminLog entry = new SysAdminLog();
            LoginUser user = SecurityUtils.getLoginUser();
            if (user != null) {
                entry.setAdminId(user.getId());
                entry.setAdminUsername(user.getUsername());
            }
            entry.setModule(joinPoint.getTarget().getClass().getSimpleName());
            entry.setAction(joinPoint.getSignature().getName());
            entry.setMethod(request.getMethod());
            entry.setUri(request.getRequestURI());
            entry.setIp(clientIp(request));
            entry.setParams(serializeArgs(joinPoint.getArgs()));
            entry.setCostMs((int) costMs);
            if (error != null) {
                entry.setStatus(0);
                entry.setErrorMsg(truncate(error.getMessage(), 500));
            } else if (result instanceof Result<?> apiResult) {
                entry.setStatus(apiResult.getCode() == 200 ? 1 : 0);
                if (apiResult.getCode() != 200) {
                    entry.setErrorMsg(truncate(apiResult.getMessage(), 500));
                }
            } else {
                entry.setStatus(1);
            }
            adminLogMapper.insert(entry);
        } catch (Exception e) {
            log.warn("操作日志记录失败: {}", e.getMessage());
        }
    }

    private String serializeArgs(Object[] args) {
        try {
            Object[] filtered = Arrays.stream(args == null ? new Object[0] : args)
                    .filter(arg -> !(arg instanceof MultipartFile)
                            && !(arg instanceof HttpServletRequest)
                            && !(arg instanceof jakarta.servlet.http.HttpServletResponse))
                    .toArray();
            String json = objectMapper.writeValueAsString(filtered);
            json = SENSITIVE_FIELD.matcher(json).replaceAll("$1:\"***\"");
            return truncate(json, 1000);
        } catch (Exception e) {
            return null;
        }
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private String truncate(String value, int max) {
        if (value == null) {
            return null;
        }
        return value.length() <= max ? value : value.substring(0, max);
    }

    private HttpServletRequest currentRequest() {
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attributes == null ? null : attributes.getRequest();
    }
}
