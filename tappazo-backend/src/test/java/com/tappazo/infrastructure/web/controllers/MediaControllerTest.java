package com.tappazo.infrastructure.web.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tappazo.application.port.out.FileStoragePort;
import com.tappazo.infrastructure.security.JwtTokenProvider;
import com.tappazo.infrastructure.web.dto.WebDTOs.PresignedUrlRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Duration;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("MediaController REST Integration Tests")
class MediaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private FileStoragePort fileStoragePort;

    private String validToken;

    @BeforeEach
    void setUp() {
        validToken = jwtTokenProvider.generateToken("user-123", "user@test.com", "TestUser");
    }

    @Test
    @DisplayName("POST /api/v1/media/presigned-upload returns presigned URL and file key")
    void generatePresignedUploadUrl_success() throws Exception {
        when(fileStoragePort.generatePresignedUploadUrl(anyString(), any(Duration.class)))
                .thenReturn("http://localhost:9000/tappazo-media/reveals/test.jpg?put-signature");
        when(fileStoragePort.generatePresignedDownloadUrl(anyString(), any(Duration.class)))
                .thenReturn("http://localhost:9000/tappazo-media/reveals/test.jpg?get-signature");

        PresignedUrlRequest request = new PresignedUrlRequest("photo.jpg", "image/jpeg", "reveals");

        mockMvc.perform(post("/api/v1/media/presigned-upload")
                        .header("Authorization", "Bearer " + validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.uploadUrl").value("http://localhost:9000/tappazo-media/reveals/test.jpg?put-signature"))
                .andExpect(jsonPath("$.fileKey").isNotEmpty())
                .andExpect(jsonPath("$.downloadUrl").value("http://localhost:9000/tappazo-media/reveals/test.jpg?get-signature"));
    }

    @Test
    @DisplayName("POST /api/v1/media/presigned-upload requires authentication")
    void generatePresignedUploadUrl_unauthorized() throws Exception {
        PresignedUrlRequest request = new PresignedUrlRequest("photo.jpg", "image/jpeg", "reveals");

        mockMvc.perform(post("/api/v1/media/presigned-upload")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /api/v1/media/presigned-download returns presigned GET URL")
    void generatePresignedDownloadUrl_success() throws Exception {
        when(fileStoragePort.generatePresignedDownloadUrl(anyString(), any(Duration.class)))
                .thenReturn("http://localhost:9000/tappazo-media/avatars/user.jpg?get-token");

        mockMvc.perform(get("/api/v1/media/presigned-download")
                        .header("Authorization", "Bearer " + validToken)
                        .param("fileKey", "avatars/user.jpg"))
                .andExpect(status().isOk());
    }
}
