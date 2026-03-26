package com.listen.portfolio.api.v1.projects;

import com.listen.portfolio.api.v1.projects.dto.ProjectDto;
import com.listen.portfolio.common.ApiResponse;
import com.listen.portfolio.common.Constants;
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
@DisplayName("ProjectController Unit Tests")
class ProjectControllerTest {

    @Mock
    private ProjectService projectService;

    @InjectMocks
    private ProjectController projectController;

    private List<ProjectDto> mockProjectList;
    private ProjectDto mockProject1;
    private ProjectDto mockProject2;

    @BeforeEach
    void setUp() {
        // 初始化测试数据
        mockProject1 = new ProjectDto();
        mockProject1.setId(1L);
        mockProject1.setBusinessId("proj-001");
        mockProject1.setTitle("Project Alpha");
        mockProject1.setSubtitle("Amazing Web Application");
        mockProject1.setDesc("A full-stack web application built with modern technologies");
        mockProject1.setImageUrl("https://example.com/project1.jpg");
        mockProject1.setGithubUrl("https://github.com/example/project1");
        mockProject1.setTechStack(Arrays.asList("Java", "Spring Boot", "React", "PostgreSQL"));

        mockProject2 = new ProjectDto();
        mockProject2.setId(2L);
        mockProject2.setBusinessId("proj-002");
        mockProject2.setTitle("Project Beta");
        mockProject2.setSubtitle("Mobile App Solution");
        mockProject2.setDesc("Cross-platform mobile application for iOS and Android");
        mockProject2.setImageUrl("https://example.com/project2.jpg");
        mockProject2.setGithubUrl("https://github.com/example/project2");
        mockProject2.setTechStack(Arrays.asList("React Native", "Node.js", "MongoDB"));

        mockProjectList = Arrays.asList(mockProject1, mockProject2);
    }

    @Test
    @DisplayName("getProjects - 成功获取项目列表")
    void testGetProjects_Success() {
        // Given
        when(projectService.getProjects()).thenReturn(mockProjectList);

        // When
        ApiResponse<List<ProjectDto>> response = projectController.getProjects();

        // Then
        assertNotNull(response);
        assertEquals("0", response.getResult());
        assertEquals("", response.getMessageId());
        assertEquals("", response.getMessage());
        assertNotNull(response.getBody());
        
        List<ProjectDto> projects = response.getBody();
        assertEquals(2, projects.size());
        
        // 验证第一个项目
        ProjectDto project1 = projects.get(0);
        assertEquals(1L, project1.getId());
        assertEquals("proj-001", project1.getBusinessId());
        assertEquals("Project Alpha", project1.getTitle());
        assertEquals("Amazing Web Application", project1.getSubtitle());
        assertEquals("A full-stack web application built with modern technologies", project1.getDesc());
        assertEquals("https://example.com/project1.jpg", project1.getImageUrl());
        assertEquals("https://github.com/example/project1", project1.getGithubUrl());
        assertEquals(Arrays.asList("Java", "Spring Boot", "React", "PostgreSQL"), project1.getTechStack());
        
        // 验证第二个项目
        ProjectDto project2 = projects.get(1);
        assertEquals(2L, project2.getId());
        assertEquals("proj-002", project2.getBusinessId());
        assertEquals("Project Beta", project2.getTitle());
        assertEquals("Mobile App Solution", project2.getSubtitle());
        assertEquals("Cross-platform mobile application for iOS and Android", project2.getDesc());
        assertEquals("https://example.com/project2.jpg", project2.getImageUrl());
        assertEquals("https://github.com/example/project2", project2.getGithubUrl());
        assertEquals(Arrays.asList("React Native", "Node.js", "MongoDB"), project2.getTechStack());

        // 验证服务调用
        verify(projectService, times(1)).getProjects();
    }

    @Test
    @DisplayName("getProjects - 项目列表为空返回错误")
    void testGetProjects_EmptyList() {
        // Given
        when(projectService.getProjects()).thenReturn(Collections.emptyList());

        // When
        ApiResponse<List<ProjectDto>> response = projectController.getProjects();

        // Then
        assertNotNull(response);
        assertEquals("1", response.getResult());
        assertEquals(Constants.DEFAULT_SERVER_ERROR, response.getMessageId());
        assertEquals("No projects found", response.getMessage());
        assertNull(response.getBody());

        // 验证服务调用
        verify(projectService, times(1)).getProjects();
    }

    @Test
    @DisplayName("getProjects - 服务返回null时处理")
    void testGetProjects_NullResponse() {
        // Given
        when(projectService.getProjects()).thenReturn(null);

        // When & Then
        assertThrows(NullPointerException.class, () -> {
            projectController.getProjects();
        });

        // 验证服务调用
        verify(projectService, times(1)).getProjects();
    }

    @Test
    @DisplayName("getProjects - 单个项目列表")
    void testGetProjects_SingleProject() {
        // Given
        List<ProjectDto> singleProjectList = Arrays.asList(mockProject1);
        when(projectService.getProjects()).thenReturn(singleProjectList);

        // When
        ApiResponse<List<ProjectDto>> response = projectController.getProjects();

        // Then
        assertNotNull(response);
        assertEquals("0", response.getResult());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        assertEquals("Project Alpha", response.getBody().get(0).getTitle());

        // 验证服务调用
        verify(projectService, times(1)).getProjects();
    }

    @Test
    @DisplayName("getProjects - 大量项目列表")
    void testGetProjects_LargeProjectList() {
        // Given
        List<ProjectDto> largeProjectList = Arrays.asList(mockProject1, mockProject2, mockProject1, mockProject2);
        when(projectService.getProjects()).thenReturn(largeProjectList);

        // When
        ApiResponse<List<ProjectDto>> response = projectController.getProjects();

        // Then
        assertNotNull(response);
        assertEquals("0", response.getResult());
        assertNotNull(response.getBody());
        assertEquals(4, response.getBody().size());

        // 验证服务调用
        verify(projectService, times(1)).getProjects();
    }

    @Test
    @DisplayName("getProjects - 项目包含空技术栈")
    void testGetProjects_ProjectWithEmptyTechStack() {
        // Given
        ProjectDto projectWithEmptyTechStack = new ProjectDto();
        projectWithEmptyTechStack.setId(3L);
        projectWithEmptyTechStack.setTitle("Project Gamma");
        projectWithEmptyTechStack.setTechStack(Collections.emptyList());
        
        List<ProjectDto> projectList = Arrays.asList(projectWithEmptyTechStack);
        when(projectService.getProjects()).thenReturn(projectList);

        // When
        ApiResponse<List<ProjectDto>> response = projectController.getProjects();

        // Then
        assertNotNull(response);
        assertEquals("0", response.getResult());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        assertTrue(response.getBody().get(0).getTechStack().isEmpty());

        // 验证服务调用
        verify(projectService, times(1)).getProjects();
    }

    @Test
    @DisplayName("getProjects - 项目包含null字段")
    void testGetProjects_ProjectWithNullFields() {
        // Given
        ProjectDto projectWithNullFields = new ProjectDto();
        projectWithNullFields.setId(4L);
        projectWithNullFields.setTitle("Project Delta");
        // 其他字段保持 null
        
        List<ProjectDto> projectList = Arrays.asList(projectWithNullFields);
        when(projectService.getProjects()).thenReturn(projectList);

        // When
        ApiResponse<List<ProjectDto>> response = projectController.getProjects();

        // Then
        assertNotNull(response);
        assertEquals("0", response.getResult());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        
        ProjectDto resultProject = response.getBody().get(0);
        assertEquals(4L, resultProject.getId());
        assertEquals("Project Delta", resultProject.getTitle());
        assertNull(resultProject.getSubtitle());
        assertNull(resultProject.getDesc());
        assertNull(resultProject.getImageUrl());
        assertNull(resultProject.getGithubUrl());
        assertNull(resultProject.getTechStack());

        // 验证服务调用
        verify(projectService, times(1)).getProjects();
    }

    @Test
    @DisplayName("构造函数注入验证")
    void testConstructorInjection() {
        // 验证通过 @InjectMocks 创建的实例不为 null
        assertNotNull(projectController);
        assertNotNull(projectService);
    }

    @Test
    @DisplayName("控制器注解验证")
    void testControllerAnnotations() {
        // 验证控制器相关的注解
        assertTrue(projectController.getClass().isAnnotationPresent(org.springframework.web.bind.annotation.RestController.class));
        
        // 验证RequestMapping注解
        org.springframework.web.bind.annotation.RequestMapping requestMapping = 
            projectController.getClass().getAnnotation(org.springframework.web.bind.annotation.RequestMapping.class);
        assertNotNull(requestMapping);
        assertArrayEquals(new String[]{"/v1/projects"}, requestMapping.value());
    }

    @Test
    @DisplayName("响应结构验证 - 成功情况")
    void testResponseStructure_Success() {
        // Given
        when(projectService.getProjects()).thenReturn(mockProjectList);

        // When
        ApiResponse<List<ProjectDto>> response = projectController.getProjects();

        // Then
        assertNotNull(response);
        
        // 验证响应结构完整性
        assertDoesNotThrow(() -> {
            response.getResult();
            response.getMessageId();
            response.getMessage();
            response.getBody();
        });
        
        // 验证响应内容类型
        assertTrue(response.getBody() instanceof List);
        assertFalse(response.getBody().isEmpty());
    }

    @Test
    @DisplayName("响应结构验证 - 错误情况")
    void testResponseStructure_Error() {
        // Given
        when(projectService.getProjects()).thenReturn(Collections.emptyList());

        // When
        ApiResponse<List<ProjectDto>> response = projectController.getProjects();

        // Then
        assertNotNull(response);
        
        // 验证错误响应结构
        assertEquals("1", response.getResult());
        assertEquals(Constants.DEFAULT_SERVER_ERROR, response.getMessageId());
        assertEquals("No projects found", response.getMessage());
        assertNull(response.getBody());
    }

    @Test
    @DisplayName("服务调用验证 - 确保只调用一次")
    void testServiceCallVerification() {
        // Given
        when(projectService.getProjects()).thenReturn(mockProjectList);

        // When
        projectController.getProjects();

        // Then
        verify(projectService, times(1)).getProjects();
        verifyNoMoreInteractions(projectService);
    }
}
