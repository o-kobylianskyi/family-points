package com.olehkobylianskyi.familypoints.dto;

import java.util.List;

/** Read-only purchase history. Status is the persisted purchase status, not delivery. */
public record RewardPurchaseHistoryPage(
        List<RewardPurchaseResponse> items,
        String nextCursor,
        boolean hasMore
) {}
