package jp.go.mext.dao;

import jp.go.mext.dto.DirectionsExecutionDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface DirectionsExecutionDao {

    DirectionsExecutionDto findById(@Param("executionId") String executionId);

    List<DirectionsExecutionDto> findWaitList();

    int countByStatus(@Param("status") String status);

    int insert(DirectionsExecutionDto dto);

    int updateStatus(@Param("executionId") String executionId,
                     @Param("status") String status,
                     @Param("errorMessage") String errorMessage,
                     @Param("updateUser") String updateUser);

    int updateFilePath(@Param("executionId") String executionId,
                       @Param("filePath") String filePath);
}
