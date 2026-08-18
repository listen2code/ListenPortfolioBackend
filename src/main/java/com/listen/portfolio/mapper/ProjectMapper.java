package com.listen.portfolio.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.listen.portfolio.entity.ProjectEntity;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ProjectMapper extends BaseMapper<ProjectEntity> {

    @Select("SELECT tech_name FROM project_tech_stack WHERE project_id = #{projectId}")
    List<String> findTechStackByProjectId(@Param("projectId") Long projectId);

    @Insert("INSERT INTO project_tech_stack (project_id, tech_name) VALUES (#{projectId}, #{techName})")
    int insertTechStack(@Param("projectId") Long projectId, @Param("techName") String techName);

    @Delete("DELETE FROM project_tech_stack WHERE project_id = #{projectId}")
    int deleteTechStackByProjectId(@Param("projectId") Long projectId);
}
