package com.company.jobmanagement.repository;

import com.company.jobmanagement.model.enums.Role;
import com.company.jobmanagement.model.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    /**
     * The SecurityContext's authenticated User (from CurrentUser) is loaded
     * in the JWT filter's own session, which is closed by the time a
     * controller/service touches its lazy associations — use this to get a
     * fresh, current-transaction-bound copy with lead/manager walkable two
     * levels deep (needed to resolve a Member's manager through their Lead).
     */
    @Query("SELECT u FROM User u LEFT JOIN FETCH u.lead l LEFT JOIN FETCH l.manager WHERE u.id = :id")
    Optional<User> findByIdWithLeadAndManager(@Param("id") Long id);

    @Query("SELECT u FROM User u WHERE u.role = :role AND u.isActive = true")
    List<User> findByRoleAndActive(@Param("role") Role role);

    @Query("SELECT u FROM User u WHERE u.manager.id = :managerId AND u.isActive = true")
    List<User> findByManagerId(@Param("managerId") Long managerId);

    @Query("SELECT u FROM User u WHERE u.lead.id = :leadId AND u.isActive = true")
    List<User> findByLeadId(@Param("leadId") Long leadId);

    @Query("SELECT u FROM User u WHERE u.role = :role AND u.isActive = true AND u.manager.id = :managerId")
    List<User> findByRoleAndManagerId(@Param("role") Role role, @Param("managerId") Long managerId);

    @Query("SELECT u FROM User u WHERE u.email = :email AND u.isActive = true")
    Optional<User> findByEmailAndActive(@Param("email") String email);

    @Query("SELECT u FROM User u WHERE u.role = :role AND u.isActive = true ORDER BY u.id LIMIT 1")
    Optional<User> findFirstByRoleOrderById(@Param("role") Role role);

}
