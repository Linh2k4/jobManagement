package com.company.jobmanagement.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI/Swagger configuration for API documentation.
 * Provides a complete API specification with grouped endpoints by domain/controller.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
            .info(buildInfo())
            .components(buildComponents())
            .addSecurityItem(new SecurityRequirement().addList("bearer-jwt"))
            .addServersItem(new Server()
                .url("http://localhost:8080")
                .description("Development Server"))
            .addServersItem(new Server()
                .url("https://api.example.com")
                .description("Production Server"));
    }

    private Info buildInfo() {
        return new Info()
            .title("Job Management API")
            .version("1.0.0")
            .description("Role-based work/task management system with evaluation and KPI tracking\n\n" +
                "## Features\n" +
                "- User authentication with JWT\n" +
                "- Role-based access control (Manager, Lead, Member)\n" +
                "- Configurable task types and templates\n" +
                "- Multi-step task management\n" +
                "- Task assignment with role-based rules\n" +
                "- Performance evaluation and KPI tracking\n" +
                "- Notifications and reminders\n\n" +
                "## Authentication\n" +
                "All endpoints except `/api/v1/auth/login` require JWT Bearer token in the `Authorization` header.\n\n" +
                "Example: `Authorization: Bearer <token>`")
            .contact(new Contact()
                .name("API Support")
                .email("support@example.com")
                .url("https://example.com"))
            .license(new License()
                .name("Apache 2.0")
                .url("https://www.apache.org/licenses/LICENSE-2.0.html"));
    }

    private Components buildComponents() {
        return new Components()
            .addSecuritySchemes("bearer-jwt", new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .description("JWT Bearer token for authentication. Obtain token from /api/v1/auth/login endpoint."));
    }

    // =========================================================================
    // Swagger Groups (chia theo nhóm chức năng / endpoint trong controller)
    // =========================================================================

    @Bean
    public GroupedOpenApi allApi() {
        return GroupedOpenApi.builder()
            .group("00. Tất cả API (All Endpoints)")
            .pathsToMatch("/api/v1/**")
            .build();
    }

    @Bean
    public GroupedOpenApi authApi() {
        return GroupedOpenApi.builder()
            .group("01. Xác thực (Authentication)")
            .pathsToMatch("/api/v1/auth/**")
            .build();
    }

    @Bean
    public GroupedOpenApi userApi() {
        return GroupedOpenApi.builder()
            .group("02. Người dùng (Users)")
            .pathsToMatch("/api/v1/users/**")
            .build();
    }

    @Bean
    public GroupedOpenApi taskApi() {
        return GroupedOpenApi.builder()
            .group("03. Công việc & Quy trình (Tasks & Subtasks)")
            .pathsToMatch(
                "/api/v1/tasks/**",
                "/api/v1/task-types/**",
                "/api/v1/subtasks/**",
                "/api/v1/deadline-extensions/**"
            )
            .build();
    }

    @Bean
    public GroupedOpenApi evaluationApi() {
        return GroupedOpenApi.builder()
            .group("04. Đánh giá hiệu suất (Evaluations)")
            .pathsToMatch("/api/v1/evaluations/**")
            .build();
    }

    @Bean
    public GroupedOpenApi kpiApi() {
        return GroupedOpenApi.builder()
            .group("05. KPI & Thống kê (KPI & Analytics)")
            .pathsToMatch("/api/v1/kpi/**")
            .build();
    }

    @Bean
    public GroupedOpenApi dashboardApi() {
        return GroupedOpenApi.builder()
            .group("06. Bảng điều khiển (Dashboard)")
            .pathsToMatch("/api/v1/dashboard/**")
            .build();
    }

    @Bean
    public GroupedOpenApi notificationApi() {
        return GroupedOpenApi.builder()
            .group("07. Thông báo (Notifications)")
            .pathsToMatch("/api/v1/notifications/**")
            .build();
    }

    @Bean
    public GroupedOpenApi reminderApi() {
        return GroupedOpenApi.builder()
            .group("08. Nhắc nhở (Reminders)")
            .pathsToMatch("/api/v1/reminders/**")
            .build();
    }

    @Bean
    public GroupedOpenApi groupApi() {
        return GroupedOpenApi.builder()
            .group("09. Nhóm & Phân quyền (Groups & Teams)")
            .pathsToMatch(
                "/api/v1/groups/**",
                "/api/v1/members/*/groups/**"
            )
            .build();
    }

    @Bean
    public GroupedOpenApi leaveApi() {
        return GroupedOpenApi.builder()
            .group("10. Nghỉ phép & Bàn giao (Leave & Handover)")
            .pathsToMatch(
                "/api/v1/leave-requests/**",
                "/api/v1/members/*/leave-requests/**"
            )
            .build();
    }

    @Bean
    public GroupedOpenApi categoryApi() {
        return GroupedOpenApi.builder()
            .group("11. Danh mục công việc (Categories)")
            .pathsToMatch("/api/v1/categories/**")
            .build();
    }
}
