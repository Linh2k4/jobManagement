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
 * Self-service password change (settings page) had no backend endpoint at
 * all — the FE form only console.log'd the values. Covers the new
 * PUT /users/me/password: wrong current password rejected, correct change
 * round-trips (old password stops working, new one logs in), restoring the
 * seed password afterwards so other tests relying on admin123 keep working.
 */
@SpringBootTest(classes = JobManagementApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class ChangePasswordIntegrationTest {

    @LocalServerPort
    private int port;

    private final TestRestTemplate rest = new TestRestTemplate();

    private String baseUrl(String path) {
        return "http://localhost:" + port + path;
    }

    private ResponseEntity<Map> login(String email, String password) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return rest.postForEntity(baseUrl("/api/v1/auth/login"), new HttpEntity<>(new LoginRequest(email, password), headers), Map.class);
    }

    private String token(String email, String password) {
        ResponseEntity<Map> response = login(email, password);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> data = (Map<String, Object>) response.getBody().get("data");
        return (String) data.get("access_token");
    }

    private HttpEntity<Map<String, Object>> changeReq(String token, String current, String next) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(Map.of("currentPassword", current, "newPassword", next), headers);
    }

    @Test
    void changePassword_wrongCurrentPassword_isRejected() {
        String token = token("member4@company.com", "admin123");

        ResponseEntity<Map> response = rest.exchange(
                baseUrl("/api/v1/users/me/password"), HttpMethod.PUT, changeReq(token, "not-the-password", "whatever123"), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void changePassword_correctCurrentPassword_roundTrips() {
        String token = token("member4@company.com", "admin123");

        ResponseEntity<Map> change = rest.exchange(
                baseUrl("/api/v1/users/me/password"), HttpMethod.PUT, changeReq(token, "admin123", "temp-pass-456"), Map.class);
        assertThat(change.getStatusCode()).isEqualTo(HttpStatus.OK);

        assertThat(login("member4@company.com", "admin123").getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(login("member4@company.com", "temp-pass-456").getStatusCode()).isEqualTo(HttpStatus.OK);

        String newToken = token("member4@company.com", "temp-pass-456");
        ResponseEntity<Map> restore = rest.exchange(
                baseUrl("/api/v1/users/me/password"), HttpMethod.PUT, changeReq(newToken, "temp-pass-456", "admin123"), Map.class);
        assertThat(restore.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(login("member4@company.com", "admin123").getStatusCode()).isEqualTo(HttpStatus.OK);
    }
}
