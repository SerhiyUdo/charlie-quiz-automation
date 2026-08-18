package com.charlie.quiz;

import com.microsoft.playwright.Page;
import java.text.MessageFormat;
import java.time.Duration;

final class QuizFlow {
  private final Page page;
  private final QuizStepDriver driver;
  private final BusinessOutcomeVerifier verifier;
  private final Duration timeout;

  QuizFlow(Page page, QuizStepDriver driver, BusinessOutcomeVerifier verifier, Duration timeout) {
    this.page = page;
    this.driver = driver;
    this.verifier = verifier;
    this.timeout = timeout;
  }

  void complete() {
    long deadline = System.nanoTime() + timeout.toNanos();
    int iterations = 0;
    while (System.nanoTime() < deadline && iterations++ < 100) {
      verifier.observe();
      if (verifier.complete()) {
        return;
      }

      String beforeStep = driver.currentStepName();
      driver.advanceOneIteration();
      waitForSettledTransition(beforeStep);
    }
    verifier.observe();
    if (!verifier.complete()) {
      throw new AssertionError(MessageFormat.format(
          "Business outcome incomplete after {0} iterations; bookingObserved={1}, accountObserved={2}, url={3}, step={4}",
          iterations,
          verifier.bookingObserved(),
          verifier.accountObserved(),
          page.url(),
          driver.currentStepName()));
    }
  }

  private void waitForSettledTransition(String previousStep) {
    try {
      page.waitForFunction(
          """
          previous => { const visible = [...document.querySelectorAll('[data-step-name]')]
            .find(e => { const s=getComputedStyle(e); return s.visibility !== 'hidden' && s.display !== 'none'; });
          return !visible || visible.dataset.stepName !== previous; }
          """,
          previousStep,
          new Page.WaitForFunctionOptions().setTimeout(2_500));
    } catch (RuntimeException ignored) {
      // A multi-select or booking screen legitimately remains on the same step for another iteration.
    }
    page.waitForTimeout(250);
  }
}
