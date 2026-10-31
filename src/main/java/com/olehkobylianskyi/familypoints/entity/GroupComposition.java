package com.olehkobylianskyi.familypoints.entity;

import jakarta.persistence.*;

@Entity
@Table(name="group_compositions", uniqueConstraints=@UniqueConstraint(name="uk_group_composition", columnNames={"parent_group_id","child_group_id"}))
public class GroupComposition {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch=FetchType.LAZY, optional=false) @JoinColumn(name="parent_group_id", nullable=false) private MemberGroup parentGroup;
    @ManyToOne(fetch=FetchType.LAZY, optional=false) @JoinColumn(name="child_group_id", nullable=false) private MemberGroup childGroup;
    @Column(nullable=false) private boolean active=true;
    protected GroupComposition() {}
    public GroupComposition(MemberGroup parentGroup, MemberGroup childGroup){this.parentGroup=parentGroup;this.childGroup=childGroup;}
    public Long getId(){return id;} public MemberGroup getParentGroup(){return parentGroup;} public MemberGroup getChildGroup(){return childGroup;}
    public boolean isActive(){return active;} public void setActive(boolean active){this.active=active;}
}
