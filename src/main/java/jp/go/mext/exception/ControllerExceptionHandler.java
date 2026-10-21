package jp.go.mext.exception;

import jp.go.mext.enums.ResultStatus;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.extern.log4j.Log4j2;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import java.util.HashMap;
import java.util.Map;

/**
 * 全局异常处理：
 * 1. BusinessException → 业务错误码（AJAX 返回 JSON；表单返回错误页）
 * 2. SessionExpiredException → 清理 session 并跳登录
 * 3. 其它 → SYSTEM_ERROR
 */
@Log4j2
@ControllerAdvice
public class ControllerExceptionHandler {

    /** AJAX 调用预期返回 JSON */
    @ExceptionHandler(BusinessException.class)
    @ResponseBody
    public Map<String, Object> handleBusiness(BusinessException ex, HttpServletRequest req) {
        log.warn("Business exception: {}", ex.getMessage());
        Map<String, Object> result = new HashMap<>();
        result.put("statusCode", ex.getStatus().getCode());
        result.put("message", ex.getMessage());
        result.put("path", req.getRequestURI());
        return result;
    }

    @ExceptionHandler(SessionExpiredException.class)
    public ModelAndView handleSessionExpired(SessionExpiredException ex, HttpServletRequest req) {
        log.warn("Session expired: {}", ex.getMessage());
        HttpSession session = req.getSession(false);
        if (session != null) {
            session.removeAttribute("userSession");
            session.removeAttribute("menuData");
            session.removeAttribute("headerInfo");
            try { session.invalidate(); } catch (IllegalStateException ignore) {}
        }
        ModelAndView mav = new ModelAndView("redirect:/login");
        mav.addObject("errorMessage", "会话已失效，请重新登录");
        return mav;
    }

    @ExceptionHandler(Exception.class)
    @ResponseBody
    public Map<String, Object> handleAny(Exception ex, HttpServletRequest req) {
        log.error("Unhandled exception", ex);
        Map<String, Object> result = new HashMap<>();
        result.put("statusCode", ResultStatus.SYSTEM_ERROR.getCode());
        result.put("message", "系统异常: " + ex.getMessage());
        result.put("path", req.getRequestURI());
        return result;
    }
}
