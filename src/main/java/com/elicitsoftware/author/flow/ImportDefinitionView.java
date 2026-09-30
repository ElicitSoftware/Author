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

import com.elicitsoftware.author.ai.AuthoringSpecification;
import com.elicitsoftware.author.definition.DefinitionException;
import com.elicitsoftware.author.definition.ElicitFormat;
import com.elicitsoftware.author.definition.ImportResult;
import com.elicitsoftware.author.definition.SurveyDefinitionImporter;
import com.elicitsoftware.author.security.AuthorRoles;
import com.vaadin.flow.component.AttachEvent;
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
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteParameters;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.server.streams.UploadHandler;
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;

import java.io.ByteArrayInputStream;

/** Import a {@code .elicit} definition into Author as a new working copy (UC-005). */
@Route(value = "import", layout = MainLayout.class)
@RolesAllowed(AuthorRoles.AUTHOR)
public class ImportDefinitionView extends VerticalLayout implements HasDynamicTitle {

    @Inject
    SurveyDefinitionImporter importer;

    /** Offered only by a build that packaged the AI authoring specification (UC-042 A5). */
    @Inject
    AuthoringSpecification authoringSpecification;

    final Upload upload;
    final Div result = new Div();
    /** The other end of the round trip: where the draft being uploaded here comes from (UC-042). */
    final Div draftOrigin = new Div(new RouterLink(getTranslation("importDefinitionView.aiDraftLink"), AiDraftView.class));

    public ImportDefinitionView() {
        add(new H2(getTranslation("importDefinitionView.title")));
        add(new Paragraph(getTranslation("importDefinitionView.intro")));
        add(new Paragraph(getTranslation("importDefinitionView.introDraft")));
        upload = new Upload(UploadHandler.inMemory((metadata, data) ->
                getUI().ifPresent(ui -> ui.access(() -> handleUpload(data, metadata.fileName())))));
        upload.setAcceptedFileTypes(ElicitFormat.FILE_EXTENSION);
        upload.setMaxFileSize(ElicitFormat.MAX_FILE_BYTES);
        upload.setMaxFiles(1);
        draftOrigin.setVisible(false); // until the injected bean says this build carries the specification
        add(draftOrigin, upload, result);
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        draftOrigin.setVisible(authoringSpecification != null && authoringSpecification.isAvailable());
    }

    @Override
    public String getPageTitle() {
        return getTranslation("importDefinitionView.pageTitle");
    }

    /** Package-private so tests can feed a file without the browser upload. */
    void handleUpload(byte[] data, String fileName) {
        result.removeAll();
        try {
            ImportResult imported = importer.importFile(new ByteArrayInputStream(data));
            H3 importedHeading = new H3(getTranslation("importDefinitionView.imported", imported.surveyName()));
            importedHeading.getElement().setAttribute("data-i18n-content", ""); // carries the authored survey name
            result.add(importedHeading);
            if (imported.draftFormat()) {
                result.add(new Paragraph(
                        getTranslation("importDefinitionView.draftNotice", ElicitFormat.DRAFT_FORMAT_VERSION)));
            }
            UnorderedList counts = new UnorderedList();
            imported.imported().forEach((table, n) -> {
                if (n > 0) {
                    counts.add(new ListItem(table + ": " + n));
                }
            });
            String keyLine = getTranslation("importDefinitionView.surveyKey", String.valueOf(imported.surveyKey()))
                    + (imported.fileRevision() != null
                            ? getTranslation("importDefinitionView.fileRevision", String.valueOf(imported.fileRevision())) : "");
            String countLine = getTranslation("importDefinitionView.recordsImported", String.valueOf(imported.totalImported()), fileName)
                    + (imported.totalSkipped() > 0
                            ? getTranslation("importDefinitionView.retiredSkipped", String.valueOf(imported.totalSkipped())) : "");
            result.add(new Paragraph(keyLine + ". " + countLine + "."));
            result.add(counts);
            Button open = new Button(getTranslation("importDefinitionView.openInEditor"), e -> getUI().ifPresent(ui -> ui.navigate(SurveyEditorView.class,
                    new RouteParameters(SurveyEditorView.SURVEY_ID, String.valueOf(imported.surveyId())))));
            open.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
            result.add(open);
        } catch (DefinitionException e) {
            Paragraph p = new Paragraph(getTranslation("importDefinitionView.importFailed", e.getMessage()));
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
