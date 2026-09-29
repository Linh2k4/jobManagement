package com.company.jobmanagement.controller.group;

import com.company.jobmanagement.controller.BaseController;
import com.company.jobmanagement.dto.response.ApiResponse;
import com.company.jobmanagement.dto.response.GroupMembershipResponse;
import com.company.jobmanagement.dto.response.GroupResponse;
import com.company.jobmanagement.dto.response.UserInfoResponse;
import com.company.jobmanagement.model.entity.Group;
import com.company.jobmanagement.model.entity.GroupMembership;
import com.company.jobmanagement.model.entity.User;
import com.company.jobmanagement.service.GroupService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Multi-group management (Scope.md §13.7). Additive to the single Lead
 * relationship — see V20 migration comment for why.
 */
@RestController
@RequestMapping("/api/v1/groups")
@RequiredArgsConstructor
@Tag(name = "Groups", description = "Multi-group membership and KPI split")
public class GroupController extends BaseController {

    private final GroupService groupService;

    @PostMapping
    @PreAuthorize("hasRole('MANAGER')")
    @Operation(summary = "Create a group", operationId = "createGroup")
    @SecurityRequirement(name = "bearer-jwt")
    public ResponseEntity<ApiResponse<GroupResponse>> create(@RequestBody Map<String, Object> body) {
        String name = (String) body.get("name");
        String description = (String) body.get("description");
        Long leadId = ((Number) body.get("leadId")).longValue();
        Group group = groupService.createGroup(name, description, leadId);
        return created(ApiResponse.success("Group created", toDTO(group)));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('LEAD','MANAGER')")
    @Operation(summary = "List all groups", operationId = "getAllGroups")
    @SecurityRequirement(name = "bearer-jwt")
    public ResponseEntity<ApiResponse<List<GroupResponse>>> getAll() {
        List<GroupResponse> responses = groupService.getAllGroups().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
        return ok(ApiResponse.success(responses));
    }

    @GetMapping("/{id}/members")
    @Operation(summary = "List a group's members", operationId = "getGroupMembers")
    @SecurityRequirement(name = "bearer-jwt")
    public ResponseEntity<ApiResponse<List<GroupMembershipResponse>>> getMembers(@PathVariable Long id) {
        List<GroupMembershipResponse> responses = groupService.getMembersOfGroup(id).stream()
                .map(this::toMembershipDTO)
                .collect(Collectors.toList());
        return ok(ApiResponse.success(responses));
    }

    @PostMapping("/{id}/members")
    @PreAuthorize("hasAnyRole('LEAD','MANAGER')")
    @Operation(summary = "Add a member to a group", operationId = "addGroupMember")
    @SecurityRequirement(name = "bearer-jwt")
    public ResponseEntity<ApiResponse<GroupMembershipResponse>> addMember(
            @PathVariable Long id, @RequestBody Map<String, Object> body) {
        Long memberId = ((Number) body.get("memberId")).longValue();
        boolean isPrimary = Boolean.TRUE.equals(body.get("isPrimary"));
        GroupMembership membership = groupService.addMember(id, memberId, isPrimary);
        return created(ApiResponse.success("Member added", toMembershipDTO(membership)));
    }

    @DeleteMapping("/{id}/members/{memberId}")
    @PreAuthorize("hasAnyRole('LEAD','MANAGER')")
    @Operation(summary = "Remove a member from a group", operationId = "removeGroupMember")
    @SecurityRequirement(name = "bearer-jwt")
    public ResponseEntity<ApiResponse<?>> removeMember(@PathVariable Long id, @PathVariable Long memberId) {
        groupService.removeMember(id, memberId);
        return ok(ApiResponse.success("Member removed"));
    }

    private GroupResponse toDTO(Group g) {
        return GroupResponse.builder()
                .id(g.getId())
                .name(g.getName())
                .description(g.getDescription())
                .lead(toUserInfo(g.getLead()))
                .memberCount(groupService.getMembersOfGroup(g.getId()).size())
                .build();
    }

    private GroupMembershipResponse toMembershipDTO(GroupMembership gm) {
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
