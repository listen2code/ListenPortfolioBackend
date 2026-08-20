package com.listen.portfolio.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("实体类全字段 Getter/Setter 覆盖率测试")
class EntityCoverageTest {

    @Test
    @DisplayName("ProjectEntity 全属性与别名测试")
    void testProjectEntity() {
        ProjectEntity project = new ProjectEntity();
        project.setId(100L);
        project.setBusinessId("proj-1");
        project.setTitle("Title EN");
        project.setTitleZh("Title ZH");
        project.setTitleJa("Title JA");
        project.setSubtitle("Subtitle EN");
        project.setSubtitleZh("Subtitle ZH");
        project.setSubtitleJa("Subtitle JA");
        project.setProjectDesc("Desc EN");
        project.setProjectDescZh("Desc ZH");
        project.setProjectDescJa("Desc JA");
        project.setImageUrl("https://example.com/img.png");
        project.setGithubUrl("https://github.com/test");
        List<String> techStack = Arrays.asList("Java", "Spring", "Flutter");
        project.setTechStack(techStack);

        assertEquals(100L, project.getId());
        assertEquals("proj-1", project.getBusinessId());
        assertEquals("Title EN", project.getTitle());
        assertEquals("Title ZH", project.getTitleZh());
        assertEquals("Title JA", project.getTitleJa());
        assertEquals("Subtitle EN", project.getSubtitle());
        assertEquals("Subtitle ZH", project.getSubtitleZh());
        assertEquals("Subtitle JA", project.getSubtitleJa());
        assertEquals("Desc EN", project.getProjectDesc());
        assertEquals("Desc ZH", project.getProjectDescZh());
        assertEquals("Desc JA", project.getProjectDescJa());
        assertEquals("https://example.com/img.png", project.getImageUrl());
        assertEquals("https://github.com/test", project.getGithubUrl());
        assertEquals(techStack, project.getTechStack());

        // Test alias getDesc / setDesc methods
        project.setDesc("New Desc EN");
        project.setDescZh("New Desc ZH");
        project.setDescJa("New Desc JA");
        assertEquals("New Desc EN", project.getDesc());
        assertEquals("New Desc ZH", project.getDescZh());
        assertEquals("New Desc JA", project.getDescJa());
    }

    @Test
    @DisplayName("UserEntity 全属性测试")
    void testUserEntity() {
        UserEntity user = new UserEntity();
        user.setId(1L);
        user.setName("Listen");
        user.setEmail("listen@test.com");
        user.setPassword("hashedPwd");
        user.setLocation("Tokyo");
        user.setLocationZh("东京");
        user.setLocationJa("東京");
        user.setAvatarUrl("https://avatar.png");
        user.setStatus("active");
        user.setJobTitle("Engineer");
        user.setJobTitleZh("工程师");
        user.setJobTitleJa("エンジニア");
        user.setBio("Bio EN");
        user.setBioZh("Bio ZH");
        user.setBioJa("Bio JA");
        user.setGraduationYear("2013");
        user.setGithubUrl("https://github.com/listen");
        user.setMajor("CS");
        user.setMajorZh("计算机");
        user.setMajorJa("情報科学");
        
        LocalDateTime now = LocalDateTime.now();
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        user.setDeleted(true);

        List<String> certs = Arrays.asList("JLPT N1", "BJT J2");
        user.setCertifications(certs);

        List<StatEntity> stats = new ArrayList<>();
        user.setStats(stats);

        List<ExperienceEntity> experiences = new ArrayList<>();
        user.setExperiences(experiences);

        List<EducationEntity> education = new ArrayList<>();
        user.setEducation(education);

        List<SkillEntity> skills = new ArrayList<>();
        user.setSkills(skills);

        List<LanguageEntity> languages = new ArrayList<>();
        user.setLanguages(languages);

        assertEquals(1L, user.getId());
        assertEquals("Listen", user.getName());
        assertEquals("listen@test.com", user.getEmail());
        assertEquals("hashedPwd", user.getPassword());
        assertEquals("Tokyo", user.getLocation());
        assertEquals("东京", user.getLocationZh());
        assertEquals("東京", user.getLocationJa());
        assertEquals("https://avatar.png", user.getAvatarUrl());
        assertEquals("active", user.getStatus());
        assertEquals("Engineer", user.getJobTitle());
        assertEquals("工程师", user.getJobTitleZh());
        assertEquals("エンジニア", user.getJobTitleJa());
        assertEquals("Bio EN", user.getBio());
        assertEquals("Bio ZH", user.getBioZh());
        assertEquals("Bio JA", user.getBioJa());
        assertEquals("2013", user.getGraduationYear());
        assertEquals("https://github.com/listen", user.getGithubUrl());
        assertEquals("CS", user.getMajor());
        assertEquals("计算机", user.getMajorZh());
        assertEquals("情報科学", user.getMajorJa());
        assertEquals(now, user.getCreatedAt());
        assertEquals(now, user.getUpdatedAt());
        assertTrue(user.isDeleted());
        assertEquals(certs, user.getCertifications());
        assertEquals(stats, user.getStats());
        assertEquals(experiences, user.getExperiences());
        assertEquals(education, user.getEducation());
        assertEquals(skills, user.getSkills());
        assertEquals(languages, user.getLanguages());
    }

    @Test
    @DisplayName("ExperienceEntity 全属性测试")
    void testExperienceEntity() {
        ExperienceEntity exp = new ExperienceEntity();
        exp.setId(10L);
        exp.setUserId(1L);
        exp.setTitle("Lead Dev");
        exp.setTitleZh("研发专家");
        exp.setTitleJa("リードエンジニア");
        exp.setCompany("Company EN");
        exp.setCompanyZh("公司中文");
        exp.setCompanyJa("会社日文");
        exp.setPeriod("2021-2023");
        exp.setDescription("Description EN");
        exp.setDescriptionZh("Description ZH");
        exp.setDescriptionJa("Description JA");

        assertEquals(10L, exp.getId());
        assertEquals(1L, exp.getUserId());
        assertEquals("Lead Dev", exp.getTitle());
        assertEquals("研发专家", exp.getTitleZh());
        assertEquals("リードエンジニア", exp.getTitleJa());
        assertEquals("Company EN", exp.getCompany());
        assertEquals("公司中文", exp.getCompanyZh());
        assertEquals("会社日文", exp.getCompanyJa());
        assertEquals("2021-2023", exp.getPeriod());
        assertEquals("Description EN", exp.getDescription());
        assertEquals("Description ZH", exp.getDescriptionZh());
        assertEquals("Description JA", exp.getDescriptionJa());
    }

    @Test
    @DisplayName("EducationEntity 全属性测试")
    void testEducationEntity() {
        EducationEntity edu = new EducationEntity();
        edu.setId(20L);
        edu.setUserId(1L);
        edu.setDegree("Bachelor");
        edu.setDegreeZh("学士");
        edu.setDegreeJa("学士");
        edu.setSchool("University EN");
        edu.setSchoolZh("大学中文");
        edu.setSchoolJa("大学日文");
        edu.setPeriod("2009-2013");
        edu.setDescription("Edu desc EN");
        edu.setDescriptionZh("Edu desc ZH");
        edu.setDescriptionJa("Edu desc JA");

        assertEquals(20L, edu.getId());
        assertEquals(1L, edu.getUserId());
        assertEquals("Bachelor", edu.getDegree());
        assertEquals("学士", edu.getDegreeZh());
        assertEquals("学士", edu.getDegreeJa());
        assertEquals("University EN", edu.getSchool());
        assertEquals("大学中文", edu.getSchoolZh());
        assertEquals("大学日文", edu.getSchoolJa());
        assertEquals("2009-2013", edu.getPeriod());
        assertEquals("Edu desc EN", edu.getDescription());
        assertEquals("Edu desc ZH", edu.getDescriptionZh());
        assertEquals("Edu desc JA", edu.getDescriptionJa());
    }

    @Test
    @DisplayName("LanguageEntity 全属性测试")
    void testLanguageEntity() {
        LanguageEntity lang = new LanguageEntity();
        lang.setId(30L);
        lang.setUserId(1L);
        lang.setName("Japanese");
        lang.setNameZh("日语");
        lang.setNameJa("日本語");
        lang.setLevel("N1");
        lang.setLevelZh("N1级");
        lang.setLevelJa("N1級");

        assertEquals(30L, lang.getId());
        assertEquals(1L, lang.getUserId());
        assertEquals("Japanese", lang.getName());
        assertEquals("日语", lang.getNameZh());
        assertEquals("日本語", lang.getNameJa());
        assertEquals("N1", lang.getLevel());
        assertEquals("N1级", lang.getLevelZh());
        assertEquals("N1級", lang.getLevelJa());
    }

    @Test
    @DisplayName("StatEntity 全属性测试")
    void testStatEntity() {
        StatEntity stat = new StatEntity();
        stat.setId(40L);
        stat.setUserId(1L);
        stat.setBusinessId("flutter");
        stat.setYear("3");
        stat.setLabel("flutterExp");
        List<String> tags = Arrays.asList("Riverpod", "CleanArch");
        stat.setTags(tags);

        assertEquals(40L, stat.getId());
        assertEquals(1L, stat.getUserId());
        assertEquals("flutter", stat.getBusinessId());
        assertEquals("3", stat.getYear());
        assertEquals("flutterExp", stat.getLabel());
        assertEquals(tags, stat.getTags());
    }

    @Test
    @DisplayName("SkillEntity 全属性测试")
    void testSkillEntity() {
        SkillEntity skill = new SkillEntity();
        skill.setId(50L);
        skill.setUserId(1L);
        skill.setCategory("Mobile");
        List<String> items = Arrays.asList("Flutter", "Android");
        skill.setItems(items);

        assertEquals(50L, skill.getId());
        assertEquals(1L, skill.getUserId());
        assertEquals("Mobile", skill.getCategory());
        assertEquals(items, skill.getItems());
    }
}
