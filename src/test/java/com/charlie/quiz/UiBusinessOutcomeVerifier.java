package com.charlie.quiz;

import com.microsoft.playwright.Page;
import java.util.regex.Pattern;

final class UiBusinessOutcomeVerifier implements BusinessOutcomeVerifier {
  private static final Pattern DASHBOARD = Pattern.compile("/app/dashboard(?:[/?#]|$)");

  private final Page page;
  private boolean bookingObserved;
  private boolean accountObserved;

  UiBusinessOutcomeVerifier(Page page) {
    this.page = page;
  }

  @Override
  public void observe() {
    boolean bookingScreen = page.locator("[data-step-name='telegram-bot']:visible").count() > 0;
    boolean lessonDate = page.locator("[data-test-lesson-date]:visible").count() > 0;
    bookingObserved |= bookingScreen && lessonDate;
    accountObserved |= DASHBOARD.matcher(page.url()).find();
  }

  @Override
  public boolean bookingObserved() {
    return bookingObserved;
  }

  @Override
  public boolean accountObserved() {
    return accountObserved;
  }
}
