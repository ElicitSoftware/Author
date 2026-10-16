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
import java.util.Set;

/**
 * The fields of each element a content translation may target (Survey V019; UC-043 BR-002).
 * <p>
 * A copy of Admin's {@code SurveyDefinitionFileFields.TRANSLATABLE_FIELDS}, which is canonical
 * (C-015: Author ports Admin's logic rather than inventing it). {@code TranslatableFieldsParityTest}
 * holds the two lists to each other, because a field this end accepts and that end rejects loses a
 * translation silently at apply time.
 * <p>
 * What is absent is as deliberate as what is present. A survey's {@code name} is an identifier used
 * in file names and console lists; {@code dimension_name} and ontology tags are reporting
 * identifiers ({@code ReportingNames}); a question's {@code default_value} is written into
 * {@code answers.text_value} and analyzed, so translating it would fork stored data by language;
 * {@code coded_value}, {@code url}, {@code mask}, {@code variant}, {@code sample}, {@code token}
 * and {@code reference_value} are read by machines. {@code override_upstream_value} has no runtime
 * reader. Nothing on {@code select_groups}, {@code post_survey_actions}, {@code ontology},
 * {@code dimensions} or {@code metadata} is respondent-facing.
 */
public final class TranslatableFields {

    /** Element type to the base columns that may be translated, keyed as the file's records are named. */
    public static final Map<String, Set<String>> BY_ELEMENT_TYPE = Map.of(
            "surveys", Set.of("title", "description"),
            "steps", Set.of("name", "description"),
            "sections", Set.of("name", "description"),
            "questions", Set.of("text", "short_text", "tool_tip", "placeholder", "validation_text"),
            "select_items", Set.of("display_text"),
            "relationships", Set.of("default_upstream_value"),
            "reports", Set.of("name", "description"),
            "report_items", Set.of("label"));

    private TranslatableFields() {
    }

    /**
     * Whether {@code field} of {@code elementType} may carry a translation.
     *
     * @param elementType the table the translated element lives in
     * @param field       the base column's name
     * @return true when the pair is in {@link #BY_ELEMENT_TYPE}
     */
    public static boolean isTranslatable(String elementType, String field) {
        Set<String> fields = BY_ELEMENT_TYPE.get(elementType);
        return fields != null && fields.contains(field);
    }
}
