package com.listen.portfolio.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;
import java.util.List;

/**
 * UserEntity（MyBatis-Plus 实体类）。
 * 
 * 映射 users 表，通过 MyBatis-Plus 实现 ORM 持久化与逻辑删除。
 */
@TableName("users")
public class UserEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;
    private String email;
    private String password;

    private String location;
    private String locationZh;
    private String locationJa;
    private String avatarUrl;
    private String status;
    private String jobTitle;
    private String jobTitleZh;
    private String jobTitleJa;

    private String bio;
    private String bioZh;
    private String bioJa;

    private String graduationYear;
    private String githubUrl;
    private String major;
    private String majorZh;
    private String majorJa;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic(value = "false", delval = "true")
    private boolean deleted = false;

    @TableField(exist = false)
    private List<String> certifications;

    @TableField(exist = false)
    private List<StatEntity> stats;

    @TableField(exist = false)
    private List<ExperienceEntity> experiences;

    @TableField(exist = false)
    private List<EducationEntity> education;

    @TableField(exist = false)
    private List<LanguageEntity> languages;

    @TableField(exist = false)
    private List<SkillEntity> skills;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getJobTitle() {
        return jobTitle;
    }

    public void setJobTitle(String jobTitle) {
        this.jobTitle = jobTitle;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public String getGraduationYear() {
        return graduationYear;
    }

    public void setGraduationYear(String graduationYear) {
        this.graduationYear = graduationYear;
    }

    public String getGithubUrl() {
        return githubUrl;
    }

    public void setGithubUrl(String githubUrl) {
        this.githubUrl = githubUrl;
    }

    public String getMajor() {
        return major;
    }

    public void setMajor(String major) {
        this.major = major;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }

    public List<String> getCertifications() {
        return certifications;
    }

    public void setCertifications(List<String> certifications) {
        this.certifications = certifications;
    }

    public List<StatEntity> getStats() {
        return stats;
    }

    public void setStats(List<StatEntity> stats) {
        this.stats = stats;
    }

    public List<ExperienceEntity> getExperiences() {
        return experiences;
    }

    public void setExperiences(List<ExperienceEntity> experiences) {
        this.experiences = experiences;
    }

    public List<EducationEntity> getEducation() {
        return education;
    }

    public void setEducation(List<EducationEntity> education) {
        this.education = education;
    }

    public List<LanguageEntity> getLanguages() {
        return languages;
    }

    public void setLanguages(List<LanguageEntity> languages) {
        this.languages = languages;
    }

    public List<SkillEntity> getSkills() {
        return skills;
    }

    public void setSkills(List<SkillEntity> skills) {
        this.skills = skills;
    }

    public String getLocationZh() {
        return locationZh;
    }

    public void setLocationZh(String locationZh) {
        this.locationZh = locationZh;
    }

    public String getLocationJa() {
        return locationJa;
    }

    public void setLocationJa(String locationJa) {
        this.locationJa = locationJa;
    }

    public String getJobTitleZh() {
        return jobTitleZh;
    }

    public void setJobTitleZh(String jobTitleZh) {
        this.jobTitleZh = jobTitleZh;
    }

    public String getJobTitleJa() {
        return jobTitleJa;
    }

    public void setJobTitleJa(String jobTitleJa) {
        this.jobTitleJa = jobTitleJa;
    }

    public String getBioZh() {
        return bioZh;
    }

    public void setBioZh(String bioZh) {
        this.bioZh = bioZh;
    }

    public String getBioJa() {
        return bioJa;
    }

    public void setBioJa(String bioJa) {
        this.bioJa = bioJa;
    }

    public String getMajorZh() {
        return majorZh;
    }

    public void setMajorZh(String majorZh) {
        this.majorZh = majorZh;
    }

    public String getMajorJa() {
        return majorJa;
    }

    public void setMajorJa(String majorJa) {
        this.majorJa = majorJa;
    }
}
