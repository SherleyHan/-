package jp.go.mext.enums;

/**
 * 批处理指示状态机:
 *   WAIT (待执行) → RUN (执行中) → END (正常结束) / ABEND (异常终止)
 */
public enum BatchStatus {
    WAIT("WAIT", "等待执行"),
    RUN("RUN", "正在执行"),
    END("END", "正常结束"),
    ABEND("ABEND", "异常终止");

    private final String code;
    private final String description;

    BatchStatus(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    /** 状态机：合法的下一状态 */
    public boolean canTransitionTo(BatchStatus next) {
        if (next == null) return false;
        return switch (this) {
            case WAIT  -> next == RUN || next == ABEND;
            case RUN   -> next == END || next == ABEND;
            case END, ABEND -> false; // 终态
        };
    }
}
