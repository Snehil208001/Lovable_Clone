package com.snehil.project.lovable_clone.repository;

import com.snehil.project.lovable_clone.entity.UsageLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Optional;

public interface UsageLogRepository extends JpaRepository<UsageLog, Long> {

    @Query("SELECT u FROM UsageLog u WHERE u.user.id = :userId AND u.date = :today")
    Optional<UsageLog> findByUserIdAndDate(@Param("userId") Long userId, @Param("today") LocalDate today);
}
