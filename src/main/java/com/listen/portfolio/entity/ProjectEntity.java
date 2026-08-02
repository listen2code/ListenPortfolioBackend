package com.listen.portfolio.entity;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

import java.util.List;

@Entity
@Table(name = "projects")
/**
 * ProjectEntity（JPA Entity）。
 *
 * 说明：
 * - 该类是数据库 projects 表的映射对象，属于“数据访问层/持久化层”的核心模型
 * - 之前项目使用 ProjectResponse 作为 Entity，命名与职责混淆（看起来像 API Response，但实际是表映射）
 * - 这里调整为 ProjectEntity：明确它是 Entity，API 层通过 DTO 暴露数据，避免实体透传
 */
public class ProjectEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "business_id", unique = true)
    private String businessId;

    private String title;
    private String titleZh;
    private String titleJa;

    private String subtitle;
    private String subtitleZh;
    private String subtitleJa;

    @Column(name = "project_desc", columnDefinition = "TEXT")
    private String desc;
    @Column(name = "project_desc_zh", columnDefinition = "TEXT")
    private String descZh;
    @Column(name = "project_desc_ja", columnDefinition = "TEXT")
    private String descJa;

    private String imageUrl;
    private String githubUrl;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "project_tech_stack", joinColumns = @JoinColumn(name = "project_id"))
    @Column(name = "tech_name", nullable = false)
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

