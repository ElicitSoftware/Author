package com.elicitsoftware.author.flow.reporting;

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
import com.elicitsoftware.author.flow.designer.FieldHelp;
import com.elicitsoftware.author.survey.ReportingNames;
import com.elicitsoftware.author.survey.ReportingService;
import com.elicitsoftware.author.survey.ReportingService.Assignment;
import com.elicitsoftware.author.survey.StructureNode;
import com.elicitsoftware.model.Metadata;
import com.elicitsoftware.model.Metadata.ElementRef;
import com.elicitsoftware.model.Ontology;
import com.elicitsoftware.model.Survey;
import com.vaadin.flow.component.HasValidation;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.radiobutton.RadioGroupVariant;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.theme.lumo.LumoUtility;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * UC-021: assign a tag to an element, choosing its scope (the question everywhere, the question in
 * this section only, or the section in this step) and whether the respondent's answer or a fixed
 * value is reported. UC-022: edit an existing assignment's tag or value behind a retroactivity
 * acknowledgement; the scope of an existing assignment is fixed (remove and add to change it).
 */
public class TagAssignmentDialog extends Dialog {

    static final String USE_ANSWER = "Use the respondent's answer";
    static final String FIXED_VALUE = "Fixed value";

    final ComboBox<StructureNode> element = new ComboBox<>("Element");
    final Span elementLabel = new Span();
    final RadioButtonGroup<ElementRef> scope = new RadioButtonGroup<>("Applies to");
    final ComboBox<Ontology> tag = new ComboBox<>("Tag");
    final Button newTag = new Button("New tag…");
    final RadioButtonGroup<String> valueMode = new RadioButtonGroup<>("Reported value");
    final TextField value = new TextField("Fixed value");
    final Span impact = new Span();
    final Div warning = new Div();
    final Checkbox acknowledge = new Checkbox("I understand that answers already recorded at deployed sites are reclassified");
    final Button save = new Button("Save");
    final Button cancel = new Button("Cancel");

    private final Map<String, HasValidation> fieldsByProperty = Map.of("scope", scope, "tag", tag, "value", value);
    private final ReportingService service;
    private final Survey survey;
    private final StructureNode context;
    private final Assignment existing;

    /**
     * @param nodes    the sections and questions an assignment may target (ignored when {@code context} or {@code existing} is given)
     * @param context  the board node the dialog was opened from, or {@code null} to pick one
     * @param existing the assignment to edit, or {@code null} to create one
     */
    public TagAssignmentDialog(ReportingService service, Survey survey, List<StructureNode> nodes, StructureNode context,
                               Assignment existing, Consumer<Metadata> onSaved) {
        this.service = service;
        this.survey = survey;
        this.context = context;
        this.existing = existing;
        boolean editing = existing != null;
        setHeaderTitle(editing ? "Edit reporting tag" : "Assign reporting tag");
        setModal(true);
        setWidth("40rem");

        List<StructureNode> targets = nodes == null ? List.of() : nodes.stream()
                .filter(n -> n.kind() != StructureNode.Kind.STEP && !n.retired()).toList();
        element.setItems(targets);
        element.setItemLabelGenerator(n -> (n.kind() == StructureNode.Kind.SECTION ? "Section " : "Question ") + n.label()
                + " — " + ReportingService.path(n.parent()));
        element.setRequiredIndicatorVisible(true);
        element.setWidthFull();
        element.addValueChangeListener(e -> scopeFor(e.getValue()));
        elementLabel.addClassName(LumoUtility.FontWeight.SEMIBOLD);

        scope.addThemeVariants(RadioGroupVariant.LUMO_VERTICAL);
        scope.setRequiredIndicatorVisible(true);
        scope.addValueChangeListener(e -> refresh());

        tag.setItems(service.tags(survey.id));
        tag.setItemLabelGenerator(o -> o.tag + " → " + ReportingNames.dimTable(o));
        tag.setRequiredIndicatorVisible(true);
        tag.setWidthFull();
        tag.addValueChangeListener(e -> refresh());
        newTag.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
        newTag.addClickListener(e -> new TagDialog(service, survey, null, created -> {
            tag.setItems(service.tags(survey.id));
            tag.setValue(created);
        }).open());

        valueMode.setItems(USE_ANSWER, FIXED_VALUE);
        valueMode.addThemeVariants(RadioGroupVariant.LUMO_VERTICAL);
        valueMode.addValueChangeListener(e -> refresh());
        value.setMaxLength(255);
        value.setHelperText("Stored lowercased and trimmed whenever the element is answered");
        value.addValueChangeListener(e -> refresh());

        FieldHelp.help(tag, "The reporting tag. Its column in the report is filled from this element.");
        FieldHelp.help(value, "A constant reported instead of the answer, e.g. 'Female' on the section about the respondent's mother.");
        impact.addClassName("reporting-preview");
        warning.setText("This assignment is exported already. Changing its tag or value is retroactive: answers already recorded "
                + "at deployed sites are reclassified, not only answers recorded from now on.");
        warning.addClassName("rename-warning");
        warning.getElement().setAttribute("role", "alert");
        warning.setVisible(false);
        acknowledge.setVisible(false);
        acknowledge.addValueChangeListener(e -> refresh());

        Div elementRow = new Div();
        if (editing) {
            elementLabel.setText(existing.scopeLabel() + ": " + existing.elementLabel()
                    + (existing.where().isBlank() ? "" : " (" + existing.where() + ")"));
            elementRow.add(elementLabel);
            scope.setVisible(false);
            tag.setValue(existing.metadata().ontology);
            valueMode.setValue(existing.metadata().value == null ? USE_ANSWER : FIXED_VALUE);
            value.setValue(existing.metadata().value == null ? "" : existing.metadata().value);
        } else if (context != null) {
            elementLabel.setText((context.kind() == StructureNode.Kind.SECTION ? "Section " : "Question ") + context.label()
                    + " — " + ReportingService.path(context.parent()));
            elementRow.add(elementLabel);
            scopeFor(context);
        } else {
            elementRow.add(element);
            scopeFor(null);
        }
        HorizontalLayout tagRow = new HorizontalLayout(tag, newTag);
        tagRow.setAlignItems(HorizontalLayout.Alignment.END);
        tagRow.setWidthFull();
        tagRow.expand(tag);
        FormLayout form = new FormLayout(elementRow, scope, tagRow, valueMode, value);
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));
        add(form, impact, warning, acknowledge);
        refresh();

        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        save.addClickListener(e -> saveClicked(onSaved));
        cancel.addClickListener(e -> close());
        getFooter().add(cancel, save);
    }

    /** The scopes a node offers: two for a question (UC-021 BR-002), one for a section. */
    private void scopeFor(StructureNode node) {
        if (node == null) {
            scope.setItems(List.of());
            scope.setVisible(false);
            return;
        }
        scope.setVisible(true);
        if (node.kind() == StructureNode.Kind.SECTION) {
            scope.setItems(ElementRef.STEPS_SECTIONS);
            scope.setItemLabelGenerator(r -> "This section as it appears in step '" + labelOf(node.parent()) + "'");
            scope.setValue(ElementRef.STEPS_SECTIONS);
            if (valueMode.getValue() == null) {
                valueMode.setValue(FIXED_VALUE);
            }
        } else {
            scope.setItems(ElementRef.QUESTION, ElementRef.SECTIONS_QUESTION);
            scope.setItemLabelGenerator(r -> r == ElementRef.QUESTION
                    ? "This question wherever it appears"
                    : "This question only in section '" + labelOf(node.parent()) + "'");
            scope.setValue(ElementRef.QUESTION);
            if (valueMode.getValue() == null) {
                valueMode.setValue(USE_ANSWER);
            }
        }
    }

    private static String labelOf(StructureNode n) {
        return n == null ? "" : n.label();
    }

    void refresh() {
        boolean fixed = FIXED_VALUE.equals(valueMode.getValue());
        value.setVisible(fixed);
        value.setRequiredIndicatorVisible(fixed);
        Ontology o = tag.getValue();
        if (o == null) {
            impact.setText("Choose a tag to see the table and column it fills.");
        } else {
            impact.setText("Values go to surveyreport." + ReportingNames.dimTable(o) + " · report column "
                    + ReportingNames.factColumn(o.tag) + (fixed ? " · constant" : " · from the answer"));
        }
        boolean changed = existing != null && (
                !Objects.equals(existing.metadata().ontology == null ? null : existing.metadata().ontology.id, o == null ? null : o.id)
                        || !Objects.equals(existing.metadata().value, currentValue()));
        warning.setVisible(changed);
        acknowledge.setVisible(changed);
        if (!changed) {
            acknowledge.setValue(false);
        }
        save.setEnabled(!changed || Boolean.TRUE.equals(acknowledge.getValue()));
    }

    boolean warningShown() {
        return warning.isVisible();
    }

    private String currentValue() {
        if (!FIXED_VALUE.equals(valueMode.getValue()) || value.getValue() == null || value.getValue().isBlank()) {
            return null;
        }
        return value.getValue().trim();
    }

    private void saveClicked(Consumer<Metadata> onSaved) {
        Ontology o = tag.getValue();
        if (o == null) {
            tag.setInvalid(true);
            tag.setErrorMessage("Choose a tag");
            return;
        }
        if (FIXED_VALUE.equals(valueMode.getValue()) && currentValue() == null) {
            value.setInvalid(true);
            value.setErrorMessage("Enter the fixed value or report the answer instead");
            return;
        }
        try {
            Metadata saved;
            if (existing != null) {
                saved = service.reassign(survey.id, existing.metadata().id, o.id, currentValue());
            } else {
                StructureNode node = context != null ? context : element.getValue();
                if (node == null) {
                    element.setInvalid(true);
                    element.setErrorMessage("Choose an element");
                    return;
                }
                ElementRef ref = scope.getValue();
                if (ref == null) {
                    scope.setInvalid(true);
                    scope.setErrorMessage("Choose what the tag applies to");
                    return;
                }
                int durableId = ref == ElementRef.QUESTION ? node.durableId() : node.assignmentId();
                saved = service.assign(survey.id, ref, durableId, o.id, currentValue());
            }
            close();
            Notification.show((existing != null ? "Updated tag '" : "Assigned tag '") + o.tag + "'", 3000, Notification.Position.BOTTOM_START)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            onSaved.accept(saved);
        } catch (AuthoringException ae) {
            HasValidation field = ae.field() == null ? null : fieldsByProperty.get(ae.field());
            if (field != null) {
                field.setInvalid(true);
                field.setErrorMessage(ae.getMessage());
            } else {
                Notification.show(ae.getMessage(), 5000, Notification.Position.MIDDLE).addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        }
    }

    /** Every section and question node of a tree, for the element picker. */
    public static List<StructureNode> targets(List<StructureNode> all) {
        List<StructureNode> out = new ArrayList<>();
        for (StructureNode n : all) {
            if (n.kind() != StructureNode.Kind.STEP) {
                out.add(n);
            }
        }
        return out;
    }
}
