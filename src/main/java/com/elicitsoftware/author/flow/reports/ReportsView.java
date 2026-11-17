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
package com.elicitsoftware.author.flow.reports;

import com.elicitsoftware.author.AuthoringException;
import com.elicitsoftware.author.flow.MainLayout;
import com.elicitsoftware.author.flow.SurveyEditorView;
import com.elicitsoftware.author.flow.designer.SurveyDesignerView;
import com.elicitsoftware.author.security.AuthorRoles;
import com.elicitsoftware.author.survey.ReportMarkService;
import com.elicitsoftware.author.survey.ReportMarkService.Mark;
import com.elicitsoftware.author.survey.ReportService;
import com.elicitsoftware.author.survey.SurveyService;
import com.elicitsoftware.author.survey.SurveyStructureQuery;
import com.elicitsoftware.model.Report;
import com.elicitsoftware.model.ReportItem.ReportType;
import com.elicitsoftware.model.Survey;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.dnd.GridDropLocation;
import com.vaadin.flow.component.grid.dnd.GridDropMode;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.router.NotFoundException;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteParameters;
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * The survey's report services (UC-048) and report marks (UC-049): what respondents see after
 * they finish and what the administrator report monitors. Both lists are the order the reports
 * use; a row is dragged to reorder.
 */
@Route(value = "survey/:" + SurveyEditorView.SURVEY_ID + "(\\d+)/reports", layout = MainLayout.class)
@RolesAllowed(AuthorRoles.AUTHOR)
public class ReportsView extends VerticalLayout implements BeforeEnterObserver, HasDynamicTitle {

    @Inject
    SurveyService surveyService;

    @Inject
    ReportService reports;

    @Inject
    ReportMarkService marks;

    @Inject
    SurveyStructureQuery structureQuery;

    /** UC-048 A5: where the respondent report can be seen for a preview respondent. */
    @Inject
    @ConfigProperty(name = "author.preview.survey.url")
    java.util.Optional<String> previewSurveyUrl;

    /** UC-048 BR-007: the preview's address as a browser opens it, when it differs from the one above. */
    @Inject
    @ConfigProperty(name = "author.preview.survey.public-url")
    java.util.Optional<String> previewSurveyPublicUrl;

    final H2 heading = new H2();
    final Button overview = new Button(VaadinIcon.ARROW_LEFT.create());
    final Button openDesigner = new Button(VaadinIcon.SITEMAP.create());
    final Button addService = new Button(VaadinIcon.PLUS.create());
    final Button markElement = new Button(VaadinIcon.CLIPBOARD_TEXT.create());
    final Grid<Report> services = new Grid<>(Report.class, false);
    final Paragraph servicesEmpty = new Paragraph();
    final Tabs tabs = new Tabs();
    final Tab respondentTab = new Tab();
    final Tab administratorTab = new Tab();
    final Grid<Mark> marksGrid = new Grid<>(Mark.class, false);
    final Paragraph marksEmpty = new Paragraph();
    final Anchor preview = new Anchor();
    final Paragraph previewHint = new Paragraph();

    public Survey survey;
    ReportType shownReport = ReportType.RESPONDENT;
    private Report draggedService;
    private Mark draggedMark;

    public ReportsView() {
        overview.setText(getTranslation("reportsView.surveyOverview"));
        openDesigner.setText(getTranslation("reportsView.openInDesigner"));
        addService.setText(getTranslation("reportsView.addService"));
        markElement.setText(getTranslation("reportsView.markElement"));
        respondentTab.setLabel(getTranslation("reportsView.tab.respondent"));
        administratorTab.setLabel(getTranslation("reportsView.tab.administrator"));
        servicesEmpty.setText(getTranslation("reportsView.services.empty"));
        marksEmpty.setText(getTranslation("reportsView.marks.empty"));
        preview.setText(getTranslation("reportsView.preview"));
        preview.setTarget("_blank");
        preview.addClassName(LumoUtility.FontWeight.SEMIBOLD);
        previewHint.setText(getTranslation("reportsView.preview.hint"));
        previewHint.addClassName(LumoUtility.TextColor.SECONDARY);
        heading.getElement().setAttribute("data-i18n-content", "");
        overview.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        overview.addClickListener(e -> getUI().ifPresent(ui -> ui.navigate(SurveyEditorView.class,
                new RouteParameters(SurveyEditorView.SURVEY_ID, String.valueOf(survey.id)))));
        openDesigner.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        openDesigner.addClickListener(e -> getUI().ifPresent(ui -> ui.navigate(SurveyDesignerView.class,
                new RouteParameters(SurveyEditorView.SURVEY_ID, String.valueOf(survey.id)))));
        addService.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        addService.addClickListener(e -> new ReportServiceDialog(reports, survey, null, saved -> load()).open());
        markElement.addClickListener(e -> new ReportMarkDialog(marks, survey,
                structureQuery.tree(survey.id, false).all(), null, null, saved -> load()).open());

        Paragraph intro = new Paragraph(getTranslation("reportsView.intro"));
        Paragraph dragHint = new Paragraph(getTranslation("reportsView.dragHint"));
        dragHint.addClassName(LumoUtility.TextColor.SECONDARY);

        services.getElement().setAttribute("data-i18n-content", ""); // names, descriptions and addresses are authored
        services.addColumn(r -> r.displayOrder).setHeader(getTranslation("reportsView.servicesGrid.order")).setAutoWidth(true).setFlexGrow(0);
        services.addComponentColumn(r -> {
            Span name = new Span(r.name);
            if (!ReportService.isBuiltIn(r)) {
                return name;
            }
            Span badge = new Span(getTranslation("reportsView.builtIn"));
            badge.addClassName("designer-badge");
            badge.setTitle(getTranslation("reportsView.builtIn.title"));
            HorizontalLayout cell = new HorizontalLayout(name, badge);
            cell.setAlignItems(HorizontalLayout.Alignment.BASELINE);
            cell.setSpacing(true);
            return cell;
        }).setHeader(getTranslation("reportsView.servicesGrid.name")).setAutoWidth(true);
        services.addColumn(r -> r.description).setHeader(getTranslation("reportsView.servicesGrid.description")).setAutoWidth(true);
        services.addColumn(r -> r.url).setHeader(getTranslation("reportsView.servicesGrid.url")).setAutoWidth(true);
        services.addComponentColumn(r -> {
            Button edit = new Button(getTranslation("reportsView.edit"), e -> new ReportServiceDialog(reports, survey, r, saved -> load()).open());
            edit.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
            Button remove = new Button(getTranslation("common.remove"), e -> confirmRemoveService(r));
            remove.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ERROR);
            return new HorizontalLayout(edit, remove);
        }).setHeader("").setAutoWidth(true);
        services.setAllRowsVisible(true);
        services.setRowsDraggable(true);
        services.setDropMode(GridDropMode.BETWEEN);
        services.addDragStartListener(e -> draggedService = e.getDraggedItems().get(0));
        services.addDragEndListener(e -> draggedService = null);
        services.addDropListener(e -> e.getDropTargetItem().ifPresent(target -> {
            if (draggedService != null) {
                moveService(draggedService, target, e.getDropLocation() == GridDropLocation.ABOVE);
            }
        }));

        tabs.add(respondentTab, administratorTab);
        tabs.addSelectedChangeListener(e -> {
            shownReport = e.getSelectedTab() == administratorTab ? ReportType.ADMINISTRATOR : ReportType.RESPONDENT;
            refreshMarks();
        });

        marksGrid.getElement().setAttribute("data-i18n-content", ""); // element names and labels are authored
        marksGrid.addColumn(m -> m.item().displayOrder).setHeader(getTranslation("reportsView.marksGrid.order")).setAutoWidth(true).setFlexGrow(0);
        marksGrid.addColumn(Mark::elementLabel).setHeader(getTranslation("reportsView.marksGrid.element")).setAutoWidth(true);
        marksGrid.addColumn(Mark::where).setHeader(getTranslation("reportsView.marksGrid.where")).setAutoWidth(true);
        marksGrid.addColumn(Mark::scopeLabel).setHeader(getTranslation("reportsView.marksGrid.appliesTo")).setAutoWidth(true);
        marksGrid.addColumn(Mark::shownAs).setHeader(getTranslation("reportsView.marksGrid.label")).setAutoWidth(true);
        marksGrid.addComponentColumn(m -> {
            Span badge = new Span(m.dangling() ? getTranslation("reportsView.removedElement") : "");
            badge.addClassName("designer-badge");
            badge.setVisible(m.dangling());
            return badge;
        }).setHeader("").setAutoWidth(true);
        marksGrid.addComponentColumn(m -> {
            Button edit = new Button(getTranslation("reportsView.editLabel"), e -> new ReportMarkDialog(marks, survey, null, null, m, saved -> load()).open());
            edit.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
            Button unmark = new Button(getTranslation("reportsView.unmark"), e -> confirmUnmark(m));
            unmark.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ERROR);
            return new HorizontalLayout(edit, unmark);
        }).setHeader("").setAutoWidth(true);
        marksGrid.setAllRowsVisible(true);
        marksGrid.setRowsDraggable(true);
        marksGrid.setDropMode(GridDropMode.BETWEEN);
        marksGrid.addDragStartListener(e -> draggedMark = e.getDraggedItems().get(0));
        marksGrid.addDragEndListener(e -> draggedMark = null);
        marksGrid.addDropListener(e -> e.getDropTargetItem().ifPresent(target -> {
            if (draggedMark != null) {
                moveMark(draggedMark, target, e.getDropLocation() == GridDropLocation.ABOVE);
            }
        }));

        HorizontalLayout toolbar = new HorizontalLayout(overview, openDesigner, addService, markElement);
        add(heading, intro, dragHint, toolbar,
                new H3(getTranslation("reportsView.services")), services, servicesEmpty,
                new H3(getTranslation("reportsView.marks")), tabs, marksGrid, marksEmpty, preview, previewHint);
    }

    @Override
    public String getPageTitle() {
        return getTranslation("reportsView.pageTitle");
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
        heading.setText(getTranslation("reportsView.heading", survey.title == null || survey.title.isBlank() ? survey.name : survey.title));
        // UC-048 A5: the preview Survey renders the built-in report for a preview respondent.
        String previewUrl = previewLink(previewSurveyPublicUrl, previewSurveyUrl);
        preview.setVisible(previewUrl != null);
        previewHint.setVisible(previewUrl != null);
        if (previewUrl != null) {
            preview.setHref(previewUrl);
        }
        List<Report> rows = reports.reports(survey.id);
        services.setItems(rows);
        services.setVisible(!rows.isEmpty());
        servicesEmpty.setVisible(rows.isEmpty());
        refreshMarks();
    }

    /**
     * The preview link's address (UC-048 BR-007): the browser-facing address when one is set,
     * else the address Author calls the preview at, else none.
     *
     * @param publicUrl {@code author.preview.survey.public-url}
     * @param url       {@code author.preview.survey.url}
     * @return the address to link to, or {@code null} to show no link
     */
    static String previewLink(java.util.Optional<String> publicUrl, java.util.Optional<String> url) {
        for (java.util.Optional<String> candidate : List.of(
                publicUrl == null ? java.util.Optional.<String>empty() : publicUrl,
                url == null ? java.util.Optional.<String>empty() : url)) {
            String address = candidate.map(String::strip).filter(u -> !u.isEmpty()).orElse(null);
            if (address != null) {
                return address;
            }
        }
        return null;
    }

    private void refreshMarks() {
        List<Mark> rows = marks.marks(survey.id, shownReport);
        marksGrid.setItems(rows);
        marksGrid.setVisible(!rows.isEmpty());
        marksEmpty.setVisible(rows.isEmpty());
    }

    /** UC-048 step 5: the dragged row lands before or after the target; the service renumbers. */
    void moveService(Report moved, Report target, boolean before) {
        if (Objects.equals(moved.id, target.id)) {
            return;
        }
        List<Integer> ids = new ArrayList<>();
        for (Report r : reports.reports(survey.id)) {
            if (!Objects.equals(r.id, moved.id)) {
                ids.add(r.id);
            }
        }
        int at = ids.indexOf(target.id) + (before ? 0 : 1);
        ids.add(at, moved.id);
        reports.reorder(survey.id, ids);
        load();
    }

    /** UC-049 step 5: same on the shown report's marks. */
    void moveMark(Mark moved, Mark target, boolean before) {
        if (Objects.equals(moved.item().id, target.item().id)) {
            return;
        }
        List<Integer> ids = new ArrayList<>();
        for (Mark m : marks.marks(survey.id, shownReport)) {
            if (!Objects.equals(m.item().id, moved.item().id)) {
                ids.add(m.item().id);
            }
        }
        int at = ids.indexOf(target.item().id) + (before ? 0 : 1);
        ids.add(at, moved.item().id);
        marks.reorder(survey.id, shownReport, ids);
        load();
    }

    private void confirmRemoveService(Report r) {
        // UC-048 A3: the built-in row goes like any other, but the author is told what that means.
        String text = ReportService.isBuiltIn(r)
                ? getTranslation("reportsView.removeService.builtInText", r.name)
                : getTranslation("reportsView.removeService.text", r.name);
        new ConfirmDialog(getTranslation("reportsView.removeService.header"), text, getTranslation("common.remove"), e -> {
            try {
                reports.remove(survey.id, r.id);
                Notification.show(getTranslation("reportsView.removedService", r.name), 3000, Notification.Position.BOTTOM_START)
                        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                load();
            } catch (AuthoringException ae) {
                Notification.show(ae.getMessage(), 5000, Notification.Position.MIDDLE).addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        }, getTranslation("common.cancel"), e -> { }).open();
    }

    private void confirmUnmark(Mark m) {
        String element = m.elementLabel().isEmpty() ? "#" + m.item().elementId() : m.elementLabel();
        new ConfirmDialog(getTranslation("reportsView.unmark.header"),
                getTranslation("reportsView.unmark.text", element, ReportMarkService.reportLabel(m.item().reportType)),
                getTranslation("reportsView.unmark"), e -> {
            try {
                marks.unmark(survey.id, m.item().id);
                Notification.show(getTranslation("reportsView.unmarked", element), 3000, Notification.Position.BOTTOM_START)
                        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                load();
            } catch (AuthoringException ae) {
                Notification.show(ae.getMessage(), 5000, Notification.Position.MIDDLE).addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        }, getTranslation("common.cancel"), e -> { }).open();
    }
}
