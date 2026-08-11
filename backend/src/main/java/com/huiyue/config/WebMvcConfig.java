package com.huiyue.config;

import com.huiyue.security.AuthInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 注册登录拦截器与本地上传目录静态映射
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final AuthInterceptor authInterceptor;
    private final String uploadDir;
    private final String uploadUrlPrefix;

    public WebMvcConfig(AuthInterceptor authInterceptor,
                        @org.springframework.beans.factory.annotation.Value("${huiyue.upload.dir}") String uploadDir,
                        @org.springframework.beans.factory.annotation.Value("${huiyue.upload.url-prefix}") String uploadUrlPrefix) {
        this.authInterceptor = authInterceptor;
        this.uploadDir = uploadDir;
        this.uploadUrlPrefix = uploadUrlPrefix;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns(
                        "/api/v1/auth/login",
                        "/api/v1/auth/register",
                        "/api/v1/projects",
                        "/api/v1/projects/**"
                );
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler(uploadUrlPrefix + "/**")
                .addResourceLocations("file:" + uploadDir + "/");
    }
}
