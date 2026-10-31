package com.olehkobylianskyi.familypoints.entity;

import jakarta.persistence.*;

@Entity
@Table(
        name = "point_exchange_rates",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_exchange_rate_family_from_to",
                        columnNames = {
                                "family_id",
                                "from_point_type_id",
                                "to_point_type_id"
                        }
                )
        }
)
public class PointExchangeRate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "family_id", nullable = false)
    private Workspace workspace;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "from_point_type_id", nullable = false)
    private PointType fromPointType;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "to_point_type_id", nullable = false)
    private PointType toPointType;

    @Column(nullable = false)
    private int fromAmount;

    @Column(nullable = false)
    private int toAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PointExchangeApprovalMode approvalMode;

    @Column(nullable = false)
    private boolean active;

    protected PointExchangeRate() {
    }

    public PointExchangeRate(
            Workspace workspace,
            PointType fromPointType,
            PointType toPointType,
            int fromAmount,
            int toAmount,
            PointExchangeApprovalMode approvalMode
    ) {
        this.workspace = workspace;
        this.fromPointType = fromPointType;
        this.toPointType = toPointType;
        this.fromAmount = fromAmount;
        this.toAmount = toAmount;
        this.approvalMode = approvalMode;
        this.active = true;
    }

    public Long getId() {
        return id;
    }

    public Workspace getWorkspace() {
        return workspace;
    }

    public PointType getFromPointType() {
        return fromPointType;
    }

    public PointType getToPointType() {
        return toPointType;
    }

    public int getFromAmount() {
        return fromAmount;
    }

    public int getToAmount() {
        return toAmount;
    }

    public PointExchangeApprovalMode getApprovalMode() {
        return approvalMode;
    }

    public boolean isActive() {
        return active;
    }

    public void setFromAmount(int fromAmount) {
        this.fromAmount = fromAmount;
    }

    public void setToAmount(int toAmount) {
        this.toAmount = toAmount;
    }

    public void setApprovalMode(
            PointExchangeApprovalMode approvalMode
    ) {
        this.approvalMode = approvalMode;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}