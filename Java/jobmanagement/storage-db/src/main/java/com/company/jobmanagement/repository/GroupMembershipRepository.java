package com.company.jobmanagement.repository;

import com.company.jobmanagement.model.entity.GroupMembership;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GroupMembershipRepository extends JpaRepository<GroupMembership, Long> {

    @Query("SELECT gm FROM GroupMembership gm JOIN FETCH gm.group g JOIN FETCH g.lead JOIN FETCH gm.member WHERE gm.member.id = :memberId")
    List<GroupMembership> findByMemberId(@Param("memberId") Long memberId);

    @Query("SELECT gm FROM GroupMembership gm JOIN FETCH gm.member JOIN FETCH gm.group WHERE gm.group.id = :groupId")
    List<GroupMembership> findByGroupId(@Param("groupId") Long groupId);

    @Query("SELECT gm FROM GroupMembership gm JOIN FETCH gm.group g JOIN FETCH g.lead WHERE gm.member.id = :memberId AND gm.isPrimary = true")
    Optional<GroupMembership> findPrimaryForMember(@Param("memberId") Long memberId);

    @Query("SELECT gm FROM GroupMembership gm WHERE gm.group.id = :groupId AND gm.member.id = :memberId")
    Optional<GroupMembership> findByGroupIdAndMemberId(@Param("groupId") Long groupId, @Param("memberId") Long memberId);

    long countByMemberId(Long memberId);
}
