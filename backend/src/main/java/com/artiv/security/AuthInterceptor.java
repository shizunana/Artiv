package com.artiv.security;

import com.artiv.common.BusinessException;
import com.artiv.common.ResultCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import com.artiv.entity.User;
import com.artiv.mapper.UserMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * JWT 登录拦截器：校验 Authorization 头，并把用户写入 ThreadLocal
 */
@Component
public class AuthInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;
    private final UserMapper userMapper;

    public AuthInterceptor(JwtUtil jwtUtil, UserMapper userMapper) {
        this.jwtUtil = jwtUtil;
        this.userMapper = userMapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        // 公开端点允许匿名访问；若携带 token，仍解析身份供详情页做发布者/历史买家鉴权。
        if (isPublicPath(request)) {
            String optionalAuth = request.getHeader("Authorization");
            if (optionalAuth != null && optionalAuth.startsWith("Bearer ")) {
                String optionalToken = optionalAuth.substring(7);
                if (!jwtUtil.validate(optionalToken)) {
                    throw new BusinessException(ResultCode.UNAUTHORIZED);
                }
                UserContext.set(new LoginUser(jwtUtil.parseUserId(optionalToken), null, null));
            }
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
        if (request.getRequestURI().startsWith("/api/v1/admin/")) {
            User user = userMapper.selectById(userId);
            if (user == null || !Integer.valueOf(3).equals(user.getRole())
                    || !Integer.valueOf(0).equals(user.getStatus())
                    || Integer.valueOf(1).equals(user.getDeleted())) {
                throw new BusinessException(ResultCode.NO_PERMISSION.getCode(), "仅正常状态的管理员可访问后台");
            }
            UserContext.set(new LoginUser(userId, user.getUsername(), user.getRole()));
        } else if (request.getRequestURI().equals("/api/v1/guide")
                || request.getRequestURI().startsWith("/api/v1/guide/")) {
            // 向导开放不依赖默认工作台或画师认证，但逐请求检查实际账号状态。
            User user = userMapper.selectById(userId);
            if (user == null || !java.util.Set.of(1, 2, 3).contains(user.getRole() == null ? 0 : user.getRole())
                    || !Integer.valueOf(0).equals(user.getStatus())
                    || !Integer.valueOf(0).equals(user.getDeleted())) {
                throw new BusinessException(ResultCode.NO_PERMISSION.getCode(), "仅正常状态的登录账号可使用艺绘向导");
            }
            UserContext.set(new LoginUser(userId, user.getUsername(), user.getRole()));
        } else {
            UserContext.set(new LoginUser(userId, null, null));
        }
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
        if (uri.equals("/api/v1/users/artists")) {
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
        // 企划详情中的应征画师为公开展示信息。
        if (segments.length == 6) {
            if (segments[3].equals("projects")
                    && segments[4].matches("\\d+")
                    && segments[5].equals("applications")) {
                return true;
            }
            return segments[3].equals("users")
                    && segments[4].matches("\\d+")
                    && (segments[5].equals("public-profile")
                    || segments[5].equals("works")
                    || segments[5].equals("stalls")
                    || segments[5].equals("projects")
                    || segments[5].equals("reviews"));
        }
        return false;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        UserContext.clear();
    }
}
