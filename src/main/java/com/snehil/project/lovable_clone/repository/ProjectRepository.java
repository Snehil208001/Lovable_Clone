package com.snehil.project.lovable_clone.repository;

import com.snehil.project.lovable_clone.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProjectRepository extends JpaRepository<Project, Long> {

    /**
     * Projects the user is a member of (via {@code project_members}), excluding soft-deleted rows.
     */
    @Query("""
            SELECT DISTINCT p FROM Project p
            INNER JOIN ProjectMember pm ON pm.id.projectId = p.id AND pm.id.userId = :userId
            WHERE p.deletedAt IS NULL
            AND EXISTS (
                SELECT 1 FROM ProjectMember pm
                WHERE pm.id.userId = :userId
                AND pm.id.projectId = p.id
            )
            ORDER BY p.updatedAt DESC
            """
    )
    List<Project> findAllAccessibleByUser(@Param("userId") Long userId);

    /**
     * A project by id only if it is not deleted and {@code userId} has a membership row.
     */
    @Query("""
            SELECT p FROM Project p
            INNER JOIN ProjectMember pm ON pm.id.projectId = p.id AND pm.id.userId = :userId
            WHERE p.id = :projectId AND p.deletedAt IS NULL
             AND EXISTS (
                SELECT 1 FROM ProjectMember pm
                WHERE pm.id.userId = :userId
                AND pm.id.projectId = p.id
            )
            """
    )
    Optional<Project> findAccessibleProjectById(
            @Param("projectId") Long projectId,
            @Param("userId") Long userId);
}
