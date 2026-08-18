package com.listen.portfolio.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

/**
 * EducationEntity（MyBatis-Plus 实体类）。
 * 
 * 映射 education 表。
 */
@TableName("education")
public class EducationEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String degree;
    private String degreeZh;
    private String degreeJa;

    private String school;
    private String schoolZh;
    private String schoolJa;

    private String period;

    private String description;
    private String descriptionZh;
    private String descriptionJa;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getDegree() {
        return degree;
    }

    public void setDegree(String degree) {
        this.degree = degree;
    }

    public String getSchool() {
        return school;
    }

    public void setSchool(String school) {
        this.school = school;
    }

    public String getPeriod() {
        return period;
    }

    public void setPeriod(String period) {
        this.period = period;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDegreeZh() {
        return degreeZh;
    }

    public void setDegreeZh(String degreeZh) {
        this.degreeZh = degreeZh;
    }

    public String getDegreeJa() {
        return degreeJa;
    }

    public void setDegreeJa(String degreeJa) {
        this.degreeJa = degreeJa;
    }

    public String getSchoolZh() {
        return schoolZh;
    }

    public void setSchoolZh(String schoolZh) {
        this.schoolZh = schoolZh;
    }

    public String getSchoolJa() {
        return schoolJa;
    }

    public void setSchoolJa(String schoolJa) {
        this.schoolJa = schoolJa;
    }

    public String getDescriptionZh() {
        return descriptionZh;
    }

    public void setDescriptionZh(String descriptionZh) {
        this.descriptionZh = descriptionZh;
    }

    public String getDescriptionJa() {
        return descriptionJa;
    }

    public void setDescriptionJa(String descriptionJa) {
        this.descriptionJa = descriptionJa;
    }
}
