package com.company.jobmanagement.evaluation;

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
 * The EvaluationController endpoints return the raw JPA Evaluation entity
 * (not a DTO), which has 4 lazy @ManyToOne associations (user/lead/manager/
 * group). Two bugs found auditing this:
 *
 * 1. Same LazyInitializationException class as the KPI/Group fixes:
 *    EvaluationRepository's queries didn't JOIN FETCH those associations, so
 *    serializing the response outside any transaction (open-in-view=false)
 *    threw as soon as a real evaluation row existed. One level deeper than
 *    the earlier fixes: fetching Evaluation.group wasn't enough — Group
 *    itself has a lazy `memberships` collection Jackson walks into by
 *    default, which also needed @JsonIgnore (see Group.java).
 *
 * 2. Security bug surfaced by fixing #1: serializing Evaluation.user would
 *    have dumped the full User entity — including passwordHash and the
 *    UserDetails-inherited getPassword() — into the response body. Fixed by
 *    introducing EvaluationResponse, a flat DTO (userId/userFullName/...)
 *    that never touches the entity at the HTTP boundary, plus @JsonIgnore
 *    on User as defense in depth for any other place a User ends up nested
 *    in a response.
 *
 * No @Transactional — see MemberKpiDetailIntegrationTest for why.
 */
@SpringBootTest(classes = JobManagementApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class EvaluationSerializationIntegrationTest {

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

    private void assertNoPasswordLeak(Map<String, Object> evaluation) {
        assertThat(evaluation).doesNotContainKeys("passwordHash", "password", "user", "lead", "manager");
    }

    @Test
    void getTeamEvaluations_returns200WithoutLazyErrorOrPasswordLeak() {
        String token = login("lead1@company.com", "admin123");

        ResponseEntity<Map> response = rest.exchange(
                baseUrl("/api/v1/evaluations/team?periodMonth=2026-08"),
                HttpMethod.GET, authHeader(token), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> data = (Map<String, Object>) response.getBody().get("data");
        List<Map<String, Object>> content = (List<Map<String, Object>>) data.get("content");
        assertThat(content).isNotEmpty();
        for (Map<String, Object> eval : content) {
            assertThat(eval.get("userFullName")).isNotNull();
            assertNoPasswordLeak(eval);
        }
    }

    @Test
    void getAllEvaluations_returns200WithoutLazyErrorOrPasswordLeak() {
        String token = login("admin@company.com", "admin123");

        ResponseEntity<Map> response = rest.exchange(
                baseUrl("/api/v1/evaluations/all?periodMonth=2026-08"),
                HttpMethod.GET, authHeader(token), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> data = (Map<String, Object>) response.getBody().get("data");
        List<Map<String, Object>> content = (List<Map<String, Object>>) data.get("content");
        assertThat(content).isNotEmpty();
        for (Map<String, Object> eval : content) {
            assertThat(eval.get("groupName")).isNotNull();
            assertNoPasswordLeak(eval);
        }
    }

    @Test
    void getMyEvaluations_returns200() {
        String token = login("member1@company.com", "admin123");

        ResponseEntity<Map> response = rest.exchange(
                baseUrl("/api/v1/evaluations/my?periodMonth=2026-08"),
                HttpMethod.GET, authHeader(token), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> data = (Map<String, Object>) response.getBody().get("data");
        List<Map<String, Object>> content = (List<Map<String, Object>>) data.get("content");
        assertThat(content).isNotEmpty();
        content.forEach(this::assertNoPasswordLeak);
    }

    @Test
    void getEvaluationById_returns200WithoutPasswordLeak() {
        String token = login("admin@company.com", "admin123");

        ResponseEntity<Map> response = rest.exchange(
                baseUrl("/api/v1/evaluations/1"),
                HttpMethod.GET, authHeader(token), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> eval = (Map<String, Object>) response.getBody().get("data");
        assertThat(eval.get("userFullName")).isNotNull();
        assertNoPasswordLeak(eval);
    }
}
