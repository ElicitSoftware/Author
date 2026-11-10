package com.elicitsoftware.author.i18n;

/*-
 * ***LICENSE_START***
 * Elicit Author
 * %%
 * Copyright (C) 2025 The Regents of the University of Michigan - Rogel Cancer Center
 * %%
 * PolyForm Noncommercial License 1.0.0
 * <https://polyformproject.org/licenses/noncommercial/1.0.0>
 * ***LICENSE_END***
 */

import com.elicitsoftware.author.flow.ImportDefinitionView;
import com.elicitsoftware.author.flow.SurveyEditorView;
import com.elicitsoftware.author.flow.SurveyModelGuideView;
import com.elicitsoftware.author.flow.OidcDiagnosticsView;
import com.elicitsoftware.author.flow.SurveysView;
import com.elicitsoftware.author.flow.SystemBrandingView;
import com.elicitsoftware.author.flow.SystemConnectionsView;
import com.elicitsoftware.author.flow.SystemDatabaseView;
import com.elicitsoftware.author.flow.SystemOverviewView;
import com.elicitsoftware.author.flow.designer.SurveyDesignerView;
import com.elicitsoftware.author.flow.reporting.ReportingView;
import com.elicitsoftware.author.flow.translations.TranslationsView;
import com.elicitsoftware.model.Survey;
import com.elicitsoftware.test.PostgresTestResource;
import com.vaadin.browserless.quarkus.QuarkusBrowserlessTest;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.dom.Element;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * UC-040 / NFR-018 backstop: with the pseudo-locale every translated text renders as
 * {@code ⟦key⟧}, so any prose-like text on a rendered route that lacks the marker bypassed the
 * provider. Subtrees flagged {@code data-i18n-content} carry authored survey content and are skipped.
 */
@QuarkusTest
@QuarkusTestResource(PostgresTestResource.class)
class DisplayedStringsSweepTest extends QuarkusBrowserlessTest {

    private static final Pattern PROSE_LIKE = Pattern.compile("^(?=.*\\p{L})(.*\\s.*|\\p{Lu}.*|.*[.:!?])$", Pattern.DOTALL);
    private static final List<String> TEXT_ATTRIBUTES = List.of(
            "label", "placeholder", "helper-text", "aria-label", "title", "alt", "error-message");
    private static final List<String> TEXT_PROPERTIES = List.of(
            "label", "placeholder", "helperText", "errorMessage", "innerHTML", "textContent");

    @Test
    @TestSecurity(user = "sweep.author", roles = {"elicit_author"})
    void authorRoutes_haveNoUntranslatedText() {
        UI.getCurrent().setLocale(ElicitI18NProvider.PSEUDO_LOCALE);
        List<String> problems = new ArrayList<>();
        // The surveys list is the root route the harness opened before the locale was set, so leave
        // it first and come back to get an instance built in the pseudo-locale.
        navigate(SurveyModelGuideView.class);
        sweep("guide", problems);
        navigate(SurveysView.class);
        sweep("surveys", problems);
        navigate(ImportDefinitionView.class);
        sweep("import", problems);
        // UnauthorizedView is a layout-less error route the mock router cannot navigate to; UnauthorizedViewTest covers it.

        Survey fixture = Survey.find("name", "AuthorFixture").firstResult();
        if (fixture != null) {
            Map<String, String> id = Map.of(SurveyEditorView.SURVEY_ID, String.valueOf(fixture.id));
            navigate(SurveyEditorView.class, id);
            sweep("editor", problems);
            navigate(SurveyDesignerView.class, id);
            sweep("designer", problems);
            navigate(ReportingView.class, id);
            sweep("reporting", problems);
            // The fixture publishes no language, so the picker renders with nothing selected and
            // offers the common languages. Their names come from the JDK rather than the bundle,
            // which is why the item label is built from a parameterised key: the pseudo-locale
            // returns its marker before formatting parameters, so the whole label reads back as
            // one marked string. The grid itself is content (base text and translations),
            // marked as such.
            navigate(TranslationsView.class, id);
            sweep("translations", problems);
        } else {
            problems.add("<fixture> survey 'AuthorFixture' not found; designer, editor and reporting routes not swept");
        }
        assertTrue(problems.isEmpty(), report(problems));
    }

    /** UC-034..UC-038: the System pages are gated on elicit_author_admin, so they are swept as that role. */
    @Test
    @TestSecurity(user = "sweep.admin", roles = {"elicit_author_admin"})
    void systemRoutes_haveNoUntranslatedText() {
        UI.getCurrent().setLocale(ElicitI18NProvider.PSEUDO_LOCALE);
        List<String> problems = new ArrayList<>();
        navigate(SystemOverviewView.class);
        sweep("system", problems);
        navigate(SystemDatabaseView.class);
        sweep("system/database", problems);
        navigate(SystemBrandingView.class);
        sweep("system/branding", problems);
        navigate(SystemConnectionsView.class);
        sweep("system/connections", problems);
        navigate(OidcDiagnosticsView.class);
        sweep("oidc", problems);
        assertTrue(problems.isEmpty(), report(problems));
    }

    private void sweep(String route, List<String> problems) {
        UI ui = UI.getCurrent();
        check(route, "<page title>", ui.getInternals().getTitle(), problems);
        walk(route, ui.getElement(), problems);
        for (Grid<?> grid : find(Grid.class).all().stream().map(g -> (Grid<?>) g).toList()) {
            grid.getColumns().forEach(c -> check(route, "grid header", c.getHeaderText(), problems));
        }
    }

    private void walk(String route, Element element, List<String> problems) {
        if (element.isTextNode()) {
            check(route, "text", element.getText(), problems);
            return;
        }
        if (element.hasAttribute("data-i18n-content")) {
            return;
        }
        String where = element.getTag() + (element.getAttribute("id") != null ? "#" + element.getAttribute("id") : "");
        for (String attr : TEXT_ATTRIBUTES) {
            if (element.hasAttribute(attr)) {
                check(route, where + "[" + attr + "]", element.getAttribute(attr), problems);
            }
        }
        for (String prop : TEXT_PROPERTIES) {
            if (element.hasProperty(prop)) {
                check(route, where + "." + prop, element.getProperty(prop), problems);
            }
        }
        for (int i = 0; i < element.getChildCount(); i++) {
            walk(route, element.getChild(i), problems);
        }
    }

    private static void check(String route, String where, String value, List<String> problems) {
        if (value == null || value.isBlank()) {
            return;
        }
        String stripped = value.replaceAll("<[^>]+>", " ").strip();
        if (stripped.contains(ElicitI18NProvider.PSEUDO_OPEN) || !PROSE_LIKE.matcher(stripped).matches()) {
            return;
        }
        problems.add(route + " " + where + ": \"" + stripped + "\"");
    }

    private static String report(List<String> problems) {
        return problems.size() + " untranslated text(s) rendered (UC-040 / NFR-018):\n  " + String.join("\n  ", problems);
    }
}
