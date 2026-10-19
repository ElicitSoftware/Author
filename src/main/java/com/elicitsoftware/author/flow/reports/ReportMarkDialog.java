/*-
 * ***LICENSE_START***
 * Elicit Author
 * %%
 * Copyright (C) 2025 - 2026 The Regents of the University of Michigan - Rogel Cancer Center
 * %%
 * PolyForm Noncommercial License 1.0.0
 * <https://polyformproject.org/licenses/noncommercial/1.0.0>
 * ***LICENSE_END***
 */
package com.elicitsoftware.author.flow.reports;

import com.elicitsoftware.author.AuthoringException;
import com.elicitsoftware.author.flow.designer.FieldHelp;
import com.elicitsoftware.author.survey.ReportMarkService;
import com.elicitsoftware.author.survey.ReportMarkService.Mark;
import com.elicitsoftware.author.survey.ReportingService;
import com.elicitsoftware.author.survey.StructureNode;
import com.elicitsoftware.model.Metadata.ElementRef;
import com.elicitsoftware.model.ReportItem;
import com.elicitsoftware.model.ReportItem.ReportType;
import com.elicitsoftware.model.Survey;
import com.vaadin.flow.component.HasValidation;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.radiobutton.RadioGroupVariant;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.theme.lumo.LumoUtility;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * UC-049 steps 2-4: mark an element for a report, choosing its scope (the question everywhere,
 * the question in this section only, or the section in this step), the report, and an optional
 * label; step 6: edit the label of an existing mark, whose element, scope and report are fixed
 * (unmark and mark again to change them, BR-005).
 */
public class ReportMarkDialog extends Dialog {

    final ComboBox<StructureNode> element = new ComboBox<>();
    final Span elementLabel = new Span();
    final RadioButtonGroup<ElementRef> scope = new RadioButtonGroup<>();
    final RadioButtonGroup<ReportType> report = new RadioButtonGroup<>();
    final TextField label = new TextField();
    final Div disclosure = new Div();
    final Button save = new Button();
    final Button cancel = new Button();

    private final Map<String, HasValidation> fieldsByProperty = Map.of("scope", scope, "report", report, "label", label);
    private final ReportMarkService service;
    private final Survey survey;
    private final StructureNode context;
    private final Mark existing;

    /**
     * @param nodes    the sections and questions a mark may target (ignored when {@code context} or {@code existing} is given)
     * @param context  the board node the dialog was opened from, or {@code null} to pick one
     * @param existing the mark whose label is edited, or {@code null} to create one
     */
    public ReportMarkDialog(ReportMarkService service, Survey survey, List<StructureNode> nodes, StructureNode context,
                            Mark existing, Consumer<ReportItem> onSaved) {
        this.service = service;
        this.survey = survey;
        this.context = context;
        this.existing = existing;
        boolean editing = existing != null;
        element.setLabel(getTranslation("reportMarkDialog.element"));
        scope.setLabel(getTranslation("reportMarkDialog.appliesTo"));
        report.setLabel(getTranslation("reportMarkDialog.report"));
        label.setLabel(getTranslation("reportMarkDialog.label"));
        save.setText(getTranslation("common.save"));
        cancel.setText(getTranslation("common.cancel"));
        elementLabel.getElement().setAttribute("data-i18n-content", ""); // authored element name and path
        setHeaderTitle(editing ? getTranslation("reportMarkDialog.title.label") : getTranslation("reportMarkDialog.title.mark"));
        setModal(true);
        setWidth("40rem");

        // UC-049 BR-003: the picker offers only what can be marked.
        List<StructureNode> targets = nodes == null ? List.of() : nodes.stream()
                .filter(n -> !n.retired() && ReportMarkService.markable(n)).toList();
        element.setItems(targets);
        element.setItemLabelGenerator(this::elementLabel);
        element.setRequiredIndicatorVisible(true);
        element.setWidthFull();
        element.addValueChangeListener(e -> scopeFor(e.getValue()));
        elementLabel.addClassName(LumoUtility.FontWeight.SEMIBOLD);

        scope.addThemeVariants(RadioGroupVariant.LUMO_VERTICAL);
        scope.setRequiredIndicatorVisible(true);

        report.setItems(ReportType.RESPONDENT, ReportType.ADMINISTRATOR);
        report.setItemLabelGenerator(ReportMarkService::reportLabel);
        report.addThemeVariants(RadioGroupVariant.LUMO_VERTICAL);
        report.setRequiredIndicatorVisible(true);
        report.setValue(ReportType.RESPONDENT);
        report.addValueChangeListener(e -> refresh());

        label.setMaxLength(255);
        label.setWidthFull();
        label.setHelperText(getTranslation("reportMarkDialog.label.helper"));
        FieldHelp.help(label, getTranslation("reportMarkDialog.label.helper"));

        // UC-049 A4 / BR-006 of the plan's disclosure: the respondent report is as private as the access code.
        disclosure.setText(getTranslation("reportMarkDialog.disclosure"));
        disclosure.addClassName("rename-warning");
        disclosure.getElement().setAttribute("role", "note");

        Div elementRow = new Div();
        if (editing) {
            elementLabel.setText(existing.scopeLabel() + ": " + existing.elementLabel()
                    + (existing.where().isBlank() ? "" : " (" + existing.where() + ")"));
            elementRow.add(elementLabel);
            scope.setVisible(false);
            report.setValue(existing.item().reportType);
            report.setReadOnly(true);
            label.setValue(existing.item().label == null ? "" : existing.item().label);
        } else if (context != null) {
            elementLabel.setText(elementLabel(context));
            elementRow.add(elementLabel);
            scopeFor(context);
        } else {
            elementRow.add(element);
            scopeFor(null);
        }
        FormLayout form = new FormLayout(elementRow, scope, report, label);
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));
        add(form, disclosure);
        refresh();

        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        save.addClickListener(e -> saveClicked(onSaved));
        cancel.addClickListener(e -> close());
        getFooter().add(cancel, save);
    }

    /** "Section &lt;name&gt; — &lt;path&gt;" or "Question …": the element word is translated, the rest is authored. */
    private String elementLabel(StructureNode n) {
        return n.kind() == StructureNode.Kind.SECTION
                ? getTranslation("reportMarkDialog.element.section", n.label(), ReportingService.path(n.parent()))
                : getTranslation("reportMarkDialog.element.question", n.label(), ReportingService.path(n.parent()));
    }

    /** The scopes a node offers: two for a question (UC-049 BR-002), one for a section. */
    private void scopeFor(StructureNode node) {
        if (node == null) {
            scope.setItems(List.of());
            scope.setVisible(false);
            return;
        }
        scope.setVisible(true);
        if (node.kind() == StructureNode.Kind.SECTION) {
            scope.setItems(ElementRef.STEPS_SECTIONS);
            scope.setItemLabelGenerator(r -> getTranslation("reportMarkDialog.scope.section", labelOf(node.parent())));
            scope.setValue(ElementRef.STEPS_SECTIONS);
        } else {
            scope.setItems(ElementRef.QUESTION, ElementRef.SECTIONS_QUESTION);
            scope.setItemLabelGenerator(r -> r == ElementRef.QUESTION
                    ? getTranslation("reportMarkDialog.scope.question")
                    : getTranslation("reportMarkDialog.scope.questionInSection", labelOf(node.parent())));
            scope.setValue(ElementRef.QUESTION);
        }
    }

    private static String labelOf(StructureNode n) {
        return n == null ? "" : n.label();
    }

    void refresh() {
        disclosure.setVisible(report.getValue() == ReportType.RESPONDENT);
    }

    boolean disclosureShown() {
        return disclosure.isVisible();
    }

    private void saveClicked(Consumer<ReportItem> onSaved) {
        try {
            ReportItem saved;
            String element = null;
            if (existing != null) {
                saved = service.relabel(survey.id, existing.item().id, label.getValue());
            } else {
                StructureNode node = context != null ? context : this.element.getValue();
                if (node == null) {
                    this.element.setInvalid(true);
                    this.element.setErrorMessage(getTranslation("reportMarkDialog.error.chooseElement"));
                    return;
                }
                ElementRef ref = scope.getValue();
                if (ref == null) {
                    scope.setInvalid(true);
                    scope.setErrorMessage(getTranslation("reportMarkDialog.error.chooseScope"));
                    return;
                }
                ReportType type = report.getValue();
                if (type == null) {
                    report.setInvalid(true);
                    report.setErrorMessage(getTranslation("reportMarkDialog.error.chooseReport"));
                    return;
                }
                int durableId = ref == ElementRef.QUESTION ? node.durableId() : node.assignmentId();
                saved = service.mark(survey.id, ref, durableId, type, label.getValue());
                element = node.label();
            }
            close();
            Notification.show(existing != null ? getTranslation("reportMarkDialog.relabeled")
                    : getTranslation("reportMarkDialog.marked", element, ReportMarkService.reportLabel(saved.reportType)),
                    3000, Notification.Position.BOTTOM_START).addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            onSaved.accept(saved);
        } catch (AuthoringException ae) {
            HasValidation field = ae.field() == null ? null : fieldsByProperty.get(ae.field());
            if (field != null && ((com.vaadin.flow.component.Component) field).isVisible()) {
                field.setInvalid(true);
                field.setErrorMessage(ae.getMessage());
            } else {
                Notification.show(ae.getMessage(), 5000, Notification.Position.MIDDLE).addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        }
    }
}
