package jp.go.mext.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.log4j.Log4j2;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

/**
 * 应用日志拦截：记录请求/响应时长
 */
@Log4j2
public class ApplicationLogInterceptor implements HandlerInterceptor {

    private static final String START_ATTR = "_APP_LOG_START";

    @Override
    public boolean preHandle(HttpServletRequest request, jakarta.servlet.http.HttpServletResponse response, Object handler) {
        request.setAttribute(START_ATTR, System.currentTimeMillis());
        log.info("[REQ] {} {} from {}", request.getMethod(), request.getRequestURI(), request.getRemoteAddr());
        return true;
    }

    @Override
    public void postHandle(HttpServletRequest request, jakarta.servlet.http.HttpServletResponse response, Object handler, ModelAndView modelAndView) {
        Long start = (Long) request.getAttribute(START_ATTR);
        if (start != null) {
            log.info("[RES] {} {} status={} cost={}ms",
                    request.getMethod(), request.getRequestURI(),
                    response.getStatus(), System.currentTimeMillis() - start);
        }
    }
}
