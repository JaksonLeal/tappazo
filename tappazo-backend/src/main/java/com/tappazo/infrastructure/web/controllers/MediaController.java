package com.tappazo.infrastructure.web.controllers;

import com.tappazo.application.port.out.FileStoragePort;
import com.tappazo.infrastructure.security.UserPrincipal;
import com.tappazo.infrastructure.web.dto.WebDTOs.PresignedUrlRequest;
import com.tappazo.infrastructure.web.dto.WebDTOs.PresignedUrlResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.Objects;
import java.util.UUID;

/**
 * Controlador REST para operaciones multimedia y almacenamiento S3/MinIO.
 * Soporta la generación de URLs prefirmadas de subida y consulta de archivos según la Sección 26.
 */
@RestController
@RequestMapping("/api/v1/media")
public class MediaController {

    private final FileStoragePort fileStoragePort;

    public MediaController(FileStoragePort fileStoragePort) {
        this.fileStoragePort = Objects.requireNonNull(fileStoragePort);
    }

    /**
     * Genera una URL prefirmada PUT para que el cliente suba directamente su imagen (reveal, avatar, comprobante).
     */
    @PostMapping("/presigned-upload")
    public ResponseEntity<PresignedUrlResponse> generatePresignedUploadUrl(
            @Valid @RequestBody PresignedUrlRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        if (principal == null) {
            throw new AccessDeniedException("No autenticado");
        }

        String rawFolder = (request.folder() != null && !request.folder().isBlank()) ? request.folder() : "uploads";
        String folder = rawFolder.replaceAll("[^a-zA-Z0-9_-]", "");
        String rawFileName = request.fileName() != null ? request.fileName() : "file.jpg";
        String cleanFileName = rawFileName.replaceAll("[^a-zA-Z0-9._-]", "");
        String fileKey = folder + "/" + UUID.randomUUID() + "-" + cleanFileName;

        Duration uploadExpiry = Duration.ofMinutes(15);
        Duration downloadExpiry = Duration.ofHours(24);

        String uploadUrl = fileStoragePort.generatePresignedUploadUrl(fileKey, uploadExpiry);
        String downloadUrl = fileStoragePort.generatePresignedDownloadUrl(fileKey, downloadExpiry);

        return ResponseEntity.ok(new PresignedUrlResponse(uploadUrl, fileKey, downloadUrl));
    }

    /**
     * Genera una URL prefirmada GET para visualizar un archivo protegido.
     */
    @GetMapping("/presigned-download")
    public ResponseEntity<String> generatePresignedDownloadUrl(
            @RequestParam String fileKey,
            @AuthenticationPrincipal UserPrincipal principal) {
        if (principal == null) {
            throw new AccessDeniedException("No autenticado");
        }

        String downloadUrl = fileStoragePort.generatePresignedDownloadUrl(fileKey, Duration.ofHours(2));
        return ResponseEntity.ok(downloadUrl);
    }
}
