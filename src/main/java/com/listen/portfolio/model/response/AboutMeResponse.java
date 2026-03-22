package com.listen.portfolio.model.response;

import java.util.List;

public class AboutMeResponse {

    private String status;
    private String jobTitle;
    private String bio;
    private String graduationYear;
    private String github;
    private String major;
    private List<String> certifications;
    private List<StatResponse> stats;
    private List<ExperienceResponse> experiences;
    private List<EducationResponse> education;
    private List<LanguageResponse> languages;
    private List<SkillResponse> skills;

    // Getters and Setters

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

    public String getGithub() {
        return github;
    }

    public void setGithub(String github) {
        this.github = github;
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
}
