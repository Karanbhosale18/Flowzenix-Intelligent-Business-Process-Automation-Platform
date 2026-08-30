package com.example.authapp.repository;

import com.example.authapp.entity.ERole;
import com.example.authapp.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    Boolean existsByUsername(String username);

    Boolean existsByEmail(String email);

    boolean existsByAdminReferenceId(Long adminReferenceId);

    Optional<User> findByAdminReferenceId(Long adminReferenceId);

    boolean existsByManagerReferenceId(Long managerReferenceId);

    Optional<User> findByManagerReferenceId(Long managerReferenceId);

    boolean existsByFinanceManagerReferenceId(Long financeManagerReferenceId);

    Optional<User> findByfinanceManagerId(Long financeManagerReferenceId);

    /**
     * Used by the workflow engine to resolve "the Finance team" / "the HR
     * team" / "IT Admin" for steps assigned to a role rather than a named
     * person. Picks the first enabled user holding that role — fine for a
     * single-person-per-role setup; a real load-balanced assignment
     * strategy would replace this with something smarter later.
     */
    @Query("select u from User u join u.roles r where r = :role and u.enabled = true order by u.id asc")
    Optional<User> findFirstByRole(@Param("role") ERole role);
}
