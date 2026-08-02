package com.listen.portfolio.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "education")
/**
 * EducationEntity（JPA Entity）。
 *
 * 说明：
 * - 对应 education 表
 * - 归属用户通过 user_id 关联 UserEntity（多对一）
 */
public class EducationEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public UserEntity getUser() {
        return user;
    }

    public void setUser(UserEntity user) {
        this.user = user;
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

