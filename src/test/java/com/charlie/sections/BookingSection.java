package com.charlie.sections;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import java.util.regex.Pattern;

public final class BookingSection {
  private static final Pattern TIME = Pattern.compile("^\\s*([01]?\\d|2[0-3]):[0-5]\\d\\s*$");
  private static final Pattern BOOK = Pattern.compile("(?i).*(book|reserve|заброн|брон).*");

  private final Page page;

  public BookingSection(Page page) {
    this.page = page;
  }

  /** Selects a time or books it, never both in one iteration. */
  public void selectTimeOrBook() {
    Locator step = bookingStep();
    Locator booking = step.getByRole(
        AriaRole.BUTTON, new Locator.GetByRoleOptions().setName(BOOK));
    if (booking.count() > 0 && booking.first().isVisible() && booking.first().isEnabled()
        && hasSelectedEnabledTime(step)) {
      booking.first().click();
      return;
    }

    Locator buttons = step.locator(
        "button[type='button']:visible:not([disabled]), button:not([type]):visible:not([disabled])");
    for (int i = 0; i < buttons.count(); i++) {
      Locator candidate = buttons.nth(i);
      if (TIME.matcher(candidate.innerText()).matches()) {
        candidate.click();
        return;
      }
    }
    throw new IllegalStateException("Booking step has no enabled HH:mm slot or enabled booking CTA");
  }

  private boolean hasSelectedEnabledTime(Locator step) {
    Locator selected = step.locator("""
        button[aria-pressed='true']:not([disabled]),
        button[aria-selected='true']:not([disabled]),
        button[data-selected='true']:not([disabled]),
        button.selected:not([disabled]),
        button[class*='active' i]:not([disabled]),
        button[class*='selected' i]:not([disabled])
        """);
    for (int i = 0; i < selected.count(); i++) {
      if (selected.nth(i).isVisible() && TIME.matcher(selected.nth(i).innerText()).matches()) {
        return true;
      }
    }
    return false;
  }

  private Locator bookingStep() {
    Locator step = page.locator("[data-step-name='lesson-time-select']:visible");
    if (step.count() == 0) {
      throw new IllegalStateException("The lesson-time-select step is not visible");
    }
    return step.first();
  }
}
