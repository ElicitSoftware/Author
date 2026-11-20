package com.elicitsoftware.author.flow;

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
import com.elicitsoftware.author.security.AuthorRoles;
import com.elicitsoftware.author.survey.ElementService;
import com.elicitsoftware.author.survey.ReportingNames;
import com.elicitsoftware.author.survey.ReportingService;
import com.elicitsoftware.author.survey.StructureNode;
import com.elicitsoftware.author.survey.SurveyService;
import com.elicitsoftware.model.Question;
import com.elicitsoftware.model.Section;
import com.elicitsoftware.model.Step;
import com.elicitsoftware.model.StructuralElement;
import com.elicitsoftware.model.Survey;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.html.H2;
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

import java.util.Locale;
import java.util.Map;

/**
 * Detail of one structural element (UC-006 step 5) with Remove (UC-009) and Restore (UC-010).
 * Edit forms follow in the next milestone.
 */
@Route(value = "survey/:" + SurveyEditorView.SURVEY_ID + "(\\d+)/:" + ElementView.KIND + "(step|section|question)/:"
        + ElementView.DURABLE_ID + "(\\d+)", layout = MainLayout.class)
@RolesAllowed(AuthorRoles.AUTHOR)
public class ElementView extends VerticalLayout implements BeforeEnterObserver, HasDynamicTitle {

    public static final String KIND = "kind";
    public static final String DURABLE_ID = "durableId";

    @Inject
    SurveyService surveyService;

    @Inject
    ElementService elementService;

    @Inject
    ReportingService reporting;

    @Inject
    com.elicitsoftware.author.survey.ReportMarkService reportMarks;

    final H2 heading = new H2();
    final Span kindBadge = new Span();
    final UnorderedList facts = new UnorderedList();
    final Button remove = new Button(VaadinIcon.TRASH.create());
    final Button restore = new Button(VaadinIcon.ROTATE_LEFT.create());
    final Button back = new Button(VaadinIcon.ARROW_LEFT.create());
    final Button openInDesigner = new Button(VaadinIcon.SITEMAP.create());
    /** The notice shown after a removal, with its Undo (UC-009 step 6); null until a removal happened. */
    Notification removalNotice;
    Button undo;

    Survey survey;
    StructureNode.Kind kind;
    int durableId;
    StructuralElement element;

    /** Builds the view's heading, facts and buttons; the element itself is loaded on entry. */
    public ElementView() {
        remove.setText(getTranslation("common.remove"));
        restore.setText(getTranslation("common.restore"));
        back.setText(getTranslation("elementView.surveyOverview"));
        openInDesigner.setText(getTranslation("elementView.openInDesigner"));
        remove.addThemeVariants(ButtonVariant.LUMO_ERROR);
        restore.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        back.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        remove.addClickListener(e -> confirmRemoval());
        restore.addClickListener(e -> confirmRestore());
        back.addClickListener(e -> getUI().ifPresent(ui -> ui.navigate(SurveyEditorView.class,
                new RouteParameters(SurveyEditorView.SURVEY_ID, String.valueOf(survey.id)))));
        heading.getElement().setAttribute("data-i18n-content", "");
        HorizontalLayout title = new HorizontalLayout(heading, kindBadge);
        title.setAlignItems(Alignment.BASELINE);
        openInDesigner.addClickListener(e -> getUI().ifPresent(ui -> ui.navigate(
                com.elicitsoftware.author.flow.designer.SurveyDesignerView.class,
                new com.vaadin.flow.router.RouteParameters(SurveyEditorView.SURVEY_ID, String.valueOf(survey.id)),
                com.vaadin.flow.router.QueryParameters.of(com.elicitsoftware.author.flow.designer.SurveyDesignerView.SELECT,
                        kind.name().toLowerCase() + ":" + durableId))));
        add(back, title, facts, new HorizontalLayout(openInDesigner, remove, restore));
    }

    @Override
    public String getPageTitle() {
        return getTranslation("elementView.pageTitle");
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        int surveyId = event.getRouteParameters().getInteger(SurveyEditorView.SURVEY_ID).orElse(-1);
        String kindParam = event.getRouteParameters().get(KIND).orElse("");
        durableId = event.getRouteParameters().getInteger(DURABLE_ID).orElse(-1);
        survey = surveyService.find(surveyId).orElse(null);
        if (survey == null) {
            event.rerouteToError(NotFoundException.class, getTranslation("surveyDesignerView.noSurvey", String.valueOf(surveyId)));
            return;
        }
        kind = StructureNode.Kind.valueOf(kindParam.toUpperCase(Locale.ROOT));
        element = switch (kind) {
            case STEP -> elementService.step(surveyId, durableId).orElse(null);
            case SECTION -> elementService.section(surveyId, durableId).orElse(null);
            case QUESTION -> elementService.question(surveyId, durableId).orElse(null);
        };
        if (element == null) {
            event.rerouteToError(NotFoundException.class,
                    getTranslation("elementView.notFound", kindParam, String.valueOf(durableId), String.valueOf(surveyId)));
            return;
        }
        render();
    }

    void render() {
        facts.removeAll();
        String kindLabel = getTranslation("elementView.kind." + kind.name().toLowerCase(Locale.ROOT));
        kindBadge.setText(element.isRetired() ? getTranslation("elementView.badge.removed", kindLabel) : kindLabel);
        kindBadge.addClassName(LumoUtility.TextColor.SECONDARY);
        switch (kind) {
            case STEP -> {
                Step s = (Step) element;
                heading.setText(s.name);
                fact(getTranslation("elementView.fact.displayOrder"), s.displayOrder);
                fact(getTranslation("elementView.fact.dimensionName"), s.dimensionName);
                fact(getTranslation("elementView.fact.description"), s.description);
            }
            case SECTION -> {
                Section s = (Section) element;
                heading.setText(s.name);
                fact(getTranslation("elementView.fact.displayOrder"), s.displayOrder);
                fact(getTranslation("elementView.fact.dimensionName"), s.dimensionName);
                fact(getTranslation("elementView.fact.description"), s.description);
            }
            case QUESTION -> {
                Question q = (Question) element;
                heading.setText(q.shortText != null ? q.shortText : getTranslation("elementView.question.untitled", q.questionId));
                fact(getTranslation("elementView.fact.type"), q.questionType == null ? null : q.questionType.name);
                fact(getTranslation("elementView.fact.text"), q.text);
                fact(getTranslation("elementView.fact.required"), q.required);
                fact(getTranslation("elementView.fact.tooltip"), q.toolTip);
                fact(getTranslation("elementView.fact.minMax"), q.minValue == null && q.maxValue == null ? null : q.minValue + " / " + q.maxValue);
                fact(getTranslation("elementView.fact.validationText"), q.validationText);
                fact(getTranslation("elementView.fact.list"), q.selectGroupId);
                fact(getTranslation("elementView.fact.mask"), q.mask);
                fact(getTranslation("elementView.fact.placeholder"), q.placeholder);
                fact(getTranslation("elementView.fact.defaultValue"), q.defaultValue);
                fact(getTranslation("elementView.fact.variant"), q.variant);
            }
        }
        if (kind != StructureNode.Kind.STEP) {
            // UC-021: the tags this element carries, with the report column each one fills.
            StringBuilder tags = new StringBuilder();
            for (ReportingService.Assignment a : reporting.assignmentsFor(survey.id, kind, durableId)) {
                if (a.metadata().ontology == null) {
                    continue;
                }
                if (!tags.isEmpty()) {
                    tags.append("; ");
                }
                tags.append(a.metadata().ontology.tag).append(" → ").append(ReportingNames.factColumn(a.metadata().ontology.tag))
                        .append(" (").append(a.scopeLabel().toLowerCase(Locale.ROOT))
                        .append(a.metadata().value == null ? "" : ", " + getTranslation("elementView.fact.tagValue", a.metadata().value))
                        .append(")");
            }
            fact(getTranslation("elementView.fact.reportingTags"), tags.isEmpty() ? null : tags.toString());
            // UC-049: the reports this element is marked for, with the label each shows.
            StringBuilder marks = new StringBuilder();
            for (com.elicitsoftware.author.survey.ReportMarkService.Mark m : reportMarks.marksFor(survey.id, kind, durableId)) {
                if (!marks.isEmpty()) {
                    marks.append("; ");
                }
                marks.append(com.elicitsoftware.author.survey.ReportMarkService.reportLabel(m.item().reportType))
                        .append(" (").append(m.scopeLabel().toLowerCase(Locale.ROOT))
                        .append(m.item().label == null ? "" : ", " + getTranslation("elementView.fact.markLabel", m.item().label))
                        .append(")");
            }
            fact(getTranslation("elementView.fact.reportMarks"), marks.isEmpty() ? null : marks.toString());
        }
        fact(getTranslation("elementView.fact.elementKey"), element.getElementKey());
        fact(getTranslation("elementView.fact.durableId"), element.getDurableId());
        if (element.isRetired()) {
            fact(getTranslation("elementView.fact.removedAt"), element.effectiveTo);
        }
        remove.setVisible(!element.isRetired());
        restore.setVisible(element.isRetired());
    }

    /** Adds one label/value row; the label is already translated, the value is authored content. */
    private void fact(String label, Object value) {
        if (value != null && !String.valueOf(value).isBlank()) {
            ListItem li = new ListItem();
            Span l = new Span(label + ": ");
            l.addClassName(LumoUtility.FontWeight.SEMIBOLD);
            Span v = new Span(String.valueOf(value));
            v.getElement().setAttribute("data-i18n-content", "");
            li.add(l, v);
            facts.add(li);
        }
    }

    private void confirmRemoval() {
        ElementService.RemovalPlan plan;
        try {
            plan = elementService.planRemoval(survey.id, kind, durableId);
        } catch (AuthoringException ae) {
            error(ae.getMessage());
            return;
        }
        ConfirmDialog dialog = new ConfirmDialog();
        dialog.setHeader(getTranslation("elementView.remove.header", plan.label()));
        VerticalLayout body = new VerticalLayout();
        body.setPadding(false);
        body.add(new Paragraph(getTranslation("elementView.remove.explanation")));
        if (!plan.dependents().isEmpty()) {
            body.add(new Paragraph(getTranslation("elementView.remove.dependents")));
            UnorderedList list = new UnorderedList();
            list.getElement().setAttribute("data-i18n-content", "");
            plan.dependents().forEach(d -> list.add(new ListItem(d)));
            body.add(list);
        }
        dialog.add(body);
        dialog.setCancelable(true);
        dialog.setConfirmText(getTranslation("common.remove"));
        dialog.setCancelText(getTranslation("common.cancel"));
        dialog.setConfirmButtonTheme("error primary"); // i18n:ignore
        dialog.addConfirmListener(e -> {
            try {
                elementService.remove(survey.id, kind, durableId);
                showRemovalNotice(plan.label());
                reload();
            } catch (AuthoringException ae) {
                error(ae.getMessage());
            }
        });
        dialog.open();
    }

    private void showRemovalNotice(String label) {
        Notification notice = new Notification();
        notice.setDuration(8000);
        notice.setPosition(Notification.Position.BOTTOM_START);
        notice.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        Button undoButton = new Button(getTranslation("elementView.undo"), ev -> {
            notice.close();
            doRestore();
        });
        undoButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
        HorizontalLayout body = new HorizontalLayout(new Span(getTranslation("elementView.removed", label)), undoButton);
        body.setAlignItems(Alignment.CENTER);
        notice.add(body);
        removalNotice = notice;
        undo = undoButton;
        notice.open();
    }

    private void confirmRestore() {
        ElementService.RestorePlan plan;
        try {
            plan = elementService.planRestore(survey.id, kind, durableId);
        } catch (AuthoringException ae) {
            error(ae.getMessage());
            return;
        }
        if (plan.blocked()) {
            error(getTranslation("elementView.restore.blocked", plan.label(), String.join("; ", plan.blockers())));
            return;
        }
        ConfirmDialog dialog = new ConfirmDialog();
        dialog.setHeader(getTranslation("elementView.restore.header", plan.label()));
        VerticalLayout body = new VerticalLayout();
        body.setPadding(false);
        body.add(new Paragraph(getTranslation("elementView.restore.explanation")));
        if (!plan.dependents().isEmpty()) {
            body.add(new Paragraph(getTranslation("elementView.restore.dependents")));
            UnorderedList list = new UnorderedList();
            list.getElement().setAttribute("data-i18n-content", "");
            plan.dependents().forEach(d -> list.add(new ListItem(d)));
            body.add(list);
        }
        dialog.add(body);
        dialog.setCancelable(true);
        dialog.setConfirmText(getTranslation("common.restore"));
        dialog.setCancelText(getTranslation("common.cancel"));
        dialog.addConfirmListener(e -> doRestore());
        dialog.open();
    }

    private void doRestore() {
        try {
            elementService.restore(survey.id, kind, durableId);
            Notification.show(getTranslation("elementView.restored"), 3000, Notification.Position.BOTTOM_START).addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            reload();
        } catch (AuthoringException ae) {
            error(ae.getMessage());
        }
    }

    private void reload() {
        getUI().ifPresent(ui -> ui.navigate(ElementView.class, new RouteParameters(Map.of(
                SurveyEditorView.SURVEY_ID, String.valueOf(survey.id), KIND, kind.name().toLowerCase(Locale.ROOT),
                DURABLE_ID, String.valueOf(durableId)))));
        MainLayout layout = (MainLayout) getParent().filter(MainLayout.class::isInstance).orElse(null);
        if (layout != null) {
            layout.refreshStructureTree();
        }
    }

    private void error(String message) {
        Notification.show(message, 6000, Notification.Position.MIDDLE).addThemeVariants(NotificationVariant.LUMO_ERROR);
    }
}
