package com.elicitsoftware.author.flow.translations;

/*-
 * ***LICENSE_START***
 * Elicit Author
 * %%
 * Copyright (C) 2025 - 2026 Elicit Software LLC
 * %%
 * PolyForm Noncommercial License 1.0.0
 * <https://polyformproject.org/licenses/noncommercial/1.0.0>
 * ***LICENSE_END***
 */

import com.elicitsoftware.author.survey.TranslationHandoff;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.ListItem;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.UnorderedList;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.theme.lumo.LumoUtility;

/**
 * What an import of the hand-off file did (UC-046 step 5).
 * <p>
 * The counts are the point, and so is every rejection by name: one broken item never fails the
 * file, so an author has to be told exactly which strings did not land and why, or they will
 * assume the language is complete.
 */
class TranslationImportResultDialog extends Dialog {

    static final String CLOSE_ID = "translation-import-result-close";

    TranslationImportResultDialog(TranslationHandoff.ImportResult result) {
        setHeaderTitle(getTranslation("translationImport.title"));
        setWidth("40rem");

        VerticalLayout body = new VerticalLayout();
        body.setPadding(false);
        body.add(new Paragraph(getTranslation("translationImport.counts",
                String.valueOf(result.imported()), String.valueOf(result.unchanged()),
                String.valueOf(result.skipped()), String.valueOf(result.rejected()))));

        if (result.stale() > 0) {
            Paragraph stale = new Paragraph(getTranslation("translationImport.stale", String.valueOf(result.stale())));
            stale.addClassName(LumoUtility.TextColor.SECONDARY);
            body.add(stale);
        }
        if (!result.rejections().isEmpty()) {
            body.add(new Paragraph(getTranslation("translationImport.rejected")));
            body.add(list(result.rejections()));
        }
        if (!result.warnings().isEmpty()) {
            body.add(new Paragraph(getTranslation("translationImport.warnings")));
            body.add(list(result.warnings()));
        }
        add(body);

        Button close = new Button(getTranslation("common.close"), e -> close());
        close.setId(CLOSE_ID);
        close.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        getFooter().add(close);
    }

    /** The messages name element ids and authored text, so they are content, not chrome. */
    private static UnorderedList list(java.util.List<String> messages) {
        UnorderedList list = new UnorderedList();
        list.getElement().setAttribute("data-i18n-content", "");
        messages.forEach(m -> list.add(new ListItem(m)));
        return list;
    }
}
