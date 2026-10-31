package com.olehkobylianskyi.familypoints.entity;
import jakarta.persistence.*; import java.time.LocalDateTime;
@Entity @Table(name="group_point_transactions")
public class GroupPointTransaction {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="group_id",nullable=false) private MemberGroup group;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="point_type_id",nullable=false) private PointType pointType;
 @Column(nullable=false) private int amount; @Enumerated(EnumType.STRING) @Column(nullable=false,length=30) private PointTransactionType type;
 @Enumerated(EnumType.STRING) @Column(nullable=false,length=40) private PointTransactionSourceType sourceType;
 @Column(length=255) private String description; @Column(nullable=false,updatable=false) private LocalDateTime createdAt;
 protected GroupPointTransaction(){}
 public GroupPointTransaction(MemberGroup g,PointType p,int a,PointTransactionType t,String d){group=g;pointType=p;amount=a;type=t;sourceType=PointTransactionSourceType.MANUAL;description=d;createdAt=LocalDateTime.now();}
 public Long getId(){return id;} public MemberGroup getGroup(){return group;} public PointType getPointType(){return pointType;} public int getAmount(){return amount;} public PointTransactionType getType(){return type;} public LocalDateTime getCreatedAt(){return createdAt;} public String getDescription(){return description;}
}
