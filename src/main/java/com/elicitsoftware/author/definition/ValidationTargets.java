package com.elicitsoftware.author.definition;

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

import com.elicitsoftware.author.survey.DesignerAddress;
import com.elicitsoftware.author.survey.ReportingService;
import com.elicitsoftware.author.survey.RuleService;
import com.elicitsoftware.author.survey.StructureNode;
import com.elicitsoftware.author.survey.SurveyStructureQuery;
import com.elicitsoftware.model.Relationship;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Where a validation finding points (UC-007 BR-008, BR-009 / FR-061).
 *
 * <p>A finding names an element, and a name is not a place: the same section can be mounted in two
 * steps, and a rule's description says nothing about where in the survey it fires. This resolves
 * each finding to the page that can correct it and to the path a reader can follow by eye — the
 * containers of a section or a question, the whole path to a rule's target, nothing for a step,
 * whose message already names it.</p>
 *
 * <p>The tree is read once per survey, not once per finding, so a panel of forty findings is one
 * query set. Retired elements are left out on purpose: the board hides them until Show removed is
 * on, so a link to one would land nowhere (BR-009).</p>
 */
@ApplicationScoped
public class ValidationTargets {

    /** The page a finding is corrected on. */
    public enum Destination { DESIGNER, REPORTING, TRANSLATIONS }

    /**
     * @param destination the page to open
     * @param address     the designer's {@code select} value, or null for the other pages
     * @param location    the path to the element, outermost first; empty when the finding has no place
     */
    public record Target(Destination destination, DesignerAddress address, List<String> location) {

        public static Target designer(DesignerAddress address, List<String> location) {
            return new Target(Destination.DESIGNER, address, location);
        }

        public static Target page(Destination destination) {
            return new Target(destination, null, List.of());
        }
    }

    @Inject
    SurveyStructureQuery structure;

    @Inject
    RuleService rules;

    /** Resolves every finding of one survey against one reading of its structure. */
    public Resolver forSurvey(int surveyId) {
        return new Resolver(surveyId, structure.tree(surveyId, false));
    }

    /** One survey's structure, held while a panel of findings is rendered. */
    public class Resolver {

        private final int surveyId;
        private final SurveyStructureQuery.Tree tree;

        Resolver(int surveyId, SurveyStructureQuery.Tree tree) {
            this.surveyId = surveyId;
            this.tree = tree;
        }

        public Optional<Target> of(ValidationIssue issue) {
            Integer id = issue.durableId();
            if (issue.kind() == null || id == null) {
                return Optional.empty();
            }
            return switch (issue.kind()) {
                case "steps" -> element(StructureNode.Kind.STEP, id, DesignerAddress.STEP);
                case "sections" -> element(StructureNode.Kind.SECTION, id, DesignerAddress.SECTION);
                case "questions" -> element(StructureNode.Kind.QUESTION, id, DesignerAddress.QUESTION);
                case "steps_sections" -> assigned(StructureNode.Kind.SECTION, DesignerAddress.mount(id));
                case "sections_questions" -> assigned(StructureNode.Kind.QUESTION, DesignerAddress.assignment(id));
                case "relationships" -> rule(id);
                case ReportingService.KIND_TAG, ReportingService.KIND_ASSIGNMENT -> Optional.of(Target.page(Destination.REPORTING));
                case "translations" -> Optional.of(Target.page(Destination.TRANSLATIONS));
                // "surveys" is the survey itself, which the author is already looking at, and
                // "select_items" reports an option whose list is gone, so there is nothing to open.
                default -> Optional.empty();
            };
        }

        private Optional<Target> element(StructureNode.Kind kind, int durableId, String addressKind) {
            StructureNode node = tree.find(kind, durableId, null);
            return node == null ? Optional.empty()
                    : Optional.of(Target.designer(new DesignerAddress(addressKind, durableId), containersOf(node)));
        }

        private Optional<Target> assigned(StructureNode.Kind kind, DesignerAddress address) {
            StructureNode node = tree.findByAssignment(kind, address.id());
            return node == null ? Optional.empty() : Optional.of(Target.designer(address, containersOf(node)));
        }

        /**
         * A rule is corrected in its own editor, which the designer opens over the element the rule
         * targets — and that target is the only place the survey holds for a rule, so it is the whole
         * of the location too. A rule whose target no longer resolves still opens; only a rule that is
         * gone has nothing to open.
         */
        private Optional<Target> rule(int relationshipId) {
            Optional<Relationship> rule = rules.current(surveyId, relationshipId);
            if (rule.isEmpty()) {
                return Optional.empty();
            }
            StructureNode target = tree.target(rule.get());
            return Optional.of(Target.designer(DesignerAddress.rule(relationshipId),
                    target == null ? List.of() : pathOf(target)));
        }

        /** The containers an element sits in; empty for a step, whose message already names it. */
        private List<String> containersOf(StructureNode node) {
            return pathOf(node.parent());
        }

        /** Labels from the outermost step down to this node. */
        private List<String> pathOf(StructureNode node) {
            List<String> labels = new ArrayList<>();
            for (StructureNode n = node; n != null; n = n.parent()) {
                labels.add(0, n.label());
            }
            return labels;
        }
    }
}
