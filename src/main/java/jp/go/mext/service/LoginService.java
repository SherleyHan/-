package jp.go.mext.service;

import jp.go.mext.dao.UserDao;
import jp.go.mext.dto.UserDto;
import jp.go.mext.dto.UserSession;
import jp.go.mext.enums.ResultStatus;
import jp.go.mext.exception.BusinessException;
import jp.go.mext.exception.SessionExpiredException;
import jp.go.mext.util.PasswordEncoder;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 登录业务：
 * 1. 根据用户 ID 查询用户主数据
 * 2. 判断账号是否已经锁定
 * 3. 对输入密码加密
 * 4. 与数据库中的密码比较
 * 5. 密码错误时累计失败次数
 * 6. 达到配置的失败次数上限后锁定账号
 * 7. 判断密码是否过期
 * 8. 验证通过后写入用户会话信息
 */
@Log4j2
@Service
@RequiredArgsConstructor
public class LoginService {

    private final UserDao userDao;

    @Value("${mext.password.salt}")
    private String salt;

    @Value("${mext.login.max-retry:5}")
    private int maxRetry;

    /** 密码有效期（天） */
    private static final int PASSWORD_VALID_DAYS = 90;

    /**
     * 校验用户名密码
     * @return 登录成功后返回 UserSession
     * @throws BusinessException 用户不存在 / 密码错误 / 账号锁定 / 密码过期
     */
    @Transactional
    public UserSession authenticate(String userId, String rawPassword) {
        if (userId == null || userId.isBlank()) {
            throw new BusinessException(ResultStatus.CHECK_ERROR, "用户ID不能为空");
        }
        if (rawPassword == null || rawPassword.isBlank()) {
            throw new BusinessException(ResultStatus.CHECK_ERROR, "密码不能为空");
        }

        UserDto user = userDao.findById(userId);
        if (user == null) {
            log.info("用户不存在: {}", userId);
            throw new BusinessException(ResultStatus.RESULT_ERROR, "用户ID或密码错误");
        }

        // 2. 锁定检查
        if (Boolean.TRUE.equals(user.getAccountLocked())) {
            log.warn("账号已锁定: {}", userId);
            throw new BusinessException(ResultStatus.SESSION_MUKOU, "账号已被锁定，请联系管理员");
        }

        // 3. 密码校验
        if (!PasswordEncoder.matches(rawPassword, salt, user.getPassword())) {
            log.info("密码错误: {}", userId);
            userDao.incrementFailedCount(userId, maxRetry);
            throw new BusinessException(ResultStatus.RESULT_ERROR, "用户ID或密码错误");
        }

        // 7. 密码过期
        if (isPasswordExpired(user)) {
            log.warn("密码已过期: {}", userId);
            throw new BusinessException(ResultStatus.RESULT_ERROR, "密码已过期，请修改密码");
        }

        // 登录成功：重置失败计数
        userDao.resetFailedCount(userId);
        return new UserSession(user);
    }

    /** 检查会话中是否已有用户 */
    public void requireNoOtherSession(UserSession currentSession, String loginUserId) {
        if (currentSession != null
                && currentSession.getUserId() != null
                && !loginUserId.equals(currentSession.getUserId())) {
            throw new BusinessException(ResultStatus.SESSION_MUKOU,
                    "其他用户已登录，请先注销: " + currentSession.getUserId());
        }
    }

    private boolean isPasswordExpired(UserDto user) {
        if (Boolean.TRUE.equals(user.getPasswordExpired())) return true;
        if (user.getPasswordUpdateDate() == null) return false;
        return user.getPasswordUpdateDate()
                .plusDays(PASSWORD_VALID_DAYS)
                .isBefore(LocalDateTime.now());
    }

    /** 生成一个唯一执行 ID（用于指示） */
    public String generateExecutionId() {
        return "EXE_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8);
    }
}
