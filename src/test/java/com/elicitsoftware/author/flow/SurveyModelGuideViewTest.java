package com.elicitsoftware.author.flow;

/*-
 * ***LICENSE_START***
 * Elicit Author
 * %%
 * Copyright (C) 2025 - 2026 Elicit Software LLC
 * %%
 * PolyForm Noncommercial License 1.0.0
 * <https://polyformproject.org/licenses/noncommercial/1.0.0>
 * ***LICENSE_END***
 */

import com.elicitsoftware.author.flow.designer.Glyph;
import com.elicitsoftware.test.PostgresTestResource;
import com.vaadin.browserless.quarkus.QuarkusBrowserlessTest;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.sidenav.SideNavItem;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
@QuarkusTestResource(PostgresTestResource.class)
class SurveyModelGuideViewTest extends QuarkusBrowserlessTest {

    @Test
    @TestSecurity(user = "guide.author", roles = {"elicit_author"})
    void explainsTheSixIdeasInOrderWithTheDesignerGlyphs() {
        SurveyModelGuideView view = navigate(SurveyModelGuideView.class);

        List<String> titles = view.headings.stream()
                .map(h -> h.getChildren().filter(c -> c.getClass() == Span.class).map(c -> ((Span) c).getText()).findFirst().orElse(""))
                .toList();
        assertEquals(List.of(
                view.getTranslation("guideView.tree.h"),
                view.getTranslation("guideView.containers.h"),
                view.getTranslation("guideView.order.h"),
                view.getTranslation("guideView.rules.h"),
                view.getTranslation("guideView.designer.h"),
                view.getTranslation("guideView.designing.h"),
                view.getTranslation("guideView.reportingTags.h"),
                view.getTranslation("guideView.identity.h")), titles, "UC-029 BR-004: the eight ideas, in this order");

        assertFalse(view.diagram.getAlt().orElse("").isBlank(), "UC-029 BR-003: the diagram has an accessible description");
        assertTrue(view.getChildren().anyMatch(c -> c.getClassNames().contains("survey-guide-legend")), "UC-029 step 3: glyph legend");

        List<Glyph> glyphs = find(Glyph.class, view).all();
        assertTrue(glyphs.stream().anyMatch(g -> g.kind() == Glyph.Kind.STEP), "UC-029 BR-001: step glyph");
        assertTrue(glyphs.stream().anyMatch(g -> g.kind() == Glyph.Kind.SECTION), "UC-029 BR-001: section glyph");
        assertTrue(glyphs.stream().anyMatch(g -> g.kind() == Glyph.Kind.QUESTION), "UC-029 BR-001: question glyph");
        assertTrue(glyphs.stream().anyMatch(g -> g.kind() == Glyph.Kind.RULE_SHOW), "UC-029 BR-001: show rule glyph");
        assertTrue(glyphs.stream().anyMatch(g -> g.kind() == Glyph.Kind.RULE_REPEAT), "UC-029 BR-001: repeat rule glyph");
        assertTrue(glyphs.stream().anyMatch(g -> g.kind() == Glyph.Kind.RULE_TEXT), "UC-029 BR-001: text rule glyph");
        assertTrue(glyphs.stream().allMatch(g -> g.accessibleName() != null && !g.accessibleName().isBlank()),
                "UC-029 BR-003: every glyph carries an accessible name");
    }

    @Test
    @TestSecurity(user = "guide.author", roles = {"elicit_author"})
    void isReachableFromTheApplicationNavigation() {
        navigate(SurveyModelGuideView.class);
        MainLayout layout = (MainLayout) UI.getCurrent().getChildren().filter(MainLayout.class::isInstance).findFirst().orElseThrow();
        assertTrue(find(SideNavItem.class, layout).all().stream().anyMatch(i -> layout.getTranslation("mainLayout.nav.guide").equals(i.getLabel())),
                "UC-029 step 1: the guide is in the application navigation");
    }
}
