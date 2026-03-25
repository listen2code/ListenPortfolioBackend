package com.listen.portfolio.model.response;

import jakarta.persistence.*;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import java.util.List;
import java.time.LocalDateTime;


@Entity
@Table(name = "users")
@EntityListeners(AuditingEntityListener.class)
/**
 * 用户表实体（当前类名历史原因叫 UserResponse，但实际是 JPA Entity）。
 *
 * 关键注解说明：
 * - @Entity：标记为 JPA 实体，会映射到数据库表
 * - @Table(name = "users")：指定表名为 users
 * - @EntityListeners(AuditingEntityListener.class)：启用 Spring Data JPA Auditing，
 *   使 @CreatedDate/@LastModifiedDate 自动填充生效
 */
public class UserResponse {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonSerialize(using = ToStringSerializer.class)
    /**
     * 主键。
     * - @GeneratedValue(IDENTITY)：使用数据库自增主键
     * - @JsonSerialize(ToStringSerializer)：将 Long 序列化为字符串，避免前端 JS 精度丢失（> 2^53-1）
     */
    private Long id;
    @Column(nullable = false, unique = true)
    /** 用户名：非空 + 唯一 */
    private String name;
    @Column(nullable = false, unique = true)
    /** 邮箱：非空 + 唯一 */
    private String email;
    @Column(nullable = false)
    /** 密码哈希：非空（注意：这里存的是 BCrypt 等加密后的 hash，不是明文） */
    private String password;
    private String location;
    private String avatarUrl;
    private String status;
    private String jobTitle;
    @Column(columnDefinition="TEXT")
    private String bio;
    private String graduationYear;
    private String githubUrl;
    private String major;
    
    @Column(name = "created_at", nullable = true, updatable = false)
    @CreatedDate
    /**
     * 创建时间。
     * - @CreatedDate：在首次 insert 时由 Auditing 自动填充
     * - updatable=false：避免业务更新时误改创建时间
     * - nullable=true：兼容已有历史数据（旧表数据可能没有该列或存在异常值）
     */
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    @LastModifiedDate
    /**
     * 更新时间。
     * - @LastModifiedDate：在每次 update 时由 Auditing 自动填充
     */
    private LocalDateTime updatedAt;
    
    @Column(name = "deleted", nullable = false)
    /**
     * 软删除标记（预留）。
     * 当前仍是物理删除（repo.delete），后续切换为软删除时会用到该字段做过滤。
     */
    private boolean deleted = false;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "user_certifications", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "certification_name", nullable = false)
    /**
     * 证书名称列表（简单值集合）。
     * - @ElementCollection：表示这是“值类型集合”，会单独建表存储
     * - @CollectionTable：集合表名 user_certifications，通过 user_id 与 users 关联
     */
    private List<String> certifications;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    /**
     * 用户统计信息（1:N）。
     * - mappedBy = "user"：由子表实体中的 user 字段维护外键关系
     * - cascade = ALL：对 User 的保存/删除会级联到子对象
     * - orphanRemoval = true：从集合中移除的子对象会被删除
     * - @JsonManagedReference：与子对象的 @JsonBackReference 配对，避免 JSON 循环引用
     */
    private List<StatResponse> stats;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    /** 工作经历（1:N），JSON 循环引用处理同上 */
    private List<ExperienceResponse> experiences;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    /** 教育经历（1:N），JSON 循环引用处理同上 */
    private List<EducationResponse> education;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    /** 语言能力（1:N），JSON 循环引用处理同上 */
    private List<LanguageResponse> languages;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    /** 技能（1:N），JSON 循环引用处理同上 */
    private List<SkillResponse> skills;


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

    public List<String> getCertifications() {
        return certifications;
    }

    public void setCertifications(List<String> certifications) {
        this.certifications = certifications;
    }

    public List<StatResponse> getStats() {
        return stats;
    }

    public void setStats(List<StatResponse> stats) {
        this.stats = stats;
    }

    public List<ExperienceResponse> getExperiences() {
        return experiences;
    }

    public void setExperiences(List<ExperienceResponse> experiences) {
        this.experiences = experiences;
    }

    public List<EducationResponse> getEducation() {
        return education;
    }

    public void setEducation(List<EducationResponse> education) {
        this.education = education;
    }

    public List<LanguageResponse> getLanguages() {
        return languages;
    }

    public void setLanguages(List<LanguageResponse> languages) {
        this.languages = languages;
    }

    public List<SkillResponse> getSkills() {
        return skills;
    }

    public void setSkills(List<SkillResponse> skills) {
        this.skills = skills;
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
}
