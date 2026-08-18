package com.listen.portfolio.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.util.List;

/**
 * ProjectEntity（MyBatis-Plus 实体类）。
 * 
 * 映射 projects 表。
 */
@TableName("projects")
public class ProjectEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String businessId;

    private String title;
    private String titleZh;
    private String titleJa;

    private String subtitle;
    private String subtitleZh;
    private String subtitleJa;

    @TableField("project_desc")
    private String desc;

    @TableField("project_desc_zh")
    private String descZh;

    @TableField("project_desc_ja")
    private String descJa;

    private String imageUrl;
    private String githubUrl;

    @TableField(exist = false)
    private List<String> techStack;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getBusinessId() {
        return businessId;
    }

    public void setBusinessId(String businessId) {
        this.businessId = businessId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSubtitle() {
        return subtitle;
    }

    public void setSubtitle(String subtitle) {
        this.subtitle = subtitle;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getGithubUrl() {
        return githubUrl;
    }

    public void setGithubUrl(String githubUrl) {
        this.githubUrl = githubUrl;
    }

    public List<String> getTechStack() {
        return techStack;
    }

    public void setTechStack(List<String> techStack) {
        this.techStack = techStack;
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

    public String getSubtitleZh() {
        return subtitleZh;
    }

    public void setSubtitleZh(String subtitleZh) {
        this.subtitleZh = subtitleZh;
    }

    public String getSubtitleJa() {
        return subtitleJa;
    }

    public void setSubtitleJa(String subtitleJa) {
        this.subtitleJa = subtitleJa;
    }

    public String getDescZh() {
        return descZh;
    }

    public void setDescZh(String descZh) {
        this.descZh = descZh;
    }

    public String getDescJa() {
        return descJa;
    }

    public void setDescJa(String descJa) {
        this.descJa = descJa;
    }
}
