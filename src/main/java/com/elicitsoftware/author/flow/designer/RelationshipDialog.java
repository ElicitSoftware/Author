package com.elicitsoftware.author.flow.designer;

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
import com.elicitsoftware.author.survey.RuleService;
import com.elicitsoftware.author.survey.StructureNode;
import com.elicitsoftware.author.survey.SurveyStructureQuery;
import com.elicitsoftware.model.ActionType;
import com.elicitsoftware.model.OperatorType;
import com.elicitsoftware.model.Relationship;
import com.vaadin.flow.component.HasValidation;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.textfield.TextField;
import io.quarkus.panache.common.Sort;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

/**
 * UC-019 steps 4-5: the rule as a sentence — upstream question, operator, value, action, target
 * (BR-003). Opened pre-filled from a drawn arrow, or empty from a row's menu (A1). Editing an
 * existing rule offers Remove.
 */
public class RelationshipDialog extends Dialog {

    final ComboBox<StructureNode> upstream = new ComboBox<>("Reads the answer to");
    final ComboBox<OperatorType> operator = new ComboBox<>("Operator");
    final TextField referenceValue = new TextField("Value");
    final ComboBox<ActionType> action = new ComboBox<>("Action");
    final RadioButtonGroup<String> targetKind = new RadioButtonGroup<>("Target");
    final ComboBox<StructureNode> target = new ComboBox<>("Target element");
    final TextField token = new TextField("Token");
    final TextField sample = new TextField("Sample value");
    final TextField description = new TextField("Description");
    final TextField defaultUpstreamValue = new TextField("Default upstream value");
    final TextField overrideUpstreamValue = new TextField("Override upstream value");
    final Div sentence = new Div();
    final Button save = new Button("Save");
    final Button remove = new Button("Remove rule");
    final Button cancel = new Button("Cancel");

    private final Map<String, HasValidation> fieldsByProperty;
    private final List<StructureNode> questionNodes = new ArrayList<>();
    private final List<StructureNode> sectionNodes = new ArrayList<>();
    private final List<StructureNode> stepNodes = new ArrayList<>();

    /**
     * @param existing the rule to edit, or null to create
     * @param preset   values to start from when creating (upstream/downstream/action), may be null
     */
    public RelationshipDialog(int surveyId, SurveyStructureQuery.Tree tree, RuleService rules, Relationship existing, Relationship preset,
                              Consumer<Relationship> onSaved, Runnable onRemoved) {
        boolean creating = existing == null;
        setHeaderTitle(creating ? "New rule" : "Edit rule");
        setModal(true);
        setWidth("44rem");

        Set<Integer> seenQuestions = new HashSet<>();
        Set<Integer> seenSections = new HashSet<>();
        for (StructureNode n : tree.all()) {
            if (n.retired()) {
                continue;
            }
            switch (n.kind()) {
                case STEP -> stepNodes.add(n);
                case SECTION -> {
                    if (seenSections.add(n.assignmentId())) {
                        sectionNodes.add(n);
                    }
                }
                case QUESTION -> {
                    if (seenQuestions.add(n.assignmentId())) {
                        questionNodes.add(n);
                    }
                }
            }
        }
        upstream.setItems(questionNodes);
        upstream.setItemLabelGenerator(RelationshipDialog::path);
        upstream.setRequiredIndicatorVisible(true);
        upstream.setWidthFull();

        operator.setItems(OperatorType.listAll(Sort.by("id")));
        operator.setItemLabelGenerator(o -> o.name);
        operator.setRequiredIndicatorVisible(true);
        action.setItems(ActionType.listAll(Sort.by("id")));
        action.setItemLabelGenerator(a -> a.name);
        action.setRequiredIndicatorVisible(true);
        referenceValue.setMaxLength(255);
        token.setMaxLength(10);
        token.setHelperText("Replaced in the target's text at {TOKEN|default}");
        sample.setMaxLength(255);
        sample.setHelperText("Example answer for the preview");
        description.setMaxLength(255);
        defaultUpstreamValue.setMaxLength(255);
        overrideUpstreamValue.setMaxLength(255);

        targetKind.setItems("Step", "Section", "Question");
        targetKind.setValue("Question");
        targetKind.addValueChangeListener(e -> refreshTargets());
        target.setItemLabelGenerator(RelationshipDialog::path);
        target.setRequiredIndicatorVisible(true);
        target.setWidthFull();
        refreshTargets();

        FieldHelp.help(upstream, "The question whose answer this rule checks.");
        FieldHelp.help(operator, "How the answer is compared to the value — equals, greater than, contains, or just whether an answer exists.");
        FieldHelp.help(referenceValue, "The value the answer is compared against. Leave empty for operators that only test whether an answer exists.");
        FieldHelp.help(action, "What the rule does when it matches: SHOW reveals the target, REPEAT instantiates it once per the answer, TEXT substitutes the answer into the target's text.");
        FieldHelp.help(targetKind, "Whether the rule acts on a whole step, a section, or a single question.");
        FieldHelp.help(target, "The element the action applies to.");
        FieldHelp.help(token, "For a TEXT rule: the placeholder written as {TOKEN} in the target's text that the answer replaces.");
        FieldHelp.help(sample, "An example answer for the token, shown in the preview: sample \"Bob\" previews \"Hello {PName}\" as \"Hello Bob\".");
        FieldHelp.help(description, "An optional note for authors. It is not shown to respondents.");
        FieldHelp.help(defaultUpstreamValue, "The value to assume for the upstream answer when the respondent has not answered it yet.");
        FieldHelp.help(overrideUpstreamValue, "A fixed value to use for the upstream answer instead of the respondent's actual answer.");

        fieldsByProperty = Map.of("upstreamSqId", upstream, "operatorType", operator, "actionType", action,
                "downstream", target, "token", token, "upstreamStepId", upstream);

        Relationship seed = existing != null ? existing : preset;
        if (seed != null) {
            upstream.setValue(byAssignment(questionNodes, seed.upstreamSqId));
            operator.setValue(seed.operatorType);
            action.setValue(seed.actionType);
            referenceValue.setValue(seed.referenceValue == null ? "" : seed.referenceValue);
            token.setValue(seed.token == null ? "" : seed.token);
            sample.setValue(seed.sample == null ? "" : seed.sample);
            description.setValue(seed.description == null ? "" : seed.description);
            defaultUpstreamValue.setValue(seed.defaultUpstreamValue == null ? "" : seed.defaultUpstreamValue);
            overrideUpstreamValue.setValue(seed.overrideUpstreamValue == null ? "" : seed.overrideUpstreamValue);
            if (seed.downstreamStepId != null) {
                targetKind.setValue("Step");
                target.setValue(stepNodes.stream().filter(n -> n.durableId().equals(seed.downstreamStepId)).findFirst().orElse(null));
            } else if (seed.downstreamSsId != null) {
                targetKind.setValue("Section");
                target.setValue(byAssignment(sectionNodes, seed.downstreamSsId));
            } else if (seed.downstreamSqId != null) {
                targetKind.setValue("Question");
                target.setValue(byAssignment(questionNodes, seed.downstreamSqId));
            }
        }
        action.addValueChangeListener(e -> updateActionFields());
        updateActionFields();
        for (var f : List.of(upstream, operator, action, target)) {
            f.addValueChangeListener(e -> updateSentence());
        }
        referenceValue.addValueChangeListener(e -> updateSentence());
        token.addValueChangeListener(e -> updateSentence());
        updateSentence();

        sentence.addClassName("rule-sentence");
        sentence.getElement().setAttribute("aria-live", "polite");
        FormLayout form = new FormLayout(upstream, operator, referenceValue, action, token, sample, targetKind, target, description);
        form.setColspan(upstream, 2);
        form.setColspan(target, 2);
        form.setColspan(description, 2);
        Details advanced = new Details("Advanced", new FormLayout(defaultUpstreamValue, overrideUpstreamValue));
        add(sentence, form, advanced);

        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        save.addClickListener(e -> {
            Relationship bean = new Relationship();
            if (!creating) {
                bean.relationshipId = existing.relationshipId;
            }
            StructureNode up = upstream.getValue();
            bean.upstreamSqId = up == null ? null : up.assignmentId();
            bean.upstreamStepId = up == null ? null : stepOf(up);
            bean.operatorType = operator.getValue();
            bean.actionType = action.getValue();
            bean.referenceValue = blankToNull(referenceValue.getValue());
            bean.token = blankToNull(token.getValue());
            bean.sample = blankToNull(sample.getValue());
            bean.description = blankToNull(description.getValue());
            bean.defaultUpstreamValue = blankToNull(defaultUpstreamValue.getValue());
            bean.overrideUpstreamValue = blankToNull(overrideUpstreamValue.getValue());
            StructureNode t = target.getValue();
            if (t != null) {
                switch (t.kind()) {
                    case STEP -> bean.downstreamStepId = t.durableId();
                    case SECTION -> bean.downstreamSsId = t.assignmentId();
                    case QUESTION -> bean.downstreamSqId = t.assignmentId();
                }
            }
            try {
                Relationship saved = creating ? rules.create(surveyId, bean) : rules.update(surveyId, bean);
                close();
                Notification.show(creating ? "Rule created" : "Rule updated", 3000, Notification.Position.BOTTOM_START)
                        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                onSaved.accept(saved);
            } catch (AuthoringException ae) {
                showError(ae);
            }
        });
        remove.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_TERTIARY);
        remove.setVisible(!creating);
        remove.addClickListener(e -> {
            try {
                rules.retire(surveyId, existing.relationshipId);
                close();
                Notification.show("Rule removed", 3000, Notification.Position.BOTTOM_START).addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                onRemoved.run();
            } catch (AuthoringException ae) {
                showError(ae);
            }
        });
        cancel.addClickListener(e -> close());
        getFooter().add(remove, cancel, save);
    }

    /** "Step › Section › Question" so the same question in two sections is telling apart. */
    static String path(StructureNode n) {
        List<String> parts = new ArrayList<>();
        for (StructureNode p = n; p != null; p = p.parent()) {
            parts.add(0, p.label());
        }
        return String.join(" › ", parts);
    }

    private static Integer stepOf(StructureNode n) {
        for (StructureNode p = n; p != null; p = p.parent()) {
            if (p.kind() == StructureNode.Kind.STEP) {
                return p.durableId();
            }
        }
        return null;
    }

    private static StructureNode byAssignment(List<StructureNode> nodes, Integer assignmentId) {
        return assignmentId == null ? null : nodes.stream().filter(n -> Objects.equals(n.assignmentId(), assignmentId)).findFirst().orElse(null);
    }

    private void refreshTargets() {
        StructureNode keep = target.getValue();
        switch (targetKind.getValue() == null ? "Question" : targetKind.getValue()) {
            case "Step" -> target.setItems(stepNodes);
            case "Section" -> target.setItems(sectionNodes);
            default -> target.setItems(questionNodes);
        }
        if (keep != null && ((targetKind.getValue().equals("Step") && keep.kind() == StructureNode.Kind.STEP)
                || (targetKind.getValue().equals("Section") && keep.kind() == StructureNode.Kind.SECTION)
                || (targetKind.getValue().equals("Question") && keep.kind() == StructureNode.Kind.QUESTION))) {
            target.setValue(keep);
        }
    }

    /** UC-019 A2/A3 hints: the token only for Text; Repeat never targets a step. */
    void updateActionFields() {
        ActionType a = action.getValue();
        boolean text = a != null && "TEXT".equals(a.name);
        token.setVisible(text);
        token.setRequiredIndicatorVisible(text);
        sample.setVisible(text);
        boolean repeat = a != null && "REPEAT".equals(a.name);
        targetKind.setItemEnabledProvider(k -> !(repeat && "Step".equals(k)));
    }

    /** UC-029 BR-002 / UC-019 BR-003: the rule read back as one sentence. */
    void updateSentence() {
        StructureNode up = upstream.getValue();
        StructureNode t = target.getValue();
        String op = operator.getValue() == null ? "(operator)" : operator.getValue().name;
        String act = action.getValue() == null ? "(action)" : action.getValue().name;
        String val = referenceValue.getValue() == null || referenceValue.getValue().isBlank() ? "" : " " + referenceValue.getValue();
        String tok = token.isVisible() && token.getValue() != null && !token.getValue().isBlank() ? " {" + token.getValue() + "}" : "";
        sentence.removeAll();
        sentence.add(new Span("Question '" + (up == null ? "…" : up.label()) + "' " + op + val + " → " + act + tok + " '"
                + (t == null ? "…" : t.label()) + "'"));
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s;
    }

    private void showError(AuthoringException ae) {
        HasValidation field = ae.field() == null ? null : fieldsByProperty.get(ae.field());
        if (field != null) {
            field.setInvalid(true);
            field.setErrorMessage(ae.getMessage());
        } else {
            Notification.show(ae.getMessage(), 5000, Notification.Position.MIDDLE).addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }
}
