package jp.go.mext.dao;

import jp.go.mext.dto.UserDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UserDao {

    UserDto findById(@Param("userId") String userId);

    void incrementFailedCount(@Param("userId") String userId, @Param("maxRetry") int maxRetry);

    void resetFailedCount(@Param("userId") String userId);

    void lockAccount(@Param("userId") String userId);
}
