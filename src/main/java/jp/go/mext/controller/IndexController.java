package jp.go.mext.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 入口控制器：访问根路径 / login 都跳登录页
 */
@Controller
public class IndexController {

    @GetMapping({"/", "/login", "/index"})
    public String index() {
        return "forward:/loginZ000";
    }
}
