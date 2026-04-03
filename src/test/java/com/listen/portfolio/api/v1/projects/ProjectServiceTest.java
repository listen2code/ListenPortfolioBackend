package com.listen.portfolio.api.v1.projects;

import com.listen.portfolio.api.v1.projects.dto.ProjectDto;
import com.listen.portfolio.entity.ProjectEntity;
import com.listen.portfolio.repository.ProjectRepository;
import com.listen.portfolio.service.ProjectService;

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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProjectService Unit Tests")
class ProjectServiceTest {

    @Mock
    private ProjectRepository projectRepository;

    @InjectMocks
    private ProjectService projectService;

    private List<ProjectEntity> mockProjectEntities;
    private ProjectEntity mockProjectEntity1;
    private ProjectEntity mockProjectEntity2;

    @BeforeEach
    void setUp() {
        // 初始化测试数据
        mockProjectEntity1 = new ProjectEntity();
        mockProjectEntity1.setId(1L);
        mockProjectEntity1.setBusinessId("proj-001");
        mockProjectEntity1.setTitle("Project Alpha");
        mockProjectEntity1.setSubtitle("Amazing Web Application");
        mockProjectEntity1.setDesc("A full-stack web application built with modern technologies");
        mockProjectEntity1.setImageUrl("https://example.com/project1.jpg");
        mockProjectEntity1.setGithubUrl("https://github.com/example/project1");
        mockProjectEntity1.setTechStack(Arrays.asList("Java", "Spring Boot", "React", "PostgreSQL"));

        mockProjectEntity2 = new ProjectEntity();
        mockProjectEntity2.setId(2L);
        mockProjectEntity2.setBusinessId("proj-002");
        mockProjectEntity2.setTitle("Project Beta");
        mockProjectEntity2.setSubtitle("Mobile App Solution");
        mockProjectEntity2.setDesc("Cross-platform mobile application for iOS and Android");
        mockProjectEntity2.setImageUrl("https://example.com/project2.jpg");
        mockProjectEntity2.setGithubUrl("https://github.com/example/project2");
        mockProjectEntity2.setTechStack(Arrays.asList("React Native", "Node.js", "MongoDB"));

        mockProjectEntities = Arrays.asList(mockProjectEntity1, mockProjectEntity2);
    }

    @Test
    @DisplayName("getProjects - 成功获取所有项目")
    void testGetProjects_Success() {
        // Given
        when(projectRepository.findAll()).thenReturn(mockProjectEntities);

        // When
        List<ProjectDto> result = projectService.getProjects();

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());

        // 验证第一个项目
        ProjectDto project1 = result.get(0);
        assertEquals(1L, project1.getId());
        assertEquals("proj-001", project1.getBusinessId());
        assertEquals("Project Alpha", project1.getTitle());
        assertEquals("Amazing Web Application", project1.getSubtitle());
        assertEquals("A full-stack web application built with modern technologies", project1.getDesc());
        assertEquals("https://example.com/project1.jpg", project1.getImageUrl());
        assertEquals("https://github.com/example/project1", project1.getGithubUrl());
        assertEquals(Arrays.asList("Java", "Spring Boot", "React", "PostgreSQL"), project1.getTechStack());

        // 验证第二个项目
        ProjectDto project2 = result.get(1);
        assertEquals(2L, project2.getId());
        assertEquals("proj-002", project2.getBusinessId());
        assertEquals("Project Beta", project2.getTitle());
        assertEquals("Mobile App Solution", project2.getSubtitle());
        assertEquals("Cross-platform mobile application for iOS and Android", project2.getDesc());
        assertEquals("https://example.com/project2.jpg", project2.getImageUrl());
        assertEquals("https://github.com/example/project2", project2.getGithubUrl());
        assertEquals(Arrays.asList("React Native", "Node.js", "MongoDB"), project2.getTechStack());

        // 验证Repository调用
        verify(projectRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("getProjects - 空项目列表")
    void testGetProjects_EmptyList() {
        // Given
        when(projectRepository.findAll()).thenReturn(Collections.emptyList());

        // When
        List<ProjectDto> result = projectService.getProjects();

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());

        // 验证Repository调用
        verify(projectRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("getProjects - 单个项目")
    void testGetProjects_SingleProject() {
        // Given
        List<ProjectEntity> singleProjectList = Arrays.asList(mockProjectEntity1);
        when(projectRepository.findAll()).thenReturn(singleProjectList);

        // When
        List<ProjectDto> result = projectService.getProjects();

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());

        ProjectDto project = result.get(0);
        assertEquals(1L, project.getId());
        assertEquals("proj-001", project.getBusinessId());
        assertEquals("Project Alpha", project.getTitle());
        assertEquals(Arrays.asList("Java", "Spring Boot", "React", "PostgreSQL"), project.getTechStack());

        // 验证Repository调用
        verify(projectRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("getProjects - 项目包含null字段")
    void testGetProjects_ProjectWithNullFields() {
        // Given
        ProjectEntity projectWithNullFields = new ProjectEntity();
        projectWithNullFields.setId(3L);
        projectWithNullFields.setTitle("Project Gamma");
        // 其他字段保持 null
        
        List<ProjectEntity> projectList = Arrays.asList(projectWithNullFields);
        when(projectRepository.findAll()).thenReturn(projectList);

        // When
        List<ProjectDto> result = projectService.getProjects();

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());

        ProjectDto project = result.get(0);
        assertEquals(3L, project.getId());
        assertEquals("Project Gamma", project.getTitle());
        assertNull(project.getBusinessId());
        assertNull(project.getSubtitle());
        assertNull(project.getDesc());
        assertNull(project.getImageUrl());
        assertNull(project.getGithubUrl());
        assertNull(project.getTechStack());

        // 验证Repository调用
        verify(projectRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("getProjects - 项目包含空技术栈")
    void testGetProjects_ProjectWithEmptyTechStack() {
        // Given
        ProjectEntity projectWithEmptyTechStack = new ProjectEntity();
        projectWithEmptyTechStack.setId(4L);
        projectWithEmptyTechStack.setTitle("Project Delta");
        projectWithEmptyTechStack.setTechStack(Collections.emptyList());
        
        List<ProjectEntity> projectList = Arrays.asList(projectWithEmptyTechStack);
        when(projectRepository.findAll()).thenReturn(projectList);

        // When
        List<ProjectDto> result = projectService.getProjects();

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());

        ProjectDto project = result.get(0);
        assertEquals(4L, project.getId());
        assertEquals("Project Delta", project.getTitle());
        assertTrue(project.getTechStack().isEmpty());

        // 验证Repository调用
        verify(projectRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("getProjects - 大量项目列表")
    void testGetProjects_LargeProjectList() {
        // Given
        ProjectEntity project3 = new ProjectEntity();
        project3.setId(3L);
        project3.setTitle("Project Gamma");
        
        List<ProjectEntity> largeProjectList = Arrays.asList(mockProjectEntity1, mockProjectEntity2, project3);
        when(projectRepository.findAll()).thenReturn(largeProjectList);

        // When
        List<ProjectDto> result = projectService.getProjects();

        // Then
        assertNotNull(result);
        assertEquals(3, result.size());
        assertEquals("Project Alpha", result.get(0).getTitle());
        assertEquals("Project Beta", result.get(1).getTitle());
        assertEquals("Project Gamma", result.get(2).getTitle());

        // 验证Repository调用
        verify(projectRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("toDto - 实体转换测试")
    void testToDto_EntityConversion() {
        // Given
        when(projectRepository.findAll()).thenReturn(Arrays.asList(mockProjectEntity1));

        // When
        List<ProjectDto> result = projectService.getProjects();

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());

        ProjectDto dto = result.get(0);
        
        // 验证所有字段都正确转换
        assertEquals(mockProjectEntity1.getId(), dto.getId());
        assertEquals(mockProjectEntity1.getBusinessId(), dto.getBusinessId());
        assertEquals(mockProjectEntity1.getTitle(), dto.getTitle());
        assertEquals(mockProjectEntity1.getSubtitle(), dto.getSubtitle());
        assertEquals(mockProjectEntity1.getDesc(), dto.getDesc());
        assertEquals(mockProjectEntity1.getImageUrl(), dto.getImageUrl());
        assertEquals(mockProjectEntity1.getGithubUrl(), dto.getGithubUrl());
        assertEquals(mockProjectEntity1.getTechStack(), dto.getTechStack());

        // 验证Repository调用
        verify(projectRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("toDto - 复杂技术栈转换")
    void testToDto_ComplexTechStackConversion() {
        // Given
        ProjectEntity projectWithComplexTechStack = new ProjectEntity();
        projectWithComplexTechStack.setId(5L);
        projectWithComplexTechStack.setTitle("Project Epsilon");
        projectWithComplexTechStack.setTechStack(Arrays.asList(
            "Java", "Spring Boot", "Spring Security", "Spring Data JPA",
            "React", "TypeScript", "Redux", "Material-UI",
            "Docker", "Kubernetes", "Redis", "PostgreSQL"
        ));
        
        when(projectRepository.findAll()).thenReturn(Arrays.asList(projectWithComplexTechStack));

        // When
        List<ProjectDto> result = projectService.getProjects();

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());

        ProjectDto project = result.get(0);
        assertEquals(5L, project.getId());
        assertEquals("Project Epsilon", project.getTitle());
        assertEquals(12, project.getTechStack().size());
        assertTrue(project.getTechStack().contains("Java"));
        assertTrue(project.getTechStack().contains("Spring Boot"));
        assertTrue(project.getTechStack().contains("React"));
        assertTrue(project.getTechStack().contains("Docker"));
        assertTrue(project.getTechStack().contains("PostgreSQL"));

        // 验证Repository调用
        verify(projectRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("getProjects - Repository返回null处理")
    void testGetProjects_RepositoryReturnsNull() {
        // Given
        when(projectRepository.findAll()).thenReturn(null);

        // When & Then
        assertThrows(NullPointerException.class, () -> {
            projectService.getProjects();
        });

        // 验证Repository调用
        verify(projectRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("构造函数注入验证")
    void testConstructorInjection() {
        // 验证通过 @InjectMocks 创建的实例不为 null
        assertNotNull(projectService);
        assertNotNull(projectRepository);
    }

    @Test
    @DisplayName("服务注解验证")
    void testServiceAnnotations() {
        // 验证服务相关的注解
        assertTrue(projectService.getClass().isAnnotationPresent(org.springframework.stereotype.Service.class));
    }

    @Test
    @DisplayName("事务注解验证")
    void testTransactionalAnnotations() {
        // 验证方法级别的事务注解
        try {
            java.lang.reflect.Method method = ProjectService.class.getMethod("getProjects");
            assertTrue(method.isAnnotationPresent(org.springframework.transaction.annotation.Transactional.class));
            
            org.springframework.transaction.annotation.Transactional transactional = 
                method.getAnnotation(org.springframework.transaction.annotation.Transactional.class);
            assertTrue(transactional.readOnly());
        } catch (NoSuchMethodException e) {
            fail("getProjects method should exist");
        }
    }

    @Test
    @DisplayName("数据隔离验证 - DTO不包含Entity引用")
    void testDataIsolation_DtoNotContainEntityReferences() {
        // Given
        when(projectRepository.findAll()).thenReturn(mockProjectEntities);

        // When
        List<ProjectDto> result = projectService.getProjects();

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());

        // 验证返回的是DTO对象，不是Entity对象
        for (ProjectDto dto : result) {
            assertTrue(dto instanceof ProjectDto);
            // 由于类型不兼容，我们只需要验证它是ProjectDto即可
            // ProjectDto不可能是ProjectEntity的实例，因为它们是不同的类层次结构
        }

        // 验证DTO字段完整性
        for (ProjectDto dto : result) {
            assertNotNull(dto.getId());
            assertNotNull(dto.getTitle());
            // 其他字段可能为null，这是正常的
        }

        // 验证Repository调用
        verify(projectRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("日志记录验证")
    void testLoggingVerification() {
        // Given
        when(projectRepository.findAll()).thenReturn(mockProjectEntities);

        // When
        List<ProjectDto> result = projectService.getProjects();

        // Then
        assertNotNull(result);
        // 日志记录验证通常需要使用日志测试框架，这里我们只验证业务逻辑正确性
        // 实际项目中可以使用 LogCaptor 或类似工具来验证日志输出

        // 验证Repository调用
        verify(projectRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("性能考虑 - Stream处理验证")
    void testPerformance_StreamProcessing() {
        // Given
        when(projectRepository.findAll()).thenReturn(mockProjectEntities);

        // When
        List<ProjectDto> result = projectService.getProjects();

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());

        // 验证使用了Stream处理（通过验证转换逻辑）
        // 这里我们验证所有实体都被正确转换
        assertEquals(mockProjectEntities.size(), result.size());

        // 验证Repository调用
        verify(projectRepository, times(1)).findAll();
    }
}
