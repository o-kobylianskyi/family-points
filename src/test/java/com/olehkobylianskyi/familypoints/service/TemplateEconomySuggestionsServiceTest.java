package com.olehkobylianskyi.familypoints.service;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TemplateEconomySuggestionsServiceTest {
    @Test void loadsAllSuggestionsWithoutConvertingNullAmountsToZero() {
        var service = new TemplateEconomySuggestionsService(new TemplateCatalogService());
        var proposal = service.getSuggestions();
        assertEquals(110, proposal.path("tasks").size());
        assertEquals(32, proposal.path("rewards").size());
        assertFalse(proposal.path("autoApply").asBoolean(true));
        assertFalse(proposal.path("workspaceMutation").asBoolean(true));
        int taskSuggestions = 0, rewardSuggestions = 0;
        for (var task : proposal.path("tasks")) {
            if (task.path("suggestedRewardPoints").isNumber()) taskSuggestions++;
            else assertTrue(task.path("suggestedRewardPoints").isNull());
        }
        for (var reward : proposal.path("rewards")) {
            if (reward.path("suggestedPricePoints").isNumber()) rewardSuggestions++;
            else assertTrue(reward.path("suggestedPricePoints").isNull());
        }
        assertEquals(11, taskSuggestions);
        assertEquals(12, rewardSuggestions);
    }

    @Test void returnedSuggestionIsDefensiveCopy() {
        var service = new TemplateEconomySuggestionsService(new TemplateCatalogService());
        var node = (com.fasterxml.jackson.databind.node.ObjectNode) service.getSuggestions();
        node.put("autoApply", true);
        assertFalse(service.getSuggestions().path("autoApply").asBoolean(true));
    }
}
