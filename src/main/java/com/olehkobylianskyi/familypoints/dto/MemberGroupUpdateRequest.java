package com.olehkobylianskyi.familypoints.dto;
import jakarta.validation.constraints.*;
public class MemberGroupUpdateRequest {
 @NotBlank @Size(max=120) private String name; @Size(max=500) private String description; private boolean showInNavigation;
 public String getName(){return name;} public void setName(String name){this.name=name;}
 public String getDescription(){return description;} public void setDescription(String description){this.description=description;}
 public boolean isShowInNavigation(){return showInNavigation;} public void setShowInNavigation(boolean showInNavigation){this.showInNavigation=showInNavigation;}
}
