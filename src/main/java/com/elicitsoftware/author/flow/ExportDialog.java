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

    final TextArea releaseNote = new TextArea("Release note");
    final Button export = new Button("Export");
    final Button cancel = new Button("Close");
    final Div findings = new Div();
    final Div outcome = new Div();

    public ExportDialog(SurveyService surveyService, SurveyDefinitionExporter exporter, Survey survey,
                        List<ValidationIssue> issues, Consumer<Survey> onExported) {
        setHeaderTitle("Export " + survey.title);
        setModal(true);
        setWidth("40em");

        long errors = issues.stream().filter(ValidationIssue::isError).count();
        if (!issues.isEmpty()) {
            UnorderedList list = new UnorderedList();
            issues.forEach(i -> {
                ListItem li = new ListItem((i.isError() ? "Error: " : "Warning: ") + i.message());
                li.addClassName(i.isError() ? LumoUtility.TextColor.ERROR : LumoUtility.TextColor.WARNING);
                list.add(li);
            });
            findings.add(new Paragraph(errors > 0
                    ? errors + " error(s) block the export; fix them in the editor first."
                    : "Warnings do not block the export."), list);
        }
        Paragraph retro = new Paragraph("Reminder: renaming a step or section and changing a question's ontology tag apply "
                + "retroactively at deployed sites (historical reports and answers are relabelled).");
        retro.addClassName(LumoUtility.TextColor.SECONDARY);

        releaseNote.setWidthFull();
        releaseNote.setMaxLength(4000);
        releaseNote.setPlaceholder("What changed in this revision (recorded on the survey and in the file)");

        export.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        export.setEnabled(errors == 0);
        export.addClickListener(e -> {
            outcome.removeAll();
            try {
                Survey stamped = surveyService.stampExport(survey.id, releaseNote.getValue());
                ExportResult result = exporter.export(stamped);
                byte[] bytes = result.bytes();
                Anchor download = new Anchor(DownloadHandler.fromInputStream(ev ->
                        new DownloadResponse(new ByteArrayInputStream(bytes), result.fileName(), "text/plain", bytes.length)),
                        "Download " + result.fileName());
                download.getElement().setAttribute("download", true);
                StringBuilder summary = new StringBuilder("Revision " + result.revision() + ": ");
                result.counts().forEach((t, n) -> {
                    if (n > 0) {
                        summary.append(t).append(' ').append(n).append(", ");
                    }
                });
                outcome.add(new Paragraph(summary.substring(0, summary.length() - 2)), download);
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
