package jp.go.mext.interceptor;

import jp.go.mext.controller.LoginController;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.extern.log4j.Log4j2;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 登录拦截：除登录、静态资源、错误页外，未登录跳登录
 */
@Log4j2
public class LoginInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String uri = request.getRequestURI();
        if (uri.endsWith("/login")
            // hjh 0928 s
                || uri.endsWith("/loginZ000")   
            // hjh 0928 e
                || uri.endsWith("/loginConfirm")
                || uri.endsWith("/logout")
                || uri.contains("/css/")
                || uri.contains("/js/")
                || uri.contains("/webjars/")
                || uri.contains("/h2-console")) {
            return true;
        }

        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute(LoginController.SESSION_KEY) == null) {
            log.info("未登录访问 {} → 跳 login", uri);
            // hjh 0928 s
            // response.sendRedirect(request.getContextPath() + "/login");
            response.sendRedirect(request.getContextPath() + "/loginZ000");
            // hjh 0928 e
            return false;
        }
        return true;
    }
}
