package com.company.jobmanagement.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Self-service profile update (Cài đặt → Hồ sơ). Fields are nullable/blank-
 * safe — a caller can send only what changed; fullName/email stay untouched
 * here (name changes likely need an admin/HR flow, not a self-service one).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Update the authenticated user's own profile fields")
public class UpdateProfileRequest {

    private String phone;
    private LocalDate birthDate;
    private String address;
    private String bio;
}
