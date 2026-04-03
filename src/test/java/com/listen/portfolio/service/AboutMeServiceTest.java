package com.listen.portfolio.service;

import com.listen.portfolio.api.v1.about.dto.*;
import com.listen.portfolio.entity.*;
import com.listen.portfolio.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * AboutMeService 单元测试
 * 
 * 说明：测试 AboutMeService 的所有公共和私有方法
 * 目的：确保用户信息转换和 DTO 构建逻辑正确
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AboutMeService Unit Tests")
class AboutMeServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AboutMeService aboutMeService;

    private UserEntity mockUserEntity;
    private List<StatEntity> mockStats;
    private List<ExperienceEntity> mockExperiences;
    private List<EducationEntity> mockEducation;
    private List<LanguageEntity> mockLanguages;
    private List<SkillEntity> mockSkills;

    @BeforeEach
    void setUp() {
        // 初始化测试数据
        mockUserEntity = new UserEntity();
        mockUserEntity.setId(1L);
        mockUserEntity.setName("testuser");
        mockUserEntity.setStatus("Active");
        mockUserEntity.setJobTitle("Software Engineer");
        mockUserEntity.setBio("Passionate developer");
        mockUserEntity.setGraduationYear("2020");
        mockUserEntity.setGithubUrl("https://github.com/example");
        mockUserEntity.setMajor("Computer Science");
        mockUserEntity.setCertifications(Arrays.asList("AWS", "Java"));

        // 初始化统计数据
        mockStats = Arrays.asList(
            createStatEntity(1L, "stat1", "2023", "Performance", Arrays.asList("tag1", "tag2")),
            createStatEntity(2L, "stat2", "2022", "Growth", Arrays.asList("tag3"))
        );

        // 初始化经验数据
        mockExperiences = Arrays.asList(
            createExperienceEntity(1L, "Senior Developer", "Tech Corp", "2020-2023", "Led development team"),
            createExperienceEntity(2L, "Junior Developer", "Startup Inc", "2018-2020", "Built features")
        );

        // 初始化教育数据
        mockEducation = Arrays.asList(
            createEducationEntity(1L, "Bachelor", "University", "2016-2020", "Computer Science degree"),
            createEducationEntity(2L, "Master", "Tech Institute", "2020-2022", "Advanced studies")
        );

        // 初始化语言数据
        mockLanguages = Arrays.asList(
            createLanguageEntity(1L, "English", "Native"),
            createLanguageEntity(2L, "Spanish", "Intermediate")
        );

        // 初始化技能数据
        mockSkills = Arrays.asList(
            createSkillEntity(1L, "Programming", Arrays.asList("Java", "Python", "JavaScript")),
            createSkillEntity(2L, "Database", Arrays.asList("MySQL", "PostgreSQL"))
        );

        // 设置用户实体的关联数据
        mockUserEntity.setStats(mockStats);
        mockUserEntity.setExperiences(mockExperiences);
        mockUserEntity.setEducation(mockEducation);
        mockUserEntity.setLanguages(mockLanguages);
        mockUserEntity.setSkills(mockSkills);
    }

    @Test
    @DisplayName("getAboutMeDto - 成功获取用户信息")
    void testGetAboutMeDto_Success() {
        // Given
        when(userRepository.findByName("testuser")).thenReturn(Optional.of(mockUserEntity));

        // When
        Optional<AboutMeDto> result = aboutMeService.getAboutMeDto("testuser");

        // Then
        assertTrue(result.isPresent());
        AboutMeDto dto = result.get();
        
        // 验证基本信息
        assertEquals("Active", dto.getStatus());
        assertEquals("Software Engineer", dto.getJobTitle());
        assertEquals("Passionate developer", dto.getBio());
        assertEquals("2020", dto.getGraduationYear());
        assertEquals("https://github.com/example", dto.getGithub());
        assertEquals("Computer Science", dto.getMajor());
        assertEquals(Arrays.asList("AWS", "Java"), dto.getCertifications());

        // 验证统计数据
        assertNotNull(dto.getStats());
        assertEquals(2, dto.getStats().size());
        assertEquals("Performance", dto.getStats().get(0).getLabel());
        assertEquals("2023", dto.getStats().get(0).getYear());
        assertEquals(Arrays.asList("tag1", "tag2"), dto.getStats().get(0).getTags());

        // 验证经验数据
        assertNotNull(dto.getExperiences());
        assertEquals(2, dto.getExperiences().size());
        assertEquals("Senior Developer", dto.getExperiences().get(0).getTitle());
        assertEquals("Tech Corp", dto.getExperiences().get(0).getCompany());

        // 验证教育数据
        assertNotNull(dto.getEducation());
        assertEquals(2, dto.getEducation().size());
        assertEquals("Bachelor", dto.getEducation().get(0).getDegree());
        assertEquals("University", dto.getEducation().get(0).getSchool());

        // 验证语言数据
        assertNotNull(dto.getLanguages());
        assertEquals(2, dto.getLanguages().size());
        assertEquals("English", dto.getLanguages().get(0).getName());
        assertEquals("Native", dto.getLanguages().get(0).getLevel());

        // 验证技能数据
        assertNotNull(dto.getSkills());
        assertEquals(2, dto.getSkills().size());
        assertEquals("Programming", dto.getSkills().get(0).getCategory());
        assertEquals(Arrays.asList("Java", "Python", "JavaScript"), dto.getSkills().get(0).getItems());

        verify(userRepository).findByName("testuser");
    }

    @Test
    @DisplayName("getAboutMeDto - 用户不存在返回空Optional")
    void testGetAboutMeDto_UserNotFound() {
        // Given
        when(userRepository.findByName("nonexistent")).thenReturn(Optional.empty());

        // When
        Optional<AboutMeDto> result = aboutMeService.getAboutMeDto("nonexistent");

        // Then
        assertFalse(result.isPresent());
        verify(userRepository).findByName("nonexistent");
    }

    @Test
    @DisplayName("getAboutMeDto - null用户名处理")
    void testGetAboutMeDto_NullUsername() {
        // Given
        when(userRepository.findByName(null)).thenReturn(Optional.empty());

        // When
        Optional<AboutMeDto> result = aboutMeService.getAboutMeDto(null);

        // Then
        assertFalse(result.isPresent());
        verify(userRepository).findByName(null);
    }

    @Test
    @DisplayName("getAboutMeDto - 空用户名处理")
    void testGetAboutMeDto_EmptyUsername() {
        // Given
        when(userRepository.findByName("")).thenReturn(Optional.empty());

        // When
        Optional<AboutMeDto> result = aboutMeService.getAboutMeDto("");

        // Then
        assertFalse(result.isPresent());
        verify(userRepository).findByName("");
    }

    @Test
    @DisplayName("getAboutMeDto - 用户信息为null的处理")
    void testGetAboutMeDto_UserWithNullFields() {
        // Given - 创建包含 null 字段的用户
        UserEntity userWithNulls = new UserEntity();
        userWithNulls.setName("testuser");
        userWithNulls.setStats(null);
        userWithNulls.setExperiences(null);
        userWithNulls.setEducation(null);
        userWithNulls.setLanguages(null);
        userWithNulls.setSkills(null);
        userWithNulls.setCertifications(null);

        when(userRepository.findByName("testuser")).thenReturn(Optional.of(userWithNulls));

        // When
        Optional<AboutMeDto> result = aboutMeService.getAboutMeDto("testuser");

        // Then
        assertTrue(result.isPresent());
        AboutMeDto dto = result.get();
        
        // 验证 null 字段被正确处理
        assertTrue(dto.getStats().isEmpty());
        assertTrue(dto.getExperiences().isEmpty());
        assertTrue(dto.getEducation().isEmpty());
        assertTrue(dto.getLanguages().isEmpty());
        assertTrue(dto.getSkills().isEmpty());
        assertTrue(dto.getCertifications().isEmpty());
    }

    // ========== 私有方法测试 ==========

    @Test
    @DisplayName("nullToEmpty - null输入返回空列表")
    void testNullToEmpty_NullInput() throws Exception {
        // Given
        Method method = AboutMeService.class.getDeclaredMethod("nullToEmpty", List.class);
        method.setAccessible(true);

        // When
        @SuppressWarnings("unchecked")
        List<String> result = (List<String>) method.invoke(aboutMeService, (List<String>) null);

        // Then
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("nullToEmpty - 非null输入返回原列表")
    void testNullToEmpty_NonNullInput() throws Exception {
        // Given
        Method method = AboutMeService.class.getDeclaredMethod("nullToEmpty", List.class);
        method.setAccessible(true);
        List<String> inputList = Arrays.asList("item1", "item2");

        // When
        @SuppressWarnings("unchecked")
        List<String> result = (List<String>) method.invoke(aboutMeService, inputList);

        // Then
        assertEquals(inputList, result);
        assertEquals(2, result.size());
    }

    @Test
    @DisplayName("toStatDtos - null输入返回空列表")
    void testToStatDtos_NullInput() throws Exception {
        // Given
        Method method = AboutMeService.class.getDeclaredMethod("toStatDtos", List.class);
        method.setAccessible(true);

        // When
        @SuppressWarnings("unchecked")
        List<StatDto> result = (List<StatDto>) method.invoke(aboutMeService, (List<StatEntity>) null);

        // Then
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("toStatDtos - 有效输入正确转换")
    void testToStatDtos_ValidInput() throws Exception {
        // Given
        Method method = AboutMeService.class.getDeclaredMethod("toStatDtos", List.class);
        method.setAccessible(true);

        // When
        @SuppressWarnings("unchecked")
        List<StatDto> result = (List<StatDto>) method.invoke(aboutMeService, mockStats);

        // Then
        assertEquals(2, result.size());
        assertEquals("Performance", result.get(0).getLabel());
        assertEquals("2023", result.get(0).getYear());
        assertEquals(Arrays.asList("tag1", "tag2"), result.get(0).getTags());
        assertEquals(1L, result.get(0).getId());
        assertEquals("stat1", result.get(0).getBusinessId());
    }

    @Test
    @DisplayName("toStatDto - 实体正确转换为DTO")
    void testToStatDto_ValidEntity() throws Exception {
        // Given
        Method method = AboutMeService.class.getDeclaredMethod("toStatDto", StatEntity.class);
        method.setAccessible(true);
        StatEntity entity = createStatEntity(1L, "test", "2023", "Test Stat", Arrays.asList("tag1"));

        // When
        StatDto result = (StatDto) method.invoke(aboutMeService, entity);

        // Then
        assertEquals(1L, result.getId());
        assertEquals("test", result.getBusinessId());
        assertEquals("2023", result.getYear());
        assertEquals("Test Stat", result.getLabel());
        assertEquals(Arrays.asList("tag1"), result.getTags());
    }

    @Test
    @DisplayName("toExperienceDtos - null输入返回空列表")
    void testToExperienceDtos_NullInput() throws Exception {
        // Given
        Method method = AboutMeService.class.getDeclaredMethod("toExperienceDtos", List.class);
        method.setAccessible(true);

        // When
        @SuppressWarnings("unchecked")
        List<ExperienceDto> result = (List<ExperienceDto>) method.invoke(aboutMeService, (List<ExperienceEntity>) null);

        // Then
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("toExperienceDtos - 有效输入正确转换")
    void testToExperienceDtos_ValidInput() throws Exception {
        // Given
        Method method = AboutMeService.class.getDeclaredMethod("toExperienceDtos", List.class);
        method.setAccessible(true);

        // When
        @SuppressWarnings("unchecked")
        List<ExperienceDto> result = (List<ExperienceDto>) method.invoke(aboutMeService, mockExperiences);

        // Then
        assertEquals(2, result.size());
        assertEquals("Senior Developer", result.get(0).getTitle());
        assertEquals("Tech Corp", result.get(0).getCompany());
        assertEquals("2020-2023", result.get(0).getPeriod());
        assertEquals("Led development team", result.get(0).getDescription());
    }

    @Test
    @DisplayName("toExperienceDto - 实体正确转换为DTO")
    void testToExperienceDto_ValidEntity() throws Exception {
        // Given
        Method method = AboutMeService.class.getDeclaredMethod("toExperienceDto", ExperienceEntity.class);
        method.setAccessible(true);
        ExperienceEntity entity = createExperienceEntity(1L, "Test Job", "Test Company", "2020-2023", "Test description");

        // When
        ExperienceDto result = (ExperienceDto) method.invoke(aboutMeService, entity);

        // Then
        assertEquals(1L, result.getId());
        assertEquals("Test Job", result.getTitle());
        assertEquals("Test Company", result.getCompany());
        assertEquals("2020-2023", result.getPeriod());
        assertEquals("Test description", result.getDescription());
    }

    @Test
    @DisplayName("toEducationDtos - null输入返回空列表")
    void testToEducationDtos_NullInput() throws Exception {
        // Given
        Method method = AboutMeService.class.getDeclaredMethod("toEducationDtos", List.class);
        method.setAccessible(true);

        // When
        @SuppressWarnings("unchecked")
        List<EducationDto> result = (List<EducationDto>) method.invoke(aboutMeService, (List<EducationEntity>) null);

        // Then
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("toEducationDtos - 有效输入正确转换")
    void testToEducationDtos_ValidInput() throws Exception {
        // Given
        Method method = AboutMeService.class.getDeclaredMethod("toEducationDtos", List.class);
        method.setAccessible(true);

        // When
        @SuppressWarnings("unchecked")
        List<EducationDto> result = (List<EducationDto>) method.invoke(aboutMeService, mockEducation);

        // Then
        assertEquals(2, result.size());
        assertEquals("Bachelor", result.get(0).getDegree());
        assertEquals("University", result.get(0).getSchool());
        assertEquals("2016-2020", result.get(0).getPeriod());
        assertEquals("Computer Science degree", result.get(0).getDescription());
    }

    @Test
    @DisplayName("toEducationDto - 实体正确转换为DTO")
    void testToEducationDto_ValidEntity() throws Exception {
        // Given
        Method method = AboutMeService.class.getDeclaredMethod("toEducationDto", EducationEntity.class);
        method.setAccessible(true);
        EducationEntity entity = createEducationEntity(1L, "Test Degree", "Test School", "2020-2023", "Test description");

        // When
        EducationDto result = (EducationDto) method.invoke(aboutMeService, entity);

        // Then
        assertEquals(1L, result.getId());
        assertEquals("Test Degree", result.getDegree());
        assertEquals("Test School", result.getSchool());
        assertEquals("2020-2023", result.getPeriod());
        assertEquals("Test description", result.getDescription());
    }

    @Test
    @DisplayName("toLanguageDtos - null输入返回空列表")
    void testToLanguageDtos_NullInput() throws Exception {
        // Given
        Method method = AboutMeService.class.getDeclaredMethod("toLanguageDtos", List.class);
        method.setAccessible(true);

        // When
        @SuppressWarnings("unchecked")
        List<LanguageDto> result = (List<LanguageDto>) method.invoke(aboutMeService, (List<LanguageEntity>) null);

        // Then
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("toLanguageDtos - 有效输入正确转换")
    void testToLanguageDtos_ValidInput() throws Exception {
        // Given
        Method method = AboutMeService.class.getDeclaredMethod("toLanguageDtos", List.class);
        method.setAccessible(true);

        // When
        @SuppressWarnings("unchecked")
        List<LanguageDto> result = (List<LanguageDto>) method.invoke(aboutMeService, mockLanguages);

        // Then
        assertEquals(2, result.size());
        assertEquals("English", result.get(0).getName());
        assertEquals("Native", result.get(0).getLevel());
        assertEquals(1L, result.get(0).getId());
    }

    @Test
    @DisplayName("toLanguageDto - 实体正确转换为DTO")
    void testToLanguageDto_ValidEntity() throws Exception {
        // Given
        Method method = AboutMeService.class.getDeclaredMethod("toLanguageDto", LanguageEntity.class);
        method.setAccessible(true);
        LanguageEntity entity = createLanguageEntity(1L, "Test Language", "Test Level");

        // When
        LanguageDto result = (LanguageDto) method.invoke(aboutMeService, entity);

        // Then
        assertEquals(1L, result.getId());
        assertEquals("Test Language", result.getName());
        assertEquals("Test Level", result.getLevel());
    }

    @Test
    @DisplayName("toSkillDtos - null输入返回空列表")
    void testToSkillDtos_NullInput() throws Exception {
        // Given
        Method method = AboutMeService.class.getDeclaredMethod("toSkillDtos", List.class);
        method.setAccessible(true);

        // When
        @SuppressWarnings("unchecked")
        List<SkillDto> result = (List<SkillDto>) method.invoke(aboutMeService, (List<SkillEntity>) null);

        // Then
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("toSkillDtos - 有效输入正确转换")
    void testToSkillDtos_ValidInput() throws Exception {
        // Given
        Method method = AboutMeService.class.getDeclaredMethod("toSkillDtos", List.class);
        method.setAccessible(true);

        // When
        @SuppressWarnings("unchecked")
        List<SkillDto> result = (List<SkillDto>) method.invoke(aboutMeService, mockSkills);

        // Then
        assertEquals(2, result.size());
        assertEquals("Programming", result.get(0).getCategory());
        assertEquals(Arrays.asList("Java", "Python", "JavaScript"), result.get(0).getItems());
        assertEquals(1L, result.get(0).getId());
    }

    @Test
    @DisplayName("toSkillDto - 实体正确转换为DTO")
    void testToSkillDto_ValidEntity() throws Exception {
        // Given
        Method method = AboutMeService.class.getDeclaredMethod("toSkillDto", SkillEntity.class);
        method.setAccessible(true);
        SkillEntity entity = createSkillEntity(1L, "Test Category", Arrays.asList("item1", "item2"));

        // When
        SkillDto result = (SkillDto) method.invoke(aboutMeService, entity);

        // Then
        assertEquals(1L, result.getId());
        assertEquals("Test Category", result.getCategory());
        assertEquals(Arrays.asList("item1", "item2"), result.getItems());
    }

    // ========== 边界和异常测试 ==========

    @Test
    @DisplayName("边界测试 - 空集合处理")
    void testEdgeCase_EmptyCollections() throws Exception {
        // Given - 创建包含空集合的用户
        UserEntity userWithEmptyCollections = new UserEntity();
        userWithEmptyCollections.setName("testuser");
        userWithEmptyCollections.setStats(new ArrayList<>());
        userWithEmptyCollections.setExperiences(new ArrayList<>());
        userWithEmptyCollections.setEducation(new ArrayList<>());
        userWithEmptyCollections.setLanguages(new ArrayList<>());
        userWithEmptyCollections.setSkills(new ArrayList<>());

        when(userRepository.findByName("testuser")).thenReturn(Optional.of(userWithEmptyCollections));

        // When
        Optional<AboutMeDto> result = aboutMeService.getAboutMeDto("testuser");

        // Then
        assertTrue(result.isPresent());
        AboutMeDto dto = result.get();
        
        // 验证空集合被正确处理
        assertTrue(dto.getStats().isEmpty());
        assertTrue(dto.getExperiences().isEmpty());
        assertTrue(dto.getEducation().isEmpty());
        assertTrue(dto.getLanguages().isEmpty());
        assertTrue(dto.getSkills().isEmpty());
    }

    @Test
    @DisplayName("性能测试 - 大量数据转换")
    void testPerformance_BulkDataConversion() throws Exception {
        // Given - 创建大量数据
        List<StatEntity> largeStats = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            largeStats.add(createStatEntity((long) i, "stat" + i, "2023", "label" + i, Arrays.asList("tag" + i)));
        }

        Method method = AboutMeService.class.getDeclaredMethod("toStatDtos", List.class);
        method.setAccessible(true);

        long startTime = System.currentTimeMillis();

        // When
        @SuppressWarnings("unchecked")
        List<StatDto> result = (List<StatDto>) method.invoke(aboutMeService, largeStats);

        long endTime = System.currentTimeMillis();

        // Then
        assertEquals(1000, result.size());
        assertTrue(endTime - startTime < 1000, "Bulk conversion should complete within 1 second");
    }

    // ========== 辅助方法 ==========

    private StatEntity createStatEntity(Long id, String businessId, String year, String label, List<String> tags) {
        StatEntity entity = new StatEntity();
        entity.setId(id);
        entity.setBusinessId(businessId);
        entity.setYear(year);
        entity.setLabel(label);
        entity.setTags(tags);
        return entity;
    }

    private ExperienceEntity createExperienceEntity(Long id, String title, String company, String period, String description) {
        ExperienceEntity entity = new ExperienceEntity();
        entity.setId(id);
        entity.setTitle(title);
        entity.setCompany(company);
        entity.setPeriod(period);
        entity.setDescription(description);
        return entity;
    }

    private EducationEntity createEducationEntity(Long id, String degree, String school, String period, String description) {
        EducationEntity entity = new EducationEntity();
        entity.setId(id);
        entity.setDegree(degree);
        entity.setSchool(school);
        entity.setPeriod(period);
        entity.setDescription(description);
        return entity;
    }

    private LanguageEntity createLanguageEntity(Long id, String name, String level) {
        LanguageEntity entity = new LanguageEntity();
        entity.setId(id);
        entity.setName(name);
        entity.setLevel(level);
        return entity;
    }

    private SkillEntity createSkillEntity(Long id, String category, List<String> items) {
        SkillEntity entity = new SkillEntity();
        entity.setId(id);
        entity.setCategory(category);
        entity.setItems(items);
        return entity;
    }
}
