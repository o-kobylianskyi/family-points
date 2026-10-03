package com.olehkobylianskyi.familypoints.entity;

import jakarta.persistence.*;

@Entity
@Table(
        name = "economy_settings",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_economy_settings_family",
                        columnNames = "family_id"
                )
        }
)
public class EconomySettings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "family_id", nullable = false)
    private Workspace workspace;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EconomyMode mode;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "default_point_type_id", nullable = false)
    private PointType defaultPointType;

    protected EconomySettings() {
    }

    public EconomySettings(
            Workspace workspace,
            EconomyMode mode,
            PointType defaultPointType
    ) {
        this.workspace = workspace;
        this.mode = mode;
        this.defaultPointType = defaultPointType;
    }

    public Long getId() {
        return id;
    }

    public Workspace getWorkspace() {
        return workspace;
    }

    public EconomyMode getMode() {
        return mode;
    }

    public PointType getDefaultPointType() {
        return defaultPointType;
    }

    public void setMode(EconomyMode mode) {
        this.mode = mode;
    }

    public void setDefaultPointType(PointType defaultPointType) {
        this.defaultPointType = defaultPointType;
    }
}