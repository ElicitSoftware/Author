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

import com.elicitsoftware.author.security.AuthorRoles;
import com.elicitsoftware.author.survey.SurveyService;
import com.elicitsoftware.model.Survey;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteParameters;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;

/** Surveys list: create (UC-003), edit metadata (UC-004), open (UC-006), export (UC-008). */
@Route(value = "", layout = MainLayout.class)
@RolesAllowed(AuthorRoles.AUTHOR)
public class SurveysView extends VerticalLayout implements HasDynamicTitle {

    final Grid<Survey> grid = new Grid<>(Survey.class, false);
    final Button newSurvey = new Button(getTranslation("surveysView.newSurvey"), VaadinIcon.PLUS.create());

    @Inject
    SurveyService surveyService;

    public SurveysView() {
        setSizeFull();
        HorizontalLayout toolbar = new HorizontalLayout(new H2(getTranslation("surveysView.title")), newSurvey);
        toolbar.setAlignItems(Alignment.BASELINE);
        toolbar.setWidthFull();
        toolbar.expand(toolbar.getComponentAt(0));
        newSurvey.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        newSurvey.addClickListener(e -> new SurveyMetadataDialog(surveyService, null, saved -> refresh()).open());

        grid.addColumn(s -> s.name).setHeader(getTranslation("surveysView.grid.name")).setAutoWidth(true).setSortable(true);
        grid.addColumn(s -> s.title).setHeader(getTranslation("surveysView.grid.title")).setAutoWidth(true).setFlexGrow(1);
        grid.addColumn(s -> s.displayOrder).setHeader(getTranslation("surveysView.grid.order")).setAutoWidth(true);
        grid.addColumn(s -> s.publishedBy).setHeader(getTranslation("surveysView.grid.lastEditedBy")).setAutoWidth(true);
        grid.addColumn(s -> s.publishedComment).setHeader(getTranslation("surveysView.grid.releaseNote")).setAutoWidth(true);
        grid.addComponentColumn(s -> {
            Button open = new Button(getTranslation("surveysView.open"), VaadinIcon.EDIT.create(), e ->
                    getUI().ifPresent(ui -> ui.navigate(SurveyEditorView.class,
                            new RouteParameters(SurveyEditorView.SURVEY_ID, String.valueOf(s.id)))));
            open.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
            Button edit = new Button(getTranslation("surveysView.editDetails"), VaadinIcon.PENCIL.create(), e ->
                    new SurveyMetadataDialog(surveyService, s, saved -> refresh()).open());
            edit.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
            return new HorizontalLayout(open, edit);
        }).setHeader("").setAutoWidth(true);
        // A double click on a row goes straight to the designer; Open keeps the overview one click away.
        grid.addItemDoubleClickListener(e -> getUI().ifPresent(ui -> ui.navigate(
                com.elicitsoftware.author.flow.designer.SurveyDesignerView.class,
                new RouteParameters(SurveyEditorView.SURVEY_ID, String.valueOf(e.getItem().id)))));
        grid.setSizeFull();
        add(toolbar, grid);
    }

    @Override
    public String getPageTitle() {
        return getTranslation("surveysView.pageTitle");
    }

    @jakarta.annotation.PostConstruct
    void refresh() {
        grid.setItems(surveyService.list());
    }
}
