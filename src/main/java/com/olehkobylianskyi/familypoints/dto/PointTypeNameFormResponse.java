package com.olehkobylianskyi.familypoints.dto;

import com.olehkobylianskyi.familypoints.entity.PointTypeNameForm;
public record PointTypeNameFormResponse(String language, String one, String few, String many) {
    public static PointTypeNameFormResponse from(PointTypeNameForm form) {
        return new PointTypeNameFormResponse(form.getLanguage(), form.getOne(), form.getFew(), form.getMany());
    }
}
