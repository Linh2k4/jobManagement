package com.company.jobmanagement.repository;

import com.company.jobmanagement.model.entity.Group;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GroupRepository extends JpaRepository<Group, Long> {

    @Query("SELECT g FROM Group g JOIN FETCH g.lead WHERE g.lead.id = :leadId")
    List<Group> findByLeadId(@Param("leadId") Long leadId);

    @Query("SELECT g FROM Group g JOIN FETCH g.lead ORDER BY g.name ASC")
    List<Group> findAllWithLead();
}
