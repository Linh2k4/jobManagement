package com.company.jobmanagement.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.core.io.Resource;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Base controller providing common response helper methods.
 * <p>
 * Provides consistent HTTP response patterns for all API endpoints:
 * - Success responses (200, 201, 204)
 * - File downloads with proper headers
 * - Default HTTP status handling
 * </p>
 *
 * @author Khánh VD
 * @since 2026-06-29
 */
public abstract class BaseController {

    /**
     * Returns a ResponseEntity with HTTP 200 OK and response body.
     *
     * @param data response data
     * @param <T> response type
     * @return ResponseEntity with 200 status and data
     */
    protected <T> ResponseEntity<T> ok(T data) {
        return ResponseEntity.ok(data);
    }

    /**
     * Returns a ResponseEntity from Optional, 200 OK if present, 404 NOT_FOUND if empty.
     *
     * @param optional optional response data
     * @param <T> response type
     * @return ResponseEntity with 200 if present, 404 if empty
     */
    protected <T> ResponseEntity<T> ok(Optional<T> optional) {
        return optional.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Returns a ResponseEntity with HTTP 201 CREATED and response body.
     *
     * @param data created resource data
     * @param <T> response type
     * @return ResponseEntity with 201 status and data
     */
    protected <T> ResponseEntity<T> created(T data) {
        return ResponseEntity.status(HttpStatus.CREATED).body(data);
    }

    /**
     * Returns a ResponseEntity with HTTP 204 NO_CONTENT (empty response).
     *
     * @return ResponseEntity with 204 status and no body
     */
    protected ResponseEntity<Void> noContent() {
        return ResponseEntity.noContent().build();
    }

    /**
     * Returns a simple success response with status message.
     *
     * @return ResponseEntity with {"status": "OK"}
     */
    protected ResponseEntity<Map<String, String>> success() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "OK");
        return ResponseEntity.ok(response);
    }

    /**
     * Returns a ResponseEntity for file download with proper Content-Disposition header.
     *
     * @param resource the file resource to download
     * @param filename the filename for Content-Disposition header
     * @return ResponseEntity with OCTET_STREAM content type and download headers
     */
    protected ResponseEntity<Resource> download(Resource resource, String filename) {
        return ResponseEntity.ok()
                .header("Content-Type", "application/octet-stream")
                .header("Content-Disposition", "attachment; filename=\"" + filename + "\"")
                .body(resource);
    }

    /**
     * Returns a ResponseEntity with custom HTTP status.
     *
     * @param data response data
     * @param status HTTP status code
     * @param <T> response type
     * @return ResponseEntity with custom status and data
     */
    protected <T> ResponseEntity<T> response(T data, HttpStatus status) {
        return ResponseEntity.status(status).body(data);
    }
}
