package jp.go.mext.batch;

import jp.go.mext.enums.BatchStatus;
import jp.go.mext.enums.ResultStatus;
import jp.go.mext.exception.BusinessException;
import jp.go.mext.dao.DirectionsExecutionDao;
import jp.go.mext.dto.DirectionsExecutionDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;

/**
 * BT_MA010101：指示执行主控
 *
 * 1. 查询所有 status = WAIT 的指示
 * 2. 检查同一作业是否正在执行（status = RUN）
 * 3. 找到对应调查类别的邮件目录
 * 4. 将 mail_to_new.txt 重命名为 mail_to.txt
 * 5. 更新指示状态为 END；异常时更新为 ABEND
 */
@Log4j2
@Service
@RequiredArgsConstructor
public class BT_MA010101 {

    private final DirectionsExecutionDao executionDao;

    /** 批处理执行一次（一次处理所有 WAIT 指示） */
    @Transactional
    public int executeOnce(String mailBaseDir) {
        List<DirectionsExecutionDto> waitList = executionDao.findWaitList();
        log.info("BT_MA010101 取得 WAIT 指示 {} 件", waitList.size());

        int success = 0;
        for (DirectionsExecutionDto exec : waitList) {
            if (!canStart(exec)) {
                log.warn("実行不可：executionId={}", exec.getExecutionId());
                continue;
            }
            try {
                processOne(exec, mailBaseDir);
                success++;
            } catch (Exception e) {
                log.error("指示実行失敗", e);
                executionDao.updateStatus(exec.getExecutionId(),
                        BatchStatus.ABEND.getCode(),
                        e.getMessage(),
                        "BATCH");
            }
        }
        return success;
    }

    /** 检查是否可以执行：不存在 RUN 中的同调查类型指示 */
    private boolean canStart(DirectionsExecutionDto exec) {
        int runningCount = executionDao.countByStatus(BatchStatus.RUN.getCode());
        return runningCount == 0;
    }

    private void processOne(DirectionsExecutionDto exec, String mailBaseDir) throws IOException {
        // RUN
        executionDao.updateStatus(exec.getExecutionId(),
                BatchStatus.RUN.getCode(),
                null,
                "BATCH");

        // 处理通知邮件：将 mail_to_new.txt 替换为 mail_to.txt
        if ("NOTIFY_MAIL".equals(exec.getBusinessType())) {
            Path baseDir = Paths.get(mailBaseDir, exec.getSurveyType());
            Path newFile = baseDir.resolve("mail_to_new.txt");
            Path curFile = baseDir.resolve("mail_to.txt");

            if (!Files.exists(newFile)) {
                throw new BusinessException(ResultStatus.RESULT_ERROR,
                        "新邮件文件不存在: " + newFile);
            }
            Files.move(newFile, curFile, StandardCopyOption.REPLACE_EXISTING);
            log.info("メールファイル差し替え完了: {}", curFile);
        }

        // END
        executionDao.updateStatus(exec.getExecutionId(),
                BatchStatus.END.getCode(),
                null,
                "BATCH");
    }
}
