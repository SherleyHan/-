package jp.go.mext.service;

import jp.go.mext.dao.UserDao;
import jp.go.mext.dto.UserDto;
import jp.go.mext.dto.UserSession;
import jp.go.mext.enums.ResultStatus;
import jp.go.mext.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("LoginService 单元测试（Mockito 隔离 DAO）")
class LoginServiceTest {

    @Mock private UserDao userDao;
    @InjectMocks private LoginService loginService;

    private static final String SALT = "mext2026";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(loginService, "salt", SALT);
        ReflectionTestUtils.setField(loginService, "maxRetry", 5);
    }

    private UserDto newUser(String id, String rawPwd, boolean locked, boolean pwdExpired) {
        UserDto u = new UserDto();
        u.setUserId(id);
        u.setUserName("测试用户 " + id);
        u.setPassword(jp.go.mext.util.PasswordEncoder.encode(rawPwd, SALT));
        u.setRiyoushaCd("U");
        u.setResearchType("SY");
        u.setAuthorityCd("USR");
        u.setFailedCount(0);
        u.setAccountLocked(locked);
        u.setPasswordExpired(pwdExpired);
        u.setPasswordUpdateDate(LocalDateTime.now());
        u.setDeleteFlag(false);
        return u;
    }

    @Test
    @DisplayName("正常登录：用户存在 + 密码正确 → 返回 UserSession")
    void loginSuccess() {
        when(userDao.findById("admin")).thenReturn(newUser("admin", "password123", false, false));

        UserSession session = loginService.authenticate("admin", "password123");

        assertNotNull(session);
        assertEquals("admin", session.getUserId());
        verify(userDao, times(1)).resetFailedCount("admin");
        verify(userDao, never()).incrementFailedCount(any(), anyInt());
    }

    @Test
    @DisplayName("用户名为空 → CHECK_ERROR")
    void emptyUserId() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> loginService.authenticate("", "pwd"));
        assertEquals(ResultStatus.CHECK_ERROR, ex.getStatus());
        verify(userDao, never()).findById(any());
    }

    @Test
    @DisplayName("密码为空 → CHECK_ERROR")
    void emptyPassword() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> loginService.authenticate("admin", ""));
        assertEquals(ResultStatus.CHECK_ERROR, ex.getStatus());
    }

    @Test
    @DisplayName("用户不存在 → RESULT_ERROR（不暴露用户是否存在）")
    void userNotFound() {
        when(userDao.findById("ghost")).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> loginService.authenticate("ghost", "anypwd"));
        assertEquals(ResultStatus.RESULT_ERROR, ex.getStatus());
        verify(userDao, never()).incrementFailedCount(any(), anyInt());
    }

    @Test
    @DisplayName("密码错误 → 增加失败计数")
    void wrongPasswordIncrementsCounter() {
        when(userDao.findById("admin")).thenReturn(newUser("admin", "password123", false, false));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> loginService.authenticate("admin", "wrong"));
        assertEquals(ResultStatus.RESULT_ERROR, ex.getStatus());
        verify(userDao, times(1)).incrementFailedCount(eq("admin"), eq(5));
        verify(userDao, never()).resetFailedCount(any());
    }

    @Test
    @DisplayName("账号已锁定 → SESSION_MUKOU，不校验密码")
    void lockedAccount() {
        when(userDao.findById("locked")).thenReturn(newUser("locked", "password123", true, false));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> loginService.authenticate("locked", "password123"));
        assertEquals(ResultStatus.SESSION_MUKOU, ex.getStatus());
        verify(userDao, never()).incrementFailedCount(any(), anyInt());
    }

    @Test
    @DisplayName("密码已过期 → RESULT_ERROR")
    void expiredPassword() {
        UserDto u = newUser("admin", "password123", false, false);
        u.setPasswordUpdateDate(LocalDateTime.now().minusDays(120));
        when(userDao.findById("admin")).thenReturn(u);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> loginService.authenticate("admin", "password123"));
        assertEquals(ResultStatus.RESULT_ERROR, ex.getStatus());
        assertTrue(ex.getMessage().contains("密码已过期"));
    }

    @Test
    @DisplayName("其他用户已登录 → SESSION_MUKOU")
    void otherUserAlreadyLoggedIn() {
        UserSession existing = new UserSession();
        existing.setUserId("other");

        BusinessException ex = assertThrows(BusinessException.class,
                () -> loginService.requireNoOtherSession(existing, "admin"));
        assertEquals(ResultStatus.SESSION_MUKOU, ex.getStatus());
    }
}
