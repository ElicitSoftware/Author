package com.elicitsoftware.author.i18n;

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

import com.vaadin.flow.component.UI;
import com.vaadin.flow.i18n.I18NProvider;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinSession;

import java.util.Locale;

/**
 * Translation lookup for code that is not a Vaadin component (validators, the PDF renderer, REST
 * resources). Uses the running service's provider when there is one and falls back to a
 * standalone {@link ElicitI18NProvider} reading the classpath bundles, so plain unit tests and
 * non-Vaadin threads translate too.
 */
public final class Translations {

    private static volatile ElicitI18NProvider standalone;

    private Translations() {
    }

    /**
     * Translates for the current UI's locale (or the session's, or English).
     *
     * @param key    the translation key
     * @param params values for the message's placeholders
     * @return the translated text, as the active provider renders it
     */
    public static String get(String key, Object... params) {
        return get(currentLocale(), key, params);
    }

    /**
     * Translates for the given locale.
     *
     * @param locale the locale; null means English
     * @param key    the translation key
     * @param params values for the message's placeholders
     * @return the translated text, as the active provider renders it
     */
    public static String get(Locale locale, String key, Object... params) {
        return provider().getTranslation(key, locale == null ? ElicitI18NProvider.DEFAULT_LOCALE : locale, params);
    }

    /**
     * Returns the current UI's locale, else the current session's, else English.
     *
     * @return the locale translations are looked up for, never null
     */
    public static Locale currentLocale() {
        UI ui = UI.getCurrent();
        if (ui != null) {
            return ui.getLocale();
        }
        VaadinSession session = VaadinSession.getCurrent();
        if (session != null && session.getLocale() != null) {
            return session.getLocale();
        }
        return ElicitI18NProvider.DEFAULT_LOCALE;
    }

    static I18NProvider provider() {
        VaadinService service = VaadinService.getCurrent();
        if (service != null) {
            I18NProvider fromService = service.getInstantiator().getI18NProvider();
            if (fromService != null) {
                return fromService;
            }
        }
        ElicitI18NProvider p = standalone;
        if (p == null) {
            p = new ElicitI18NProvider();
            standalone = p;
        }
        return p;
    }
}
