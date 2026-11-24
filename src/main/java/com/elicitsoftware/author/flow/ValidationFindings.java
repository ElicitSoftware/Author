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

import com.elicitsoftware.author.definition.ValidationIssue;
import com.elicitsoftware.author.definition.ValidationTargets;
import com.elicitsoftware.author.flow.designer.SurveyDesignerView;
import com.elicitsoftware.author.flow.reporting.ReportingView;
import com.elicitsoftware.author.flow.translations.TranslationsView;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.ListItem;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.html.UnorderedList;
import com.vaadin.flow.router.QueryParameters;
import com.vaadin.flow.router.RouteParameters;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.theme.lumo.LumoUtility;

import java.util.List;
import java.util.Optional;

/**
 * The list of validation findings (UC-007 BR-008, BR-009 / FR-061), shared by the editor's
 * validation panel and the export dialog.
 *
 * <p>Each finding says where its element sits and, in the panel, is a link that opens it. The
 * dialog gets the same text without the links: it is modal, and a link followed from inside it
 * would leave the dialog standing over the page it navigated to.</p>
 */
public class ValidationFindings extends UnorderedList {

    /** Between the labels of a path, as the designer's breadcrumb separates its crumbs. */
    private static final String SEPARATOR = " › ";

    public ValidationFindings(int surveyId, List<ValidationIssue> issues, ValidationTargets.Resolver targets,
                              boolean navigable, String messageKeyPrefix) {
        addClassName("validation-findings");
        RouteParameters survey = new RouteParameters(SurveyEditorView.SURVEY_ID, String.valueOf(surveyId));
        for (ValidationIssue issue : issues) {
            Optional<ValidationTargets.Target> target = targets.of(issue);
            String message = getTranslation(messageKeyPrefix + (issue.isError() ? ".error" : ".warning"), issue.message());

            ListItem item = new ListItem();
            item.addClassName(issue.isError() ? LumoUtility.TextColor.ERROR : LumoUtility.TextColor.WARNING);
            item.add(navigable
                    ? target.map(t -> link(message, t, survey)).orElseGet(() -> new Span(message))
                    : new Span(message));
            target.map(ValidationTargets.Target::location).filter(path -> !path.isEmpty())
                    .ifPresent(path -> item.add(location(path)));
            add(item);
        }
    }

    private Component link(String message, ValidationTargets.Target target, RouteParameters survey) {
        RouterLink link = switch (target.destination()) {
            case DESIGNER -> new RouterLink(message, SurveyDesignerView.class, survey);
            case REPORTING -> new RouterLink(message, ReportingView.class, survey);
            case TRANSLATIONS -> new RouterLink(message, TranslationsView.class, survey);
        };
        if (target.address() != null) {
            link.setQueryParameters(QueryParameters.of(SurveyDesignerView.SELECT, target.address().toString()));
        }
        link.addClassName("validation-finding-link");
        return link;
    }

    private Component location(List<String> path) {
        Span where = new Span(getTranslation("validation.finding.location", String.join(SEPARATOR, path)));
        where.addClassName("validation-finding-location");
        return where;
    }
}
