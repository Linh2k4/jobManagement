package com.company.jobmanagement.kpi;

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
 * End-to-end regression test for the KPI member-detail popup (Scope.md §13,
 * multi-group). Reproduces exactly the request the FE sends from
 * "Thành viên & KPI" → "Chi tiết" that used to fail with HTTP 500
 * (LazyInitializationException on KpiComponent.group — see
 * KpiCacheService.getOrCalculateKpi).
 *
 * Deliberately does NOT use @Transactional: wrapping the test in a
 * transaction would keep the Hibernate session open for the whole test and
 * hide the bug it exists to catch. It talks to the app over real HTTP
 * (RANDOM_PORT), exactly like the Angular client, against the local dev
 * Postgres (see application-test.yml) that already has the Liquibase dev
 * seed data (admin@company.com / admin123, "Member One" in "Group A").
 */
@SpringBootTest(classes = JobManagementApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class MemberKpiDetailIntegrationTest {

    @LocalServerPort
    private int port;

    private final TestRestTemplate rest = new TestRestTemplate();

    private static final String ADMIN_EMAIL = "admin@company.com";
    private static final String ADMIN_PASSWORD = "admin123";
    // Seed data: "Member One" (MEMBER) belongs to "Group A" (id 3) among others (Scope.md §13).
    private static final long MEMBER_USER_ID = 4L;
    private static final long GROUP_ID = 3L;

    private String baseUrl(String path) {
        return "http://localhost:" + port + path;
    }

    private String login() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        LoginRequest body = new LoginRequest(ADMIN_EMAIL, ADMIN_PASSWORD);

        ResponseEntity<Map> response = rest.postForEntity(
                baseUrl("/api/v1/auth/login"), new HttpEntity<>(body, headers), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> data = (Map<String, Object>) response.getBody().get("data");
        return (String) data.get("access_token");
    }

    @Test
    void getMemberKpiDetail_withGroupId_returns200NotLazyInitError() {
        String token = login();

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);

        ResponseEntity<Map> response = rest.exchange(
                baseUrl("/api/v1/kpi/members/" + MEMBER_USER_ID + "?periodMonth=2026-08&groupId=" + GROUP_ID),
                HttpMethod.GET, new HttpEntity<>(headers), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        Map<String, Object> data = (Map<String, Object>) response.getBody().get("data");
        assertThat(((Number) data.get("userId")).longValue()).isEqualTo(MEMBER_USER_ID);
        assertThat(((Number) data.get("groupId")).longValue()).isEqualTo(GROUP_ID);
        assertThat(data.get("groupName")).isEqualTo("Group A");
    }
}
