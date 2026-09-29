package com.company.jobmanagement.group;

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
 * Same LazyInitializationException class of bug as
 * {@link com.company.jobmanagement.kpi.MemberKpiDetailIntegrationTest}, found
 * while auditing other GroupMembership.group / .member lazy accesses after
 * that fix: GroupMembershipRepository.findByGroupId and .findByMemberId read
 * gm.getGroup()/.getMember() in the controller's DTO mapping (outside any
 * @Transactional boundary, and spring.jpa.open-in-view=false) without a
 * matching JOIN FETCH — same shape as the KPI bug, just not yet hit because
 * no caller had exercised it with real data. Fixed by adding the missing
 * JOIN FETCH to both queries.
 *
 * Deliberately no @Transactional — see the KPI test for why.
 */
@SpringBootTest(classes = JobManagementApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class GroupMembershipLazyLoadIntegrationTest {

    @LocalServerPort
    private int port;

    private final TestRestTemplate rest = new TestRestTemplate();

    private static final String ADMIN_EMAIL = "admin@company.com";
    private static final String ADMIN_PASSWORD = "admin123";
    // Seed data (Scope.md §13): "Member One" (user id 4) belongs to group 1
    // ("Nhóm của Lead One") and group 3 ("Group A").
    private static final long MEMBER_USER_ID = 4L;
    private static final long GROUP_ID = 1L;

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

    private HttpEntity<Void> authHeader(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return new HttpEntity<>(headers);
    }

    @Test
    void getGroupMembers_returnsGroupNameWithoutLazyInitError() {
        String token = login();

        ResponseEntity<Map> response = rest.exchange(
                baseUrl("/api/v1/groups/" + GROUP_ID + "/members"),
                HttpMethod.GET, authHeader(token), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        List<Map<String, Object>> data = (List<Map<String, Object>>) response.getBody().get("data");
        assertThat(data).isNotEmpty();
        for (Map<String, Object> membership : data) {
            assertThat(membership.get("groupName")).isNotNull();
            Map<String, Object> member = (Map<String, Object>) membership.get("member");
            assertThat(member.get("fullName")).isNotNull();
        }
    }

    @Test
    void getMemberGroups_returnsMemberInfoWithoutLazyInitError() {
        String token = login();

        ResponseEntity<Map> response = rest.exchange(
                baseUrl("/api/v1/members/" + MEMBER_USER_ID + "/groups"),
                HttpMethod.GET, authHeader(token), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        List<Map<String, Object>> data = (List<Map<String, Object>>) response.getBody().get("data");
        assertThat(data).isNotEmpty();
        for (Map<String, Object> membership : data) {
            assertThat(membership.get("groupName")).isNotNull();
            Map<String, Object> member = (Map<String, Object>) membership.get("member");
            assertThat(member.get("fullName")).isEqualTo("Member One");
        }
    }
}
