package com.olehkobylianskyi.familypoints.dto;
import jakarta.validation.constraints.NotNull;
public class GroupChildRequest { @NotNull private Long childGroupId; public Long getChildGroupId(){return childGroupId;} public void setChildGroupId(Long v){childGroupId=v;} }
