package com.listen.portfolio.api.v1.about;

import com.listen.portfolio.api.v1.about.dto.AboutMeDto;
import com.listen.portfolio.api.v1.about.dto.EducationDto;
import com.listen.portfolio.api.v1.about.dto.ExperienceDto;
import com.listen.portfolio.api.v1.about.dto.LanguageDto;
import com.listen.portfolio.api.v1.about.dto.SkillDto;
import com.listen.portfolio.api.v1.about.dto.StatDto;
import com.listen.portfolio.infrastructure.persistence.entity.EducationEntity;
import com.listen.portfolio.infrastructure.persistence.entity.ExperienceEntity;
import com.listen.portfolio.infrastructure.persistence.entity.LanguageEntity;
import com.listen.portfolio.infrastructure.persistence.entity.SkillEntity;
import com.listen.portfolio.infrastructure.persistence.entity.StatEntity;
import com.listen.portfolio.infrastructure.persistence.entity.UserEntity;
import com.listen.portfolio.repository.UserRepository;
import com.listen.portfolio.service.AboutMeService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AboutMeService Unit Tests")
class AboutMeServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AboutMeService aboutMeService;

    private UserEntity mockUserEntity;
    private StatEntity mockStatEntity;
    private ExperienceEntity mockExperienceEntity;
    private EducationEntity mockEducationEntity;
    private LanguageEntity mockLanguageEntity;
    private SkillEntity mockSkillEntity;

    @BeforeEach
    void setUp() {
        // Setup mock UserEntity
        mockUserEntity = new UserEntity();
        mockUserEntity.setId(1L);
        mockUserEntity.setStatus("Active");
        mockUserEntity.setJobTitle("Software Engineer");
        mockUserEntity.setBio("Passionate developer");
        mockUserEntity.setGraduationYear("2020");
        mockUserEntity.setGithubUrl("https://github.com/example");
        mockUserEntity.setMajor("Computer Science");
        mockUserEntity.setCertifications(Arrays.asList("Java", "Spring"));

        // Setup mock StatEntity
        mockStatEntity = new StatEntity();
        mockStatEntity.setId(1L);
        mockStatEntity.setBusinessId("1");
        mockStatEntity.setYear("2023");
        mockStatEntity.setLabel("Projects");
        mockStatEntity.setTags(Arrays.asList("web", "mobile"));

        // Setup mock ExperienceEntity
        mockExperienceEntity = new ExperienceEntity();
        mockExperienceEntity.setId(1L);
        mockExperienceEntity.setTitle("Senior Developer");
        mockExperienceEntity.setCompany("Tech Corp");
        mockExperienceEntity.setPeriod("2020-2023");
        mockExperienceEntity.setDescription("Developed web applications");

        // Setup mock EducationEntity
        mockEducationEntity = new EducationEntity();
        mockEducationEntity.setId(1L);
        mockEducationEntity.setDegree("Bachelor's");
        mockEducationEntity.setSchool("University");
        mockEducationEntity.setPeriod("2016-2020");
        mockEducationEntity.setDescription("Computer Science degree");

        // Setup mock LanguageEntity
        mockLanguageEntity = new LanguageEntity();
        mockLanguageEntity.setId(1L);
        mockLanguageEntity.setName("English");
        mockLanguageEntity.setLevel("Fluent");

        // Setup mock SkillEntity
        mockSkillEntity = new SkillEntity();
        mockSkillEntity.setId(1L);
        mockSkillEntity.setCategory("Programming");
        mockSkillEntity.setItems(Arrays.asList("Java", "Python", "JavaScript"));

        mockUserEntity.setStats(Arrays.asList(mockStatEntity));
        mockUserEntity.setExperiences(Arrays.asList(mockExperienceEntity));
        mockUserEntity.setEducation(Arrays.asList(mockEducationEntity));
        mockUserEntity.setLanguages(Arrays.asList(mockLanguageEntity));
        mockUserEntity.setSkills(Arrays.asList(mockSkillEntity));
    }

    @Test
    @DisplayName("Should return AboutMeDto when user is found")
    void getAboutMeDto_WhenUserExists_ShouldReturnAboutMeDto() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(mockUserEntity));

        // When
        Optional<AboutMeDto> result = aboutMeService.getAboutMeDto();

        // Then
        assertTrue(result.isPresent());
        AboutMeDto dto = result.get();
        
        assertEquals(mockUserEntity.getStatus(), dto.getStatus());
        assertEquals(mockUserEntity.getJobTitle(), dto.getJobTitle());
        assertEquals(mockUserEntity.getBio(), dto.getBio());
        assertEquals(mockUserEntity.getGraduationYear(), dto.getGraduationYear());
        assertEquals(mockUserEntity.getGithubUrl(), dto.getGithub());
        assertEquals(mockUserEntity.getMajor(), dto.getMajor());
        assertEquals(mockUserEntity.getCertifications(), dto.getCertifications());

        // Verify collections are not null and have correct size
        assertNotNull(dto.getStats());
        assertEquals(1, dto.getStats().size());
        assertNotNull(dto.getExperiences());
        assertEquals(1, dto.getExperiences().size());
        assertNotNull(dto.getEducation());
        assertEquals(1, dto.getEducation().size());
        assertNotNull(dto.getLanguages());
        assertEquals(1, dto.getLanguages().size());
        assertNotNull(dto.getSkills());
        assertEquals(1, dto.getSkills().size());

        verify(userRepository, times(1)).findById(1L);
    }

    @Test
    @DisplayName("Should return empty Optional when user is not found")
    void getAboutMeDto_WhenUserNotFound_ShouldReturnEmptyOptional() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        // When
        Optional<AboutMeDto> result = aboutMeService.getAboutMeDto();

        // Then
        assertFalse(result.isPresent());
        verify(userRepository, times(1)).findById(1L);
    }

    @Test
    @DisplayName("Should handle null collections gracefully")
    void getAboutMeDto_WhenCollectionsAreNull_ShouldHandleGracefully() {
        // Given
        mockUserEntity.setCertifications(null);
        mockUserEntity.setStats(null);
        mockUserEntity.setExperiences(null);
        mockUserEntity.setEducation(null);
        mockUserEntity.setLanguages(null);
        mockUserEntity.setSkills(null);
        
        when(userRepository.findById(1L)).thenReturn(Optional.of(mockUserEntity));

        // When
        Optional<AboutMeDto> result = aboutMeService.getAboutMeDto();

        // Then
        assertTrue(result.isPresent());
        AboutMeDto dto = result.get();
        
        assertEquals(mockUserEntity.getStatus(), dto.getStatus());
        assertEquals(mockUserEntity.getJobTitle(), dto.getJobTitle());
        assertEquals(mockUserEntity.getBio(), dto.getBio());
        assertEquals(mockUserEntity.getGraduationYear(), dto.getGraduationYear());
        assertEquals(mockUserEntity.getGithubUrl(), dto.getGithub());
        assertEquals(mockUserEntity.getMajor(), dto.getMajor());
        
        // Verify null collections are converted to empty lists
        assertNotNull(dto.getCertifications());
        assertTrue(dto.getCertifications().isEmpty());
        assertNotNull(dto.getStats());
        assertTrue(dto.getStats().isEmpty());
        assertNotNull(dto.getExperiences());
        assertTrue(dto.getExperiences().isEmpty());
        assertNotNull(dto.getEducation());
        assertTrue(dto.getEducation().isEmpty());
        assertNotNull(dto.getLanguages());
        assertTrue(dto.getLanguages().isEmpty());
        assertNotNull(dto.getSkills());
        assertTrue(dto.getSkills().isEmpty());

        verify(userRepository, times(1)).findById(1L);
    }

    @Test
    @DisplayName("Should correctly map StatEntity to StatDto")
    void getAboutMeDto_ShouldCorrectlyMapStatEntityToStatDto() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(mockUserEntity));

        // When
        Optional<AboutMeDto> result = aboutMeService.getAboutMeDto();

        // Then
        assertTrue(result.isPresent());
        AboutMeDto dto = result.get();
        List<StatDto> stats = dto.getStats();
        
        assertEquals(1, stats.size());
        StatDto statDto = stats.get(0);
        assertEquals(mockStatEntity.getId(), statDto.getId());
        assertEquals(mockStatEntity.getBusinessId(), statDto.getBusinessId());
        assertEquals(mockStatEntity.getYear(), statDto.getYear());
        assertEquals(mockStatEntity.getLabel(), statDto.getLabel());
        assertEquals(mockStatEntity.getTags(), statDto.getTags());

        verify(userRepository, times(1)).findById(1L);
    }

    @Test
    @DisplayName("Should correctly map ExperienceEntity to ExperienceDto")
    void getAboutMeDto_ShouldCorrectlyMapExperienceEntityToExperienceDto() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(mockUserEntity));

        // When
        Optional<AboutMeDto> result = aboutMeService.getAboutMeDto();

        // Then
        assertTrue(result.isPresent());
        AboutMeDto dto = result.get();
        List<ExperienceDto> experiences = dto.getExperiences();
        
        assertEquals(1, experiences.size());
        ExperienceDto experienceDto = experiences.get(0);
        assertEquals(mockExperienceEntity.getId(), experienceDto.getId());
        assertEquals(mockExperienceEntity.getTitle(), experienceDto.getTitle());
        assertEquals(mockExperienceEntity.getCompany(), experienceDto.getCompany());
        assertEquals(mockExperienceEntity.getPeriod(), experienceDto.getPeriod());
        assertEquals(mockExperienceEntity.getDescription(), experienceDto.getDescription());

        verify(userRepository, times(1)).findById(1L);
    }

    @Test
    @DisplayName("Should correctly map EducationEntity to EducationDto")
    void getAboutMeDto_ShouldCorrectlyMapEducationEntityToEducationDto() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(mockUserEntity));

        // When
        Optional<AboutMeDto> result = aboutMeService.getAboutMeDto();

        // Then
        assertTrue(result.isPresent());
        AboutMeDto dto = result.get();
        List<EducationDto> education = dto.getEducation();
        
        assertEquals(1, education.size());
        EducationDto educationDto = education.get(0);
        assertEquals(mockEducationEntity.getId(), educationDto.getId());
        assertEquals(mockEducationEntity.getDegree(), educationDto.getDegree());
        assertEquals(mockEducationEntity.getSchool(), educationDto.getSchool());
        assertEquals(mockEducationEntity.getPeriod(), educationDto.getPeriod());
        assertEquals(mockEducationEntity.getDescription(), educationDto.getDescription());

        verify(userRepository, times(1)).findById(1L);
    }

    @Test
    @DisplayName("Should correctly map LanguageEntity to LanguageDto")
    void getAboutMeDto_ShouldCorrectlyMapLanguageEntityToLanguageDto() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(mockUserEntity));

        // When
        Optional<AboutMeDto> result = aboutMeService.getAboutMeDto();

        // Then
        assertTrue(result.isPresent());
        AboutMeDto dto = result.get();
        List<LanguageDto> languages = dto.getLanguages();
        
        assertEquals(1, languages.size());
        LanguageDto languageDto = languages.get(0);
        assertEquals(mockLanguageEntity.getId(), languageDto.getId());
        assertEquals(mockLanguageEntity.getName(), languageDto.getName());
        assertEquals(mockLanguageEntity.getLevel(), languageDto.getLevel());

        verify(userRepository, times(1)).findById(1L);
    }

    @Test
    @DisplayName("Should correctly map SkillEntity to SkillDto")
    void getAboutMeDto_ShouldCorrectlyMapSkillEntityToSkillDto() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(mockUserEntity));

        // When
        Optional<AboutMeDto> result = aboutMeService.getAboutMeDto();

        // Then
        assertTrue(result.isPresent());
        AboutMeDto dto = result.get();
        List<SkillDto> skills = dto.getSkills();
        
        assertEquals(1, skills.size());
        SkillDto skillDto = skills.get(0);
        assertEquals(mockSkillEntity.getId(), skillDto.getId());
        assertEquals(mockSkillEntity.getCategory(), skillDto.getCategory());
        assertEquals(mockSkillEntity.getItems(), skillDto.getItems());

        verify(userRepository, times(1)).findById(1L);
    }

    @Test
    @DisplayName("Should handle empty collections correctly")
    void getAboutMeDto_WhenCollectionsAreEmpty_ShouldHandleCorrectly() {
        // Given
        mockUserEntity.setCertifications(Collections.emptyList());
        mockUserEntity.setStats(Collections.emptyList());
        mockUserEntity.setExperiences(Collections.emptyList());
        mockUserEntity.setEducation(Collections.emptyList());
        mockUserEntity.setLanguages(Collections.emptyList());
        mockUserEntity.setSkills(Collections.emptyList());
        
        when(userRepository.findById(1L)).thenReturn(Optional.of(mockUserEntity));

        // When
        Optional<AboutMeDto> result = aboutMeService.getAboutMeDto();

        // Then
        assertTrue(result.isPresent());
        AboutMeDto dto = result.get();
        
        assertNotNull(dto.getCertifications());
        assertTrue(dto.getCertifications().isEmpty());
        assertNotNull(dto.getStats());
        assertTrue(dto.getStats().isEmpty());
        assertNotNull(dto.getExperiences());
        assertTrue(dto.getExperiences().isEmpty());
        assertNotNull(dto.getEducation());
        assertTrue(dto.getEducation().isEmpty());
        assertNotNull(dto.getLanguages());
        assertTrue(dto.getLanguages().isEmpty());
        assertNotNull(dto.getSkills());
        assertTrue(dto.getSkills().isEmpty());

        verify(userRepository, times(1)).findById(1L);
    }

    @Test
    @DisplayName("Should call repository with correct user ID")
    void getAboutMeDto_ShouldCallRepositoryWithCorrectUserId() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(mockUserEntity));

        // When
        aboutMeService.getAboutMeDto();

        // Then
        verify(userRepository, times(1)).findById(1L);
        verifyNoMoreInteractions(userRepository);
    }
}
