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

import com.elicitsoftware.author.definition.DefinitionException;
import com.elicitsoftware.author.definition.ElicitFormat;
import com.elicitsoftware.author.definition.ImportResult;
import com.elicitsoftware.author.definition.SurveyDefinitionImporter;
import com.elicitsoftware.author.security.AuthorRoles;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.ListItem;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.UnorderedList;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteParameters;
import com.vaadin.flow.server.streams.UploadHandler;
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;

import java.io.ByteArrayInputStream;

/** Import a {@code .elicit} definition into Author as a new working copy (UC-005). */
@Route(value = "import", layout = MainLayout.class)
@PageTitle("Import definition")
@RolesAllowed(AuthorRoles.AUTHOR)
public class ImportDefinitionView extends VerticalLayout {

    @Inject
    SurveyDefinitionImporter importer;

    final Upload upload;
    final Div result = new Div();

    public ImportDefinitionView() {
        add(new H2("Import definition"));
        add(new Paragraph("Upload an ELICIT_SURVEY_EXPORT_V1 (.elicit) file to open it in Author as a working copy. "
                + "Every survey and element key in the file is kept exactly as it is, so a later export updates the "
                + "same rows at every site that already applied it. A survey whose key is already open here is refused."));
        add(new Paragraph("An ELICIT_SURVEY_DRAFT_V1 file is accepted too -- a definition drafted outside Author, "
                + "which leaves its keys blank for Author to mint. Only Author reads that marker, so a draft cannot be "
                + "installed at a site until it passes validation here and is exported."));
        upload = new Upload(UploadHandler.inMemory((metadata, data) ->
                getUI().ifPresent(ui -> ui.access(() -> handleUpload(data, metadata.fileName())))));
        upload.setAcceptedFileTypes(ElicitFormat.FILE_EXTENSION);
        upload.setMaxFileSize(ElicitFormat.MAX_FILE_BYTES);
        upload.setMaxFiles(1);
        add(upload, result);
    }

    /** Package-private so tests can feed a file without the browser upload. */
    void handleUpload(byte[] data, String fileName) {
        result.removeAll();
        try {
            ImportResult imported = importer.importFile(new ByteArrayInputStream(data));
            result.add(new H3("Imported " + imported.surveyName()));
            if (imported.draftFormat()) {
                result.add(new Paragraph("This came from an " + ElicitFormat.DRAFT_FORMAT_VERSION
                        + " draft, so its keys were minted here. Review it, fix anything validation reports, "
                        + "then export it to get a file a site can apply."));
            }
            UnorderedList counts = new UnorderedList();
            imported.imported().forEach((table, n) -> {
                if (n > 0) {
                    counts.add(new ListItem(table + ": " + n));
                }
            });
            result.add(new Paragraph("Survey key " + imported.surveyKey()
                    + (imported.fileRevision() != null ? ", file revision " + imported.fileRevision() : "")
                    + ". " + imported.totalImported() + " records imported from " + fileName
                    + (imported.totalSkipped() > 0 ? ", " + imported.totalSkipped() + " retired record(s) skipped" : "") + "."));
            result.add(counts);
            Button open = new Button("Open in editor", e -> getUI().ifPresent(ui -> ui.navigate(SurveyEditorView.class,
                    new RouteParameters(SurveyEditorView.SURVEY_ID, String.valueOf(imported.surveyId())))));
            open.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
            result.add(open);
        } catch (DefinitionException e) {
            Paragraph p = new Paragraph("Import failed: " + e.getMessage());
            p.addClassName(LumoUtility.TextColor.ERROR);
            result.add(p);
            if (!e.details().isEmpty()) {
                UnorderedList details = new UnorderedList();
                e.details().forEach(d -> details.add(new ListItem(d)));
                result.add(details);
            }
        }
        upload.clearFileList();
    }
}
