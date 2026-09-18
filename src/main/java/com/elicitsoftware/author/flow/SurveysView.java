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
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteParameters;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;

/** Surveys list: create (UC-003), edit metadata (UC-004), open (UC-006), export (UC-008). */
@Route(value = "", layout = MainLayout.class)
@PageTitle("Surveys")
@RolesAllowed(AuthorRoles.AUTHOR)
public class SurveysView extends VerticalLayout {

    final Grid<Survey> grid = new Grid<>(Survey.class, false);
    final Button newSurvey = new Button("New survey", VaadinIcon.PLUS.create());

    @Inject
    SurveyService surveyService;

    public SurveysView() {
        setSizeFull();
        HorizontalLayout toolbar = new HorizontalLayout(new H2("Surveys"), newSurvey);
        toolbar.setAlignItems(Alignment.BASELINE);
        toolbar.setWidthFull();
        toolbar.expand(toolbar.getComponentAt(0));
        newSurvey.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        newSurvey.addClickListener(e -> new SurveyMetadataDialog(surveyService, null, saved -> refresh()).open());

        grid.addColumn(s -> s.name).setHeader("Name").setAutoWidth(true).setSortable(true);
        grid.addColumn(s -> s.title).setHeader("Title").setAutoWidth(true).setFlexGrow(1);
        grid.addColumn(s -> s.displayOrder).setHeader("Order").setAutoWidth(true);
        grid.addColumn(s -> s.publishedBy).setHeader("Last edited by").setAutoWidth(true);
        grid.addColumn(s -> s.publishedComment).setHeader("Release note").setAutoWidth(true);
        grid.addComponentColumn(s -> {
            Button open = new Button("Open", VaadinIcon.EDIT.create(), e ->
                    getUI().ifPresent(ui -> ui.navigate(SurveyEditorView.class,
                            new RouteParameters(SurveyEditorView.SURVEY_ID, String.valueOf(s.id)))));
            open.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
            Button edit = new Button("Edit details", VaadinIcon.PENCIL.create(), e ->
                    new SurveyMetadataDialog(surveyService, s, saved -> refresh()).open());
            edit.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
            return new HorizontalLayout(open, edit);
        }).setHeader("").setAutoWidth(true);
        grid.setSizeFull();
        add(toolbar, grid);
    }

    @jakarta.annotation.PostConstruct
    void refresh() {
        grid.setItems(surveyService.list());
    }
}
