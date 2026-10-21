package jp.go.mext.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class NotificationMailDto {
    private Long id;
    private String surveyType;
    private String mailAddress;
    private String fileName;
    private String fileExt;
    private String updateUser;
    private LocalDateTime updateDate;
}
