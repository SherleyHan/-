package jp.go.mext.exception;

import jp.go.mext.enums.ResultStatus;
import lombok.Getter;

/**
 * 业务异常：触发后由 ControllerExceptionHandler 统一转换为 ResultStatus
 */
@Getter
public class BusinessException extends RuntimeException {
    private final ResultStatus status;

    public BusinessException(ResultStatus status) {
        super(status.getMessage());
        this.status = status;
    }

    public BusinessException(ResultStatus status, String message) {
        super(message);
        this.status = status;
    }
}
