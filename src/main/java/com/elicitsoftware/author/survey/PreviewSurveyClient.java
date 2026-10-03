package com.elicitsoftware.author.survey;

/*-
 * ***LICENSE_START***
 * Elicit Author
 * %%
 * Copyright (C) 2025 - 2026 The Regents of the University of Michigan - Rogel Cancer Center
 * %%
 * PolyForm Noncommercial License 1.0.0
 * <https://polyformproject.org/licenses/noncommercial/1.0.0>
 * ***LICENSE_END***
 */

import com.elicitsoftware.author.AuthoringException;
import com.elicitsoftware.author.i18n.Translations;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.UUID;

/**
 * Author's one call to the preview Survey, the Survey instance that renders drafts against
 * Author's own database: dropping a survey's reporting schema before the survey is deleted
 * (UC-047 BR-008, Survey UC-011). Author connects to the database as {@code survey_user}, which
 * cannot drop a schema; the preview Survey owns the reporting schemas and exposes the drop.
 * <p>
 * The address is {@code author.preview.survey.url}. The call is bounded: a few seconds to
 * connect and a minute for the drop, which is a single DDL statement under Survey's rebuild lock
 * and may wait for a build in progress.
 */
@ApplicationScoped
public class PreviewSurveyClient {

    static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);
    static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(60);

    @ConfigProperty(name = "author.preview.survey.url")
    String previewSurveyUrl;

    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(CONNECT_TIMEOUT)
            .followRedirects(HttpClient.Redirect.NEVER)
            .build();

    public PreviewSurveyClient() {
        // CDI managed bean
    }

    /**
     * Asks the preview Survey to drop the survey's reporting schema (Survey UC-011). A 200
     * means it is gone, or there was nothing to drop; anything else refuses the delete.
     *
     * @param surveyKey the survey's portable key
     * @throws AuthoringException when the preview Survey cannot be reached or does not answer 200
     */
    public void dropReportingSchema(UUID surveyKey) {
        URI uri = URI.create(base() + "api/etl/schema/" + surveyKey);
        HttpRequest request = HttpRequest.newBuilder(uri)
                .DELETE()
                .header("Accept", "application/json")
                .timeout(REQUEST_TIMEOUT)
                .build();
        HttpResponse<String> response;
        try {
            response = client.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException e) {
            Log.warn("Preview Survey at " + uri + " could not be reached to drop a reporting schema: " + e);
            throw new AuthoringException(Translations.get("error.survey.delete.previewUnreachable", uri.toString(), String.valueOf(e.getMessage())));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AuthoringException(Translations.get("error.survey.delete.previewUnreachable", uri.toString(), "interrupted"));
        }
        if (response.statusCode() != 200) {
            Log.warn("Preview Survey refused to drop the reporting schema of " + surveyKey + ": " + response.statusCode() + " " + response.body());
            throw new AuthoringException(Translations.get("error.survey.delete.previewRefused",
                    Integer.toString(response.statusCode()), response.body()));
        }
        Log.info("Preview Survey dropped the reporting schema of " + surveyKey + ": " + response.body());
    }

    private String base() {
        return previewSurveyUrl.endsWith("/") ? previewSurveyUrl : previewSurveyUrl + "/";
    }
}
