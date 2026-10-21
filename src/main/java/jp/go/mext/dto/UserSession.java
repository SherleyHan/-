package jp.go.mext.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * Session 中保存的用户会话信息（对应原系统的 userSession）
 */
@Data
public class UserSession implements Serializable {
    private String userId;
    private String userName;
    private String riyoushaCd;
    private String researchType;
    private String authorityCd;

    public UserSession() {}

    public UserSession(UserDto dto) {
        if (dto != null) {
            this.userId = dto.getUserId();
            this.userName = dto.getUserName();
            this.riyoushaCd = dto.getRiyoushaCd();
            this.researchType = dto.getResearchType();
            this.authorityCd = dto.getAuthorityCd();
        }
    }
}
