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

import com.elicitsoftware.model.Survey;
import com.elicitsoftware.test.PostgresTestResource;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** UC-006 Browse Survey Structure: the read model behind the tree. */
@QuarkusTest
@QuarkusTestResource(PostgresTestResource.class)
class SurveyStructureQueryTest {

    @Inject
    SurveyStructureQuery query;

    @Test
    void fixtureTreeIsStepsSectionsQuestionsInDisplayOrder() {
        Survey s = Survey.find("name", "AuthorFixture").firstResult();
        SurveyStructureQuery.Tree tree = query.tree(s.id, false);

        assertEquals(List.of("Fixture Step One", "Fixture Step Two"), tree.roots().stream().map(StructureNode::label).toList());
        StructureNode stepOne = tree.roots().get(0);
        assertEquals(List.of("Fixture Section A", "Fixture Section B"), tree.childrenOf(stepOne).stream().map(StructureNode::label).toList());
        StructureNode sectionA = tree.childrenOf(stepOne).get(0);
        assertEquals(List.of("FixtureQ1", "FixtureQ2"), tree.childrenOf(sectionA).stream().map(StructureNode::label).toList());
        assertEquals(StructureNode.Kind.QUESTION, tree.childrenOf(sectionA).get(0).kind());
        assertEquals("HTML", tree.childrenOf(sectionA).get(0).detail());
        assertEquals(2 + 3 + 5, tree.all().size());
        assertTrue(tree.all().stream().allMatch(n -> n.elementKey() != null));
    }

    @Test
    void emptySurveyHasNoRoots() {
        Survey s = Survey.find("name", "AuthorFixtureEmpty").firstResult();
        assertTrue(query.tree(s.id, false).roots().isEmpty());
    }

    @Test
    void plainTextShortensHtml() {
        assertEquals("Welcome to the fixture survey.", SurveyStructureQuery.plain("<p>Welcome to the fixture survey.</p>"));
        assertEquals(58, SurveyStructureQuery.plain("x".repeat(100)).length(), "57 characters plus an ellipsis");
    }

    /**
     * UC-006 / UC-045 BR-003: a label is where an author sees that a text is personalized, so the
     * token inside a placeholder survives even though it looks exactly like a tag.
     */
    @Test
    void plainTextKeepsTokensInsidePlaceholders() {
        assertEquals("How old is {<NAME>|this person}?",
                SurveyStructureQuery.plain("How old is {<NAME>|this person}?"));
        assertEquals("{Does <S1>|Do you} currently have cancer?",
                SurveyStructureQuery.plain("{Does <S1>|Do you} currently have cancer?"));
        assertEquals("{<S1>}", SurveyStructureQuery.plain("{<S1>}"), "a placeholder with no default");
    }

    /** Markup around a placeholder still goes, and a bare tag is still markup wherever it sits. */
    @Test
    void plainTextStillStripsMarkupOutsidePlaceholders() {
        assertEquals("Ask {<S1>'s|your} mother",
                SurveyStructureQuery.plain("<p>Ask <b>{<S1>'s|your}</b> mother</p>"));
        assertEquals("bold then {<NAME>|someone}",
                SurveyStructureQuery.plain("<b>bold</b> then {<NAME>|someone}"));
        assertEquals("Name of person {Q#}", SurveyStructureQuery.plain("Name of person {Q#}"),
                "the instance counter is not a token and is left alone either way");
    }
}
