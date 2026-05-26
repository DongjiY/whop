package com.whop.backend.task;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TaskRepository extends JpaRepository<TaskEntity, UUID> {
    List<TaskEntity> findByStatusOrderByCreatedAtDesc(TaskStatus status);

    @Query(
            """
            SELECT t
            FROM TaskEntity t
            LEFT JOIN t.acceptedOffer ao
            WHERE t.status = :status
              AND (t.owner.id = :userId OR ao.seller.id = :userId)
            ORDER BY t.createdAt DESC
            """)
    List<TaskEntity> findJobsForUserByStatus(
            @Param("userId") UUID userId, @Param("status") TaskStatus status);
}
