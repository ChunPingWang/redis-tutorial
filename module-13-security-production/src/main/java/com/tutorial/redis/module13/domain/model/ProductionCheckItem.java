package com.tutorial.redis.module13.domain.model;

/**
 * A single item on a Redis production-readiness checklist.
 *
 * <p>Used by {@link com.tutorial.redis.module13.domain.service.ProductionChecklistService}
 * to generate a comprehensive checklist of best practices that should be
 * verified before running Redis in production.</p>
 *
 * <p>Each item belongs to a {@code category} (e.g. "Security", "Memory",
 * "Persistence") and has a human-readable {@code checkItem} description.
 * The {@code checked} flag indicates whether the item has been satisfied.</p>
 */
public class ProductionCheckItem {

    private final String category;
    private final String checkItem;
    private final boolean checked;

    public ProductionCheckItem(String category, String checkItem, boolean checked) {
        this.category = category;
        this.checkItem = checkItem;
        this.checked = checked;
    }

    public String getCategory() {
        return category;
    }

    public String getCheckItem() {
        return checkItem;
    }

    public boolean isChecked() {
        return checked;
    }

    @Override
    public String toString() {
        return "ProductionCheckItem{category='" + category + "', checkItem='" + checkItem
                + "', checked=" + checked + '}';
    }
}
