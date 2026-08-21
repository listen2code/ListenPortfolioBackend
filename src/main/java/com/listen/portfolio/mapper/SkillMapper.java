package com.listen.portfolio.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.listen.portfolio.entity.SkillEntity;
import com.listen.portfolio.entity.SkillItemEntity;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SkillMapper extends BaseMapper<SkillEntity> {

    @Select("SELECT id, skill_id, item_name, item_name_zh, item_name_ja FROM skill_items WHERE skill_id = #{skillId}")
    List<SkillItemEntity> findSkillItemEntitiesBySkillId(@Param("skillId") Long skillId);

    @Insert("INSERT INTO skill_items (skill_id, item_name, item_name_zh, item_name_ja) VALUES (#{skillId}, #{itemName}, #{itemNameZh}, #{itemNameJa})")
    int insertSkillItem(@Param("skillId") Long skillId, @Param("itemName") String itemName, @Param("itemNameZh") String itemNameZh, @Param("itemNameJa") String itemNameJa);

    @Delete("DELETE FROM skill_items WHERE skill_id = #{skillId}")
    int deleteSkillItemsBySkillId(@Param("skillId") Long skillId);
}
