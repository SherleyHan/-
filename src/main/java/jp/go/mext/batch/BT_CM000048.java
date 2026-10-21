package jp.go.mext.batch;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;

/**
 * BT_CM000048：通知邮件发送
 *
 * 简化实现：将通知邮件写入发送历史文件 + 控制台打印
 * 生产环境应替换为 JavaMailSender / 第三方邮件服务
 */
@Log4j2
@Service
@RequiredArgsConstructor
public class BT_CM000048 {

    @Value("${mext.mail.folder}")
    private String mailFolder;

    /**
     * 发送通知邮件（按调查类别读取 mail_to.txt）
     * @param surveyType 调查类别
     * @return 发送成功返回 true
     */
    public boolean sendNotification(String surveyType, String subject, String body) {
        Path mailFile = Paths.get(mailFolder, surveyType, "mail_to.txt");
        if (!Files.exists(mailFile)) {
            log.warn("送信先ファイル不存在: {}", mailFile);
            return false;
        }
        try {
            String content = Files.readString(mailFile);
            Set<String> recipients = parseRecipients(content);
            log.info("=== メール送信シミュレーション ===");
            log.info("件名: {}", subject);
            log.info("宛先: {}", recipients);
            log.info("本文: {}", body);
            log.info("=================================");
            return true;
        } catch (IOException e) {
            log.error("メール送信失敗", e);
            return false;
        }
    }

    private Set<String> parseRecipients(String content) {
        Set<String> result = new HashSet<>();
        for (String s : content.split("[;:\n]")) {
            String trimmed = s.trim();
            if (!trimmed.isEmpty()) result.add(trimmed);
        }
        return result;
    }
}
