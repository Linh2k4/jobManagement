package com.company.jobmanagement.user;

import com.company.jobmanagement.JobManagementApplication;
import com.company.jobmanagement.dto.request.LoginRequest;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The profile form (Cài đặt → Hồ sơ) collected phone/birthDate/address/bio
 * but User had no columns for them and the FE only console.log'd — covers
 * the new PUT /users/me end to end: saves, and a follow-up GET /users/me
 * reflects the same values (persisted, not just echoed back).
 */
@SpringBootTest(classes = JobManagementApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class UpdateProfileIntegrationTest {

    @LocalServerPort
    private int port;

    private final TestRestTemplate rest = new TestRestTemplate();

    private String baseUrl(String path) {
        return "http://localhost:" + port + path;
    }

    private String token(String email, String password) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<Map> response = rest.postForEntity(baseUrl("/api/v1/auth/login"), new HttpEntity<>(new LoginRequest(email, password), headers), Map.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> data = (Map<String, Object>) response.getBody().get("data");
        return (String) data.get("access_token");
    }

    private HttpEntity<Map<String, Object>> body(String token, Map<String, Object> payload) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(payload, headers);
    }

    @Test
    void updateProfile_persistsAndIsReflectedOnGetMe() {
        String token = token("member2@company.com", "admin123");

        ResponseEntity<Map> update = rest.exchange(
                baseUrl("/api/v1/users/me"), HttpMethod.PUT,
                body(token, Map.of("phone", "0909999999", "birthDate", "1999-01-15", "address", "Test address", "bio", "Test bio")),
                Map.class);
        assertThat(update.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> updated = (Map<String, Object>) update.getBody().get("data");
        assertThat(updated.get("phone")).isEqualTo("0909999999");
        assertThat(updated.get("bio")).isEqualTo("Test bio");

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        ResponseEntity<Map> get = rest.exchange(baseUrl("/api/v1/users/me"), HttpMethod.GET, new HttpEntity<>(headers), Map.class);
        Map<String, Object> data = (Map<String, Object>) get.getBody().get("data");
        assertThat(data.get("phone")).isEqualTo("0909999999");
        assertThat(data.get("address")).isEqualTo("Test address");
        assertThat(data.get("birthDate")).isEqualTo("1999-01-15");
    }
}
