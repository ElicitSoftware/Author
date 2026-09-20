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
import com.elicitsoftware.author.definition.DefinitionException;
import com.elicitsoftware.author.definition.ExportResult;
import com.elicitsoftware.author.definition.SurveyDefinitionExporter;
import com.elicitsoftware.author.definition.ValidationIssue;
import com.elicitsoftware.author.survey.SurveyService;
import com.elicitsoftware.model.Survey;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.ListItem;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.UnorderedList;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.server.streams.DownloadHandler;
import com.vaadin.flow.server.streams.DownloadResponse;
import com.vaadin.flow.theme.lumo.LumoUtility;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.function.Consumer;

/** Export a survey as a {@code .elicit} file (UC-008), after validation (UC-007). */
public class ExportDialog extends Dialog {

    final TextArea releaseNote = new TextArea();
    final Button export = new Button();
    final Button cancel = new Button();
    final Div findings = new Div();
    final Div outcome = new Div();

    public ExportDialog(SurveyService surveyService, SurveyDefinitionExporter exporter, Survey survey,
                        List<ValidationIssue> issues, Consumer<Survey> onExported) {
        releaseNote.setLabel(getTranslation("exportDialog.releaseNote"));
        export.setText(getTranslation("exportDialog.export"));
        cancel.setText(getTranslation("common.close"));
        setHeaderTitle(getTranslation("exportDialog.title", survey.title));
        setModal(true);
        setWidth("40em");

        long errors = issues.stream().filter(ValidationIssue::isError).count();
        if (!issues.isEmpty()) {
            UnorderedList list = new UnorderedList();
            issues.forEach(i -> {
                ListItem li = new ListItem(getTranslation(i.isError() ? "exportDialog.issue.error" : "exportDialog.issue.warning", i.message()));
                li.addClassName(i.isError() ? LumoUtility.TextColor.ERROR : LumoUtility.TextColor.WARNING);
                list.add(li);
            });
            findings.add(new Paragraph(errors > 0
                    ? getTranslation("exportDialog.errorsBlock", String.valueOf(errors))
                    : getTranslation("exportDialog.warningsDoNotBlock")), list);
        }
        Paragraph retro = new Paragraph(getTranslation("exportDialog.retroactiveReminder"));
        retro.addClassName(LumoUtility.TextColor.SECONDARY);

        releaseNote.setWidthFull();
        releaseNote.setMaxLength(4000);
        releaseNote.setPlaceholder(getTranslation("exportDialog.releaseNote.placeholder"));
        releaseNote.setRequiredIndicatorVisible(true);
        releaseNote.setErrorMessage(getTranslation("exportDialog.releaseNote.required"));

        export.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        export.setEnabled(errors == 0);
        export.addClickListener(e -> {
            outcome.removeAll();
            // UC-008 step 3: the note is the export attribution; a stored note is also what marks a
            // survey as exported (surveys list, purge guard), so it cannot be blank.
            if (releaseNote.getValue() == null || releaseNote.getValue().isBlank()) {
                releaseNote.setInvalid(true);
                return;
            }
            releaseNote.setInvalid(false);
            try {
                Survey stamped = surveyService.stampExport(survey.id, releaseNote.getValue());
                ExportResult result = exporter.export(stamped);
                byte[] bytes = result.bytes();
                Anchor download = new Anchor(DownloadHandler.fromInputStream(ev ->
                        new DownloadResponse(new ByteArrayInputStream(bytes), result.fileName(), "text/plain", bytes.length)),
                        getTranslation("exportDialog.download", result.fileName()));
                download.getElement().setAttribute("download", true);
                StringBuilder counts = new StringBuilder();
                result.counts().forEach((t, n) -> {
                    if (n > 0) {
                        counts.append(t).append(' ').append(n).append(", ");
                    }
                });
                String countsText = counts.length() >= 2 ? counts.substring(0, counts.length() - 2) : counts.toString();
                outcome.add(new Paragraph(getTranslation("exportDialog.revisionSummary", String.valueOf(result.revision()), countsText)), download);
                export.setEnabled(false);
                onExported.accept(stamped);
            } catch (DefinitionException de) {
                Paragraph p = new Paragraph(de.getMessage());
                p.addClassName(LumoUtility.TextColor.ERROR);
                outcome.add(p);
                UnorderedList list = new UnorderedList();
                de.details().forEach(d -> list.add(new ListItem(d)));
                outcome.add(list);
            } catch (AuthoringException ae) {
                Paragraph p = new Paragraph(ae.getMessage());
                p.addClassName(LumoUtility.TextColor.ERROR);
                outcome.add(p);
            }
        });
        cancel.addClickListener(e -> close());
        add(findings, retro, releaseNote, outcome);
        getFooter().add(cancel, export);
    }
}
