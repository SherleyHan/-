package jp.go.mext.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 菜单页 Controller：
 * - GET /menu : 显示主菜单页面（需已登录）
 *
 * 注意：必须使用 @Controller（而非 @RestController），
 * 否则返回的 "menu" 会被 @ResponseBody 当作响应文本，而不是视图名。
 */
@Log4j2
@Controller
public class MenuController {

    /** 主菜单页 */
    @GetMapping("/menu")
    public String menu(HttpServletRequest req) {
        HttpSession session = req.getSession(false);
        if (session == null || session.getAttribute(LoginController.SESSION_KEY) == null) {
            return "redirect:/login";
        }
        return "menu";
    }
}
