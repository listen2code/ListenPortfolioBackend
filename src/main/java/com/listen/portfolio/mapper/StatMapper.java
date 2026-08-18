package com.listen.portfolio.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.listen.portfolio.entity.StatEntity;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface StatMapper extends BaseMapper<StatEntity> {

    @Select("SELECT tag_name FROM stat_tags WHERE stat_id = #{statId}")
    List<String> findTagsByStatId(@Param("statId") Long statId);

    @Insert("INSERT INTO stat_tags (stat_id, tag_name) VALUES (#{statId}, #{tagName})")
    int insertStatTag(@Param("statId") Long statId, @Param("tagName") String tagName);

    @Delete("DELETE FROM stat_tags WHERE stat_id = #{statId}")
    int deleteStatTagsByStatId(@Param("statId") Long statId);
}
