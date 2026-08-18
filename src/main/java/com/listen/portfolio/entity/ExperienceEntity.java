package com.listen.portfolio.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

/**
 * ExperienceEntity（MyBatis-Plus 实体类）。
 * 
 * 映射 experiences 表。
 */
@TableName("experiences")
public class ExperienceEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String title;
    private String titleZh;
    private String titleJa;

    private String company;
    private String companyZh;
    private String companyJa;

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

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getCompany() {
        return company;
    }

    public void setCompany(String company) {
        this.company = company;
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

    public String getTitleZh() {
        return titleZh;
    }

    public void setTitleZh(String titleZh) {
        this.titleZh = titleZh;
    }

    public String getTitleJa() {
        return titleJa;
    }

    public void setTitleJa(String titleJa) {
        this.titleJa = titleJa;
    }

    public String getCompanyZh() {
        return companyZh;
    }

    public void setCompanyZh(String companyZh) {
        this.companyZh = companyZh;
    }

    public String getCompanyJa() {
        return companyJa;
    }

    public void setCompanyJa(String companyJa) {
        this.companyJa = companyJa;
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
