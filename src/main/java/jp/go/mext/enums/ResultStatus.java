package jp.go.mext.enums;

/**
 * 业务统一返回状态码
 *
 * 与原系统保持一致:
 * - RESULT_OK: 正常处理成功
 * - RESULT_ERROR: 业务处理失败
 * - CHECK_ERROR: 参数校验失败
 * - SESSION_MUKOU: Session 失效或无权限
 */
public enum ResultStatus {
    RESULT_OK("0", "处理成功"),
    RESULT_ERROR("-1", "业务处理失败"),
    CHECK_ERROR("1", "参数校验失败"),
    SESSION_MUKOU("90", "会话失效或无权限"),
    SYSTEM_ERROR("99", "系统异常");

    private final String code;
    private final String message;

    ResultStatus(String code, String message) {
        this.code = code;
        this.message = message;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public boolean isSuccess() {
        return this == RESULT_OK;
    }
}
