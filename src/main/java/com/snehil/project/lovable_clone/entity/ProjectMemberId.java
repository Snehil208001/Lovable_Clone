package com.snehil.project.lovable_clone.entity;

import jakarta.persistence.Embeddable;
import lombok.*;

import java.io.Serializable;

@Data // Adds equals(), hashCode(), getters, and setters
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Embeddable // Required for @EmbeddedId to work
public class ProjectMemberId implements Serializable { // Must implement Serializable

    private Long projectId;
    private Long userId;

}