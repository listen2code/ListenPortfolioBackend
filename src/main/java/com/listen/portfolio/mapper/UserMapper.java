package com.listen.portfolio.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.listen.portfolio.entity.UserEntity;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface UserMapper extends BaseMapper<UserEntity> {

    @Select("SELECT * FROM users WHERE BINARY(name) = BINARY(#{name}) AND deleted = FALSE LIMIT 1")
    UserEntity findByNameCaseSensitive(@Param("name") String name);

    @Select("SELECT certification_name FROM user_certifications WHERE user_id = #{userId}")
    List<String> findCertificationsByUserId(@Param("userId") Long userId);

    @Insert("INSERT INTO user_certifications (user_id, certification_name) VALUES (#{userId}, #{certificationName})")
    int insertCertification(@Param("userId") Long userId, @Param("certificationName") String certificationName);

    @Delete("DELETE FROM user_certifications WHERE user_id = #{userId}")
    int deleteCertificationsByUserId(@Param("userId") Long userId);
}
