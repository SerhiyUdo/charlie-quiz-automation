package com.charlie.sections;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import java.util.regex.Pattern;

public final class QuizModalSection {
  private static final Pattern QUESTIONNAIRE = Pattern.compile("(?i).*(questionnaire|анкет).*");
  private static final Pattern PARENT = Pattern.compile(
      "(?i).*(parent|guardian|adult|mother|father|бать|дорос|мам|тат).*"
  );

  private final Page page;

  public QuizModalSection(Page page) {
    this.page = page;
  }

  /** Returns true after handling the modal with exactly one click. */
  public boolean chooseParentIfVisible() {
    Locator dialog = page.getByRole(AriaRole.DIALOG).filter(
        new Locator.FilterOptions().setHas(page.getByText(QUESTIONNAIRE)));
    if (dialog.count() == 0 || !dialog.first().isVisible()) {
      return false;
    }

    Locator choices = dialog.first().getByRole(AriaRole.BUTTON);
    for (int i = 0; i < choices.count(); i++) {
      Locator choice = choices.nth(i);
      if (choice.isVisible() && choice.isEnabled() && PARENT.matcher(choice.innerText()).matches()) {
        choice.click();
        return true;
      }
    }
    throw new IllegalStateException("Owner modal is visible but no semantic parent/guardian choice was found");
  }
}
