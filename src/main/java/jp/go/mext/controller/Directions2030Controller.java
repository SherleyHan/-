package jp.go.mext.controller;

import jp.go.mext.dto.Directions2030Form;
import jp.go.mext.dto.Directions2030Result;
import jp.go.mext.dto.UserSession;
import jp.go.mext.enums.ResultStatus;
import jp.go.mext.exception.BusinessException;
import jp.go.mext.exception.SessionExpiredException;
import jp.go.mext.service.Directions2030Service;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 通知邮件管理 Controller：
 * - POST /directions2030/detail/{gamenSyubetu}         打开编辑画面
 * - POST /directions2030/detail/reflect                上传文件反映
 * - POST /directions2030/detail/modify                 编辑 → 进入确认页
 * - POST /directions2030/detail/execute                执行指示（AJAX）
 * - POST /directions2030/detail/compressionDownload    压缩下载
 */
@Log4j2
@Controller
@RequestMapping("/directions2030/detail")
@RequiredArgsConstructor
public class Directions2030Controller {

    private final Directions2030Service directions2030Service;

    /** 打开通知邮件管理画面 */
    // @RequestMapping("/{gamenSyubetu}")
    @RequestMapping(value="/{gamenSyubetu}", method={RequestMethod.GET, RequestMethod.POST})
    public String detail(@PathVariable String gamenSyubetu,
                         HttpServletRequest req,
                         Model model) {
        requireSession(req);
        Directions2030Form form = directions2030Service.init(gamenSyubetu);
        model.addAttribute("form", form);
        return "directions2030/directions2030";
    }

    /** 上传文件反映到页面 */
    @PostMapping("/reflect")
    public String reflect(@RequestParam("gamenSyubetu") String gamenSyubetu,
                          @RequestParam("uploadFile") org.springframework.web.multipart.MultipartFile uploadFile,
                          HttpServletRequest req,
                          Model model) {
        requireSession(req);
        Directions2030Form form = directions2030Service.reflect(gamenSyubetu, uploadFile);
        model.addAttribute("form", form);
        model.addAttribute("reflected", true);
        return "directions2030/directions2030";
    }

    /** 直接编辑邮件地址 → 进入确认页 */
    @PostMapping("/modify")
    public String modify(@ModelAttribute Directions2030Form form,
                         HttpServletRequest req,
                         Model model) {
        requireSession(req);
        Directions2030Form result = directions2030Service.modify(form);
        model.addAttribute("form", result);
        model.addAttribute("confirmMode", true);
        return "common/commonConfirm";
    }

    /** 执行指示（AJAX） */
    @PostMapping("/execute")
    @ResponseBody
    public Directions2030Result execute(@ModelAttribute Directions2030Form form,
                                        HttpServletRequest req) {
        UserSession session = requireSession(req);
        return directions2030Service.execute(form, session.getUserId());
    }

    /** 压缩下载 */
    @PostMapping("/compressionDownload")
    public ResponseEntity<FileSystemResource> compressionDownload(@RequestParam String gamenSyubetu,
                                                                  HttpServletRequest req) {
        requireSession(req);
        File file = directions2030Service.compressionDownload(gamenSyubetu);
        Path path = Paths.get(file.getAbsolutePath());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + file.getName() + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(new FileSystemResource(path));
    }

    // ---------- 权限/会话辅助 ----------
    private UserSession requireSession(HttpServletRequest req) {
        HttpSession session = req.getSession(false);
        if (session == null) throw new SessionExpiredException();
        Object o = session.getAttribute(LoginController.SESSION_KEY);
        if (!(o instanceof UserSession)) throw new SessionExpiredException();
        UserSession us = (UserSession) o;
        if (us.getResearchType() == null || us.getResearchType().isBlank()) {
            throw new BusinessException(ResultStatus.SESSION_MUKOU, "用户无调查权限");
        }
        return us;
    }
}
