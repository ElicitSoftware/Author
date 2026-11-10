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

import com.elicitsoftware.model.Dimension;
import com.elicitsoftware.model.Ontology;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

/** UC-024 BR-001: the names Author previews are the names Survey's ETL derives. */
class ReportingNamesTest {

    private static Ontology tag(String tag, String dimension) {
        Ontology o = new Ontology();
        o.tag = tag;
        if (dimension != null) {
            o.dimension = new Dimension();
            o.dimension.name = dimension;
        }
        return o;
    }

    @Test
    void tagOnADimensionSharesTheDimensionTable() {
        Ontology o = tag("Breast Cancer Age", "age");
        assertEquals("dim_age", ReportingNames.dimTable(o), "UC-024 BR-001: dim_<lower(dimension)>");
        assertEquals("breast_cancer_age_key", ReportingNames.factColumn(o.tag), "spaces become underscores");
    }

    @Test
    void tagWithoutADimensionNamesItsOwnTable() {
        Ontology o = tag("Vital Status", null);
        assertEquals("dim_vital_status", ReportingNames.dimTable(o));
        assertEquals("vital_status_key", ReportingNames.factColumn(o.tag));
    }

    @Test
    void hyphensAreDroppedFromTheColumnButKeptInAnOwnTableName() {
        assertEquals("bladdercancer_key", ReportingNames.factColumn("Bladder-Cancer"), "ETL: replace(tag,'-','')");
        assertEquals("dim_bladder-cancer", ReportingNames.dimTableForTag("Bladder-Cancer"), "ETL keeps the hyphen in the table name");
        assertTrue(ReportingNames.identifierDefect(ReportingNames.dimTableForTag("Bladder-Cancer")).isPresent(),
                "UC-023 BR-003: a hyphenated own-table tag would fail the site's build");
        assertTrue(ReportingNames.identifierDefect(ReportingNames.dimTableForDimension("cancer")).isEmpty());
    }

    @Test
    void identifiersLongerThanPostgresAllowsAreProblems() {
        String tag = "x".repeat(64);
        assertTrue(ReportingNames.identifierDefect(ReportingNames.factColumn(tag)).orElse(null) == ReportingNames.IdentifierDefect.TOO_LONG,
                "UC-024 BR-002: 63 characters is the PostgreSQL limit");
        assertTrue(ReportingNames.identifierDefect("").isPresent());
        assertTrue(ReportingNames.identifierDefect("dim_ok_1").isEmpty());
    }

    @Test
    void characterRules() {
        assertTrue(ReportingNames.TAG_CHARS.matcher("Kidney renal cell Cancer").matches());
        assertTrue(ReportingNames.TAG_CHARS.matcher("Non-Melanoma Skin Cancer").matches());
        assertTrue(!ReportingNames.TAG_CHARS.matcher("Age (years)").matches(), "parentheses are not allowed");
        assertTrue(!ReportingNames.TAG_CHARS.matcher(" Age").matches(), "must start with a letter or digit");
        assertTrue(ReportingNames.DIMENSION_CHARS.matcher("multiple_cancers").matches());
        assertTrue(!ReportingNames.DIMENSION_CHARS.matcher("multiple cancers").matches(), "UC-023 BR-003: no spaces in a dimension");
    }

    @Test
    void dimTable_dimensionNameWinsOverTheTag() {
        Ontology tagged = new Ontology();
        tagged.tag = "Smoker";
        assertEquals(ReportingNames.dimTableForTag("Smoker"), ReportingNames.dimTable(tagged), "UC-023: no dimension");

        tagged.dimension = new Dimension();
        assertEquals(ReportingNames.dimTableForTag("Smoker"), ReportingNames.dimTable(tagged), "a dimension with no name");

        tagged.dimension.name = "Race";
        assertEquals("dim_race", ReportingNames.dimTable(tagged));

        assertEquals("dim_race", ReportingNames.dimTable("Smoker", "Race"));
        assertEquals(ReportingNames.dimTableForTag("Smoker"), ReportingNames.dimTable("Smoker", null));
        assertEquals(ReportingNames.dimTableForTag("Smoker"), ReportingNames.dimTable("Smoker", " "));
    }

    @Test
    void reservedColumn_theFourKeyColumns() {
        assertTrue(ReportingNames.reservedColumn("step_key"));
        assertTrue(ReportingNames.reservedColumn("item_key"));
        assertFalse(ReportingNames.reservedColumn("smoker"));
        assertFalse(ReportingNames.reservedColumn(null));
    }

    @Test
    void identifierDefect_postgresLimitIsSixtyThreeCharacters() {
        assertEquals(java.util.Optional.empty(), ReportingNames.identifierDefect("a".repeat(63)));
        assertEquals(java.util.Optional.of(ReportingNames.IdentifierDefect.TOO_LONG), ReportingNames.identifierDefect("a".repeat(64)));
    }

    @Test
    void identifierDefect_keySuffixes() {
        assertEquals("empty", ReportingNames.IdentifierDefect.EMPTY.keySuffix());
        assertEquals("characters", ReportingNames.IdentifierDefect.CHARACTERS.keySuffix());
        assertEquals("tooLong", ReportingNames.IdentifierDefect.TOO_LONG.keySuffix());
    }
}
