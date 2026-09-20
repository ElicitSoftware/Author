package com.elicitsoftware.author.flow.reporting;

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
import com.elicitsoftware.author.definition.ValidationIssue;
import com.elicitsoftware.author.flow.MainLayout;
import com.elicitsoftware.author.flow.SurveyEditorView;
import com.elicitsoftware.author.flow.designer.SurveyDesignerView;
import com.elicitsoftware.author.security.AuthorRoles;
import com.elicitsoftware.author.survey.ReportingNames;
import com.elicitsoftware.author.survey.ReportingService;
import com.elicitsoftware.author.survey.ReportingService.Assignment;
import com.elicitsoftware.author.survey.ReportingService.DimTable;
import com.elicitsoftware.author.survey.ReportingService.FactColumn;
import com.elicitsoftware.author.survey.ReportingService.Impact;
import com.elicitsoftware.author.survey.StructureNode;
import com.elicitsoftware.author.survey.SurveyService;
import com.elicitsoftware.author.survey.SurveyStructureQuery;
import com.elicitsoftware.model.Ontology;
import com.elicitsoftware.model.Survey;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.ListItem;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.html.UnorderedList;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.NotFoundException;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteParameters;
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The survey's reporting tags (UC-023), their assignments (UC-021, UC-022) and the dimensional
 * impact preview (UC-024): the tables and columns the site's ETL derives from them, and anything
 * that would make that build fail.
 */
@Route(value = "survey/:" + SurveyEditorView.SURVEY_ID + "(\\d+)/reporting", layout = MainLayout.class)
@PageTitle("Reporting tags")
@RolesAllowed(AuthorRoles.AUTHOR)
public class ReportingView extends VerticalLayout implements BeforeEnterObserver {

    @Inject
    SurveyService surveyService;

    @Inject
    ReportingService reporting;

    @Inject
    SurveyStructureQuery structureQuery;

    final H2 heading = new H2();
    final Button overview = new Button("Survey overview", VaadinIcon.ARROW_LEFT.create());
    final Button openDesigner = new Button("Open in designer", VaadinIcon.SITEMAP.create());
    final Button newTag = new Button("New tag…", VaadinIcon.PLUS.create());
    final Button assign = new Button("Assign tag…", VaadinIcon.TAG.create());
    final Grid<Ontology> tags = new Grid<>(Ontology.class, false);
    final Grid<Assignment> assignments = new Grid<>(Assignment.class, false);
    final Div impactPanel = new Div();

    public Survey survey;
    Impact impact;
    List<Assignment> allAssignments = List.of();
    final Map<Integer, Long> usesByTag = new HashMap<>();

    public ReportingView() {
        overview.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        overview.addClickListener(e -> getUI().ifPresent(ui -> ui.navigate(SurveyEditorView.class,
                new RouteParameters(SurveyEditorView.SURVEY_ID, String.valueOf(survey.id)))));
        openDesigner.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        openDesigner.addClickListener(e -> getUI().ifPresent(ui -> ui.navigate(SurveyDesignerView.class,
                new RouteParameters(SurveyEditorView.SURVEY_ID, String.valueOf(survey.id)))));
        newTag.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        newTag.addClickListener(e -> new TagDialog(reporting, survey, null, saved -> load()).open());
        assign.addClickListener(e -> new TagAssignmentDialog(reporting, survey,
                TagAssignmentDialog.targets(structureQuery.tree(survey.id, false).all()), null, null, saved -> load()).open());

        Paragraph intro = new Paragraph("A reporting tag names a column of the site's report. Assign it to a question and the "
                + "answers fill that column; assign it to a section with a fixed value and every respondent who reaches the section "
                + "is classified by it. Tags on the same dimension share one table of values.");
        Paragraph retro = new Paragraph("Changing or removing an assigned tag is retroactive: answers already recorded at deployed "
                + "sites are reclassified when the survey is exported and applied.");
        retro.addClassName(LumoUtility.TextColor.SECONDARY);

        tags.addColumn(o -> o.tag).setHeader("Tag").setAutoWidth(true);
        tags.addColumn(o -> o.dimension == null ? "(own table)" : o.dimension.name).setHeader("Dimension").setAutoWidth(true);
        tags.addColumn(ReportingNames::dimTable).setHeader("Table").setAutoWidth(true);
        tags.addColumn(o -> ReportingNames.factColumn(o.tag)).setHeader("Report column").setAutoWidth(true);
        tags.addColumn(o -> usesByTag.getOrDefault(o.id, 0L)).setHeader("Assignments").setAutoWidth(true);
        tags.addComponentColumn(o -> {
            long uses = usesByTag.getOrDefault(o.id, 0L);
            Button edit = new Button("Edit…", e -> new TagDialog(reporting, survey, o, saved -> load()).open());
            edit.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
            Button remove = new Button("Remove", e -> confirmRemoveTag(o));
            remove.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ERROR);
            remove.setEnabled(uses == 0);
            remove.setTooltipText(uses == 0 ? "Remove this tag" : "Remove its assignments first");
            return new HorizontalLayout(edit, remove);
        }).setHeader("").setAutoWidth(true);
        tags.setAllRowsVisible(true);
        tags.asSingleSelect().addValueChangeListener(e -> filterAssignments(e.getValue()));

        assignments.addColumn(Assignment::elementLabel).setHeader("Element").setAutoWidth(true);
        assignments.addColumn(Assignment::where).setHeader("Where").setAutoWidth(true);
        assignments.addColumn(Assignment::scopeLabel).setHeader("Applies to").setAutoWidth(true);
        assignments.addColumn(a -> a.metadata().ontology == null ? "" : a.metadata().ontology.tag).setHeader("Tag").setAutoWidth(true);
        assignments.addColumn(Assignment::valueLabel).setHeader("Value").setAutoWidth(true);
        assignments.addComponentColumn(a -> {
            Span badge = new Span(a.dangling() ? "removed element" : "");
            badge.addClassName("designer-badge");
            badge.setVisible(a.dangling());
            return badge;
        }).setHeader("").setAutoWidth(true);
        assignments.addComponentColumn(a -> {
            Button edit = new Button("Edit…", e -> new TagAssignmentDialog(reporting, survey, null, null, a, saved -> load()).open());
            edit.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
            Button remove = new Button("Remove", e -> confirmRemoveAssignment(a));
            remove.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ERROR);
            return new HorizontalLayout(edit, remove);
        }).setHeader("").setAutoWidth(true);
        assignments.setAllRowsVisible(true);

        impactPanel.addClassName("reporting-impact");

        HorizontalLayout toolbar = new HorizontalLayout(overview, openDesigner, newTag, assign);
        add(heading, intro, retro, toolbar, new H3("Tags"), tags, new H3("Assignments"), assignments, impactPanel);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        int id = event.getRouteParameters().getInteger(SurveyEditorView.SURVEY_ID).orElse(-1);
        Survey s = surveyService.find(id).orElse(null);
        if (s == null) {
            event.rerouteToError(NotFoundException.class, "No survey with id " + id);
            return;
        }
        survey = s;
        load();
    }

    void load() {
        survey = surveyService.find(survey.id).orElseThrow();
        heading.setText("Reporting tags — " + (survey.title == null || survey.title.isBlank() ? survey.name : survey.title));
        allAssignments = reporting.assignments(survey.id);
        usesByTag.clear();
        for (Assignment a : allAssignments) {
            if (a.metadata().ontology != null) {
                usesByTag.merge(a.metadata().ontology.id, 1L, Long::sum);
            }
        }
        Ontology selected = tags.asSingleSelect().getValue();
        tags.setItems(reporting.tags(survey.id));
        filterAssignments(selected);
        impact = reporting.impact(survey.id);
        renderImpact();
    }

    private void filterAssignments(Ontology tag) {
        assignments.setItems(tag == null ? allAssignments : allAssignments.stream()
                .filter(a -> a.metadata().ontology != null && a.metadata().ontology.id.equals(tag.id)).toList());
    }

    /** UC-024 steps 2-3: what the site's reporting schema gains, then what would break it. */
    private void renderImpact() {
        impactPanel.removeAll();
        impactPanel.add(new H3("Dimensional impact"));
        impactPanel.add(new Paragraph("When the exported survey is applied and answers arrive, the site's reporting build creates "
                + "these objects in the surveyreport schema."));
        if (impact.tables().isEmpty()) {
            impactPanel.add(new Paragraph("No tags yet: the survey adds nothing to the reporting schema."));
        } else {
            UnorderedList tableList = new UnorderedList();
            for (DimTable t : impact.tables()) {
                tableList.add(new ListItem("surveyreport." + t.name() + " — "
                        + (t.sharedDimension() ? "shared dimension '" + t.source() + "'" : "own table of tag '" + t.source() + "'")
                        + " — tags: " + String.join(", ", t.tags()) + " — " + t.assignments()
                        + (t.assignments() == 1 ? " assignment" : " assignments")));
            }
            impactPanel.add(new Span("Dimension tables"), tableList);
            UnorderedList columnList = new UnorderedList();
            for (FactColumn c : impact.columns()) {
                columnList.add(new ListItem("fact_sections." + c.column() + " → " + c.dimTable() + " (tag '" + c.tag() + "')"));
            }
            impactPanel.add(new Span("Report columns"), columnList);
        }
        if (!impact.problems().isEmpty()) {
            UnorderedList problems = new UnorderedList();
            for (ValidationIssue i : impact.problems()) {
                ListItem li = new ListItem((i.isError() ? "Error: " : "Warning: ") + i.message());
                li.addClassName(i.isError() ? LumoUtility.TextColor.ERROR : LumoUtility.TextColor.WARNING);
                problems.add(li);
            }
            impactPanel.add(new Span("Findings"), problems);
        }
    }

    private void confirmRemoveTag(Ontology o) {
        new ConfirmDialog("Remove tag", "Remove the tag '" + o.tag + "'? It has no assignments.", "Remove", e -> {
            try {
                reporting.removeTag(survey.id, o.id);
                success("Removed tag '" + o.tag + "'");
                load();
            } catch (AuthoringException ae) {
                error(ae.getMessage());
            }
        }, "Cancel", e -> { }).open();
    }

    private void confirmRemoveAssignment(Assignment a) {
        String tag = a.metadata().ontology == null ? "?" : a.metadata().ontology.tag;
        new ConfirmDialog("Remove reporting tag", "Remove tag '" + tag + "' from " + a.elementLabel() + "? Answers already recorded "
                + "at deployed sites stop being classified by it once the survey is exported and applied.", "Remove", e -> {
            try {
                reporting.unassign(survey.id, a.metadata().id);
                success("Removed tag '" + tag + "'");
                load();
            } catch (AuthoringException ae) {
                error(ae.getMessage());
            }
        }, "Cancel", e -> { }).open();
    }

    private static void success(String message) {
        Notification.show(message, 3000, Notification.Position.BOTTOM_START).addThemeVariants(NotificationVariant.LUMO_SUCCESS);
    }

    private static void error(String message) {
        Notification.show(message, 5000, Notification.Position.MIDDLE).addThemeVariants(NotificationVariant.LUMO_ERROR);
    }

    /** For the designer: the nodes an assignment may target. */
    public static List<StructureNode> targets(SurveyStructureQuery.Tree tree) {
        return TagAssignmentDialog.targets(tree.all());
    }
}
