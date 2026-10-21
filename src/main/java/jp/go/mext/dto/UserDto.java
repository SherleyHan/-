package jp.go.mext.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserDto {
    private String userId;
    private String userName;
    private String password;
    private String riyoushaCd;
    private String researchType;
    private String authorityCd;
    private Integer failedCount;
    private Boolean accountLocked;
    private Boolean passwordExpired;
    private Boolean deleteFlag;
    private LocalDateTime passwordUpdateDate;
}
