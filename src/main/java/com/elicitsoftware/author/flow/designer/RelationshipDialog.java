package com.elicitsoftware.author.flow.designer;

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
import com.elicitsoftware.author.survey.RuleService;
import com.elicitsoftware.author.survey.StructureNode;
import com.elicitsoftware.author.survey.SurveyStructureQuery;
import com.elicitsoftware.author.survey.Tokens;
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
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * UC-019 steps 4-5: the rule as a sentence — upstream question, operator, value, action, target
 * (BR-003). Opened pre-filled from a drawn arrow, or empty from a row's menu (A1). Editing an
 * existing rule offers Remove.
 */
public class RelationshipDialog extends Dialog {

    /** Target-kind values of the radio group (technical identifiers; labels come from translations). */
    static final String KIND_STEP = "step";
    static final String KIND_SECTION = "section";
    static final String KIND_QUESTION = "question";

    final ComboBox<StructureNode> upstream = new ComboBox<>();
    final ComboBox<OperatorType> operator = new ComboBox<>();
    final TextField referenceValue = new TextField();
    final ComboBox<ActionType> action = new ComboBox<>();
    final RadioButtonGroup<String> targetKind = new RadioButtonGroup<>();
    final ComboBox<StructureNode> target = new ComboBox<>();
    final ComboBox<String> token = new ComboBox<>();
    final TextField description = new TextField();
    final TextField defaultUpstreamValue = new TextField();
    final TextField overrideUpstreamValue = new TextField();
    final Div sentence = new Div();
    final Button save = new Button();
    final Button remove = new Button();
    final Button cancel = new Button();

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
        setHeaderTitle(creating ? getTranslation("relationshipDialog.title.new") : getTranslation("relationshipDialog.title.edit"));
        setModal(true);
        setWidth("44rem");
        upstream.setLabel(getTranslation("relationshipDialog.upstream"));
        operator.setLabel(getTranslation("relationshipDialog.operator"));
        referenceValue.setLabel(getTranslation("relationshipDialog.value"));
        action.setLabel(getTranslation("relationshipDialog.action"));
        targetKind.setLabel(getTranslation("relationshipDialog.targetKind"));
        target.setLabel(getTranslation("relationshipDialog.target"));
        token.setLabel(getTranslation("relationshipDialog.token"));
        description.setLabel(getTranslation("relationshipDialog.description"));
        defaultUpstreamValue.setLabel(getTranslation("relationshipDialog.defaultUpstreamValue"));
        overrideUpstreamValue.setLabel(getTranslation("relationshipDialog.overrideUpstreamValue"));
        save.setText(getTranslation("common.save"));
        remove.setText(getTranslation("relationshipDialog.removeRule"));
        cancel.setText(getTranslation("common.cancel"));

        // Every mount is its own choice, labelled by its path (UC-019 BR-001): a rule names the mount
        // it fires from and the mount it targets, so a section shared by several steps lists each.
        for (StructureNode n : tree.all()) {
            if (n.retired()) {
                continue;
            }
            switch (n.kind()) {
                case STEP -> stepNodes.add(n);
                case SECTION -> sectionNodes.add(n);
                case QUESTION -> questionNodes.add(n);
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
        // The survey's tokens, so a name is reused rather than retyped; a new one is typed in (FR-012).
        List<String> tokens = new ArrayList<>(Tokens.known(surveyId));
        token.setItems(tokens);
        token.setAllowCustomValue(true);
        token.addCustomValueSetListener(e -> {
            String typed = e.getDetail() == null ? "" : e.getDetail().trim();
            if (!typed.isEmpty() && !tokens.contains(typed)) {
                tokens.add(typed);
                token.setItems(tokens);
            }
            token.setValue(typed.isEmpty() ? null : typed);
        });
        token.setHelperText(getTranslation("relationshipDialog.token.helper"));
        description.setMaxLength(255);
        defaultUpstreamValue.setMaxLength(255);
        overrideUpstreamValue.setMaxLength(255);

        targetKind.setItems(KIND_STEP, KIND_SECTION, KIND_QUESTION);
        targetKind.setItemLabelGenerator(k -> getTranslation("relationshipDialog.targetKind." + k));
        targetKind.setValue(KIND_QUESTION);
        targetKind.addValueChangeListener(e -> refreshTargets());
        target.setItemLabelGenerator(RelationshipDialog::path);
        target.setRequiredIndicatorVisible(true);
        target.setWidthFull();
        refreshTargets();

        FieldHelp.help(upstream, getTranslation("relationshipDialog.help.upstream"));
        FieldHelp.help(operator, getTranslation("relationshipDialog.help.operator"));
        FieldHelp.help(referenceValue, getTranslation("relationshipDialog.help.value"));
        FieldHelp.help(action, getTranslation("relationshipDialog.help.action"));
        FieldHelp.help(targetKind, getTranslation("relationshipDialog.help.targetKind"));
        FieldHelp.help(target, getTranslation("relationshipDialog.help.target"));
        FieldHelp.help(token, getTranslation("relationshipDialog.help.token"));
        FieldHelp.help(description, getTranslation("relationshipDialog.help.description"));
        FieldHelp.help(defaultUpstreamValue, getTranslation("relationshipDialog.help.defaultUpstreamValue"));
        FieldHelp.help(overrideUpstreamValue, getTranslation("relationshipDialog.help.overrideUpstreamValue"));

        fieldsByProperty = Map.of("upstreamSqId", upstream, "operatorType", operator, "actionType", action,
                "downstream", target, "token", token, "upstreamStepId", upstream);

        Relationship seed = existing != null ? existing : preset;
        if (seed != null) {
            upstream.setValue(byAssignment(questionNodes, seed.upstreamSqId, seed.upstreamStepId, null));
            operator.setValue(seed.operatorType);
            action.setValue(seed.actionType);
            referenceValue.setValue(seed.referenceValue == null ? "" : seed.referenceValue);
            if (seed.token != null && !seed.token.isBlank()) {
                if (!tokens.contains(seed.token.trim())) {
                    tokens.add(seed.token.trim());
                    token.setItems(tokens);
                }
                token.setValue(seed.token.trim());
            }
            description.setValue(seed.description == null ? "" : seed.description);
            defaultUpstreamValue.setValue(seed.defaultUpstreamValue == null ? "" : seed.defaultUpstreamValue);
            overrideUpstreamValue.setValue(seed.overrideUpstreamValue == null ? "" : seed.overrideUpstreamValue);
            if (seed.downstreamSqId != null) {
                targetKind.setValue(KIND_QUESTION);
                target.setValue(byAssignment(questionNodes, seed.downstreamSqId, seed.downstreamStepId, seed.downstreamSsId));
            } else if (seed.downstreamSsId != null) {
                targetKind.setValue(KIND_SECTION);
                target.setValue(byAssignment(sectionNodes, seed.downstreamSsId, seed.downstreamStepId, null));
            } else if (seed.downstreamStepId != null) {
                targetKind.setValue(KIND_STEP);
                target.setValue(stepNodes.stream().filter(n -> n.durableId().equals(seed.downstreamStepId)).findFirst().orElse(null));
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
        FormLayout form = new FormLayout(upstream, operator, referenceValue, action, token, targetKind, target, description);
        form.setColspan(upstream, 2);
        form.setColspan(target, 2);
        form.setColspan(description, 2);
        Details advanced = new Details(getTranslation("relationshipDialog.advanced"), new FormLayout(defaultUpstreamValue, overrideUpstreamValue));
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
            bean.description = blankToNull(description.getValue());
            bean.defaultUpstreamValue = blankToNull(defaultUpstreamValue.getValue());
            bean.overrideUpstreamValue = blankToNull(overrideUpstreamValue.getValue());
            StructureNode t = target.getValue();
            if (t != null) {
                // The chosen node's ancestry is the downstream path: step, the section mounted in it, the question in that.
                switch (t.kind()) {
                    case STEP -> bean.downstreamStepId = t.durableId();
                    case SECTION -> {
                        bean.downstreamSsId = t.assignmentId();
                        bean.downstreamStepId = stepOf(t);
                    }
                    case QUESTION -> {
                        bean.downstreamSqId = t.assignmentId();
                        // UC-019 BR-001: a question in the upstream question's own section is stored question-only —
                        // no section, no step, no upstream step — so it fires from every mount of that section and the
                        // runtime shows it on the page being answered (RulePath). A question elsewhere names its mount and step.
                        if (!sameSection(up, t)) {
                            bean.downstreamSsId = t.parent() == null ? null : t.parent().assignmentId();
                            bean.downstreamStepId = stepOf(t);
                        } else {
                            bean.upstreamStepId = null;
                        }
                    }
                }
            }
            try {
                Relationship saved = creating ? rules.create(surveyId, bean) : rules.update(surveyId, bean);
                close();
                Notification.show(creating ? getTranslation("relationshipDialog.saved.created") : getTranslation("relationshipDialog.saved.updated"), 3000, Notification.Position.BOTTOM_START)
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
                Notification.show(getTranslation("relationshipDialog.removed"), 3000, Notification.Position.BOTTOM_START).addThemeVariants(NotificationVariant.LUMO_SUCCESS);
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

    /** Whether two question nodes sit in the same section (by the section's durable id, whichever mount each is shown in). */
    private static boolean sameSection(StructureNode a, StructureNode b) {
        return a != null && b != null && a.parent() != null && b.parent() != null
                && a.parent().kind() == StructureNode.Kind.SECTION && b.parent().kind() == StructureNode.Kind.SECTION
                && Objects.equals(a.parent().durableId(), b.parent().durableId());
    }

    private static Integer stepOf(StructureNode n) {
        for (StructureNode p = n; p != null; p = p.parent()) {
            if (p.kind() == StructureNode.Kind.STEP) {
                return p.durableId();
            }
        }
        return null;
    }

    /** The node for an assignment, preferring the mount the rule's path names (its section card, else its step). */
    private static StructureNode byAssignment(List<StructureNode> nodes, Integer assignmentId, Integer stepId, Integer mountId) {
        if (assignmentId == null) {
            return null;
        }
        List<StructureNode> matches = nodes.stream().filter(n -> Objects.equals(n.assignmentId(), assignmentId)).toList();
        return matches.stream()
                .filter(n -> mountId == null || (n.parent() != null && Objects.equals(n.parent().assignmentId(), mountId)))
                .filter(n -> stepId == null || Objects.equals(stepOf(n), stepId))
                .findFirst().orElse(matches.isEmpty() ? null : matches.get(0));
    }

    private void refreshTargets() {
        StructureNode keep = target.getValue();
        switch (targetKind.getValue() == null ? KIND_QUESTION : targetKind.getValue()) {
            case KIND_STEP -> target.setItems(stepNodes);
            case KIND_SECTION -> target.setItems(sectionNodes);
            default -> target.setItems(questionNodes);
        }
        if (keep != null && ((targetKind.getValue().equals(KIND_STEP) && keep.kind() == StructureNode.Kind.STEP)
                || (targetKind.getValue().equals(KIND_SECTION) && keep.kind() == StructureNode.Kind.SECTION)
                || (targetKind.getValue().equals(KIND_QUESTION) && keep.kind() == StructureNode.Kind.QUESTION))) {
            target.setValue(keep);
        }
    }

    /**
     * UC-019 A2/A3 hints: any action may carry a token (BR-005) and only Text requires one; Repeat
     * never targets a step.
     */
    void updateActionFields() {
        ActionType a = action.getValue();
        boolean text = a != null && "TEXT".equals(a.name);
        token.setVisible(true);
        token.setRequiredIndicatorVisible(text);
        boolean repeat = a != null && "REPEAT".equals(a.name);
        targetKind.setItemEnabledProvider(k -> !(repeat && KIND_STEP.equals(k)));
    }

    /** UC-029 BR-002 / UC-019 BR-003: the rule read back as one sentence. */
    void updateSentence() {
        StructureNode up = upstream.getValue();
        StructureNode t = target.getValue();
        String op = operator.getValue() == null ? getTranslation("relationshipDialog.sentence.noOperator") : operator.getValue().name;
        String act = action.getValue() == null ? getTranslation("relationshipDialog.sentence.noAction") : action.getValue().name;
        String val = referenceValue.getValue() == null || referenceValue.getValue().isBlank() ? "" : " " + referenceValue.getValue();
        String tok = token.getValue() != null && !token.getValue().isBlank() ? " {" + token.getValue() + "}" : "";
        sentence.removeAll();
        sentence.add(new Span(getTranslation("relationshipDialog.sentence",
                up == null ? "…" : up.label(), op, val, act, tok, t == null ? "…" : t.label())));
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
