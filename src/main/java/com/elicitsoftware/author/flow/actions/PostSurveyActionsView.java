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
package com.elicitsoftware.author.flow.actions;

import com.elicitsoftware.author.AuthoringException;
import com.elicitsoftware.author.flow.MainLayout;
import com.elicitsoftware.author.flow.SurveyEditorView;
import com.elicitsoftware.author.security.AuthorRoles;
import com.elicitsoftware.author.survey.PostSurveyActionService;
import com.elicitsoftware.author.survey.SurveyService;
import com.elicitsoftware.model.PostSurveyAction;
import com.elicitsoftware.model.Survey;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.dnd.GridDropLocation;
import com.vaadin.flow.component.grid.dnd.GridDropMode;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.router.NotFoundException;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteParameters;
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * The survey's post-survey actions (UC-052): what its site calls, in this order, when a respondent
 * finishes. Respondents never see them (BR-001); a row is dragged to reorder (BR-005).
 */
@Route(value = "survey/:" + SurveyEditorView.SURVEY_ID + "(\\d+)/actions", layout = MainLayout.class)
@RolesAllowed(AuthorRoles.AUTHOR)
public class PostSurveyActionsView extends VerticalLayout implements BeforeEnterObserver, HasDynamicTitle {

    @Inject
    SurveyService surveyService;

    @Inject
    PostSurveyActionService actions;

    final H2 heading = new H2();
    final Button overview = new Button(VaadinIcon.ARROW_LEFT.create());
    final Button addAction = new Button(VaadinIcon.PLUS.create());
    final Grid<PostSurveyAction> grid = new Grid<>(PostSurveyAction.class, false);
    final Paragraph empty = new Paragraph();

    public Survey survey;
    private PostSurveyAction dragged;

    /** Builds the view's controls; the survey is loaded on navigation, in {@code beforeEnter}. */
    public PostSurveyActionsView() {
        overview.setText(getTranslation("postSurveyActionsView.surveyOverview"));
        addAction.setText(getTranslation("postSurveyActionsView.addAction"));
        empty.setText(getTranslation("postSurveyActionsView.empty"));
        heading.getElement().setAttribute("data-i18n-content", "");
        overview.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        overview.addClickListener(e -> getUI().ifPresent(ui -> ui.navigate(SurveyEditorView.class,
                new RouteParameters(SurveyEditorView.SURVEY_ID, String.valueOf(survey.id)))));
        addAction.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        addAction.addClickListener(e -> new PostSurveyActionDialog(actions, survey, null, saved -> load()).open());

        Paragraph intro = new Paragraph(getTranslation("postSurveyActionsView.intro"));
        Paragraph notTranslated = new Paragraph(getTranslation("postSurveyActionsView.notTranslated"));
        notTranslated.addClassName(LumoUtility.TextColor.SECONDARY);
        Paragraph dragHint = new Paragraph(getTranslation("postSurveyActionsView.dragHint"));
        dragHint.addClassName(LumoUtility.TextColor.SECONDARY);

        grid.getElement().setAttribute("data-i18n-content", ""); // names, descriptions and addresses are authored
        grid.addColumn(a -> a.executionOrder).setHeader(getTranslation("postSurveyActionsView.grid.order")).setAutoWidth(true).setFlexGrow(0);
        grid.addColumn(a -> a.name).setHeader(getTranslation("postSurveyActionsView.grid.name")).setAutoWidth(true);
        grid.addColumn(a -> a.description == null ? "" : a.description).setHeader(getTranslation("postSurveyActionsView.grid.description")).setAutoWidth(true);
        grid.addColumn(a -> a.method).setHeader(getTranslation("postSurveyActionsView.grid.method")).setAutoWidth(true).setFlexGrow(0);
        grid.addColumn(a -> a.url).setHeader(getTranslation("postSurveyActionsView.grid.url")).setAutoWidth(true);
        grid.addColumn(a -> a.sendsExternalId ? getTranslation("postSurveyActionsView.grid.externalId.yes") : "")
                .setHeader(getTranslation("postSurveyActionsView.grid.externalId")).setAutoWidth(true).setFlexGrow(0);
        grid.addComponentColumn(a -> {
            Button edit = new Button(getTranslation("postSurveyActionsView.edit"), e -> new PostSurveyActionDialog(actions, survey, a, saved -> load()).open());
            edit.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
            Button remove = new Button(getTranslation("common.remove"), e -> confirmRemove(a));
            remove.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ERROR);
            return new HorizontalLayout(edit, remove);
        }).setHeader("").setAutoWidth(true);
        grid.setAllRowsVisible(true);
        grid.setRowsDraggable(true);
        grid.setDropMode(GridDropMode.BETWEEN);
        grid.addDragStartListener(e -> dragged = e.getDraggedItems().get(0));
        grid.addDragEndListener(e -> dragged = null);
        grid.addDropListener(e -> e.getDropTargetItem().ifPresent(target -> {
            if (dragged != null) {
                move(dragged, target, e.getDropLocation() == GridDropLocation.ABOVE);
            }
        }));

        add(heading, intro, notTranslated, dragHint, new HorizontalLayout(overview, addAction), grid, empty);
    }

    @Override
    public String getPageTitle() {
        return getTranslation("postSurveyActionsView.pageTitle");
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
        heading.setText(getTranslation("postSurveyActionsView.heading", survey.title == null || survey.title.isBlank() ? survey.name : survey.title));
        List<PostSurveyAction> rows = actions.actions(survey.id);
        grid.setItems(rows);
        grid.setVisible(!rows.isEmpty());
        empty.setVisible(rows.isEmpty());
    }

    /** UC-052 step 5: the dragged row lands before or after the target; the service renumbers. */
    void move(PostSurveyAction moved, PostSurveyAction target, boolean before) {
        if (Objects.equals(moved.id, target.id)) {
            return;
        }
        List<Integer> ids = new ArrayList<>();
        for (PostSurveyAction a : actions.actions(survey.id)) {
            if (!Objects.equals(a.id, moved.id)) {
                ids.add(a.id);
            }
        }
        int at = ids.indexOf(target.id) + (before ? 0 : 1);
        ids.add(at, moved.id);
        actions.reorder(survey.id, ids);
        load();
    }

    /** UC-052 A3: the author is told what removal does at a site before it happens. */
    private void confirmRemove(PostSurveyAction a) {
        new ConfirmDialog(getTranslation("postSurveyActionsView.remove.header"),
                getTranslation("postSurveyActionsView.remove.text", a.name), getTranslation("common.remove"), e -> {
            try {
                actions.remove(survey.id, a.id);
                Notification.show(getTranslation("postSurveyActionsView.removed", a.name), 3000, Notification.Position.BOTTOM_START)
                        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                load();
            } catch (AuthoringException ae) {
                Notification.show(ae.getMessage(), 5000, Notification.Position.MIDDLE).addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        }, getTranslation("common.cancel"), e -> { }).open();
    }
}
