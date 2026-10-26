package com.olehkobylianskyi.familypoints.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TemplateCatalogServiceTest {
    @Test void loadsVersionedCatalogWithoutWorkspaceSideEffects() {
        var service = new TemplateCatalogService(new ObjectMapper());
        var data = service.getCatalog();
        assertEquals(1, data.path("version").asInt());
        assertTrue(data.path("templates").isArray());
        assertTrue(data.path("packs").isArray());
    }

    @Test void returnedCatalogIsDefensiveCopy() {
        var service = new TemplateCatalogService(new ObjectMapper());
        service.getCatalog().put("version", 99);
        assertEquals(1, service.getCatalog().path("version").asInt());
    }
}
