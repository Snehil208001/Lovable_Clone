package com.snehil.project.lovable_clone.dto.project;

public record FileNode(
        String path
) {

    @Override
    public String toString() {
        return path;
    }
}
