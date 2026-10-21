package jp.go.mext.batch;

import lombok.extern.log4j.Log4j2;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

import java.util.Arrays;

/**
 * BatchAPL 批处理入口
 *
 * 用法:
 *   java -jar mext-survey-system.jar batch BT_MA010101 /var/mext/mail
 *   java -jar mext-survey-system.jar batch BT_CM000048 SY "件名" "本文"
 */
@Log4j2
@SpringBootApplication(scanBasePackages = "jp.go.mext")
public class BatchAplApplication {

    public static void main(String[] args) {
        if (args.length < 1 || !"batch".equals(args[0])) {
            log.error("用法: java -jar mext-survey-system.jar batch <BATCH_ID> [params...]");
            System.exit(1);
        }
        String batchId = args[1];
        String[] batchArgs = Arrays.copyOfRange(args, 2, args.length);

        log.info("BatchAPL 启动: {}", batchId);

        // 关闭 Web 环境，仅启动核心容器
        System.setProperty("spring.main.web-application-type", "none");
        ConfigurableApplicationContext ctx = SpringApplication.run(BatchAplApplication.class, args);

        try {
            int code = dispatch(ctx, batchId, batchArgs);
            System.exit(code);
        } catch (Exception e) {
            log.error("批处理実行例外", e);
            System.exit(99);
        } finally {
            ctx.close();
        }
    }

    private static int dispatch(ConfigurableApplicationContext ctx, String batchId, String[] batchArgs) {
        switch (batchId) {
            case "BT_MA010101": {
                String mailDir = batchArgs.length > 0 ? batchArgs[0] : System.getProperty("java.io.tmpdir") + "/mext-mail";
                BT_MA010101 batch = ctx.getBean(BT_MA010101.class);
                int n = batch.executeOnce(mailDir);
                log.info("BT_MA010101 完了: 成功 {} 件", n);
                return 0;
            }
            case "BT_CM000048": {
                if (batchArgs.length < 3) {
                    log.error("BT_CM00048 缺少参数: <surveyType> <subject> <body>");
                    return 1;
                }
                BT_CM000048 batch = ctx.getBean(BT_CM000048.class);
                boolean ok = batch.sendNotification(batchArgs[0], batchArgs[1], batchArgs[2]);
                return ok ? 0 : -1;
            }
            default:
                log.error("未知批处理: {}", batchId);
                return 1;
        }
    }
}
