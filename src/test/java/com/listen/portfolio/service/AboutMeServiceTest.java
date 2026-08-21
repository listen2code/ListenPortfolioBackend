package com.listen.portfolio.service;

import com.listen.portfolio.api.v1.about.dto.*;
import com.listen.portfolio.entity.*;
import com.listen.portfolio.mapper.EducationMapper;
import com.listen.portfolio.mapper.ExperienceMapper;
import com.listen.portfolio.mapper.LanguageMapper;
import com.listen.portfolio.mapper.SkillMapper;
import com.listen.portfolio.mapper.StatMapper;
import com.listen.portfolio.mapper.UserMapper;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * AboutMeService 单元测试（MyBatis-Plus 版本）
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AboutMeService Unit Tests")
class AboutMeServiceTest {

    @Mock
    private UserMapper userMapper;

    @Mock
    private StatMapper statMapper;

    @Mock
    private ExperienceMapper experienceMapper;

    @Mock
    private EducationMapper educationMapper;

    @Mock
    private LanguageMapper languageMapper;

    @Mock
    private SkillMapper skillMapper;

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

        mockStats = Arrays.asList(
            createStatEntity(1L, "stat1", "2023", "Performance", Arrays.asList("tag1", "tag2")),
            createStatEntity(2L, "stat2", "2022", "Growth", Arrays.asList("tag3"))
        );

        mockExperiences = Arrays.asList(
            createExperienceEntity(1L, "Senior Developer", "Tech Corp", "2020-2023", "Led development team"),
            createExperienceEntity(2L, "Junior Developer", "Startup Inc", "2018-2020", "Built features")
        );

        mockEducation = Arrays.asList(
            createEducationEntity(1L, "Bachelor", "University", "2016-2020", "Computer Science degree"),
            createEducationEntity(2L, "Master", "Tech Institute", "2020-2022", "Advanced studies")
        );

        mockLanguages = Arrays.asList(
            createLanguageEntity(1L, "English", "Native"),
            createLanguageEntity(2L, "Spanish", "Intermediate")
        );

        mockSkills = Arrays.asList(
            createSkillEntity(1L, "Programming", Arrays.asList("Java", "Python", "JavaScript")),
            createSkillEntity(2L, "Database", Arrays.asList("MySQL", "PostgreSQL"))
        );
    }

    @Test
    @DisplayName("getAboutMeDto - 成功获取用户信息")
    void testGetAboutMeDto_Success() {
        when(userMapper.selectById(1L)).thenReturn(mockUserEntity);
        when(userMapper.findCertificationsByUserId(1L)).thenReturn(Arrays.asList("AWS", "Java"));
        when(statMapper.selectList(any())).thenReturn(mockStats);
        when(statMapper.findTagsByStatId(1L)).thenReturn(Arrays.asList("tag1", "tag2"));
        when(statMapper.findTagsByStatId(2L)).thenReturn(Arrays.asList("tag3"));
        when(experienceMapper.selectList(any())).thenReturn(mockExperiences);
        when(educationMapper.selectList(any())).thenReturn(mockEducation);
        when(languageMapper.selectList(any())).thenReturn(mockLanguages);
        when(skillMapper.selectList(any())).thenReturn(mockSkills);
        when(skillMapper.findSkillItemEntitiesBySkillId(1L)).thenReturn(Arrays.asList(
            createSkillItemEntity(1L, 1L, "Java"),
            createSkillItemEntity(2L, 1L, "Python"),
            createSkillItemEntity(3L, 1L, "JavaScript")
        ));
        when(skillMapper.findSkillItemEntitiesBySkillId(2L)).thenReturn(Arrays.asList(
            createSkillItemEntity(4L, 2L, "MySQL"),
            createSkillItemEntity(5L, 2L, "PostgreSQL")
        ));

        Optional<AboutMeDto> result = aboutMeService.getAboutMeDto(1L);

        assertTrue(result.isPresent());
        AboutMeDto dto = result.get();
        
        assertEquals("Active", dto.getStatus());
        assertEquals("Software Engineer", dto.getJobTitle());
        assertEquals("Passionate developer", dto.getBio());
        assertEquals("2020", dto.getGraduationYear());
        assertEquals("https://github.com/example", dto.getGithub());
        assertEquals("Computer Science", dto.getMajor());
        assertEquals(Arrays.asList("AWS", "Java"), dto.getCertifications());

        assertNotNull(dto.getStats());
        assertEquals(2, dto.getStats().size());
        assertEquals("Performance", dto.getStats().get(0).getLabel());
        assertEquals("2023", dto.getStats().get(0).getYear());
        assertEquals(Arrays.asList("tag1", "tag2"), dto.getStats().get(0).getTags());

        assertNotNull(dto.getExperiences());
        assertEquals(2, dto.getExperiences().size());
        assertEquals("Senior Developer", dto.getExperiences().get(0).getTitle());
        assertEquals("Tech Corp", dto.getExperiences().get(0).getCompany());

        assertNotNull(dto.getEducation());
        assertEquals(2, dto.getEducation().size());
        assertEquals("Bachelor", dto.getEducation().get(0).getDegree());
        assertEquals("University", dto.getEducation().get(0).getSchool());

        assertNotNull(dto.getLanguages());
        assertEquals(2, dto.getLanguages().size());
        assertEquals("English", dto.getLanguages().get(0).getName());
        assertEquals("Native", dto.getLanguages().get(0).getLevel());

        assertNotNull(dto.getSkills());
        assertEquals(2, dto.getSkills().size());
        assertEquals("Programming", dto.getSkills().get(0).getCategory());
        assertEquals(Arrays.asList("Java", "Python", "JavaScript"), dto.getSkills().get(0).getItems());

        verify(userMapper).selectById(1L);
    }

    @Test
    @DisplayName("getAboutMeDto - 用户不存在返回空Optional")
    void testGetAboutMeDto_UserNotFound() {
        when(userMapper.selectById(999L)).thenReturn(null);

        Optional<AboutMeDto> result = aboutMeService.getAboutMeDto(999L);

        assertFalse(result.isPresent());
        verify(userMapper).selectById(999L);
    }

    @Test
    @DisplayName("getAboutMeDto - null用户ID处理")
    void testGetAboutMeDto_NullUserId() {
        Optional<AboutMeDto> result = aboutMeService.getAboutMeDto(null);

        assertFalse(result.isPresent());
        verifyNoInteractions(userMapper);
    }

    @Test
    @DisplayName("getAboutMeDto - 无效用户ID处理")
    void testGetAboutMeDto_InvalidUserId() {
        when(userMapper.selectById(0L)).thenReturn(null);

        Optional<AboutMeDto> result = aboutMeService.getAboutMeDto(0L);

        assertFalse(result.isPresent());
        verify(userMapper).selectById(0L);
    }

    @Test
    @DisplayName("getAboutMeDto - 用户信息为null的处理")
    void testGetAboutMeDto_UserWithNullFields() {
        UserEntity userWithNulls = new UserEntity();
        userWithNulls.setName("testuser");

        when(userMapper.selectById(1L)).thenReturn(userWithNulls);
        when(userMapper.findCertificationsByUserId(1L)).thenReturn(null);
        when(statMapper.selectList(any())).thenReturn(null);
        when(experienceMapper.selectList(any())).thenReturn(null);
        when(educationMapper.selectList(any())).thenReturn(null);
        when(languageMapper.selectList(any())).thenReturn(null);
        when(skillMapper.selectList(any())).thenReturn(null);

        Optional<AboutMeDto> result = aboutMeService.getAboutMeDto(1L);

        assertTrue(result.isPresent());
        AboutMeDto dto = result.get();
        
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
        Method method = AboutMeService.class.getDeclaredMethod("nullToEmpty", List.class);
        method.setAccessible(true);

        @SuppressWarnings("unchecked")
        List<String> result = (List<String>) method.invoke(aboutMeService, (List<String>) null);

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("nullToEmpty - 非null输入返回原列表")
    void testNullToEmpty_NonNullInput() throws Exception {
        Method method = AboutMeService.class.getDeclaredMethod("nullToEmpty", List.class);
        method.setAccessible(true);
        List<String> inputList = Arrays.asList("item1", "item2");

        @SuppressWarnings("unchecked")
        List<String> result = (List<String>) method.invoke(aboutMeService, inputList);

        assertEquals(inputList, result);
        assertEquals(2, result.size());
    }

    @Test
    @DisplayName("toStatDtos - null输入返回空列表")
    void testToStatDtos_NullInput() throws Exception {
        Method method = AboutMeService.class.getDeclaredMethod("toStatDtos", List.class);
        method.setAccessible(true);

        @SuppressWarnings("unchecked")
        List<StatDto> result = (List<StatDto>) method.invoke(aboutMeService, (List<StatEntity>) null);

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("toStatDtos - 有效输入正确转换")
    void testToStatDtos_ValidInput() throws Exception {
        Method method = AboutMeService.class.getDeclaredMethod("toStatDtos", List.class);
        method.setAccessible(true);

        @SuppressWarnings("unchecked")
        List<StatDto> result = (List<StatDto>) method.invoke(aboutMeService, mockStats);

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
        Method method = AboutMeService.class.getDeclaredMethod("toStatDto", StatEntity.class);
        method.setAccessible(true);
        StatEntity entity = createStatEntity(1L, "test", "2023", "Test Stat", Arrays.asList("tag1"));

        StatDto result = (StatDto) method.invoke(aboutMeService, entity);

        assertEquals(1L, result.getId());
        assertEquals("test", result.getBusinessId());
        assertEquals("2023", result.getYear());
        assertEquals("Test Stat", result.getLabel());
        assertEquals(Arrays.asList("tag1"), result.getTags());
    }

    @Test
    @DisplayName("toExperienceDtos - null输入返回空列表")
    void testToExperienceDtos_NullInput() throws Exception {
        Method method = AboutMeService.class.getDeclaredMethod("toExperienceDtos", List.class, Locale.class);
        method.setAccessible(true);

        @SuppressWarnings("unchecked")
        List<ExperienceDto> result = (List<ExperienceDto>) method.invoke(aboutMeService, (List<ExperienceEntity>) null, Locale.ENGLISH);

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("toExperienceDtos - 有效输入正确转换")
    void testToExperienceDtos_ValidInput() throws Exception {
        Method method = AboutMeService.class.getDeclaredMethod("toExperienceDtos", List.class, Locale.class);
        method.setAccessible(true);

        @SuppressWarnings("unchecked")
        List<ExperienceDto> result = (List<ExperienceDto>) method.invoke(aboutMeService, mockExperiences, Locale.ENGLISH);

        assertEquals(2, result.size());
        assertEquals("Senior Developer", result.get(0).getTitle());
        assertEquals("Tech Corp", result.get(0).getCompany());
        assertEquals("2020-2023", result.get(0).getPeriod());
        assertEquals("Led development team", result.get(0).getDescription());
    }

    @Test
    @DisplayName("toExperienceDto - 实体正确转换为DTO")
    void testToExperienceDto_ValidEntity() throws Exception {
        Method method = AboutMeService.class.getDeclaredMethod("toExperienceDto", ExperienceEntity.class, Locale.class);
        method.setAccessible(true);
        ExperienceEntity entity = createExperienceEntity(1L, "Test Job", "Test Company", "2020-2023", "Test description");

        ExperienceDto result = (ExperienceDto) method.invoke(aboutMeService, entity, Locale.ENGLISH);

        assertEquals(1L, result.getId());
        assertEquals("Test Job", result.getTitle());
        assertEquals("Test Company", result.getCompany());
        assertEquals("2020-2023", result.getPeriod());
        assertEquals("Test description", result.getDescription());
    }

    @Test
    @DisplayName("toEducationDtos - null输入返回空列表")
    void testToEducationDtos_NullInput() throws Exception {
        Method method = AboutMeService.class.getDeclaredMethod("toEducationDtos", List.class, Locale.class);
        method.setAccessible(true);

        @SuppressWarnings("unchecked")
        List<EducationDto> result = (List<EducationDto>) method.invoke(aboutMeService, (List<EducationEntity>) null, Locale.ENGLISH);

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("toEducationDtos - 有效输入正确转换")
    void testToEducationDtos_ValidInput() throws Exception {
        Method method = AboutMeService.class.getDeclaredMethod("toEducationDtos", List.class, Locale.class);
        method.setAccessible(true);

        @SuppressWarnings("unchecked")
        List<EducationDto> result = (List<EducationDto>) method.invoke(aboutMeService, mockEducation, Locale.ENGLISH);

        assertEquals(2, result.size());
        assertEquals("Bachelor", result.get(0).getDegree());
        assertEquals("University", result.get(0).getSchool());
        assertEquals("2016-2020", result.get(0).getPeriod());
        assertEquals("Computer Science degree", result.get(0).getDescription());
    }

    @Test
    @DisplayName("toEducationDto - 实体正确转换为DTO")
    void testToEducationDto_ValidEntity() throws Exception {
        Method method = AboutMeService.class.getDeclaredMethod("toEducationDto", EducationEntity.class, Locale.class);
        method.setAccessible(true);
        EducationEntity entity = createEducationEntity(1L, "Test Degree", "Test School", "2020-2023", "Test description");

        EducationDto result = (EducationDto) method.invoke(aboutMeService, entity, Locale.ENGLISH);

        assertEquals(1L, result.getId());
        assertEquals("Test Degree", result.getDegree());
        assertEquals("Test School", result.getSchool());
        assertEquals("2020-2023", result.getPeriod());
        assertEquals("Test description", result.getDescription());
    }

    @Test
    @DisplayName("toLanguageDtos - null输入返回空列表")
    void testToLanguageDtos_NullInput() throws Exception {
        Method method = AboutMeService.class.getDeclaredMethod("toLanguageDtos", List.class, Locale.class);
        method.setAccessible(true);

        @SuppressWarnings("unchecked")
        List<LanguageDto> result = (List<LanguageDto>) method.invoke(aboutMeService, (List<LanguageEntity>) null, Locale.ENGLISH);

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("toLanguageDtos - 有效输入正确转换")
    void testToLanguageDtos_ValidInput() throws Exception {
        Method method = AboutMeService.class.getDeclaredMethod("toLanguageDtos", List.class, Locale.class);
        method.setAccessible(true);

        @SuppressWarnings("unchecked")
        List<LanguageDto> result = (List<LanguageDto>) method.invoke(aboutMeService, mockLanguages, Locale.ENGLISH);

        assertEquals(2, result.size());
        assertEquals("English", result.get(0).getName());
        assertEquals("Native", result.get(0).getLevel());
        assertEquals(1L, result.get(0).getId());
    }

    @Test
    @DisplayName("toLanguageDto - 实体正确转换为DTO")
    void testToLanguageDto_ValidEntity() throws Exception {
        Method method = AboutMeService.class.getDeclaredMethod("toLanguageDto", LanguageEntity.class, Locale.class);
        method.setAccessible(true);
        LanguageEntity entity = createLanguageEntity(1L, "Test Language", "Test Level");

        LanguageDto result = (LanguageDto) method.invoke(aboutMeService, entity, Locale.ENGLISH);

        assertEquals(1L, result.getId());
        assertEquals("Test Language", result.getName());
        assertEquals("Test Level", result.getLevel());
    }

    @Test
    @DisplayName("toSkillDtos - null输入返回空列表")
    void testToSkillDtos_NullInput() throws Exception {
        Method method = AboutMeService.class.getDeclaredMethod("toSkillDtos", List.class, Locale.class);
        method.setAccessible(true);

        @SuppressWarnings("unchecked")
        List<SkillDto> result = (List<SkillDto>) method.invoke(aboutMeService, (List<SkillEntity>) null, Locale.ENGLISH);

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("toSkillDtos - 有效输入正确转换")
    void testToSkillDtos_ValidInput() throws Exception {
        Method method = AboutMeService.class.getDeclaredMethod("toSkillDtos", List.class, Locale.class);
        method.setAccessible(true);

        @SuppressWarnings("unchecked")
        List<SkillDto> result = (List<SkillDto>) method.invoke(aboutMeService, mockSkills, Locale.ENGLISH);

        assertEquals(2, result.size());
        assertEquals("Programming", result.get(0).getCategory());
        assertEquals(Arrays.asList("Java", "Python", "JavaScript"), result.get(0).getItems());
        assertEquals(1L, result.get(0).getId());
    }

    @Test
    @DisplayName("toSkillDto - 实体正确转换为DTO")
    void testToSkillDto_ValidEntity() throws Exception {
        Method method = AboutMeService.class.getDeclaredMethod("toSkillDto", SkillEntity.class, Locale.class);
        method.setAccessible(true);
        SkillEntity entity = createSkillEntity(1L, "Test Category", Arrays.asList("item1", "item2"));

        SkillDto result = (SkillDto) method.invoke(aboutMeService, entity, Locale.ENGLISH);

        assertEquals(1L, result.getId());
        assertEquals("Test Category", result.getCategory());
        assertEquals(Arrays.asList("item1", "item2"), result.getItems());
    }

    // ========== 边界和异常测试 ==========

    @Test
    @DisplayName("边界测试 - 空集合处理")
    void testEdgeCase_EmptyCollections() throws Exception {
        UserEntity userWithEmptyCollections = new UserEntity();
        userWithEmptyCollections.setName("testuser");

        when(userMapper.selectById(1L)).thenReturn(userWithEmptyCollections);
        when(userMapper.findCertificationsByUserId(1L)).thenReturn(new ArrayList<>());
        when(statMapper.selectList(any())).thenReturn(new ArrayList<>());
        when(experienceMapper.selectList(any())).thenReturn(new ArrayList<>());
        when(educationMapper.selectList(any())).thenReturn(new ArrayList<>());
        when(languageMapper.selectList(any())).thenReturn(new ArrayList<>());
        when(skillMapper.selectList(any())).thenReturn(new ArrayList<>());

        Optional<AboutMeDto> result = aboutMeService.getAboutMeDto(1L);

        assertTrue(result.isPresent());
        AboutMeDto dto = result.get();
        
        assertTrue(dto.getStats().isEmpty());
        assertTrue(dto.getExperiences().isEmpty());
        assertTrue(dto.getEducation().isEmpty());
        assertTrue(dto.getLanguages().isEmpty());
        assertTrue(dto.getSkills().isEmpty());
    }

    @Test
    @DisplayName("性能测试 - 大量数据转换")
    void testPerformance_BulkDataConversion() throws Exception {
        List<StatEntity> largeStats = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            largeStats.add(createStatEntity((long) i, "stat" + i, "2023", "label" + i, Arrays.asList("tag" + i)));
        }

        Method method = AboutMeService.class.getDeclaredMethod("toStatDtos", List.class);
        method.setAccessible(true);

        long startTime = System.currentTimeMillis();

        @SuppressWarnings("unchecked")
        List<StatDto> result = (List<StatDto>) method.invoke(aboutMeService, largeStats);

        long endTime = System.currentTimeMillis();

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

    private SkillItemEntity createSkillItemEntity(Long id, Long skillId, String itemName) {
        SkillItemEntity entity = new SkillItemEntity();
        entity.setId(id);
        entity.setSkillId(skillId);
        entity.setItemName(itemName);
        return entity;
    }
}
