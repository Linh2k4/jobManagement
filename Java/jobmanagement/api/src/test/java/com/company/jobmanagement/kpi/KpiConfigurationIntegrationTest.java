package com.company.jobmanagement.kpi;

import com.company.jobmanagement.JobManagementApplication;
import com.company.jobmanagement.dto.request.LoginRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Scope.md §7.6 — KPI formula weight config. GET/PUT already existed and
 * are genuinely wired into KpiCalculationService (not cosmetic), but PUT
 * only checked that each weight group summed to 100%, not the per-field
 * min/max bounds the spec's table calls out (e.g. autoScoreWeight 20-70%).
 * That let something like autoScoreWeight=99/leadScoreWeight=0.5/
 * managerScoreWeight=0.5 through, since it still sums to 100.
 */
@SpringBootTest(classes = JobManagementApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class KpiConfigurationIntegrationTest {

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

    private HttpEntity<Map<String, Object>> body(String token, Map<String, Object> payload) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(payload, headers);
    }

    @AfterEach
    void restoreDefaults() {
        String token = login("admin@company.com", "admin123");
        rest.exchange(baseUrl("/api/v1/kpi/admin/config"), HttpMethod.PUT,
                body(token, Map.of("autoScoreWeight", 40, "leadScoreWeight", 35, "managerScoreWeight", 25,
                        "wcrWeight", 60, "viWeight", 25, "eaWeight", 15, "workingHoursPerDay", 8)),
                Map.class);
    }

    @Test
    void updateConfig_sumsTo100ButOutOfRange_isRejected() {
        String token = login("admin@company.com", "admin123");

        ResponseEntity<Map> response = rest.exchange(
                baseUrl("/api/v1/kpi/admin/config"), HttpMethod.PUT,
                body(token, Map.of("autoScoreWeight", 99, "leadScoreWeight", 0.5, "managerScoreWeight", 0.5)),
                Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat((Boolean) response.getBody().get("success")).isFalse();
    }

    @Test
    void updateConfig_validWeightsWithinRange_succeedsAndPersists() {
        String token = login("admin@company.com", "admin123");

        ResponseEntity<Map> update = rest.exchange(
                baseUrl("/api/v1/kpi/admin/config"), HttpMethod.PUT,
                body(token, Map.of("autoScoreWeight", 45, "leadScoreWeight", 30, "managerScoreWeight", 25)),
                Map.class);
        assertThat(update.getStatusCode()).isEqualTo(HttpStatus.OK);

        ResponseEntity<Map> get = rest.exchange(
                baseUrl("/api/v1/kpi/admin/config"), HttpMethod.GET, new HttpEntity<>(authHeaders(token)), Map.class);
        Map<String, Object> data = (Map<String, Object>) get.getBody().get("data");
        assertThat((Double) data.get("autoScoreWeight")).isEqualTo(45.0);
    }

    @Test
    void updateConfig_asMember_isForbidden() {
        String token = login("member1@company.com", "admin123");

        ResponseEntity<Map> response = rest.exchange(
                baseUrl("/api/v1/kpi/admin/config"), HttpMethod.PUT,
                body(token, Map.of("autoScoreWeight", 45, "leadScoreWeight", 30, "managerScoreWeight", 25)),
                Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    private HttpHeaders authHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return headers;
    }
}
