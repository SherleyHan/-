package jp.go.mext.dao;

import jp.go.mext.dto.NotificationMailDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface NotificationMailDao {

    List<NotificationMailDto> findBySurveyType(@Param("surveyType") String surveyType);

    NotificationMailDto findFileConfig(@Param("surveyType") String surveyType);

    void deleteBySurveyType(@Param("surveyType") String surveyType);

    int insert(NotificationMailDto dto);

    int batchInsert(@Param("list") List<NotificationMailDto> list);

    int countBySurveyType(@Param("surveyType") String surveyType);
}
