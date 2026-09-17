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
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;

/** Import a {@code .elicit} definition into Author (UC-005). Upload handling follows in the import step. */
@Route(value = "import", layout = MainLayout.class)
@PageTitle("Import definition")
@RolesAllowed(AuthorRoles.AUTHOR)
public class ImportDefinitionView extends VerticalLayout {

    public ImportDefinitionView() {
        add(new H2("Import definition"));
        add(new Paragraph("Upload an ELICIT_SURVEY_EXPORT_V1 (.elicit) file to open it as a working copy."));
    }
}
