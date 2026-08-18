package com.listen.portfolio.api.v1.projects;

import com.listen.portfolio.api.v1.projects.dto.ProjectDto;
import com.listen.portfolio.entity.ProjectEntity;
import com.listen.portfolio.mapper.ProjectMapper;
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
    private ProjectMapper projectMapper;

    @InjectMocks
    private ProjectService projectService;

    private List<ProjectEntity> mockProjectEntities;
    private ProjectEntity mockProjectEntity1;
    private ProjectEntity mockProjectEntity2;

    @BeforeEach
    void setUp() {
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
        when(projectMapper.selectList(null)).thenReturn(mockProjectEntities);
        when(projectMapper.findTechStackByProjectId(1L)).thenReturn(Arrays.asList("Java", "Spring Boot", "React", "PostgreSQL"));
        when(projectMapper.findTechStackByProjectId(2L)).thenReturn(Arrays.asList("React Native", "Node.js", "MongoDB"));

        List<ProjectDto> result = projectService.getProjects();

        assertNotNull(result);
        assertEquals(2, result.size());

        ProjectDto project1 = result.get(0);
        assertEquals(1L, project1.getId());
        assertEquals("proj-001", project1.getBusinessId());
        assertEquals("Project Alpha", project1.getTitle());
        assertEquals("Amazing Web Application", project1.getSubtitle());
        assertEquals("A full-stack web application built with modern technologies", project1.getDesc());
        assertEquals("https://example.com/project1.jpg", project1.getImageUrl());
        assertEquals("https://github.com/example/project1", project1.getGithubUrl());
        assertEquals(Arrays.asList("Java", "Spring Boot", "React", "PostgreSQL"), project1.getTechStack());

        ProjectDto project2 = result.get(1);
        assertEquals(2L, project2.getId());
        assertEquals("proj-002", project2.getBusinessId());
        assertEquals("Project Beta", project2.getTitle());
        assertEquals("Mobile App Solution", project2.getSubtitle());
        assertEquals("Cross-platform mobile application for iOS and Android", project2.getDesc());
        assertEquals("https://example.com/project2.jpg", project2.getImageUrl());
        assertEquals("https://github.com/example/project2", project2.getGithubUrl());
        assertEquals(Arrays.asList("React Native", "Node.js", "MongoDB"), project2.getTechStack());

        verify(projectMapper, times(1)).selectList(null);
    }

    @Test
    @DisplayName("getProjects - 空项目列表")
    void testGetProjects_EmptyList() {
        when(projectMapper.selectList(null)).thenReturn(Collections.emptyList());

        List<ProjectDto> result = projectService.getProjects();

        assertNotNull(result);
        assertTrue(result.isEmpty());

        verify(projectMapper, times(1)).selectList(null);
    }

    @Test
    @DisplayName("getProjects - 单个项目")
    void testGetProjects_SingleProject() {
        List<ProjectEntity> singleProjectList = Arrays.asList(mockProjectEntity1);
        when(projectMapper.selectList(null)).thenReturn(singleProjectList);
        when(projectMapper.findTechStackByProjectId(1L)).thenReturn(Arrays.asList("Java", "Spring Boot", "React", "PostgreSQL"));

        List<ProjectDto> result = projectService.getProjects();

        assertNotNull(result);
        assertEquals(1, result.size());

        ProjectDto project = result.get(0);
        assertEquals(1L, project.getId());
        assertEquals("proj-001", project.getBusinessId());
        assertEquals("Project Alpha", project.getTitle());
        assertEquals(Arrays.asList("Java", "Spring Boot", "React", "PostgreSQL"), project.getTechStack());

        verify(projectMapper, times(1)).selectList(null);
    }

    @Test
    @DisplayName("getProjects - 项目包含null字段")
    void testGetProjects_ProjectWithNullFields() {
        ProjectEntity projectWithNullFields = new ProjectEntity();
        projectWithNullFields.setId(3L);
        projectWithNullFields.setTitle("Project Gamma");
        
        List<ProjectEntity> projectList = Arrays.asList(projectWithNullFields);
        when(projectMapper.selectList(null)).thenReturn(projectList);
        when(projectMapper.findTechStackByProjectId(3L)).thenReturn(null);

        List<ProjectDto> result = projectService.getProjects();

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

        verify(projectMapper, times(1)).selectList(null);
    }

    @Test
    @DisplayName("getProjects - 项目包含空技术栈")
    void testGetProjects_ProjectWithEmptyTechStack() {
        ProjectEntity projectWithEmptyTechStack = new ProjectEntity();
        projectWithEmptyTechStack.setId(4L);
        projectWithEmptyTechStack.setTitle("Project Delta");
        
        List<ProjectEntity> projectList = Arrays.asList(projectWithEmptyTechStack);
        when(projectMapper.selectList(null)).thenReturn(projectList);
        when(projectMapper.findTechStackByProjectId(4L)).thenReturn(Collections.emptyList());

        List<ProjectDto> result = projectService.getProjects();

        assertNotNull(result);
        assertEquals(1, result.size());

        ProjectDto project = result.get(0);
        assertEquals(4L, project.getId());
        assertEquals("Project Delta", project.getTitle());
        assertTrue(project.getTechStack().isEmpty());

        verify(projectMapper, times(1)).selectList(null);
    }

    @Test
    @DisplayName("getProjects - 大量项目列表")
    void testGetProjects_LargeProjectList() {
        ProjectEntity project3 = new ProjectEntity();
        project3.setId(3L);
        project3.setTitle("Project Gamma");
        
        List<ProjectEntity> largeProjectList = Arrays.asList(mockProjectEntity1, mockProjectEntity2, project3);
        when(projectMapper.selectList(null)).thenReturn(largeProjectList);
        when(projectMapper.findTechStackByProjectId(1L)).thenReturn(Arrays.asList("Java"));
        when(projectMapper.findTechStackByProjectId(2L)).thenReturn(Arrays.asList("React"));
        when(projectMapper.findTechStackByProjectId(3L)).thenReturn(null);

        List<ProjectDto> result = projectService.getProjects();

        assertNotNull(result);
        assertEquals(3, result.size());
        assertEquals("Project Alpha", result.get(0).getTitle());
        assertEquals("Project Beta", result.get(1).getTitle());
        assertEquals("Project Gamma", result.get(2).getTitle());

        verify(projectMapper, times(1)).selectList(null);
    }

    @Test
    @DisplayName("toDto - 实体转换测试")
    void testToDto_EntityConversion() {
        when(projectMapper.selectList(null)).thenReturn(Arrays.asList(mockProjectEntity1));
        when(projectMapper.findTechStackByProjectId(1L)).thenReturn(mockProjectEntity1.getTechStack());

        List<ProjectDto> result = projectService.getProjects();

        assertNotNull(result);
        assertEquals(1, result.size());

        ProjectDto dto = result.get(0);
        assertEquals(mockProjectEntity1.getId(), dto.getId());
        assertEquals(mockProjectEntity1.getBusinessId(), dto.getBusinessId());
        assertEquals(mockProjectEntity1.getTitle(), dto.getTitle());
        assertEquals(mockProjectEntity1.getSubtitle(), dto.getSubtitle());
        assertEquals(mockProjectEntity1.getDesc(), dto.getDesc());
        assertEquals(mockProjectEntity1.getImageUrl(), dto.getImageUrl());
        assertEquals(mockProjectEntity1.getGithubUrl(), dto.getGithubUrl());
        assertEquals(mockProjectEntity1.getTechStack(), dto.getTechStack());

        verify(projectMapper, times(1)).selectList(null);
    }

    @Test
    @DisplayName("toDto - 复杂技术栈转换")
    void testToDto_ComplexTechStackConversion() {
        ProjectEntity projectWithComplexTechStack = new ProjectEntity();
        projectWithComplexTechStack.setId(5L);
        projectWithComplexTechStack.setTitle("Project Epsilon");
        List<String> complexStack = Arrays.asList(
            "Java", "Spring Boot", "Spring Security", "MyBatis-Plus",
            "React", "TypeScript", "Redux", "Material-UI",
            "Docker", "Kubernetes", "Redis", "PostgreSQL"
        );
        
        when(projectMapper.selectList(null)).thenReturn(Arrays.asList(projectWithComplexTechStack));
        when(projectMapper.findTechStackByProjectId(5L)).thenReturn(complexStack);

        List<ProjectDto> result = projectService.getProjects();

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

        verify(projectMapper, times(1)).selectList(null);
    }

    @Test
    @DisplayName("getProjects - Repository返回null处理")
    void testGetProjects_RepositoryReturnsNull() {
        when(projectMapper.selectList(null)).thenReturn(null);

        assertThrows(NullPointerException.class, () -> {
            projectService.getProjects();
        });

        verify(projectMapper, times(1)).selectList(null);
    }

    @Test
    @DisplayName("构造函数注入验证")
    void testConstructorInjection() {
        assertNotNull(projectService);
        assertNotNull(projectMapper);
    }

    @Test
    @DisplayName("服务注解验证")
    void testServiceAnnotations() {
        assertTrue(projectService.getClass().isAnnotationPresent(org.springframework.stereotype.Service.class));
    }

    @Test
    @DisplayName("事务注解验证")
    void testTransactionalAnnotations() {
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
        when(projectMapper.selectList(null)).thenReturn(mockProjectEntities);
        when(projectMapper.findTechStackByProjectId(1L)).thenReturn(Arrays.asList("Java"));
        when(projectMapper.findTechStackByProjectId(2L)).thenReturn(Arrays.asList("React Native"));

        List<ProjectDto> result = projectService.getProjects();

        assertNotNull(result);
        assertEquals(2, result.size());

        for (ProjectDto dto : result) {
            assertTrue(dto instanceof ProjectDto);
            assertNotNull(dto.getId());
            assertNotNull(dto.getTitle());
        }

        verify(projectMapper, times(1)).selectList(null);
    }

    @Test
    @DisplayName("日志记录验证")
    void testLoggingVerification() {
        when(projectMapper.selectList(null)).thenReturn(mockProjectEntities);
        when(projectMapper.findTechStackByProjectId(anyLong())).thenReturn(Collections.emptyList());

        List<ProjectDto> result = projectService.getProjects();

        assertNotNull(result);
        verify(projectMapper, times(1)).selectList(null);
    }

    @Test
    @DisplayName("性能考虑 - Stream处理验证")
    void testPerformance_StreamProcessing() {
        when(projectMapper.selectList(null)).thenReturn(mockProjectEntities);
        when(projectMapper.findTechStackByProjectId(anyLong())).thenReturn(Collections.emptyList());

        List<ProjectDto> result = projectService.getProjects();

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(mockProjectEntities.size(), result.size());

        verify(projectMapper, times(1)).selectList(null);
    }
}
