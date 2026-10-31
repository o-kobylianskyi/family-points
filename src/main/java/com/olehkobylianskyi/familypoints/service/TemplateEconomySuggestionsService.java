package com.olehkobylianskyi.familypoints.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;

/**
 * Read-only, preliminary economy suggestions. Never merges suggestions into
 * workspace templates, creates rewards or writes to ledger/balances.
 */
@Service
public class TemplateEconomySuggestionsService {
    private final JsonNode suggestions;

    public TemplateEconomySuggestionsService(TemplateCatalogService catalog) {
        try (InputStream in = new ClassPathResource("templates/economy-suggestions-v1.json").getInputStream()) {
            JsonNode proposal = new ObjectMapper().readTree(in);
            validate(proposal, catalog.getCatalog());
            suggestions = proposal;
        } catch (java.io.IOException e) {
            throw new IllegalStateException("Unable to load economy suggestions", e);
        }
    }

    public JsonNode getSuggestions() { return suggestions.deepCopy(); }
    public JsonNode findSuggestion(String kind, String catalogCode) {
        if (catalogCode == null || catalogCode.isBlank()) return null;
        String collection = "TASK".equals(kind) ? "tasks" : "REWARD".equals(kind) ? "rewards" : null;
        if (collection == null) return null;
        for (JsonNode item : suggestions.path(collection))
            if (catalogCode.equals(item.path("code").asText())) return item.deepCopy();
        return null;
    }


    private static void validate(JsonNode proposal, JsonNode catalog) {
        if (!"familypoints.economy.suggestions.v1".equals(proposal.path("schema").asText())
                || proposal.path("version").asInt() != 1
                || proposal.path("sourceCatalogVersion").asInt() != catalog.path("version").asInt()
                || proposal.path("autoApply").asBoolean(true)
                || proposal.path("workspaceMutation").asBoolean(true)
                || !"POINTS".equals(proposal.path("currencyCode").asText()))
            throw new IllegalStateException("Incompatible economy suggestions metadata");
        Set<String> tasks = new HashSet<>(), rewards = new HashSet<>();
        for (JsonNode t : catalog.path("templates")) {
            if ("TASK".equals(t.path("kind").asText())) tasks.add(t.path("code").asText());
            else if ("REWARD".equals(t.path("kind").asText())) rewards.add(t.path("code").asText());
        }
        checkCodes(proposal.path("tasks"), tasks, "suggestedRewardPoints");
        checkCodes(proposal.path("rewards"), rewards, "suggestedPricePoints");
    }

    private static void checkCodes(JsonNode items, Set<String> expected, String amountField) {
        if (!items.isArray() || items.size() != expected.size())
            throw new IllegalStateException("Economy suggestions missing catalog entries");
        Set<String> seen = new HashSet<>();
        for (JsonNode item : items) {
            String code = item.path("code").asText();
            if (!expected.contains(code) || !seen.add(code)
                    || !item.path("reviewRequired").asBoolean(false)
                    || !item.has(amountField)
                    || (!item.path(amountField).isNull()
                        && (!item.path(amountField).isIntegralNumber()
                            || item.path(amountField).asInt() < 0)))
                throw new IllegalStateException("Invalid economy suggestion: " + code);
        }
    }
}
