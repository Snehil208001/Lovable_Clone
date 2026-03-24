package com.snehil.project.lovable_clone.enums;

import lombok.Getter;
import java.util.Set;
import static com.snehil.project.lovable_clone.enums.ProjectPermission.*;

@Getter
public enum ProjectRole {

    // Just pass the permissions directly, the varargs constructor handles the rest!
    EDITOR(VIEW, EDIT, DELETE,VIEW_MEMBERS),
    VIEWER(VIEW,VIEW_MEMBERS),
    OWNER(VIEW, EDIT, DELETE, MANAGE_MEMBERS,VIEW_MEMBERS);

    private final Set<ProjectPermission> permissions;

    // Custom varargs constructor
    ProjectRole(ProjectPermission... permissions) {
        this.permissions = Set.of(permissions);
    }
}