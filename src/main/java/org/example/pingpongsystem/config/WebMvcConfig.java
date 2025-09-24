package org.example.pingpongsystem.config;

import org.example.pingpongsystem.interceptor.ActivationInterceptor;
import org.example.pingpongsystem.utility.Utility;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
    private final ActivationInterceptor activationInterceptor;

    public WebMvcConfig(ActivationInterceptor activationInterceptor) {
        this.activationInterceptor = activationInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(activationInterceptor)
                .addPathPatterns("/**") // 拦截所有请求
                .excludePathPatterns(
                        "/404", // 排除错误页
                        // 关键：排除超管激活相关接口，确保未激活时能访问
                        "/super_admin/login",
                        "/super_admin/pay_service_fee",
                        "/super_admin/activate_system",
                        "/super_admin/verify_activation",
                        "/token/info",
                        "/token/logout",
                        // 排除静态资源接口（避免前端加载图片被拦截）
                        "/coach-photos/**",
                        "/user-avatars/**"

                );
    }

    // 配置静态资源映射：将本地图片目录映射为URL路径
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 本地图片存储路径（从Utility中获取）
        String coachPhotoPath = Utility.CoachPhotoPath.replace("\\", "/");
        System.out.println("file:" + coachPhotoPath + "/");
        // 映射规则：前端访问 /coach-photos/** 路径时，实际访问本地CoachPhoto目录
        registry.addResourceHandler("/coach-photos/**")
                .addResourceLocations("file:" + coachPhotoPath + "/");

        // 2. 新增个人头像映射（使用单独目录，避免与教练照片混淆）
        String avatarPath = Utility.AvatarPath.replace("\\", "/"); // 需在Utility中定义头像本地目录
        registry.addResourceHandler("/user-avatars/**") // 前端访问前缀
                .addResourceLocations("file:" + avatarPath + "/"); // 本地头像存储目录
    }

    // 配置跨域：允许前端9528端口访问
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**") // 所有接口都允许跨域
                .allowedOrigins("http://localhost:9528") // 前端地址
                .allowedMethods("GET", "POST", "PUT", "DELETE") // 允许的请求方法
                .allowedHeaders("*") // 允许的请求头
                .allowCredentials(true); // 允许携带cookie
    }
}
