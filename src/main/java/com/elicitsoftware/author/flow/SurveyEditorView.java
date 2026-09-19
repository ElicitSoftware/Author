package com.elicitsoftware.author.flow;

/*-
 * ***LICENSE_START***
 * Elicit Author
 * %%
 * Copyright (C) 2026 The Regents of the University of Michigan - Rogel Cancer Center
 * %%
 * PolyForm Noncommercial License 1.0.0
 * <https://polyformproject.org/licenses/noncommercial/1.0.0>
 * ***LICENSE_END***
 */

import com.elicitsoftware.author.definition.ExportValidation;
import com.elicitsoftware.author.definition.SurveyDefinitionExporter;
import com.elicitsoftware.author.definition.ValidationIssue;
import com.elicitsoftware.author.security.AuthorRoles;
import com.elicitsoftware.author.survey.SurveyService;
import com.elicitsoftware.model.Survey;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.ListItem;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.UnorderedList;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.NotFoundException;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;

import java.util.List;

/** Survey editor entry (UC-006): summary, validation panel (UC-007) and export (UC-008). */
@Route(value = "survey/:" + SurveyEditorView.SURVEY_ID + "(\\d+)", layout = MainLayout.class)
@PageTitle("Survey")
@RolesAllowed(AuthorRoles.AUTHOR)
public class SurveyEditorView extends VerticalLayout implements BeforeEnterObserver {

    public static final String SURVEY_ID = "surveyId";

    @Inject
    SurveyService surveyService;

    @Inject
    ExportValidation validation;

    @Inject
    SurveyDefinitionExporter exporter;

    final H2 heading = new H2();
    final Paragraph summary = new Paragraph();
    final Paragraph attribution = new Paragraph();
    final Button editDetails = new Button("Edit details", VaadinIcon.PENCIL.create());
    final Button exportButton = new Button("Export…", VaadinIcon.DOWNLOAD.create());
    final Button design = new Button("Design…", VaadinIcon.SITEMAP.create());
    final Button reporting = new Button("Reporting…", VaadinIcon.TABLE.create());
    final Div validationPanel = new Div();

    Survey survey;
    List<ValidationIssue> issues = List.of();

    public SurveyEditorView() {
        HorizontalLayout toolbar = new HorizontalLayout(design, reporting, editDetails, exportButton);
        reporting.addClickListener(e -> getUI().ifPresent(ui -> ui.navigate(com.elicitsoftware.author.flow.reporting.ReportingView.class,
                new com.vaadin.flow.router.RouteParameters(SURVEY_ID, String.valueOf(survey.id)))));
        design.addClickListener(e -> getUI().ifPresent(ui -> ui.navigate(com.elicitsoftware.author.flow.designer.SurveyDesignerView.class,
                new com.vaadin.flow.router.RouteParameters(SURVEY_ID, String.valueOf(survey.id)))));
        exportButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        editDetails.addClickListener(e -> new SurveyMetadataDialog(surveyService, survey, saved -> load(saved.id)).open());
        exportButton.addClickListener(e -> new ExportDialog(surveyService, exporter, survey, issues, saved -> load(saved.id)).open());
        add(heading, summary, attribution, toolbar, validationPanel);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        int id = event.getRouteParameters().getInteger(SURVEY_ID).orElse(-1);
        if (surveyService.find(id).isEmpty()) {
            event.rerouteToError(NotFoundException.class, "No survey with id " + id);
            return;
        }
        load(id);
    }

    void load(int id) {
        survey = surveyService.find(id).orElseThrow();
        heading.setText(survey.title);
        summary.setText(survey.name + " — key " + survey.surveyKey);
        // published_by is the last editor until an export stamps it together with a release note.
        attribution.setText(survey.publishedComment == null
                ? "Never exported." + (survey.publishedBy == null ? "" : " Last edited by " + survey.publishedBy + ".")
                : "Last exported by " + survey.publishedBy + ": " + survey.publishedComment);
        issues = validation.validate(survey);
        validationPanel.removeAll();
        validationPanel.add(new H3("Validation"));
        if (issues.isEmpty()) {
            validationPanel.add(new Paragraph("Ready to export: no findings."));
        } else {
            UnorderedList list = new UnorderedList();
            issues.forEach(i -> {
                ListItem li = new ListItem((i.isError() ? "Error: " : "Warning: ") + i.message());
                li.addClassName(i.isError() ? LumoUtility.TextColor.ERROR : LumoUtility.TextColor.WARNING);
                list.add(li);
            });
            validationPanel.add(list);
        }
    }
}
