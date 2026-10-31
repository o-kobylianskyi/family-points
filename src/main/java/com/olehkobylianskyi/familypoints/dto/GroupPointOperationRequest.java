package com.olehkobylianskyi.familypoints.dto;
import com.olehkobylianskyi.familypoints.entity.PointTransactionType;
import jakarta.validation.constraints.*;
public class GroupPointOperationRequest {
 @NotNull private Long pointTypeId; @NotNull private Integer amount; @NotNull private PointTransactionType type; @Size(max=255) private String description;
 public Long getPointTypeId(){return pointTypeId;} public void setPointTypeId(Long v){pointTypeId=v;}
 public Integer getAmount(){return amount;} public void setAmount(Integer v){amount=v;} public PointTransactionType getType(){return type;} public void setType(PointTransactionType v){type=v;}
 public String getDescription(){return description;} public void setDescription(String v){description=v;}
}
