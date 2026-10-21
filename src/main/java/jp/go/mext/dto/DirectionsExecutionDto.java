package jp.go.mext.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class DirectionsExecutionDto {
    private String executionId;
    private String surveyType;
    private String businessType;
    /** WAIT / RUN / END / ABEND */
    private String status;
    private String mailAddress;
    private String filePath;
    private String errorMessage;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String createUser;
}
