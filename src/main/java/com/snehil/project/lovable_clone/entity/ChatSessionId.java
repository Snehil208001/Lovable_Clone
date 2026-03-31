package com.snehil.project.lovable_clone.entity;

import jakarta.persistence.Embeddable;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.io.Serializable;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
@EqualsAndHashCode // CRITICAL: Hibernate requires this for composite keys
@Embeddable        // CRITICAL: Required for @EmbeddedId
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ChatSessionId implements Serializable {
    Long projectId;
    Long userId;
}