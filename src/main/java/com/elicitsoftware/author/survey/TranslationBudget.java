package com.elicitsoftware.author.survey;

/*-
 * ***LICENSE_START***
 * Elicit Author
 * %%
 * Copyright (C) 2026 Elicit Software LLC
 * %%
 * PolyForm Noncommercial License 1.0.0
 * <https://polyformproject.org/licenses/noncommercial/1.0.0>
 * ***LICENSE_END***
 */

import java.util.Map;

/**
 * How long a translation of one string may be (UC-045 BR-004).
 * <p>
 * Not the base column's width. A translation is stored in {@code translations.value}, which is
 * {@code text}, so the database imposes nothing; the number protects the space the string is drawn
 * in. Taking the column literally would leave a translator almost no room on the 255-character
 * fields and none at all on a 100-character short text: a 250-character English tooltip needs about
 * 300 in Spanish. So the budget is computed from the source string with the expansion the
 * localization industry assumes for a string of that length, and never falls below the column.
 * <p>
 * One function, three callers -- the hand-off export, the import check and the editor field -- so a
 * translator is never shown a limit the import will not accept.
 */
public final class TranslationBudget {

    /** The base column widths, by element type and field. */
    private static final Map<String, Integer> WIDTHS = Map.ofEntries(
            Map.entry("surveys.title", 255),
            Map.entry("surveys.description", 2000),
            Map.entry("steps.name", 255),
            Map.entry("steps.description", 255),
            Map.entry("sections.name", 255),
            Map.entry("sections.description", 255),
            Map.entry("questions.text", 8000),
            Map.entry("questions.short_text", 100),
            Map.entry("questions.tool_tip", 255),
            Map.entry("questions.placeholder", 255),
            Map.entry("questions.validation_text", 255),
            Map.entry("select_items.display_text", 255),
            Map.entry("relationships.default_upstream_value", 255),
            Map.entry("reports.name", 255),
            Map.entry("reports.description", 255),
            Map.entry("report_items.label", 255));

    private TranslationBudget() {
    }

    /**
     * The most characters a translation of {@code source} may run to.
     *
     * @param elementType the table the element lives in
     * @param field       the base column's name
     * @param source      the base text being translated
     * @return the budget, never below the base column's width
     */
    public static int of(String elementType, String field, String source) {
        int width = WIDTHS.getOrDefault(elementType + "." + field, 255);
        if (source == null || source.isEmpty()) {
            return width;
        }
        int length = source.length();
        double factor = length <= 20 ? 2.0 : length <= 50 ? 1.6 : length <= 200 ? 1.4 : 1.3;
        return Math.max(width, (int) Math.ceil(length * factor));
    }

    /**
     * The base column's own width, for the record.
     *
     * @param elementType the table the element lives in
     * @param field       the base column's name
     * @return the column's width, or 255 for a field not listed
     */
    public static int columnWidth(String elementType, String field) {
        return WIDTHS.getOrDefault(elementType + "." + field, 255);
    }
}
