package com.olehkobylianskyi.familypoints.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.olehkobylianskyi.familypoints.service.TemplateCatalogService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Suggested templates are public catalog data; importing requires workspace authorization separately. */
@RestController
@RequestMapping("/template-catalog")
public class TemplateCatalogController {
    private final TemplateCatalogService service;
    public TemplateCatalogController(TemplateCatalogService service) { this.service = service; }
    @GetMapping
    public JsonNode getCatalog() { return service.getCatalog(); }
}
