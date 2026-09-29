package com.company.jobmanagement.service;

import com.company.jobmanagement.exception.BusinessLogicException;
import com.company.jobmanagement.exception.ForbiddenOperationException;
import com.company.jobmanagement.exception.ResourceNotFoundException;
import com.company.jobmanagement.model.entity.Group;
import com.company.jobmanagement.model.entity.GroupMembership;
import com.company.jobmanagement.model.entity.User;
import com.company.jobmanagement.repository.GroupMembershipRepository;
import com.company.jobmanagement.repository.GroupRepository;
import com.company.jobmanagement.repository.UserRepository;
import com.company.jobmanagement.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Group/GroupMembership management (Scope.md §13). Additive to
 * {@link User#getLead()} — every existing feature keeps reading the single
 * Lead FK unchanged; a member's primary membership is the practical
 * stand-in wherever a feature isn't yet group-aware.
 */
@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class GroupService {

    private static final int MAX_GROUPS_PER_MEMBER = 5;

    private final GroupRepository groupRepository;
    private final GroupMembershipRepository membershipRepository;
    private final UserRepository userRepository;
    private final CurrentUser currentUser;

    public Group createGroup(String name, String description, Long leadId) {
        User actor = currentUser.getCurrentUser();
        if (!actor.isManager()) {
            throw new ForbiddenOperationException("Only Manager can create a group");
        }
        User lead = userRepository.findById(leadId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + leadId));
        if (!lead.isLead()) {
            throw new BusinessLogicException("Group's lead must be a LEAD user");
        }

        Group group = Group.builder().name(name).description(description).lead(lead).build();
        return groupRepository.save(group);
    }

    @Transactional(readOnly = true)
    public Group getGroup(Long groupId) {
        return groupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Group not found with id: " + groupId));
    }

    @Transactional(readOnly = true)
    public List<Group> getAllGroups() {
        return groupRepository.findAllWithLead();
    }

    @Transactional(readOnly = true)
    public List<GroupMembership> getGroupsForMember(Long memberId) {
        return membershipRepository.findByMemberId(memberId);
    }

    @Transactional(readOnly = true)
    public List<GroupMembership> getMembersOfGroup(Long groupId) {
        return membershipRepository.findByGroupId(groupId);
    }

    @Transactional(readOnly = true)
    public Optional<Group> getPrimaryGroup(Long memberId) {
        return membershipRepository.findPrimaryForMember(memberId).map(GroupMembership::getGroup);
    }

    /**
     * Add a member to a group. The member's very first membership is always
     * primary regardless of what was requested — a member with any
     * membership must always have exactly one primary group.
     */
    public GroupMembership addMember(Long groupId, Long memberId, boolean requestedPrimary) {
        Group group = getGroup(groupId);
        User actor = currentUser.getCurrentUser();
        validateGroupManager(group, actor);

        User member = userRepository.findById(memberId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + memberId));
        if (!member.isMember()) {
            throw new BusinessLogicException("Only Members can be added to a group");
        }

        if (membershipRepository.findByGroupIdAndMemberId(groupId, memberId).isPresent()) {
            throw new BusinessLogicException("This member is already in the group");
        }

        long existingCount = membershipRepository.countByMemberId(memberId);
        if (existingCount >= MAX_GROUPS_PER_MEMBER) {
            throw new BusinessLogicException(
                    String.format("A member cannot belong to more than %d groups", MAX_GROUPS_PER_MEMBER));
        }

        boolean makePrimary = existingCount == 0 || requestedPrimary;
        if (makePrimary && existingCount > 0) {
            clearExistingPrimary(memberId);
        }

        GroupMembership membership = GroupMembership.builder()
                .group(group)
                .member(member)
                .isPrimary(makePrimary)
                .build();
        GroupMembership saved = membershipRepository.save(membership);
        log.info("Member added to group: groupId={}, memberId={}, isPrimary={}", groupId, memberId, makePrimary);
        return saved;
    }

    /**
     * Remove a member from a group. If the removed membership was primary
     * and another membership remains, that one is promoted to primary so
     * the member never ends up with more than zero-but-ambiguous groups.
     */
    public void removeMember(Long groupId, Long memberId) {
        Group group = getGroup(groupId);
        User actor = currentUser.getCurrentUser();
        validateGroupManager(group, actor);

        GroupMembership membership = membershipRepository.findByGroupIdAndMemberId(groupId, memberId)
                .orElseThrow(() -> new ResourceNotFoundException("Member is not in this group"));
        boolean wasPrimary = Boolean.TRUE.equals(membership.getIsPrimary());
        membershipRepository.delete(membership);

        if (wasPrimary) {
            membershipRepository.findByMemberId(memberId).stream().findFirst().ifPresent(remaining -> {
                remaining.setIsPrimary(true);
                membershipRepository.save(remaining);
            });
        }
        log.info("Member removed from group: groupId={}, memberId={}", groupId, memberId);
    }

    public void setPrimaryGroup(Long memberId, Long groupId) {
        User actor = currentUser.getCurrentUser();
        if (!actor.getId().equals(memberId) && !actor.isManager()) {
            throw new ForbiddenOperationException("Only the member themself or Manager can change the primary group");
        }
        GroupMembership target = membershipRepository.findByGroupIdAndMemberId(groupId, memberId)
                .orElseThrow(() -> new ResourceNotFoundException("Member is not in this group"));

        clearExistingPrimary(memberId);
        target.setIsPrimary(true);
        membershipRepository.save(target);
    }

    private void clearExistingPrimary(Long memberId) {
        membershipRepository.findByMemberId(memberId).stream()
                .filter(gm -> Boolean.TRUE.equals(gm.getIsPrimary()))
                .forEach(gm -> {
                    gm.setIsPrimary(false);
                    membershipRepository.save(gm);
                });
    }

    private void validateGroupManager(Group group, User actor) {
        if (actor.isManager()) {
            return;
        }
        if (actor.isLead() && group.getLead().getId().equals(actor.getId())) {
            return;
        }
        throw new ForbiddenOperationException("Only this group's Lead or a Manager can manage its members");
    }
}
