package com.olehkobylianskyi.familypoints.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Global, read-only, versioned suggested-template catalog.
 * Never creates workspace tasks, rewards, accounts or financial records.
 */
@Service
public class TemplateCatalogService {
    private final JsonNode catalog;

    public TemplateCatalogService(ObjectMapper mapper) {
        try (InputStream input = new ClassPathResource("templates/catalog-v1.json").getInputStream()) {
            this.catalog = mapper.readTree(input);
            validate(catalog);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to load template catalog", e);
        }
    }

    public JsonNode getCatalog() { return catalog.deepCopy(); }

    private static void validate(JsonNode root) {
        if (root == null || !root.isObject() || root.path("version").asInt(0) < 1
                || !root.path("templates").isArray() || !root.path("packs").isArray()) {
            throw new IllegalStateException("Invalid template catalog schema");
        }
        Set<String> keys = new HashSet<>();
        for (JsonNode item : root.path("templates")) {
            String code = item.path("code").asText("");
            String kind = item.path("kind").asText("");
            if (!code.matches("[a-z][a-z0-9_.-]{2,79}") || !List.of("TASK", "REWARD").contains(kind)
                    || !keys.add(code) || !item.path("title").isObject()) {
                throw new IllegalStateException("Invalid/duplicate template: " + code);
            }
            for (String language : List.of("uk", "ru", "en", "de")) {
                if (item.path("title").path(language).asText("").isBlank()) {
                    throw new IllegalStateException("Missing " + language + " title for " + code);
                }
            }
        }
        Set<String> packs = new HashSet<>();
        for (JsonNode pack : root.path("packs")) {
            String code = pack.path("code").asText("");
            if (!code.matches("[a-z][a-z0-9_.-]{2,79}") || !packs.add(code)
                    || !pack.path("templateCodes").isArray()) {
                throw new IllegalStateException("Invalid/duplicate pack: " + code);
            }
            for (JsonNode id : pack.path("templateCodes")) {
                if (!keys.contains(id.asText())) {
                    throw new IllegalStateException("Unknown template in pack: " + id.asText());
                }
            }
        }
    }
}
