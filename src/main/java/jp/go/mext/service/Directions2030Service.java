package jp.go.mext.service;

import jp.go.mext.dao.DirectionsExecutionDao;
import jp.go.mext.dao.NotificationMailDao;
import jp.go.mext.dto.Directions2030Form;
import jp.go.mext.dto.Directions2030Result;
import jp.go.mext.dto.DirectionsExecutionDto;
import jp.go.mext.dto.NotificationMailDto;
import jp.go.mext.enums.BatchStatus;
import jp.go.mext.enums.ResultStatus;
import jp.go.mext.exception.BusinessException;
import jp.go.mext.util.ValidationUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 通知邮件管理业务：
 * - init: 打开通知邮件管理画面
 * - reflect: 上传文件 → 解析 → 回填到页面
 * - modify: 编辑邮件地址 → 进入确认页
 * - execute: 执行指示 → 写库 + 创建批处理 WAIT 记录 + 生成新邮件文件
 * - compressionDownload: 打包当前邮件地址文件下载
 */
@Log4j2
@Service
@RequiredArgsConstructor
public class Directions2030Service {

    private final NotificationMailDao mailDao;
    private final DirectionsExecutionDao executionDao;
    private final LoginService loginService;

    @Value("${mext.upload.temp-dir}")
    private String tempDir;

    @Value("${mext.mail.folder}")
    private String mailFolder;

    // ---------- 1. init ----------
    public Directions2030Form init(String gamenSyubetu) {
        if (!ValidationUtils.isValidSurveyType(gamenSyubetu)) {
            throw new BusinessException(ResultStatus.CHECK_ERROR, "无效的调查类别: " + gamenSyubetu);
        }
        Directions2030Form form = new Directions2030Form();
        form.setGamenSyubetu(gamenSyubetu);
        form.setGamenId("directions2030_" + gamenSyubetu);
        form.setGamenName("通知邮件管理-" + gamenSyubetu);

        List<NotificationMailDto> list = mailDao.findBySurveyType(gamenSyubetu);
        form.setMailList(list.stream()
                .map(NotificationMailDto::getMailAddress)
                .collect(Collectors.toList()));

        NotificationMailDto config = mailDao.findFileConfig(gamenSyubetu);
        if (config != null) {
            form.setOriginalFileName(config.getFileName());
            form.setFileExt(config.getFileExt());
        }
        return form;
    }

    // ---------- 2. reflect ----------
    public Directions2030Form reflect(String gamenSyubetu, MultipartFile file) {
        Directions2030Form form = init(gamenSyubetu);
        validateUploadFile(file, form.getOriginalFileName(), form.getFileExt());

        Path saved;
        try {
            Files.createDirectories(Paths.get(tempDir));
            saved = Paths.get(tempDir, file.getOriginalFilename() + ".tmp");
            file.transferTo(saved.toFile());
        } catch (IOException e) {
            throw new BusinessException(ResultStatus.RESULT_ERROR, "文件保存失败: " + e.getMessage());
        }

        List<String> parsed = readMailAddresses(saved);
        form.setMailList(parsed);
        return form;
    }

    // ---------- 3. modify ----------
    public Directions2030Form modify(Directions2030Form form) {
        if (form.getMailAddressEdit() == null || form.getMailAddressEdit().isBlank()) {
            throw new BusinessException(ResultStatus.CHECK_ERROR, "邮件地址不能为空");
        }
        List<String> mails = Arrays.stream(form.getMailAddressEdit().split(":"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());

        if (mails.isEmpty()) {
            throw new BusinessException(ResultStatus.CHECK_ERROR, "至少需要一个邮件地址");
        }
        for (String m : mails) {
            if (!ValidationUtils.isValidEmail(m)) {
                throw new BusinessException(ResultStatus.CHECK_ERROR, "邮箱格式不正确: " + m);
            }
        }
        form.setMailList(mails);
        return form;
    }

    // ---------- 4. execute ----------
    @Transactional
    public Directions2030Result execute(Directions2030Form form, String userId) {
        // 校验
        if (form.getMailList() == null || form.getMailList().isEmpty()) {
            throw new BusinessException(ResultStatus.CHECK_ERROR, "邮件地址列表为空");
        }
        for (String m : form.getMailList()) {
            if (!ValidationUtils.isValidEmail(m)) {
                throw new BusinessException(ResultStatus.CHECK_ERROR, "邮箱格式不正确: " + m);
            }
        }

        // 1) 写入 notification_mail 表（先清后插）
        mailDao.deleteBySurveyType(form.getGamenSyubetu());
        NotificationMailDto config = mailDao.findFileConfig(form.getGamenSyubetu());
        String fileName = config != null ? config.getFileName() : "mail_to_new.txt";
        String fileExt = config != null ? config.getFileExt() : "txt";

        List<NotificationMailDto> dtos = form.getMailList().stream().map(addr -> {
            NotificationMailDto d = new NotificationMailDto();
            d.setSurveyType(form.getGamenSyubetu());
            d.setMailAddress(addr);
            d.setFileName(fileName);
            d.setFileExt(fileExt);
            d.setUpdateUser(userId);
            return d;
        }).collect(Collectors.toList());
        mailDao.batchInsert(dtos);

        // 2) 创建 batch 指示 WAIT
        String executionId = loginService.generateExecutionId();
        DirectionsExecutionDto execution = new DirectionsExecutionDto();
        execution.setExecutionId(executionId);
        execution.setSurveyType(form.getGamenSyubetu());
        execution.setBusinessType("NOTIFY_MAIL");
        execution.setStatus(BatchStatus.WAIT.getCode());
        execution.setMailAddress(String.join(":", form.getMailList()));
        execution.setCreateUser(userId);
        executionDao.insert(execution);

        // 3) 生成 mail_to_new.txt 文件
        String filePath = writeMailFile(form.getGamenSyubetu(), fileName, form.getMailList());
        executionDao.updateFilePath(executionId, filePath);

        return Directions2030Result.of(ResultStatus.RESULT_OK,
                "执行成功，指示ID: " + executionId);
    }

    // ---------- 5. compressionDownload ----------
    public File compressionDownload(String gamenSyubetu) {
        NotificationMailDto config = mailDao.findFileConfig(gamenSyubetu);
        if (config == null) {
            throw new BusinessException(ResultStatus.RESULT_ERROR, "无文件配置");
        }
        Path filePath = Paths.get(mailFolder, gamenSyubetu, config.getFileName());
        if (!Files.exists(filePath)) {
            throw new BusinessException(ResultStatus.RESULT_ERROR, "文件不存在: " + filePath);
        }
        // 简化版：直接返回原文件。生产环境应 zip 打包到临时目录再返回。
        return filePath.toFile();
    }

    // ============== 私有方法 ==============

    private void validateUploadFile(MultipartFile file, String expectFileName, String expectExt) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ResultStatus.CHECK_ERROR, "请选择文件");
        }
        String orig = file.getOriginalFilename();
        if (orig == null) {
            throw new BusinessException(ResultStatus.CHECK_ERROR, "文件名为空");
        }
        if (!ValidationUtils.isValidFileName(orig)) {
            throw new BusinessException(ResultStatus.CHECK_ERROR, "文件名包含非法字符");
        }
        if (expectFileName != null && !orig.startsWith(expectFileName.replace(".txt", ""))) {
            throw new BusinessException(ResultStatus.CHECK_ERROR,
                    "文件名必须以 " + expectFileName + " 开头");
        }
        if (expectExt != null && !orig.endsWith("." + expectExt)) {
            throw new BusinessException(ResultStatus.CHECK_ERROR,
                    "文件扩展名必须为 ." + expectExt);
        }
    }

    private List<String> readMailAddresses(Path file) {
        try {
            return Files.readAllLines(file, StandardCharsets.UTF_8).stream()
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .filter(ValidationUtils::isValidEmail)
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new BusinessException(ResultStatus.RESULT_ERROR, "读取文件失败: " + e.getMessage());
        }
    }

    private String writeMailFile(String gamenSyubetu, String fileName, List<String> mails) {
        try {
            Path dir = Paths.get(mailFolder, gamenSyubetu);
            Files.createDirectories(dir);
            Path file = dir.resolve(fileName);
            try (BufferedWriter w = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
                w.write(String.join(":", mails));
            }
            return file.toAbsolutePath().toString();
        } catch (IOException e) {
            throw new BusinessException(ResultStatus.RESULT_ERROR, "写入邮件文件失败: " + e.getMessage());
        }
    }
}
