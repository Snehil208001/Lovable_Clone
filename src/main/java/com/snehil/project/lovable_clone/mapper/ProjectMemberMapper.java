package com.snehil.project.lovable_clone.mapper;

import com.snehil.project.lovable_clone.dto.member.MemberResponse;
import com.snehil.project.lovable_clone.entity.ProjectMember;
import com.snehil.project.lovable_clone.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ProjectMemberMapper {

    @Mapping(target = "userId", source = "id")
    @Mapping(target = "projectRole", constant = "OWNER")
    MemberResponse toProjectMemberResponseFromOwner(User owner);

    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "username", source = "user.username") // Changed email to username
    @Mapping(target = "name", source = "user.name")
    MemberResponse toProjectMemberResponseFromMember(ProjectMember projectMember);
}