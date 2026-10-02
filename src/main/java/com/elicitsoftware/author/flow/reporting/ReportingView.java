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
import com.vaadin.flow.router.HasDynamicTitle;
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
@RolesAllowed(AuthorRoles.AUTHOR)
public class ReportingView extends VerticalLayout implements BeforeEnterObserver, HasDynamicTitle {

    @Inject
    SurveyService surveyService;

    @Inject
    ReportingService reporting;

    @Inject
    SurveyStructureQuery structureQuery;

    final H2 heading = new H2();
    final Button overview = new Button(VaadinIcon.ARROW_LEFT.create());
    final Button openDesigner = new Button(VaadinIcon.SITEMAP.create());
    final Button newTag = new Button(VaadinIcon.PLUS.create());
    final Button assign = new Button(VaadinIcon.TAG.create());
    final Grid<Ontology> tags = new Grid<>(Ontology.class, false);
    final Grid<Assignment> assignments = new Grid<>(Assignment.class, false);
    final Div impactPanel = new Div();

    public Survey survey;
    Impact impact;
    List<Assignment> allAssignments = List.of();
    final Map<Integer, Long> usesByTag = new HashMap<>();

    public ReportingView() {
        overview.setText(getTranslation("reportingView.surveyOverview"));
        openDesigner.setText(getTranslation("reportingView.openInDesigner"));
        newTag.setText(getTranslation("reportingView.newTag"));
        assign.setText(getTranslation("reportingView.assignTag"));
        heading.getElement().setAttribute("data-i18n-content", "");
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

        Paragraph intro = new Paragraph(getTranslation("reportingView.intro"));
        Paragraph retro = new Paragraph(getTranslation("reportingView.retro"));
        retro.addClassName(LumoUtility.TextColor.SECONDARY);

        tags.getElement().setAttribute("data-i18n-content", ""); // tag and dimension names are authored
        tags.addColumn(o -> o.tag).setHeader(getTranslation("reportingView.grid.tag")).setAutoWidth(true);
        tags.addColumn(o -> o.dimension == null ? getTranslation("reportingView.grid.ownTable") : o.dimension.name)
                .setHeader(getTranslation("reportingView.grid.dimension")).setAutoWidth(true);
        tags.addColumn(ReportingNames::dimTable).setHeader(getTranslation("reportingView.grid.table")).setAutoWidth(true);
        tags.addColumn(o -> ReportingNames.factColumn(o.tag)).setHeader(getTranslation("reportingView.grid.reportColumn")).setAutoWidth(true);
        tags.addColumn(o -> usesByTag.getOrDefault(o.id, 0L)).setHeader(getTranslation("reportingView.grid.assignments")).setAutoWidth(true);
        tags.addComponentColumn(o -> {
            long uses = usesByTag.getOrDefault(o.id, 0L);
            Button edit = new Button(getTranslation("reportingView.edit"), e -> new TagDialog(reporting, survey, o, saved -> load()).open());
            edit.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
            Button remove = new Button(getTranslation("common.remove"), e -> confirmRemoveTag(o));
            remove.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ERROR);
            remove.setEnabled(uses == 0);
            remove.setTooltipText(uses == 0 ? getTranslation("reportingView.removeTag.tooltip") : getTranslation("reportingView.removeTag.blocked"));
            return new HorizontalLayout(edit, remove);
        }).setHeader("").setAutoWidth(true);
        tags.setAllRowsVisible(true);
        tags.asSingleSelect().addValueChangeListener(e -> filterAssignments(e.getValue()));

        assignments.getElement().setAttribute("data-i18n-content", ""); // element, tag and value cells are authored
        assignments.addColumn(Assignment::elementLabel).setHeader(getTranslation("reportingView.assignmentsGrid.element")).setAutoWidth(true);
        assignments.addColumn(Assignment::where).setHeader(getTranslation("reportingView.assignmentsGrid.where")).setAutoWidth(true);
        assignments.addColumn(Assignment::scopeLabel).setHeader(getTranslation("reportingView.assignmentsGrid.appliesTo")).setAutoWidth(true);
        assignments.addColumn(a -> a.metadata().ontology == null ? "" : a.metadata().ontology.tag).setHeader(getTranslation("reportingView.assignmentsGrid.tag")).setAutoWidth(true);
        assignments.addColumn(Assignment::valueLabel).setHeader(getTranslation("reportingView.assignmentsGrid.value")).setAutoWidth(true);
        assignments.addComponentColumn(a -> {
            Span badge = new Span(a.dangling() ? getTranslation("reportingView.removedElement") : "");
            badge.addClassName("designer-badge");
            badge.setVisible(a.dangling());
            return badge;
        }).setHeader("").setAutoWidth(true);
        assignments.addComponentColumn(a -> {
            Button edit = new Button(getTranslation("reportingView.edit"), e -> new TagAssignmentDialog(reporting, survey, null, null, a, saved -> load()).open());
            edit.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
            Button remove = new Button(getTranslation("common.remove"), e -> confirmRemoveAssignment(a));
            remove.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ERROR);
            return new HorizontalLayout(edit, remove);
        }).setHeader("").setAutoWidth(true);
        assignments.setAllRowsVisible(true);

        impactPanel.addClassName("reporting-impact");

        HorizontalLayout toolbar = new HorizontalLayout(overview, openDesigner, newTag, assign);
        add(heading, intro, retro, toolbar, new H3(getTranslation("reportingView.tags")), tags,
                new H3(getTranslation("reportingView.assignments")), assignments, impactPanel);
    }

    @Override
    public String getPageTitle() {
        return getTranslation("reportingView.pageTitle");
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        int id = event.getRouteParameters().getInteger(SurveyEditorView.SURVEY_ID).orElse(-1);
        Survey s = surveyService.find(id).orElse(null);
        if (s == null) {
            event.rerouteToError(NotFoundException.class, getTranslation("surveyDesignerView.noSurvey", String.valueOf(id)));
            return;
        }
        survey = s;
        load();
    }

    void load() {
        survey = surveyService.find(survey.id).orElseThrow();
        heading.setText(getTranslation("reportingView.heading", survey.title == null || survey.title.isBlank() ? survey.name : survey.title));
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
        impactPanel.add(new H3(getTranslation("reportingView.impact.title")));
        impactPanel.add(new Paragraph(getTranslation("reportingView.impact.intro")));
        if (impact.tables().isEmpty()) {
            impactPanel.add(new Paragraph(getTranslation("reportingView.impact.none")));
        } else {
            UnorderedList tableList = new UnorderedList();
            tableList.getElement().setAttribute("data-i18n-content", ""); // derived table names and authored tags
            for (DimTable t : impact.tables()) {
                String source = t.sharedDimension()
                        ? getTranslation("reportingView.impact.sharedDimension", t.source())
                        : getTranslation("reportingView.impact.ownTable", t.source());
                String count = t.assignments() == 1
                        ? getTranslation("reportingView.impact.assignmentOne", t.assignments())
                        : getTranslation("reportingView.impact.assignmentMany", t.assignments());
                tableList.add(new ListItem(getTranslation("reportingView.impact.table",
                        t.name(), source, String.join(", ", t.tags()), count)));
            }
            impactPanel.add(new Span(getTranslation("reportingView.impact.dimensionTables")), tableList);
            UnorderedList columnList = new UnorderedList();
            columnList.getElement().setAttribute("data-i18n-content", "");
            for (FactColumn c : impact.columns()) {
                columnList.add(new ListItem(getTranslation("reportingView.impact.column",
                        "fact_sections." + c.column(), c.dimTable(), c.tag())));
            }
            impactPanel.add(new Span(getTranslation("reportingView.impact.reportColumns")), columnList);
        }
        if (!impact.perItemColumns().isEmpty()) {
            // Survey UC-008 BR-012: a section repeated per selected item fills the two fixed columns.
            UnorderedList fixedList = new UnorderedList();
            fixedList.getElement().setAttribute("data-i18n-content", "");
            for (FactColumn c : impact.perItemColumns()) {
                fixedList.add(new ListItem(getTranslation("reportingView.impact.perItemColumn",
                        "fact_sections." + c.column(), c.dimTable())));
            }
            impactPanel.add(new Span(getTranslation("reportingView.impact.perItemColumns")), fixedList);
        }
        if (!impact.problems().isEmpty()) {
            UnorderedList problems = new UnorderedList();
            problems.getElement().setAttribute("data-i18n-content", ""); // service findings (localized separately)
            for (ValidationIssue i : impact.problems()) {
                ListItem li = new ListItem(i.isError()
                        ? getTranslation("reportingView.impact.error", i.message())
                        : getTranslation("reportingView.impact.warning", i.message()));
                li.addClassName(i.isError() ? LumoUtility.TextColor.ERROR : LumoUtility.TextColor.WARNING);
                problems.add(li);
            }
            impactPanel.add(new Span(getTranslation("reportingView.impact.findings")), problems);
        }
    }

    private void confirmRemoveTag(Ontology o) {
        new ConfirmDialog(getTranslation("reportingView.removeTag.header"),
                getTranslation("reportingView.removeTag.text", o.tag), getTranslation("common.remove"), e -> {
            try {
                reporting.removeTag(survey.id, o.id);
                success(getTranslation("reportingView.removedTag", o.tag));
                load();
            } catch (AuthoringException ae) {
                error(ae.getMessage());
            }
        }, getTranslation("common.cancel"), e -> { }).open();
    }

    private void confirmRemoveAssignment(Assignment a) {
        String tag = a.metadata().ontology == null ? "?" : a.metadata().ontology.tag;
        new ConfirmDialog(getTranslation("reportingView.removeAssignment.header"),
                getTranslation("reportingView.removeAssignment.text", tag, a.elementLabel()), getTranslation("common.remove"), e -> {
            try {
                reporting.unassign(survey.id, a.metadata().id);
                success(getTranslation("reportingView.removedTag", tag));
                load();
            } catch (AuthoringException ae) {
                error(ae.getMessage());
            }
        }, getTranslation("common.cancel"), e -> { }).open();
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
