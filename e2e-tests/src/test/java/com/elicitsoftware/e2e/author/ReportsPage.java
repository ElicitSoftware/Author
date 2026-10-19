package com.elicitsoftware.e2e.author;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

/**
 * Author's Reports page, {@code survey/{id}/reports} (UC-048 report services, UC-049 report
 * marks). Opened by URL; the editor's toolbar caption is "Reports…".
 */
public class ReportsPage extends AuthorPageObject {

    public ReportsPage(Page page) {
        super(page);
    }

    public void waitUntilLoaded() {
        page.locator("h3:has-text(\"Report services\")").first().waitFor();
    }

    /** True while a row carries the "Built-in" badge (UC-048 BR-002). */
    public boolean listsTheBuiltInReport() {
        return servicesGrid().locator("span.designer-badge").filter(new Locator.FilterOptions().setHasText("Built-in")).count() > 0;
    }

    /**
     * Removes the built-in respondent report (UC-048 A3): Remove on its row, then the confirm
     * dialog, then the empty-list paragraph.
     */
    public void removeBuiltInReport() {
        Locator row = serviceRows().filter(new Locator.FilterOptions().setHasText("Built-in")).first();
        row.waitFor();
        buttonByText(row, "Remove").click();
        Locator confirm = page.locator("vaadin-confirm-dialog[opened]").last();
        confirm.waitFor();
        page.locator("vaadin-confirm-dialog-overlay[opened] vaadin-button")
                .filter(new Locator.FilterOptions().setHasText("Remove")).first().click();
        page.getByText("No report services.").first().waitFor();
    }

    /**
     * Mark element… (UC-049 steps 2-4): marks {@code elementLabel} for the respondent report, at
     * its default scope (wherever a question appears), under {@code label}.
     */
    public void markForRespondentReport(String elementLabel, String label) {
        buttonByText("Mark element").click(); // the caption is "Mark element…" (U+2026)
        Locator dialog = dialog("Mark for a report");
        Locator input = dialog.locator("vaadin-combo-box:has(label:text-is(\"Element\")) input");
        input.click();
        Locator item = page.locator("vaadin-combo-box-item")
                .filter(new Locator.FilterOptions().setHasText(elementLabel)).first();
        item.waitFor();
        item.click();
        fieldByLabel(dialog, "Label").fill(label);
        submitDialog(dialog, "Save");
        page.locator("vaadin-notification-card")
                .filter(new Locator.FilterOptions().setHasText("Marked")).first().waitFor();
    }

    /** The rows of the shown report's marks (the Respondent tab opens first). */
    public int markCount() {
        Locator grid = page.locator("h3:has-text(\"Report marks\") ~ vaadin-grid").first();
        if (grid.count() == 0 || !grid.isVisible()) {
            return 0;
        }
        return grid.locator("vaadin-grid-cell-content").filter(new Locator.FilterOptions().setHasText("Edit label")).count();
    }

    private Locator servicesGrid() {
        return page.locator("h3:has-text(\"Report services\") ~ vaadin-grid").first();
    }

    /** One locator per body row: the cells holding the row's Remove button. */
    private Locator serviceRows() {
        return servicesGrid().locator("vaadin-grid-cell-content").filter(new Locator.FilterOptions().setHasText("Remove"));
    }
}
