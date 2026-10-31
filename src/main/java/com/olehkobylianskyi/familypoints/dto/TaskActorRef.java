package com.olehkobylianskyi.familypoints.dto;

import com.olehkobylianskyi.familypoints.entity.ActorType;
import jakarta.validation.constraints.NotNull;

public class TaskActorRef {
    @NotNull private ActorType actorType;
    @NotNull private Long actorId;

    public TaskActorRef() {}
    public TaskActorRef(ActorType actorType, Long actorId) { this.actorType = actorType; this.actorId = actorId; }
    public ActorType getActorType() { return actorType; }
    public void setActorType(ActorType actorType) { this.actorType = actorType; }
    public Long getActorId() { return actorId; }
    public void setActorId(Long actorId) { this.actorId = actorId; }
}
