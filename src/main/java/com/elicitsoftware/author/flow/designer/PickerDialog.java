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

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

/** Pick one target (a step to copy into, a section to assign to) — the keyboard path of a drop. */
public class PickerDialog<T> extends Dialog {

    final ComboBox<T> choice = new ComboBox<>();
    final Button ok = new Button("OK");
    final Button cancel = new Button("Cancel");

    public PickerDialog(String title, String label, List<T> options, Function<T, String> labelOf, Consumer<T> onPicked) {
        setHeaderTitle(title);
        setModal(true);
        choice.setLabel(label);
        choice.setItems(options);
        choice.setItemLabelGenerator(labelOf::apply);
        choice.setWidthFull();
        add(choice);
        ok.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        ok.addClickListener(e -> {
            if (choice.getValue() != null) {
                close();
                onPicked.accept(choice.getValue());
            } else {
                choice.setInvalid(true);
                choice.setErrorMessage("Choose one");
            }
        });
        cancel.addClickListener(e -> close());
        getFooter().add(cancel, ok);
    }
}
