package com.company.jobmanagement.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(name = "GroupMembership", description = "A Member's link to one Group")
public class GroupMembershipResponse {
    private Long groupId;
    private String groupName;
    private UserInfoResponse member;
    private Boolean isPrimary;
    private ZonedDateTime joinedAt;
}
