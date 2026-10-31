package com.olehkobylianskyi.familypoints.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.olehkobylianskyi.familypoints.service.TemplateCatalogService;
import com.olehkobylianskyi.familypoints.service.TemplateEconomySuggestionsService;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import static org.junit.jupiter.api.Assertions.*;

class TemplateCatalogControllerTest {
    private final TemplateCatalogService catalog = new TemplateCatalogService();
    private final TemplateCatalogController controller = new TemplateCatalogController(
            catalog, new TemplateEconomySuggestionsService(catalog));

    @Test void catalogResponseIsActualJsonNotJacksonNodeBeanProperties() throws Exception {
        var response = controller.getCatalog();
        assertEquals(MediaType.APPLICATION_JSON, response.getHeaders().getContentType());
        var body = new ObjectMapper().readTree(response.getBody());
        assertEquals(142, body.path("templates").size());
        assertEquals(6, body.path("packs").size());
        assertFalse(body.has("nodeType"));
        assertFalse(body.has("containerNode"));
    }

    @Test void economyResponseIsActualJsonWithExplicitNullPrices() throws Exception {
        var response = controller.getEconomySuggestions();
        assertEquals(MediaType.APPLICATION_JSON, response.getHeaders().getContentType());
        var body = new ObjectMapper().readTree(response.getBody());
        assertEquals(110, body.path("tasks").size());
        assertEquals(32, body.path("rewards").size());
        assertTrue(body.path("rewards").findValues("suggestedPricePoints").stream()
                .anyMatch(com.fasterxml.jackson.databind.JsonNode::isNull));
        assertFalse(body.has("nodeType"));
    }
}
