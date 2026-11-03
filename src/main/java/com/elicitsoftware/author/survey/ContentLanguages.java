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

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * The languages a survey's content may be published in (UC-044 BR-004).
 * <p>
 * Elicit's application text is translated by ElicitSoftware and curated as one set, the
 * umbrella's {@code elicit-i18n/}, which a deployment mounts beside the applications. A survey may
 * only publish content in a language the applications can also render, because a language the
 * chrome does not have is never served: {@code ContentTranslator} in Survey skips it, silently and
 * by design, so content published in it would reach no respondent anywhere.
 * <p>
 * This list is a decision made at release time, not a fact of the filesystem, so it is stated
 * rather than discovered. It is deliberately not taken from the chrome provider's
 * {@code getProvidedLocales()}: that reports what <em>this</em> Author's own interface offers --
 * English plus whatever this deployment happens to have mounted, narrowed by
 * {@code i18n.offered.locales} -- and says nothing about what Survey renders. An institution
 * running Author in English would otherwise find it refusing to publish Arabic content that every
 * one of its sites serves.
 * <p>
 * Because the property is stated by hand it can drift from the curated set. {@code checkLanguages.sh},
 * in the umbrella and in Author's build, is what holds them together; nothing here can detect the drift.
 */
@ApplicationScoped
public class ContentLanguages {

    private static final Logger LOG = Logger.getLogger(ContentLanguages.class);

    /** What ElicitSoftware ships. Overridden per deployment only to match a custom build. */
    @ConfigProperty(name = "author.content.languages", defaultValue = "en,ar,es-419")
    String configured;

    private List<String> tags = List.of();

    @PostConstruct
    void parse() {
        Set<String> ordered = new LinkedHashSet<>();
        if (configured != null) {
            for (String raw : configured.split(",")) {
                String canonical = LanguageTags.canonical(raw);
                if (canonical == null) {
                    if (!raw.isBlank()) {
                        LOG.warnf("author.content.languages names '%s', which is not a language tag; ignoring it",
                                raw.strip());
                    }
                    continue;
                }
                ordered.add(canonical);
            }
        }
        if (ordered.isEmpty()) {
            LOG.warn("author.content.languages names no usable language; falling back to English");
            ordered.add("en");
        }
        tags = List.copyOf(ordered);
    }

    /**
     * The curated tags, canonical and in the order configured.
     *
     * @return the languages a survey may publish content in, never empty
     */
    public List<String> tags() {
        return tags;
    }

    /**
     * Whether content may be published in this language.
     *
     * @param tag a tag in any form; canonicalized before the comparison
     * @return true when the applications carry this language
     */
    public boolean supports(String tag) {
        String canonical = LanguageTags.canonical(tag);
        return canonical != null && tags.contains(canonical);
    }
}
