package com.elicitsoftware.author.diagnostics;

/*-
 * ***LICENSE_START***
 * Elicit Survey
 * %%
 * Copyright (C) 2025 - 2026 The Regents of the University of Michigan - Rogel Cancer Center
 * %%
 * PolyForm Noncommercial License 1.0.0
 * <https://polyformproject.org/licenses/noncommercial/1.0.0>
 * ***LICENSE_END***
 */

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

    private record Required(String property, String envVar, String purpose) {
    }

    private static final List<Required> REQUIRED = List.of(
            new Required("quarkus.datasource.password", "ELICIT_DB_PASSWORD",
                    "password of the database user Author connects as"),
            new Required("quarkus.oidc.credentials.secret", "OIDC_CLIENT_SECRET",
                    "client secret registered with the identity provider"));

    public RequiredConfigCheck() {
        // CDI managed bean
    }

    public List<RequiredSetting> requiredSettings() {
        Config config = ConfigProvider.getConfig();
        return REQUIRED.stream()
                .map(r -> new RequiredSetting(r.property(), r.envVar(), r.purpose(), isPresent(config, r.property())))
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
