package com.olehkobylianskyi.familypoints.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TemplateCatalogServiceTest {
    @Test void loadsVersionedCatalogWithoutWorkspaceSideEffects() {
        var service = new TemplateCatalogService();
        var data = service.getCatalog();
        assertEquals(1, data.path("version").asInt());
        assertTrue(data.path("templates").isArray());
        assertTrue(data.path("packs").isArray());
    }

    @Test void returnedCatalogIsDefensiveCopy() {
        var service = new TemplateCatalogService();
        ((com.fasterxml.jackson.databind.node.ObjectNode) service.getCatalog()).put("version", 99);
        assertEquals(1, service.getCatalog().path("version").asInt());
    }

    @Test void curatedCatalogHasExpectedKindsTranslationsAndValidPacks() {
        var catalog = new TemplateCatalogService().getCatalog();
        var templates = catalog.path("templates");
        assertEquals(142, templates.size());
        assertEquals(6, catalog.path("packs").size());
        int tasks = 0, rewards = 0;
        var codes = new java.util.HashSet<String>();
        for (var item : templates) {
            assertTrue(codes.add(item.path("code").asText()));
            var kind = item.path("kind").asText();
            assertTrue(item.path("definition").isObject(),item.path("code").asText());
            for(var lang : java.util.List.of("uk","de","en","ru")) {
                assertFalse(item.path("title").path(lang).asText().isBlank());
                assertFalse(item.path("description").path(lang).asText().isBlank());
            }
            if ("TASK".equals(kind)) {
                tasks++;
                assertFalse(item.path("definition").path("recurrenceType").asText().isBlank());
            } else if ("REWARD".equals(kind)) {
                rewards++;
                assertFalse(item.path("definition").path("rewardKind").asText().isBlank());
            } else fail("Unexpected template kind: "+kind);
        }
        assertEquals(110,tasks);
        assertEquals(32,rewards);
        for(var pack : catalog.path("packs")) {
            for(var code : pack.path("templateCodes"))
                assertTrue(codes.contains(code.asText()),"Unknown pack code "+code.asText());
        }
    }
}
