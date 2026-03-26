package com.listen.portfolio.api.v1.about;

import com.listen.portfolio.api.v1.about.dto.AboutMeDto;
import com.listen.portfolio.common.ApiResponse;
import com.listen.portfolio.common.Constants;
import com.listen.portfolio.service.AboutMeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AboutMeController Unit Tests")
class AboutMeControllerTest {

    @Mock
    private AboutMeService aboutMeService;

    @InjectMocks
    private AboutMeController aboutMeController;

    private AboutMeDto mockAboutMeDto;

    @BeforeEach
    void setUp() {
        mockAboutMeDto = new AboutMeDto();
        mockAboutMeDto.setStatus("Active");
        mockAboutMeDto.setJobTitle("Software Engineer");
        mockAboutMeDto.setBio("Passionate developer");
        mockAboutMeDto.setGraduationYear("2020");
        mockAboutMeDto.setGithub("https://github.com/example");
        mockAboutMeDto.setMajor("Computer Science");
    }

    @Test
    @DisplayName("Should return success response when AboutMe data is found")
    void getAboutMe_WhenDataExists_ShouldReturnSuccessResponse() {
        // Given
        when(aboutMeService.getAboutMeDto()).thenReturn(Optional.of(mockAboutMeDto));

        // When
        ApiResponse<AboutMeDto> response = aboutMeController.getAboutMe();

        // Then
        assertNotNull(response);
        assertEquals("0", response.getResult());
        assertEquals("", response.getMessageId());
        assertEquals("", response.getMessage());
        assertNotNull(response.getBody());
        assertEquals(mockAboutMeDto.getStatus(), response.getBody().getStatus());
        assertEquals(mockAboutMeDto.getJobTitle(), response.getBody().getJobTitle());
        assertEquals(mockAboutMeDto.getBio(), response.getBody().getBio());
        assertEquals(mockAboutMeDto.getGraduationYear(), response.getBody().getGraduationYear());
        assertEquals(mockAboutMeDto.getGithub(), response.getBody().getGithub());
        assertEquals(mockAboutMeDto.getMajor(), response.getBody().getMajor());

        verify(aboutMeService, times(1)).getAboutMeDto();
    }

    @Test
    @DisplayName("Should return error response when AboutMe data is not found")
    void getAboutMe_WhenDataNotExists_ShouldReturnErrorResponse() {
        // Given
        when(aboutMeService.getAboutMeDto()).thenReturn(Optional.empty());

        // When
        ApiResponse<AboutMeDto> response = aboutMeController.getAboutMe();

        // Then
        assertNotNull(response);
        assertEquals("1", response.getResult());
        assertEquals(Constants.DEFAULT_SERVER_ERROR, response.getMessageId());
        assertEquals("About me not found", response.getMessage());
        assertNull(response.getBody());

        verify(aboutMeService, times(1)).getAboutMeDto();
    }

    @Test
    @DisplayName("Should call service method exactly once")
    void getAboutMe_ShouldCallServiceMethodOnce() {
        // Given
        when(aboutMeService.getAboutMeDto()).thenReturn(Optional.of(mockAboutMeDto));

        // When
        aboutMeController.getAboutMe();

        // Then
        verify(aboutMeService, times(1)).getAboutMeDto();
        verifyNoMoreInteractions(aboutMeService);
    }

    @Test
    @DisplayName("Should handle null AboutMeDto gracefully")
    void getAboutMe_WhenServiceReturnsNull_ShouldHandleGracefully() {
        // Given
        when(aboutMeService.getAboutMeDto()).thenReturn(null);

        // When & Then
        assertThrows(NullPointerException.class, () -> {
            aboutMeController.getAboutMe();
        });

        verify(aboutMeService, times(1)).getAboutMeDto();
    }

    @Test
    @DisplayName("Should return response with correct structure for success case")
    void getAboutMe_WhenSuccessful_ShouldReturnCorrectResponseStructure() {
        // Given
        when(aboutMeService.getAboutMeDto()).thenReturn(Optional.of(mockAboutMeDto));

        // When
        ApiResponse<AboutMeDto> response = aboutMeController.getAboutMe();

        // Then
        assertNotNull(response);
        assertNotNull(response.getResult());
        assertNotNull(response.getMessageId());
        assertNotNull(response.getMessage());
        assertNotNull(response.getBody());
        
        // Verify response structure matches expected format
        assertDoesNotThrow(() -> {
            response.getResult();
            response.getMessageId();
            response.getMessage();
            response.getBody();
        });
    }
}
