package jp.go.mext.dto;

import jp.go.mext.enums.ResultStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Controller 统一返回结果（与 JSP / AJAX 调用约定一致）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Directions2030Result {
    private String statusCode;
    private String message;

    public static Directions2030Result of(ResultStatus status) {
        return new Directions2030Result(status.getCode(), status.getMessage());
    }

    public static Directions2030Result of(ResultStatus status, String message) {
        return new Directions2030Result(status.getCode(), message);
    }

    public boolean isOk() {
        return ResultStatus.RESULT_OK.getCode().equals(statusCode);
    }
}
