package com.company.jobmanagement.controller.group;

import com.company.jobmanagement.controller.BaseController;
import com.company.jobmanagement.dto.response.ApiResponse;
import com.company.jobmanagement.dto.response.GroupMembershipResponse;
import com.company.jobmanagement.dto.response.UserInfoResponse;
import com.company.jobmanagement.model.entity.GroupMembership;
import com.company.jobmanagement.model.entity.User;
import com.company.jobmanagement.service.GroupService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/members/{id}/groups")
@RequiredArgsConstructor
@Tag(name = "Groups", description = "A member's own group memberships")
public class MemberGroupController extends BaseController {

    private final GroupService groupService;

    @GetMapping
    @Operation(summary = "List the groups a member belongs to", operationId = "getMemberGroups")
    @SecurityRequirement(name = "bearer-jwt")
    public ResponseEntity<ApiResponse<List<GroupMembershipResponse>>> getGroups(@PathVariable Long id) {
        List<GroupMembershipResponse> responses = groupService.getGroupsForMember(id).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
        return ok(ApiResponse.success(responses));
    }

    @PutMapping("/primary")
    @Operation(summary = "Set a member's primary group", operationId = "setPrimaryGroup")
    @SecurityRequirement(name = "bearer-jwt")
    public ResponseEntity<ApiResponse<?>> setPrimary(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Long groupId = ((Number) body.get("groupId")).longValue();
        groupService.setPrimaryGroup(id, groupId);
        return ok(ApiResponse.success("Primary group updated"));
    }

    private GroupMembershipResponse toDTO(GroupMembership gm) {
        return GroupMembershipResponse.builder()
                .groupId(gm.getGroup().getId())
                .groupName(gm.getGroup().getName())
                .member(toUserInfo(gm.getMember()))
                .isPrimary(gm.getIsPrimary())
                .joinedAt(gm.getJoinedAt())
                .build();
    }

    private UserInfoResponse toUserInfo(User u) {
        return UserInfoResponse.builder()
                .id(u.getId()).email(u.getEmail()).fullName(u.getFullName()).role(u.getRole().name())
                .build();
    }
}
