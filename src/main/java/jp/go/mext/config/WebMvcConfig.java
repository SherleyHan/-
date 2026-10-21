package jp.go.mext.config;

import jp.go.mext.interceptor.ApplicationLogInterceptor;
import jp.go.mext.interceptor.LoginInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 应用日志拦截器：所有请求
        registry.addInterceptor(new ApplicationLogInterceptor())
                .addPathPatterns("/**");

        // 登录拦截：排除登录、静态资源、h2-console
        registry.addInterceptor(new LoginInterceptor())
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/login", "/loginConfirm", "/logout",
                        "/css/**", "/js/**", "/webjars/**", "/h2-console/**",
                        "/error", "/"
                );
    }

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        // 直接把 /loginZ000 映射到 JSP（与原系统一致）
        registry.addViewController("/loginZ000").setViewName("login/loginZ000");
    }
}
