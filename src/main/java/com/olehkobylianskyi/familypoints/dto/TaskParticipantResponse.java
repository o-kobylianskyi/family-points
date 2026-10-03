package com.olehkobylianskyi.familypoints.dto;

import com.olehkobylianskyi.familypoints.entity.*;

public class TaskParticipantResponse {
    private final Long id;
    private final TaskParticipantRole role;
    private final ActorType actorType;
    private final Long actorId;
    private final String actorName;

    public TaskParticipantResponse(TaskParticipant p, String actorName) {
        this.id=p.getId(); this.role=p.getRole(); this.actorType=p.getActorType(); this.actorId=p.getActorId(); this.actorName=actorName;
    }
    public Long getId(){return id;} public TaskParticipantRole getRole(){return role;} public ActorType getActorType(){return actorType;} public Long getActorId(){return actorId;} public String getActorName(){return actorName;}
}
