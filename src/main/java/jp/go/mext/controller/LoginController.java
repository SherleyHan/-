package jp.go.mext.controller;

import jp.go.mext.dto.UserSession;
import jp.go.mext.enums.ResultStatus;
import jp.go.mext.exception.BusinessException;
import jp.go.mext.service.LoginService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 登录 Controller：
 * - POST /login              : AJAX 登录预校验
 * - POST /loginConfirm       : 正式确认登录，写入 session
 * - POST /logout             : 注销
 */
@Log4j2
@RestController
@RequiredArgsConstructor
public class LoginController {

    private final LoginService loginService;

    public static final String SESSION_KEY = "userSession";

    /** AJAX 登录预校验 */
    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestParam String userId,
                                                      @RequestParam String password,
                                                      HttpServletRequest req) {
        Map<String, Object> result = new HashMap<>();
        try {
            // hjh 0928 s
            // UserSession existing = (UserSession) req.getSession(false) == null
            //         ? null : (UserSession) req.getSession(false).getAttribute(SESSION_KEY);
            HttpSession httpSession = req.getSession(false);
            UserSession existing = (httpSession == null) ? null
                    : (UserSession) httpSession.getAttribute(SESSION_KEY);
            // hjh 0928 e

            // 预校验：密码不真正写入 session，先让用户确认
            UserSession userSession = loginService.authenticate(userId, password);

            // 检查其它会话
            if (existing != null && existing.getUserId() != null
                    && !existing.getUserId().equals(userId)) {
                result.put("statusCode", ResultStatus.SESSION_MUKOU.getCode());
                result.put("message", "其他用户 [" + existing.getUserId() + "] 已登录，是否继续？");
                result.put("requireConfirm", true);
                return ResponseEntity.ok(result);
            }

            result.put("statusCode", ResultStatus.RESULT_OK.getCode());
            result.put("message", "登录成功");
            result.put("userName", userSession.getUserName());
            return ResponseEntity.ok(result);
        } catch (BusinessException ex) {
            result.put("statusCode", ex.getStatus().getCode());
            result.put("message", ex.getMessage());
            return ResponseEntity.ok(result);
        }
    }

    /** 正式确认登录：写入 session */
    @PostMapping("/loginConfirm")
    public ResponseEntity<Map<String, Object>> loginConfirm(@RequestParam String userId,
                                                             @RequestParam String password,
                                                             HttpServletRequest req) {
        UserSession session = loginService.authenticate(userId, password);
        HttpSession httpSession = req.getSession(true);
        httpSession.setAttribute(SESSION_KEY, session);

        Map<String, Object> result = new HashMap<>();
        result.put("statusCode", ResultStatus.RESULT_OK.getCode());
        result.put("message", "登录成功");
        result.put("redirect", "/menu");
        return ResponseEntity.ok(result);
    }

    /** 注销 */
    @PostMapping("/logout")
    public ResponseEntity<Map<String, Object>> logout(HttpServletRequest req) {
        HttpSession session = req.getSession(false);
        if (session != null) {
            session.removeAttribute(SESSION_KEY);
            try { session.invalidate(); } catch (IllegalStateException ignore) {}
        }
        Map<String, Object> result = new HashMap<>();
        result.put("statusCode", ResultStatus.RESULT_OK.getCode());
        result.put("message", "已注销");
        result.put("redirect", "/login");
        return ResponseEntity.ok(result);
    }
}
