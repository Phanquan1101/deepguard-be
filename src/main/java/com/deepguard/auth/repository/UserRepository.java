package com.deepguard.auth.repository;

import com.deepguard.auth.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, String> {

    Optional<User> findByEmail(String email);

    Optional<User> findByUsername(String username);

    Optional<User> findByEmailOrUsername(String email, String username);

    @Query("""
            select u
            from User u
            left join fetch u.role
            where u.email = :identifier or u.username = :identifier
            """)
    Optional<User> findAuthUserByIdentifier(@Param("identifier") String identifier);

    boolean existsByEmail(String email);

    boolean existsByUsername(String username);

    // ── Admin queries ──────────────────────────────────────────────────────────

    /**
     * Paginated list of all users, eagerly loading role.
     * Supports optional keyword filter on email or username,
     * and optional status filter.
     */
    @EntityGraph(attributePaths = {"role"})
    @Query("""
            select u from User u
            where (cast(:keyword as string) is null
                   or lower(u.email) like lower(concat('%', cast(:keyword as string), '%'))
                   or lower(u.username) like lower(concat('%', cast(:keyword as string), '%')))
              and (cast(:status as string) is null or u.status = cast(:status as string))
              and (cast(:roleName as string) is null or u.role.name = cast(:roleName as string))
            """)
    Page<User> findAllWithFilters(
            @Param("keyword") String keyword,
            @Param("status") String status,
            @Param("roleName") String roleName,
            Pageable pageable);

    /** Fetch a user with role eagerly loaded — for detail view. */
    @EntityGraph(attributePaths = {"role"})
    Optional<User> findWithRoleById(String id);

    // ── Stats queries ──────────────────────────────────────────────────────────

    long countByStatus(String status);

    long countByRoleName(String roleName);
}
