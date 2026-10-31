package com.olehkobylianskyi.familypoints.controller;

import com.olehkobylianskyi.familypoints.service.TemplateCatalogService;
import com.olehkobylianskyi.familypoints.service.TemplateEconomySuggestionsService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Boot 4 uses Jackson 3 for HTTP conversion, whereas curated catalog resources
 * are Jackson 2 JsonNode. Return serialized JSON text as application/json to
 * avoid exposing JsonNode bean introspection fields (array, nodeType, etc.).
 */
@RestController
@RequestMapping("/template-catalog")
public class TemplateCatalogController {
    private final TemplateCatalogService service;
    private final TemplateEconomySuggestionsService economy;

    public TemplateCatalogController(TemplateCatalogService service,
                                     TemplateEconomySuggestionsService economy) {
        this.service = service;
        this.economy = economy;
    }

    @GetMapping(value="/economy-suggestions", produces=MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> getEconomySuggestions() {
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON)
                .body(economy.getSuggestions().toString());
    }

    @GetMapping(produces=MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> getCatalog() {
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON)
                .body(service.getCatalog().toString());
    }
}
