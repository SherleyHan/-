package jp.go.mext.util;

import java.util.regex.Pattern;

/**
 * 校验工具：邮箱、文件名、调查类别
 */
public final class ValidationUtils {

    /** 邮箱格式：必须以 @xxx.xx 结尾，且 @ 后不能以 . 开头（xx 至少 1 个字符） */
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9][A-Za-z0-9.-]*\\.[A-Za-z]{1,}$");

    /** 文件名校验：仅允许字母数字下划线短横线点号 */
    private static final Pattern FILE_NAME_PATTERN =
            Pattern.compile("^[A-Za-z0-9_.-]+$");

    /** 调查类别枚举（按真实项目定义） */
    private static final String[] VALID_SURVEY_TYPES = {"SY","HI","HL","TE","TS","TI","TG","HG"};

    private ValidationUtils() {}

    public static boolean isValidEmail(String email) {
        return email != null && EMAIL_PATTERN.matcher(email).matches();
    }

    public static boolean isValidFileName(String name) {
        return name != null && FILE_NAME_PATTERN.matcher(name).matches();
    }

    public static boolean isValidSurveyType(String type) {
        if (type == null) return false;
        for (String t : VALID_SURVEY_TYPES) {
            if (t.equals(type)) return true;
        }
        return false;
    }
}
