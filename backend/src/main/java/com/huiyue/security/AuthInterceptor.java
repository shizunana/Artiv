package com.huiyue.security;

import com.huiyue.common.BusinessException;
import com.huiyue.common.ResultCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * JWT 登录拦截器：校验 Authorization 头，并把用户写入 ThreadLocal
 */
@Component
public class AuthInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;

    public AuthInterceptor(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        // 公开端点（GET 列表 / 数字 ID 详情）无需登录；写操作一律要求登录
        if (isPublicPath(request)) {
            return true;
        }
        String auth = request.getHeader("Authorization");
        if (auth == null || !auth.startsWith("Bearer ")) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        String token = auth.substring(7);
        if (!jwtUtil.validate(token)) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        Long userId = jwtUtil.parseUserId(token);
        UserContext.set(new LoginUser(userId, null, null));
        return true;
    }

    /** 公开端点判断：仅 GET 的列表页与数字 ID 详情页公开（/mine、/status 等业务路径需登录） */
    private boolean isPublicPath(HttpServletRequest request) {
        if (!"GET".equalsIgnoreCase(request.getMethod())) {
            return false;
        }
        String uri = request.getRequestURI();
        if (uri.startsWith("/api/v1/reviews")) {
            return true;
        }
        String[] segments = uri.split("/");
        // /api/v1/{module} 或 /api/v1/{module}/{数字ID}
        if (segments.length == 4) {
            String module = segments[3];
            return module.equals("projects") || module.equals("stalls") || module.equals("works");
        }
        if (segments.length == 5) {
            String module = segments[3];
            return (module.equals("projects") || module.equals("stalls") || module.equals("works"))
                    && segments[4].matches("\\d+");
        }
        return false;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        UserContext.clear();
    }
}
