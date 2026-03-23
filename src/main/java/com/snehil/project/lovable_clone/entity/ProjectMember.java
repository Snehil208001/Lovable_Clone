package com.snehil.project.lovable_clone.entity;

import com.snehil.project.lovable_clone.enums.ProjectRole;
import jakarta.persistence.*; // Ensure these are imported
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.Instant;

@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(name = "project_members")
public class ProjectMember {

    @EmbeddedId
    ProjectMemberId id;

    // 1. Add ManyToOne and MapsId for the Project
    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("projectId") // Matches the field name in ProjectMemberId
    @JoinColumn(name = "project_id")
    Project project;

    // 2. Add ManyToOne and MapsId for the User
    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("userId") // Matches the field name in ProjectMemberId
    @JoinColumn(name = "user_id")
    User user;

    // 3. (Best Practice) Map enums as Strings rather than Integers
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    ProjectRole projectRole;

    Instant invitedAt;
    Instant acceptedAt;
}