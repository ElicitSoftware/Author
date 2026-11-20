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

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.datetimepicker.DateTimePicker;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.shared.HasSuffix;
import com.vaadin.flow.component.shared.HasTooltip;
import com.vaadin.flow.component.shared.Tooltip;

/**
 * Attaches a short "what does this control do?" explanation to an edit control: the text becomes the
 * field's tooltip, and — where the field has a free suffix slot — a small info icon is shown as a
 * hover affordance next to it.
 */
public final class FieldHelp {

    private FieldHelp() {
    }

    /**
     * Explain a control: set its tooltip and, for plain text fields, show an info icon suffix.
     * The icon replaces any suffix component the field already had.
     *
     * @param field the control; a tooltip is set only when it supports one
     * @param text  the explanation
     */
    public static void help(Component field, String text) {
        if (field instanceof HasTooltip tooltip) {
            tooltip.setTooltipText(text);
        }
        // Pickers use the suffix slot for their own toggle, so only decorate fields that leave it free.
        boolean picker = field instanceof ComboBox || field instanceof MultiSelectComboBox
                || field instanceof DatePicker || field instanceof DateTimePicker;
        if (field instanceof HasSuffix suffix && !picker) {
            Icon icon = VaadinIcon.INFO_CIRCLE.create();
            icon.addClassName("field-info");
            Tooltip.forComponent(icon).withText(text);
            suffix.setSuffixComponent(icon);
        }
    }
}
