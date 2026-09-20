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

import com.elicitsoftware.author.flow.SurveysView;
import com.elicitsoftware.test.PostgresTestResource;
import com.vaadin.browserless.quarkus.QuarkusBrowserlessTest;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.router.QueryParameters;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;

/**
 * UC-034 BR-003/BR-004: direction and language attributes follow the locale, a {@code ?lang=}
 * query parameter selects a language before the view is built, and the language switcher in the
 * header offers the shipped locales.
 */
@QuarkusTest
@QuarkusTestResource(PostgresTestResource.class)
class LocaleLayoutTest extends QuarkusBrowserlessTest {

    @Inject
    LocaleLayout layout;

    @Inject
    LocaleSelection selection;

    @Test
    void arabic_isRightToLeft() {
        UI ui = UI.getCurrent();
        layout.apply(ui, Locale.forLanguageTag("ar"));

        assertEquals("rtl", ui.getElement().getAttribute("dir"));
        assertEquals("ar", ui.getElement().getAttribute("lang"));
    }

    @Test
    void latinAmericanSpanish_isLeftToRight() {
        UI ui = UI.getCurrent();
        layout.apply(ui, Locale.forLanguageTag("es-419"));

        assertEquals("ltr", ui.getElement().getAttribute("dir"));
        assertEquals("es-419", ui.getElement().getAttribute("lang"));
    }

    @Test
    @TestSecurity(user = "locale.author", roles = {"elicit_author"})
    void langQueryParameter_selectsLanguageAndRemembersIt() {
        UI.getCurrent().navigate("", QueryParameters.of("lang", "ar"));
        assertInstanceOf(SurveysView.class, getCurrentView());

        UI ui = UI.getCurrent();
        assertEquals("ar", ui.getLocale().toLanguageTag());
        assertEquals("rtl", ui.getElement().getAttribute("dir"));
        assertEquals(Locale.forLanguageTag("ar"), ui.getSession().getAttribute(LocaleSelection.SESSION_ATTRIBUTE));
    }

    @Test
    void countryVariant_resolvesToProvidedLanguage() {
        assertEquals(Locale.forLanguageTag("es-419"), selection.resolve("es-GT").orElseThrow());
        assertEquals(Locale.forLanguageTag("ar"), selection.resolve("ar-EG").orElseThrow());
        assertTrue(selection.resolve("zz").isEmpty());
    }

    @Test
    @TestSecurity(user = "locale.author", roles = {"elicit_author"})
    void switcher_listsShippedLocales_andTracksCurrent() {
        navigate(SurveysView.class);
        LanguageSwitcher switcher = find(LanguageSwitcher.class).single();

        var items = switcher.getListDataView().getItems().toList();
        assertTrue(items.contains(Locale.ENGLISH), items.toString());
        assertTrue(items.contains(Locale.forLanguageTag("es-419")), items.toString());
        assertTrue(items.contains(Locale.forLanguageTag("ar")), items.toString());
        assertFalse(items.contains(ElicitI18NProvider.PSEUDO_LOCALE), "pseudo-locale is never offered to users");
        assertEquals(UI.getCurrent().getLocale().getLanguage(), switcher.getValue().getLanguage());
    }
}
