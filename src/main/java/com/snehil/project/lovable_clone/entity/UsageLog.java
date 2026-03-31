package com.snehil.project.lovable_clone.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.time.LocalDate;

@Getter
@Setter
@Entity
@Table(name = "usage_logs")
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UsageLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id")
    Project project;

    String action;

    @Column(name = "tokens_user")
    Integer tokensUser;

    Integer durationMs;

    @Column(columnDefinition = "text")
    String metaData;

    @Column(nullable = false)
    LocalDate date;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    Instant createdAt;
}
