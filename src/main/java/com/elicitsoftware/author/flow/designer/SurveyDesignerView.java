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
import com.elicitsoftware.author.flow.ElementView;
import com.elicitsoftware.author.flow.MainLayout;
import com.elicitsoftware.author.flow.SurveyEditorView;
import com.elicitsoftware.author.flow.SurveyModelGuideView;
import com.elicitsoftware.author.security.AuthorRoles;
import com.elicitsoftware.author.survey.DesignerService;
import com.elicitsoftware.author.survey.ElementService;
import com.elicitsoftware.author.survey.RelationshipQuery;
import com.elicitsoftware.author.survey.RuleService;
import com.elicitsoftware.author.survey.StructureNode;
import com.elicitsoftware.author.survey.SurveyService;
import com.elicitsoftware.author.survey.SurveyStructureQuery;
import com.elicitsoftware.model.ActionType;
import com.elicitsoftware.model.Question;
import com.elicitsoftware.model.QuestionType;
import com.elicitsoftware.model.Relationship;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.elicitsoftware.model.Section;
import com.elicitsoftware.model.Step;
import com.elicitsoftware.model.Survey;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.NotFoundException;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteParameters;
import com.vaadin.flow.router.RouterLink;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * The designer board (UC-011, UC-015, UC-016, UC-018): steps as lanes, sections as collapsible
 * cards, questions as rows; every glyph a drag handle, every drag with a menu equivalent. The
 * board is rebuilt from {@link SurveyStructureQuery} after each change; selection is mirrored in
 * the breadcrumb, the drawer tree and the address (UC-006 BR-003).
 */
@Route(value = "survey/:" + SurveyEditorView.SURVEY_ID + "(\\d+)/design", layout = MainLayout.class)
@PageTitle("Designer")
@RolesAllowed(AuthorRoles.AUTHOR)
public class SurveyDesignerView extends Div implements BeforeEnterObserver {

    public static final String SELECT = "select";

    @Inject
    SurveyService surveyService;

    @Inject
    SurveyStructureQuery structureQuery;

    @Inject
    DesignerService designer;

    @Inject
    ElementService elements;

    @Inject
    RuleService rules;

    @Inject
    RelationshipQuery relationshipQuery;

    private static final ObjectMapper JSON = new ObjectMapper();

    Survey survey;
    SurveyStructureQuery.Tree tree;
    StructureNode selected;
    // Open (expanded) step and section nodes, keyed by kind so a step id and an assignment id
    // of the same number never collide. Steps have a null assignmentId, so they key on the step id.
    final Set<String> openNodes = new HashSet<>();

    final Div breadcrumb = new Div();
    final Div board = new Div();
    final Checkbox showRemoved = new Checkbox("Show removed");
    final Checkbox showRules = new Checkbox("Show rules", true);
    final RuleArrowsOverlay overlay = new RuleArrowsOverlay();
    final Button overview = new Button("Overview", VaadinIcon.ARROW_LEFT.create());
    final List<StepLane> lanes = new ArrayList<>();
    DesignerPalette palette;

    public SurveyDesignerView() {
        addClassName("designer");
        breadcrumb.addClassName("designer-breadcrumb");
        breadcrumb.getElement().setAttribute("aria-label", "Where you are in the survey");
        board.addClassName("designer-board");
        showRemoved.addValueChangeListener(e -> load());
        showRules.addValueChangeListener(e -> render());
        overlay.addConnectRequestedListener(e -> connectRequested(e.sourceAnchor(), e.targetAnchor(), e.action()));
        overlay.addEdgeSelectedListener(e -> editRule(e.edgeId()));
        overlay.addEdgeRepointedListener(e -> repoint(e.edgeId(), e.end(), e.targetAnchor()));
        overview.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
        overview.addClickListener(e -> getUI().ifPresent(ui -> ui.navigate(SurveyEditorView.class,
                new RouteParameters(SurveyEditorView.SURVEY_ID, String.valueOf(survey.id)))));
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        int id = event.getRouteParameters().getInteger(SurveyEditorView.SURVEY_ID).orElse(-1);
        Optional<Survey> found = surveyService.find(id);
        if (found.isEmpty()) {
            event.rerouteToError(NotFoundException.class, "No survey with id " + id);
            return;
        }
        survey = found.get();
        if (palette == null) {
            palette = new DesignerPalette(this::dragStarted, this::dragEnded);
            // The palette (add-tools + rule glyphs) now lives in the drawer, placed there by
            // MainLayout.syncWith; the in-view bar keeps only the back link and the view toggles.
            HorizontalLayout toolbar = new HorizontalLayout(overview, showRules, showRemoved);
            toolbar.addClassName("designer-toolbar");
            toolbar.setAlignItems(HorizontalLayout.Alignment.CENTER);
            toolbar.setWidthFull();
            add(breadcrumb, toolbar, board);
        }
        List<String> select = event.getLocation().getQueryParameters().getParameters().getOrDefault(SELECT, List.of());
        tree = structureQuery.tree(survey.id, Boolean.TRUE.equals(showRemoved.getValue()));
        selected = select.isEmpty() ? null : resolve(select.get(0));
        openAncestors(selected);
        render();
    }

    // ---- state -----------------------------------------------------------------------------

    public Survey survey() {
        return survey;
    }

    public StructureNode selected() {
        return selected;
    }

    /** The tools palette, placed in the drawer by {@link MainLayout} (built in {@code beforeEnter}). */
    public DesignerPalette palette() {
        return palette;
    }

    void load() {
        tree = structureQuery.tree(survey.id, Boolean.TRUE.equals(showRemoved.getValue()));
        selected = selected == null ? null : find(selected.kind(), selected.durableId(), selected.assignmentId());
        openAncestors(selected);
        render();
    }

    private void render() {
        rebuildBoard();
        updateBreadcrumb();
        syncAddress();
        mainLayout().ifPresent(layout -> {
            layout.refreshStructureTree(); // the drawer tree reads the same tables; keep it in step (UC-006 BR-003)
            layout.syncWith(this);
        });
    }

    private void rebuildBoard() {
        // A drop rebuilds the board, so the dragged element (and its dragend) never comes back.
        board.removeClassName("dragging");
        board.removeAll();
        lanes.clear();
        List<StructureNode> roots = tree.roots();
        for (int i = 0; i < roots.size(); i++) {
            board.add(new DropZone(new DropSpot(null, i, StructureNode.Kind.STEP), this::onDrop));
            StepLane lane = new StepLane(this, roots.get(i), i + 1, childrenOf(roots.get(i)), i, roots.size());
            lanes.add(lane);
            board.add(lane);
        }
        board.add(new DropZone(new DropSpot(null, roots.size(), StructureNode.Kind.STEP), this::onDrop));
        overlay.setEdges(Boolean.TRUE.equals(showRules.getValue()) ? edgesJson() : "[]");
        overlay.setSelectedAnchor(anchorOf(selected));
        board.add(overlay);
        if (roots.isEmpty()) {
            Div empty = new Div(new Span("This survey has no steps yet. Drag the Step glyph here or use Add step. "),
                    new RouterLink("How surveys work", SurveyModelGuideView.class));
            empty.addClassName("designer-empty-board");
            board.add(empty);
        }
    }

    private void updateBreadcrumb() {
        breadcrumb.removeAll();
        RouterLink root = new RouterLink(survey.title == null || survey.title.isBlank() ? survey.name : survey.title, SurveyEditorView.class,
                new RouteParameters(SurveyEditorView.SURVEY_ID, String.valueOf(survey.id)));
        root.addClassName("designer-crumb");
        breadcrumb.add(root);
        List<StructureNode> path = new ArrayList<>();
        for (StructureNode n = selected; n != null; n = n.parent()) {
            path.add(0, n);
        }
        for (int i = 0; i < path.size(); i++) {
            StructureNode n = path.get(i);
            Span separator = new Span("›");
            separator.addClassName("designer-crumb-separator");
            separator.getElement().setAttribute("aria-hidden", "true");
            breadcrumb.add(separator);
            if (i == path.size() - 1) {
                Span current = new Span(crumbLabel(n));
                current.addClassNames("designer-crumb", "designer-crumb-current");
                current.getElement().setAttribute("aria-current", "location");
                breadcrumb.add(current);
            } else {
                Button crumb = new Button(crumbLabel(n), e -> select(n));
                crumb.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE, ButtonVariant.LUMO_SMALL);
                crumb.addClassName("designer-crumb");
                breadcrumb.add(crumb);
            }
        }
    }

    private static String crumbLabel(StructureNode n) {
        return switch (n.kind()) {
            case STEP -> "Step: " + n.label();
            case SECTION -> "Section: " + n.label();
            case QUESTION -> "Question: " + n.label();
        };
    }

    private void syncAddress() {
        String path = "survey/" + survey.id + "/design" + (selected == null ? "" : "?" + SELECT + "=" + selected.routeKind() + ":" + selected.durableId());
        getUI().ifPresent(ui -> ui.getPage().executeJs("history.replaceState(history.state, '', $0)", path));
    }

    private Optional<MainLayout> mainLayout() {
        UI ui = getUI().orElse(UI.getCurrent());
        return ui == null ? Optional.empty() : ui.getChildren().filter(MainLayout.class::isInstance).map(MainLayout.class::cast).findFirst();
    }

    private StructureNode resolve(String selectParam) {
        String[] parts = selectParam.split(":");
        if (parts.length < 2) {
            return null;
        }
        try {
            StructureNode.Kind kind = StructureNode.Kind.valueOf(parts[0].toUpperCase());
            return find(kind, Integer.parseInt(parts[1]), null);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private StructureNode find(StructureNode.Kind kind, Integer durableId, Integer assignmentId) {
        return tree.all().stream()
                .filter(n -> n.kind() == kind && Objects.equals(n.durableId(), durableId))
                .filter(n -> assignmentId == null || Objects.equals(n.assignmentId(), assignmentId))
                .findFirst()
                .orElseGet(() -> assignmentId == null ? null : find(kind, durableId, null));
    }

    private void openAncestors(StructureNode node) {
        for (StructureNode n = node; n != null; n = n.parent()) {
            String key = openKey(n);
            if (key != null) {
                openNodes.add(key);
            }
        }
    }

    /** The overlay anchor id for a node: step-/ss-/sq- as the board tags its boxes. */
    private static String anchorOf(StructureNode node) {
        if (node == null) {
            return "";
        }
        return switch (node.kind()) {
            case STEP -> "step-" + node.durableId();
            case SECTION -> "ss-" + node.assignmentId();
            case QUESTION -> "sq-" + node.assignmentId();
        };
    }

    /** Mark a collapsible node (step or section) expanded so a new or moved child is visible. */
    void open(StructureNode node) {
        String key = node == null ? null : openKey(node);
        if (key != null) {
            openNodes.add(key);
        }
    }

    /** Expansion key for a collapsible node (step or section); null for questions and leaves. */
    private static String openKey(StructureNode node) {
        return switch (node.kind()) {
            case STEP -> "step-" + node.durableId();
            case SECTION -> "ss-" + node.assignmentId();
            default -> null;
        };
    }

    // ---- queries used by the components ------------------------------------------------------

    public List<StructureNode> childrenOf(StructureNode node) {
        return tree.childrenOf(node);
    }

    public int mountCount(StructureNode section) {
        return (int) tree.all().stream().filter(n -> n.kind() == StructureNode.Kind.SECTION && !n.retired()
                && Objects.equals(n.durableId(), section.durableId())).count();
    }

    public boolean isOpen(StructureNode node) {
        String key = openKey(node);
        return key != null && openNodes.contains(key);
    }

    public boolean isSelected(StructureNode node) {
        return selected != null && selected.kind() == node.kind() && Objects.equals(selected.durableId(), node.durableId())
                && Objects.equals(selected.assignmentId(), node.assignmentId());
    }

    // ---- interaction -------------------------------------------------------------------------

    public void select(StructureNode node) {
        selected = node;
        openAncestors(node);
        render();
    }


    /** From the drawer tree: the tree's nodes are equal by value to the board's. */
    public void selectFromTree(StructureNode node) {
        select(find(node.kind(), node.durableId(), node.assignmentId()));
    }

    public void toggleAndSelect(StructureNode node) {
        String key = openKey(node);
        if (key != null && !openNodes.remove(key)) {
            openNodes.add(key);
        }
        selected = node;
        openAncestors(node.parent());
        render();
    }

    public void dragStarted() {
        board.addClassName("dragging");
    }

    public void dragEnded() {
        board.removeClassName("dragging");
    }

    /** Central drop dispatcher: palette items create, nodes reorder within or copy across containers. */
    public void onDrop(Object data, DropSpot spot) {
        try {
            if (data instanceof PaletteItem item) {
                if (item.kind() != spot.accepts()) {
                    return;
                }
                switch (item.kind()) {
                    case STEP -> createStepAt(spot.index());
                    case SECTION -> createSectionAt(spot.container(), spot.index());
                    case QUESTION -> createQuestionAt(spot.container(), spot.index(), typeNamed(item.questionType()));
                }
            } else if (data instanceof StructureNode node) {
                if (node.kind() != spot.accepts() || node.retired()) {
                    return;
                }
                switch (node.kind()) {
                    case STEP -> {
                        int from = indexIn(tree.roots(), node);
                        designer.moveStep(survey.id, node.durableId(), targetIndex(from, spot.index()));
                        afterChange(node, "Moved step '" + node.label() + "'");
                    }
                    case SECTION -> {
                        if (Objects.equals(node.parent().durableId(), spot.container().durableId())) {
                            int from = indexIn(childrenOf(spot.container()), node);
                            designer.moveSectionWithinStep(survey.id, node.assignmentId(), targetIndex(from, spot.index()));
                            afterChange(node, "Moved section '" + node.label() + "'");
                        } else {
                            Section copy = designer.duplicateSectionInto(survey.id, node.durableId(), spot.container().durableId(), spot.index());
                            copiedSection(copy, spot.container());
                        }
                    }
                    case QUESTION -> {
                        if (Objects.equals(node.parent().durableId(), spot.container().durableId())) {
                            int from = indexIn(childrenOf(spot.container()), node);
                            designer.moveQuestionWithinSection(survey.id, node.assignmentId(), targetIndex(from, spot.index()));
                            afterChange(node, "Moved question '" + node.label() + "'");
                        } else {
                            designer.assignQuestionInto(survey.id, node.durableId(), spot.container().durableId(), spot.index());
                            open(spot.container());
                            load();
                            select(find(StructureNode.Kind.QUESTION, node.durableId(), null));
                            success("Copied question '" + node.label() + "' into section '" + spot.container().label() + "'");
                        }
                    }
                }
            }
        } catch (AuthoringException ae) {
            error(ae.getMessage());
        }
    }

    public void edit(StructureNode node) {
        try {
            switch (node.kind()) {
                case STEP -> ElementDialog.step(designer.currentStep(survey.id, node.durableId()),
                        e -> designer.rename(survey.id, e), saved -> load()).open();
                case SECTION -> ElementDialog.section(designer.currentSection(survey.id, node.durableId()),
                        e -> designer.rename(survey.id, e), saved -> load()).open();
                case QUESTION -> new QuestionDialog(survey.id, designer.currentQuestion(survey.id, node.durableId()), null,
                        q -> designer.update(survey.id, q), saved -> load()).open();
            }
        } catch (AuthoringException ae) {
            error(ae.getMessage());
        }
    }

    public void addChild(StructureNode node) {
        switch (node.kind()) {
            case STEP -> createSectionAt(node, childrenOf(node).size());
            case SECTION -> createQuestionAt(node, childrenOf(node).size(), palette.selectedQuestionType());
            default -> { }
        }
    }

    public void move(StructureNode node, int delta) {
        try {
            switch (node.kind()) {
                case STEP -> designer.moveStep(survey.id, node.durableId(), indexIn(tree.roots(), node) + delta);
                case SECTION -> designer.moveSectionWithinStep(survey.id, node.assignmentId(), indexIn(childrenOf(node.parent()), node) + delta);
                case QUESTION -> designer.moveQuestionWithinSection(survey.id, node.assignmentId(), indexIn(childrenOf(node.parent()), node) + delta);
            }
            afterChange(node, "Moved " + node.routeKind() + " '" + node.label() + "'");
        } catch (AuthoringException ae) {
            error(ae.getMessage());
        }
    }

    /** Keyboard equivalent of dragging into another container (UC-015 A5, UC-011 A4). */
    public void copyTo(StructureNode node) {
        switch (node.kind()) {
            case SECTION -> new PickerDialog<>("Copy section '" + node.label() + "'", "Into step", tree.roots(), StructureNode::label, step -> {
                try {
                    Section copy = designer.duplicateSectionInto(survey.id, node.durableId(), step.durableId(), childrenOf(step).size());
                    copiedSection(copy, step);
                } catch (AuthoringException ae) {
                    error(ae.getMessage());
                }
            }).open();
            case QUESTION -> new PickerDialog<>("Copy question '" + node.label() + "'", "Into section", sectionsDistinct(), this::sectionPath, section -> {
                try {
                    designer.assignQuestionInto(survey.id, node.durableId(), section.durableId(), childrenOf(section).size());
                    open(section);
                    load();
                    success("Copied question '" + node.label() + "' into section '" + section.label() + "'");
                } catch (AuthoringException ae) {
                    error(ae.getMessage());
                }
            }).open();
            default -> { }
        }
    }

    /** UC-015 A1: the same section in another step. */
    public void mountIn(StructureNode section) {
        new PickerDialog<>("Mount section '" + section.label() + "'", "Also in step", tree.roots(), StructureNode::label, step -> {
            try {
                designer.mountSection(survey.id, section.durableId(), step.durableId(), childrenOf(step).size());
                load();
                success("Section '" + section.label() + "' is now also in step '" + step.label() + "'");
            } catch (AuthoringException ae) {
                error(ae.getMessage());
            }
        }).open();
    }

    public void openDetails(StructureNode node) {
        getUI().ifPresent(ui -> ui.navigate(ElementView.class, new RouteParameters(Map.of(
                SurveyEditorView.SURVEY_ID, String.valueOf(survey.id),
                ElementView.KIND, node.routeKind(),
                ElementView.DURABLE_ID, String.valueOf(node.durableId())))));
    }

    /** UC-015 A3 / UC-011 A5: remove one assignment; the last one retires the element. */
    public void removeAssignment(StructureNode node) {
        boolean last = node.kind() == StructureNode.Kind.SECTION ? mountCount(node) <= 1
                : tree.all().stream().filter(n -> n.kind() == StructureNode.Kind.QUESTION && !n.retired()
                && Objects.equals(n.durableId(), node.durableId())).count() <= 1;
        String what = node.kind() == StructureNode.Kind.SECTION ? "section" : "question";
        String where = node.kind() == StructureNode.Kind.SECTION ? "step" : "section";
        String text = "Remove " + what + " '" + node.label() + "' from " + where + " '" + node.parent().label() + "'?"
                + (last ? " This is its only place in the survey, so the " + what + " itself is removed (it can be restored from its details page)."
                : " It stays in the other places it is used.");
        new ConfirmDialog("Remove from " + where, text, "Remove", e -> {
            try {
                if (node.kind() == StructureNode.Kind.SECTION) {
                    designer.retireSectionAssignment(survey.id, node.assignmentId());
                } else {
                    designer.retireQuestionAssignment(survey.id, node.assignmentId());
                }
                selected = node.parent();
                load();
                success("Removed " + what + " '" + node.label() + "' from " + where + " '" + node.parent().label() + "'");
            } catch (AuthoringException ae) {
                error(ae.getMessage());
            }
        }, "Cancel", e -> { }).open();
    }

    public void removeStep(StructureNode step) {
        ElementService.RemovalPlan plan = elements.planRemoval(survey.id, StructureNode.Kind.STEP, step.durableId());
        String text = "Remove step '" + step.label() + "'?" + (plan.dependents().isEmpty() ? ""
                : " Removed with it: " + String.join("; ", plan.dependents()) + ".") + " It can be restored from its details page.";
        new ConfirmDialog("Remove step", text, "Remove", e -> {
            try {
                designer.removeStep(survey.id, step.durableId());
                selected = null;
                load();
                success("Removed step '" + step.label() + "'");
            } catch (AuthoringException ae) {
                error(ae.getMessage());
            }
        }, "Cancel", e -> { }).open();
    }

    // ---- rules (UC-019, UC-020) ------------------------------------------------------------

    String edgesJson() {
        try {
            return JSON.writeValueAsString(relationshipQuery.edges(survey.id, Boolean.TRUE.equals(showRemoved.getValue())));
        } catch (JsonProcessingException e) {
            return "[]";
        }
    }

    /** UC-019 step 1-2: a rule glyph landed on a question row; the overlay now follows the pointer. */
    public void startRule(String action, StructureNode question) {
        showRules.setValue(true);
        selected = question;
        openAncestors(question);
        render();
        overlay.setPending("sq-" + question.assignmentId(), action);
    }

    /** UC-019 A1: the keyboard path — upstream fixed, target chosen in the dialog. */
    public void addRule(StructureNode question) {
        Relationship preset = new Relationship();
        preset.upstreamSqId = question.assignmentId();
        new RelationshipDialog(survey.id, tree, rules, null, preset, saved -> ruleSaved(saved), this::load).open();
    }

    void connectRequested(String sourceAnchor, String targetAnchor, String action) {
        overlay.setPending("", "");
        Integer sq = anchorId(sourceAnchor, "sq");
        if (sq == null || targetAnchor == null) {
            return;
        }
        Relationship preset = new Relationship();
        preset.upstreamSqId = sq;
        preset.actionType = action == null || action.isBlank() ? null : ActionType.find("name", action).firstResult();
        Integer step = anchorId(targetAnchor, "step");
        Integer ss = anchorId(targetAnchor, "ss");
        Integer tsq = anchorId(targetAnchor, "sq");
        if (step != null) {
            preset.downstreamStepId = step;
        } else if (ss != null) {
            preset.downstreamSsId = ss;
        } else if (tsq != null) {
            preset.downstreamSqId = tsq;
        } else {
            info("Drop the arrow on a step, a section or a question");
            return;
        }
        new RelationshipDialog(survey.id, tree, rules, null, preset, saved -> ruleSaved(saved), this::load).open();
    }

    void editRule(int relationshipId) {
        Optional<Relationship> rule = rules.any(survey.id, relationshipId);
        if (rule.isEmpty()) {
            info("This rule no longer exists");
            return;
        }
        overlay.setSelectedEdgeId(relationshipId);
        new RelationshipDialog(survey.id, tree, rules, rule.get(), null, saved -> ruleSaved(saved), () -> {
            overlay.setSelectedEdgeId(null);
            load();
        }).open();
    }

    void repoint(int relationshipId, String end, String targetAnchor) {
        try {
            rules.repoint(survey.id, relationshipId, "SOURCE".equals(end) ? RuleService.End.SOURCE : RuleService.End.TARGET, targetAnchor);
            load();
            success("Rule re-pointed");
        } catch (AuthoringException ae) {
            error(ae.getMessage());
            load();
        }
    }

    private void ruleSaved(Relationship saved) {
        overlay.setSelectedEdgeId(saved.relationshipId);
        showRules.setValue(true);
        load();
    }

    private static Integer anchorId(String anchor, String prefix) {
        if (anchor == null || !anchor.startsWith(prefix + "-")) {
            return null;
        }
        try {
            return Integer.parseInt(anchor.substring(prefix.length() + 1));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    // ---- creation --------------------------------------------------------------------------

    void createStepAt(int index) {
        ElementDialog.step(null, draft -> designer.createStep(survey.id, draft, index), saved -> {
            load();
            select(find(StructureNode.Kind.STEP, saved.stepId, null));
        }).open();
    }

    void createSectionAt(StructureNode step, int index) {
        ElementDialog.section(null, draft -> designer.createSectionIn(survey.id, step.durableId(), draft, index), saved -> {
            load();
            StructureNode node = find(StructureNode.Kind.SECTION, saved.sectionId, null);
            if (node != null) {
                open(node);
            }
            select(node);
        }).open();
    }

    void createQuestionAt(StructureNode section, int index, QuestionType type) {
        new QuestionDialog(survey.id, null, type, draft -> designer.createQuestionIn(survey.id, section.durableId(), draft, index), saved -> {
            open(section);
            load();
            select(find(StructureNode.Kind.QUESTION, saved.questionId, null));
        }).open();
    }

    // ---- helpers ---------------------------------------------------------------------------

    private void copiedSection(Section copy, StructureNode targetStep) {
        load();
        StructureNode node = find(StructureNode.Kind.SECTION, copy.sectionId, null);
        select(node);
        Button undo = new Button("Undo", e -> {
            try {
                designer.removeSection(survey.id, copy.sectionId);
                selected = null;
                load();
            } catch (AuthoringException ae) {
                error(ae.getMessage());
            }
        });
        undo.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
        Notification n = new Notification(new Span("Copied section '" + copy.name + "' into step '" + targetStep.label() + "'. "), undo);
        n.setDuration(8000);
        n.setPosition(Notification.Position.BOTTOM_START);
        n.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        n.open();
    }

    private void afterChange(StructureNode node, String message) {
        selected = node;
        load();
        success(message);
    }

    private static int indexIn(List<StructureNode> siblings, StructureNode node) {
        for (int i = 0; i < siblings.size(); i++) {
            StructureNode s = siblings.get(i);
            if (Objects.equals(s.durableId(), node.durableId()) && Objects.equals(s.assignmentId(), node.assignmentId())) {
                return i;
            }
        }
        return -1;
    }

    /** A zone index counts the dragged element itself when it sits before the zone. */
    private static int targetIndex(int from, int zoneIndex) {
        return from >= 0 && zoneIndex > from ? zoneIndex - 1 : zoneIndex;
    }

    private List<StructureNode> sectionsDistinct() {
        List<StructureNode> out = new ArrayList<>();
        Set<Integer> seen = new HashSet<>();
        for (StructureNode n : tree.all()) {
            if (n.kind() == StructureNode.Kind.SECTION && !n.retired() && seen.add(n.durableId())) {
                out.add(n);
            }
        }
        return out;
    }

    private String sectionPath(StructureNode section) {
        return section.parent().label() + " › " + section.label();
    }

    private static QuestionType typeNamed(String name) {
        return name == null ? null : QuestionType.find("name", name).firstResult();
    }

    private static void success(String message) {
        Notification.show(message, 3000, Notification.Position.BOTTOM_START).addThemeVariants(NotificationVariant.LUMO_SUCCESS);
    }

    private static void info(String message) {
        Notification.show(message, 3000, Notification.Position.BOTTOM_START);
    }

    private static void error(String message) {
        Notification.show(message, 5000, Notification.Position.MIDDLE).addThemeVariants(NotificationVariant.LUMO_ERROR);
    }
}
