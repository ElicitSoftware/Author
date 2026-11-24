package com.elicitsoftware.author.diagnostics;

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

import com.elicitsoftware.author.i18n.Translations;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;

import java.util.List;

/**
 * Presence of every setting the deployment must supply because the packaged configuration
 * gives it no default (UC-034 step 4), reported as present or absent and never by value
 * (BR-001, NFR-014).
 */
@ApplicationScoped
public class RequiredConfigCheck {

    /**
     * One deployment-supplied setting.
     *
     * @param property the configuration property
     * @param envVar   the environment variable the packaged configuration reads it from
     * @param purpose  what it is for
     * @param present  whether a non-blank value is configured
     */
    public record RequiredSetting(String property, String envVar, String purpose, boolean present) {
    }

    /** {@code purposeKey} names the translation of what the setting is for (UC-040). */
    private record Required(String property, String envVar, String purposeKey) {
    }

    private static final List<Required> REQUIRED = List.of(
            new Required("quarkus.datasource.password", "ELICIT_DB_PASSWORD",
                    "systemOverviewView.setting.datasourcePassword"),
            new Required("quarkus.oidc.credentials.secret", "OIDC_CLIENT_SECRET",
                    "systemOverviewView.setting.oidcSecret"),
            new Required("elicit.service.token", "ELICIT_SERVICE_TOKEN",
                    "systemOverviewView.setting.serviceToken"));

    /** Creates the bean; CDI calls this. */
    public RequiredConfigCheck() {
        // CDI managed bean
    }

    /**
     * Every required setting and whether it is configured; a value that cannot be expanded counts
     * as absent.
     *
     * @return one entry per setting, purpose in the reader's language, in a fixed order
     */
    public List<RequiredSetting> requiredSettings() {
        Config config = ConfigProvider.getConfig();
        return REQUIRED.stream()
                .map(r -> new RequiredSetting(r.property(), r.envVar(), Translations.get(r.purposeKey()),
                        isPresent(config, r.property())))
                .toList();
    }

    static boolean isPresent(Config config, String property) {
        try {
            return config.getOptionalValue(property, String.class).filter(v -> !v.isBlank()).isPresent();
        } catch (RuntimeException e) {
            // An unexpandable value (a ${VAR} with no default and no VAR) counts as absent.
            return false;
        }
    }
}
