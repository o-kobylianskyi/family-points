package com.olehkobylianskyi.familypoints.dto;
import jakarta.validation.constraints.NotNull;
public class TaskDelegationRequest { @NotNull private Long toMemberId; private String reason; public Long getToMemberId(){return toMemberId;} public void setToMemberId(Long v){toMemberId=v;} public String getReason(){return reason;} public void setReason(String v){reason=v;} }
