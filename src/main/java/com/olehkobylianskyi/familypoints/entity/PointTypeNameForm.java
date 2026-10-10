package com.olehkobylianskyi.familypoints.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "point_type_name_forms",
    uniqueConstraints = @UniqueConstraint(columnNames = {"point_type_id", "language"}))
public class PointTypeNameForm {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "point_type_id", nullable = false) private PointType pointType;
    @Column(nullable = false, length = 5) private String language;
    @Column(nullable = false, length = 100) private String one;
    @Column(nullable = false, length = 100) private String few;
    @Column(nullable = false, length = 100) private String many;

    protected PointTypeNameForm() {}
    public PointTypeNameForm(PointType pointType, String language, String one, String few, String many) {
        this.pointType = pointType; this.language = language;
        this.one = one; this.few = few; this.many = many;
    }
    public Long getId() { return id; }
    public String getLanguage() { return language; }
    public String getOne() { return one; }
    public String getFew() { return few; }
    public String getMany() { return many; }
    public void setForms(String one, String few, String many) {
        this.one = one; this.few = few; this.many = many;
    }
}
