package com.company.jobmanagement.user;

import com.company.jobmanagement.JobManagementApplication;
import com.company.jobmanagement.dto.request.LoginRequest;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The Nhân sự (HR admin) FE screen used to render 4 hardcoded mock rows —
 * there was no GET /api/v1/users at all to wire it to. This covers the new
 * list endpoint end-to-end: real seed data, correct role gating (Manager
 * only), groupName resolved per role, and no password leak (same class of
 * bug fixed in Evaluation — User is mapped through UserInfoResponse here,
 * not serialized raw, but worth locking in given the pattern).
 */
@SpringBootTest(classes = JobManagementApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class UserListIntegrationTest {

    @LocalServerPort
    private int port;

    private final TestRestTemplate rest = new TestRestTemplate();

    private String baseUrl(String path) {
        return "http://localhost:" + port + path;
    }

    private String login(String email, String password) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<Map> response = rest.postForEntity(
                baseUrl("/api/v1/auth/login"), new HttpEntity<>(new LoginRequest(email, password), headers), Map.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> data = (Map<String, Object>) response.getBody().get("data");
        return (String) data.get("access_token");
    }

    private HttpEntity<Void> authHeader(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return new HttpEntity<>(headers);
    }

    @Test
    void listUsers_asManager_returnsEveryUserWithGroupNameAndNoPassword() {
        String token = login("admin@company.com", "admin123");

        ResponseEntity<Map> response = rest.exchange(
                baseUrl("/api/v1/users"), HttpMethod.GET, authHeader(token), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        List<Map<String, Object>> users = (List<Map<String, Object>>) response.getBody().get("data");
        assertThat(users).hasSizeGreaterThanOrEqualTo(8);

        Map<String, Object> member1 = users.stream()
                .filter(u -> "member1@company.com".equals(u.get("email")))
                .findFirst().orElseThrow();
        assertThat(member1.get("groupName")).isEqualTo("Group A");
        assertThat(member1).doesNotContainKeys("passwordHash", "password");

        Map<String, Object> lead1 = users.stream()
                .filter(u -> "lead1@company.com".equals(u.get("email")))
                .findFirst().orElseThrow();
        assertThat((String) lead1.get("groupName")).contains("Lead One");
    }

    @Test
    void listUsers_asMember_isForbidden() {
        String token = login("member1@company.com", "admin123");

        ResponseEntity<Map> response = rest.exchange(
                baseUrl("/api/v1/users"), HttpMethod.GET, authHeader(token), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void getCurrentUser_returnsTheAuthenticatedUser_notAnEmptyStub() {
        String token = login("lead1@company.com", "admin123");

        ResponseEntity<Map> response = rest.exchange(
                baseUrl("/api/v1/users/me"), HttpMethod.GET, authHeader(token), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> me = (Map<String, Object>) response.getBody().get("data");
        assertThat(me.get("email")).isEqualTo("lead1@company.com");
        assertThat(me.get("id")).isNotNull();
    }

    @Test
    void deactivateThenActivate_roundTrips() {
        String token = login("admin@company.com", "admin123");

        ResponseEntity<Void> deactivate = rest.exchange(
                baseUrl("/api/v1/users/8"), HttpMethod.DELETE, authHeader(token), Void.class);
        assertThat(deactivate.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        ResponseEntity<Map> activate = rest.exchange(
                baseUrl("/api/v1/users/8/activate"), HttpMethod.PATCH, authHeader(token), Map.class);
        assertThat(activate.getStatusCode()).isEqualTo(HttpStatus.OK);

        ResponseEntity<Map> listResponse = rest.exchange(
                baseUrl("/api/v1/users"), HttpMethod.GET, authHeader(token), Map.class);
        List<Map<String, Object>> users = (List<Map<String, Object>>) listResponse.getBody().get("data");
        Map<String, Object> member5 = users.stream()
                .filter(u -> Integer.valueOf(8).equals(u.get("id")))
                .findFirst().orElseThrow();
        assertThat(member5.get("isActive")).isEqualTo(true);
    }
}
