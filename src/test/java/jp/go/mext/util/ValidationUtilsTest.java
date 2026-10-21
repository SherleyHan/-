package jp.go.mext.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ValidationUtilsTest {

    // ---------- 邮箱校验 ----------
    @ParameterizedTest
    @ValueSource(strings = {
            "user@example.com",
            "user.name+tag@example.co.jp",
            "a@b.c"
    })
    void validEmails(String email) {
        assertTrue(ValidationUtils.isValidEmail(email), email + " 应被识别为合法邮箱");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "plainaddress",
            "@no-local.com",
            "user@",
            "user@.com",
            "user @space.com",
            ""
    })
    void invalidEmails(String email) {
        assertFalse(ValidationUtils.isValidEmail(email), email + " 应被识别为非法邮箱");
    }

    // ---------- 文件名校验 ----------
    @Test
    void validFileName() {
        assertTrue(ValidationUtils.isValidFileName("mail_to_new.txt"));
        assertTrue(ValidationUtils.isValidFileName("report-2026.csv"));
        assertTrue(ValidationUtils.isValidFileName("file_2026.tar.gz"));
    }

    @Test
    void invalidFileName() {
        assertFalse(ValidationUtils.isValidFileName("../etc/passwd"));
        assertFalse(ValidationUtils.isValidFileName("file 中文.txt"));
        assertFalse(ValidationUtils.isValidFileName("file;rm.txt"));
    }

    // ---------- 调查类别校验 ----------
    @Test
    void validSurveyTypes() {
        assertTrue(ValidationUtils.isValidSurveyType("SY"));
        assertTrue(ValidationUtils.isValidSurveyType("HG"));
    }

    @Test
    void invalidSurveyTypes() {
        assertFalse(ValidationUtils.isValidSurveyType("XX"));
        assertFalse(ValidationUtils.isValidSurveyType(""));
        assertFalse(ValidationUtils.isValidSurveyType(null));
    }
}
