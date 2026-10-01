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
import com.elicitsoftware.author.definition.ExportResult;
import com.elicitsoftware.author.definition.SurveyDefinitionExporter;
import com.elicitsoftware.author.survey.SurveyService;
import com.elicitsoftware.model.Survey;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.server.streams.DownloadHandler;
import com.vaadin.flow.server.streams.DownloadResponse;
import com.vaadin.flow.theme.lumo.LumoUtility;

import java.io.ByteArrayInputStream;

/**
 * UC-047: the guarded delete of a whole survey. Says what goes, urges a backup first (the file is
 * the only way back), and for an exported survey says that the sites keep their copies and can
 * only be changed again from that file. Delete stays disabled until the survey's name is typed.
 */
public class DeleteSurveyDialog extends Dialog {

    final Paragraph permanent = new Paragraph();
    final Paragraph exportState = new Paragraph();
    final Paragraph advice = new Paragraph();
    final Anchor backup;
    final TextField confirmName = new TextField();
    final Paragraph error = new Paragraph();
    final Button delete = new Button();
    final Button cancel = new Button();

    /** @param onDeleted called once the survey is gone */
    public DeleteSurveyDialog(SurveyService surveyService, SurveyDefinitionExporter exporter, Survey survey, Runnable onDeleted) {
        setHeaderTitle(getTranslation("deleteSurveyDialog.title", survey.name));
        setModal(true);
        setWidth("36em");

        permanent.setText(getTranslation("deleteSurveyDialog.permanent"));
        // BR-003: an exported survey is the master of copies Author cannot reach.
        exportState.setText(SurveyService.isExported(survey)
                ? getTranslation("deleteSurveyDialog.exported", survey.publishedBy, survey.publishedComment)
                : getTranslation("deleteSurveyDialog.neverExported"));
        advice.setText(getTranslation("deleteSurveyDialog.advice"));

        // BR-006: built when clicked, so it holds the survey as it stands at that moment, errors and all.
        backup = new Anchor(DownloadHandler.fromInputStream(ev -> {
            ExportResult file = exporter.backup(survey);
            byte[] bytes = file.bytes();
            return new DownloadResponse(new ByteArrayInputStream(bytes), file.fileName(), "text/plain", bytes.length);
        }), "");
        backup.getElement().setAttribute("download", true);
        Button backupButton = new Button(getTranslation("deleteSurveyDialog.backup"), VaadinIcon.DOWNLOAD.create());
        backupButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        backup.add(backupButton);

        // BR-002: typing the name is what turns a stray click into a decision.
        confirmName.setLabel(getTranslation("deleteSurveyDialog.confirmName", survey.name));
        confirmName.setWidthFull();
        confirmName.setValueChangeMode(ValueChangeMode.EAGER);
        confirmName.addValueChangeListener(e -> delete.setEnabled(survey.name.equals(e.getValue().trim())));

        error.addClassName(LumoUtility.TextColor.ERROR);
        error.setVisible(false);

        delete.setText(getTranslation("deleteSurveyDialog.delete"));
        delete.addThemeVariants(ButtonVariant.LUMO_ERROR);
        delete.setEnabled(false);
        delete.addClickListener(e -> {
            try {
                surveyService.delete(survey.id);
                close();
                onDeleted.run();
            } catch (AuthoringException ae) {
                error.setText(ae.getMessage());
                error.setVisible(true);
            }
        });
        cancel.setText(getTranslation("common.cancel"));
        cancel.addClickListener(e -> close());

        add(permanent, exportState, advice, backup, confirmName, error);
        getFooter().add(cancel, delete);
    }
}
