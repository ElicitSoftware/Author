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
import com.elicitsoftware.author.security.AuthorRoles;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.clipboard.Clipboard;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.AnchorTarget;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.ListItem;
import com.vaadin.flow.component.html.OrderedList;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLink;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;

/**
 * UC-042: the third way to start a survey, made reachable from the tool itself.
 * <p>
 * The other two paths — the blank form (UC-003) and importing a definition (UC-005) — are
 * actions Author performs. This one is a round trip that happens somewhere else: the author
 * takes the specification to an AI session, describes the survey they want, and comes back with
 * a {@code .elicit} draft to import. So the page's job is to hand over the specification and to
 * state the round trip, not to perform any of it (BR-006).
 * <p>
 * The specification itself is English, the language it is written and gated in (NFR-017). The
 * preamble copied in front of it is chrome, so it comes from the translation bundle: the author
 * addresses the agent in their own language even though the specification it reads is not
 * (UC-040).
 */
@Route(value = "ai-draft", layout = MainLayout.class)
@RolesAllowed(AuthorRoles.AUTHOR)
public class AiDraftView extends VerticalLayout implements HasDynamicTitle {

    /** Where {@link com.elicitsoftware.author.ai.AuthoringSpecificationResource} serves the file. */
    static final String DOWNLOAD_HREF = "/api/authoring-spec";

    @Inject
    AuthoringSpecification specification;

    final Button copy = new Button(getTranslation("aiDraftView.copy"), VaadinIcon.COPY.create());
    final Anchor download = new Anchor(DOWNLOAD_HREF, getTranslation("aiDraftView.download"));
    final RouterLink importLink = new RouterLink(getTranslation("aiDraftView.goToImport"), ImportDefinitionView.class);
    final Paragraph unavailable = new Paragraph(getTranslation("aiDraftView.unavailable"));

    /** The clipboard binding is a click listener; attaching twice would copy twice. */
    private boolean clipboardBound;

    public AiDraftView() {
        addClassName("ai-draft");
        add(new H2(getTranslation("aiDraftView.title")));
        add(new Paragraph(getTranslation("aiDraftView.intro")));

        add(new OrderedList(
                new ListItem(getTranslation("aiDraftView.step1")),
                new ListItem(getTranslation("aiDraftView.step2")),
                new ListItem(getTranslation("aiDraftView.step3")),
                new ListItem(getTranslation("aiDraftView.step4"))));

        copy.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        // An anchor, not a button: the browser has to fetch a file, and /api/authoring-spec is
        // served outside the Vaadin router, which would otherwise intercept a relative href.
        download.removeAll();
        download.add(VaadinIcon.DOWNLOAD.create(), new Span(getTranslation("aiDraftView.download")));
        download.setRouterIgnore(true);
        download.setTarget(AnchorTarget.BLANK);
        download.addClassName("ai-draft-download");

        HorizontalLayout actions = new HorizontalLayout(copy, download);
        actions.setAlignItems(Alignment.CENTER);
        actions.addClassName("ai-draft-actions");
        add(actions);

        unavailable.setVisible(false);
        add(unavailable);

        add(new H3(getTranslation("aiDraftView.whatComesBack.h")));
        add(new Paragraph(getTranslation("aiDraftView.whatComesBack.p1")));
        add(new Paragraph(getTranslation("aiDraftView.whatComesBack.p2")));

        Div next = new Div(importLink);
        next.addClassName("ai-draft-next");
        add(next);
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        if (clipboardBound) {
            return;
        }
        clipboardBound = true;
        bindClipboard();
    }

    /**
     * Binds the copy button, once the bean is injected. The payload is resolved on the server and
     * written during the click's own DOM event, which is the fresh user gesture the browser
     * requires before it allows a clipboard write.
     */
    private void bindClipboard() {
        String payload = promptPayload();
        if (payload == null) {
            // A build that packaged no specification (UC-042 A5): say so rather than copy nothing.
            copy.setEnabled(false);
            download.setVisible(false);
            unavailable.setVisible(true);
            return;
        }
        Clipboard.onClick(copy).writeText(payload,
                copied -> Notification.show(getTranslation("aiDraftView.copied"), 3000,
                        Notification.Position.BOTTOM_START),
                error -> Notification.show(getTranslation("aiDraftView.copyFailed"), 5000,
                        Notification.Position.MIDDLE).addThemeVariants(NotificationVariant.LUMO_ERROR));
    }

    /**
     * The clipboard payload: the author's instruction to the agent, then the specification
     * verbatim.
     *
     * @return the prompt, or {@code null} when this build carries no specification
     */
    String promptPayload() {
        String document = specification == null ? null : specification.text();
        if (document == null) {
            return null;
        }
        return getTranslation("aiDraftView.promptPreamble") + "\n\n---\n\n" + document;
    }

    @Override
    public String getPageTitle() {
        return getTranslation("aiDraftView.pageTitle");
    }
}
