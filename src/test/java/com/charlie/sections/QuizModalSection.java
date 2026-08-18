package com.charlie.sections;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;

import java.util.regex.Pattern;

public final class QuizModalSection {
    private static final Pattern QUESTIONNAIRE =
            Pattern.compile(
                    ".*(questionnaire|анкет).*",
                    Pattern.CASE_INSENSITIVE
            );

    private static final Pattern PARENT =
            Pattern.compile(
                    ".*(parent|guardian|adult|mother|father|бать|дорос|мам|тат).*",
                    Pattern.CASE_INSENSITIVE
            );

    private final Page page;

    public QuizModalSection(Page page) {
        this.page = page;
    }

    /**
     * Returns true after handling the modal with exactly one click.
     */
    public boolean chooseParentIfVisible() {
        Locator dialog = page.getByRole(AriaRole.DIALOG)
                .filter(new Locator.FilterOptions().setHas(page.getByText(QUESTIONNAIRE)))
                .filter(new Locator.FilterOptions().setVisible(true));

        if (dialog.count() == 0) {
            return false;
        }

        Locator parentChoice = dialog.first().getByRole(
                AriaRole.BUTTON,
                new Locator.GetByRoleOptions().setName(PARENT)
        );

        if (parentChoice.count() == 0) {
            throw new IllegalStateException(
                    "Owner modal is visible but no semantic parent/guardian choice was found"
            );
        }

        Locator choice = parentChoice.first();

        choice.dispatchEvent("click");

        page.waitForCondition(() ->
                dialog.count() == 0 || !dialog.first().isVisible()
        );

        return true;
    }
}
