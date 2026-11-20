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

import com.elicitsoftware.author.AuthoringException;
import com.elicitsoftware.author.i18n.Translations;
import com.elicitsoftware.author.security.ServiceToken;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.HttpURLConnection;
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
 * <p>
 * The request carries the deployment's service token as a bearer credential (NFR-025). The
 * header is set on this one request, never on the client, so nothing else Author sends can
 * pick it up; a redirect is never followed, so it cannot be carried to another address; and
 * without a token the call is not made. Never log the request's headers or the token.
 */
@ApplicationScoped
public class PreviewSurveyClient {

    static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);
    static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(60);

    @ConfigProperty(name = "author.preview.survey.url")
    String previewSurveyUrl;

    @Inject
    ServiceToken serviceToken;

    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(CONNECT_TIMEOUT)
            .followRedirects(HttpClient.Redirect.NEVER)
            .build();

    /** Creates the bean; CDI calls this. */
    public PreviewSurveyClient() {
        // CDI managed bean
    }

    PreviewSurveyClient(String previewSurveyUrl, ServiceToken serviceToken) {
        this.previewSurveyUrl = previewSurveyUrl;
        this.serviceToken = serviceToken;
    }

    /**
     * Asks the preview Survey to drop the survey's reporting schema (Survey UC-011). A 200
     * means it is gone, or there was nothing to drop; anything else refuses the delete.
     *
     * @param surveyKey the survey's portable key
     * @throws AuthoringException when Author has no service token, or the preview Survey cannot
     *                            be reached or does not answer 200
     */
    public void dropReportingSchema(UUID surveyKey) {
        URI uri = URI.create(base() + "api/etl/schema/" + surveyKey);
        // UC-047 A8. No token, no call: an unauthenticated request is never sent to find out.
        String authorization = serviceToken.authorization().orElse(null);
        if (authorization == null) {
            Log.warn("Preview Survey was not asked to drop the reporting schema of " + surveyKey + ": "
                    + ServiceToken.NOT_CONFIGURED + " (" + ServiceToken.VARIABLE + ")");
            throw new AuthoringException(Translations.get("error.survey.delete.serviceTokenMissing", ServiceToken.VARIABLE));
        }
        HttpRequest request = HttpRequest.newBuilder(uri)
                .DELETE()
                .header("Accept", "application/json")
                .header("Authorization", authorization)
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
        if (response.statusCode() == HttpURLConnection.HTTP_UNAUTHORIZED) {
            // UC-047 A7. The preview Survey has a token and it is not this one.
            Log.warn("Preview Survey refused to drop the reporting schema of " + surveyKey + ": 401");
            throw new AuthoringException(Translations.get("error.survey.delete.previewUnauthorized", ServiceToken.VARIABLE));
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
