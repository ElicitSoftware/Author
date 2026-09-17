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
import com.elicitsoftware.model.Survey;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import io.quarkus.panache.common.Sort;
import jakarta.annotation.security.RolesAllowed;

/** Surveys list (UC-003 / UC-004 entry point). Create and edit follow in the survey service step. */
@Route(value = "", layout = MainLayout.class)
@PageTitle("Surveys")
@RolesAllowed(AuthorRoles.AUTHOR)
public class SurveysView extends VerticalLayout {

    final Grid<Survey> grid = new Grid<>(Survey.class, false);

    public SurveysView() {
        setSizeFull();
        add(new H2("Surveys"));
        grid.addColumn(s -> s.name).setHeader("Name").setAutoWidth(true);
        grid.addColumn(s -> s.title).setHeader("Title").setAutoWidth(true).setFlexGrow(1);
        grid.addColumn(s -> s.displayOrder).setHeader("Order").setAutoWidth(true);
        grid.addColumn(s -> s.publishedBy).setHeader("Last exported by").setAutoWidth(true);
        grid.setItems(Survey.listAll(Sort.by("displayOrder")));
        grid.setSizeFull();
        add(grid);
    }
}
