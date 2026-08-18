package com.charlie.quiz;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.time.Instant;
import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.Test;

public final class QuizBusinessOutcomeTest {
  @Test
  public void createsAccountAndBooksTrialLesson() throws IOException {
    if (!Env.flag("RUN_SIDE_EFFECT_E2E")) {
      throw new SkipException("Creates real stage entities; set RUN_SIDE_EFFECT_E2E=true to opt in");
    }

    TestUser user = TestUser.unique();
    Path artifacts = Path.of(
        "test-results", MessageFormat.format("charlie-{0}", Instant.now().toEpochMilli()));
    Files.createDirectories(artifacts);

    try (Playwright playwright = Playwright.create()) {
      Browser browser = playwright.chromium().launch(BrowserTypeLaunchOptionsFactory.options());
      try (BrowserContext context = browser.newContext()) {
        Page page = context.newPage();
        NetworkEvidenceCollector network = new NetworkEvidenceCollector(page, user);
        QuizStepDriver driver = new QuizStepDriver(page, user);
        UiBusinessOutcomeVerifier verifier = new UiBusinessOutcomeVerifier(page);

        try {
          page.navigate(Env.value("CHARLIE_QUIZ_URL", Env.DEFAULT_URL));
          new QuizFlow(page, driver, verifier, Env.durationSeconds("CHARLIE_FLOW_TIMEOUT_SECONDS", 300))
              .complete();
          Assert.assertTrue(verifier.bookingObserved(), "Trial booking was not observed in the UI");
          Assert.assertTrue(verifier.accountObserved(), "Authenticated dashboard was not reached");
        } catch (Throwable failure) {
          persistDiagnostics(page, driver, network, artifacts);
          throw failure;
        } finally {
          browser.close();
        }
      }
    }
  }

  private static void persistDiagnostics(
      Page page, QuizStepDriver driver, NetworkEvidenceCollector network, Path artifacts) {
    try {
      page.screenshot(new Page.ScreenshotOptions()
          .setPath(artifacts.resolve("failure.png"))
          .setFullPage(true));
    } catch (RuntimeException ignored) {
      // Preserve the original test failure if the page/browser is already unavailable.
    }
    try {
      String details = MessageFormat.format(
          "URL: {0}{1}Step: {2}{1}Active step HTML:{1}{3}",
          page.url(),
          System.lineSeparator(),
          driver.currentStepName(),
          driver.shortenedActiveStepHtml());
      Files.writeString(artifacts.resolve("failure.txt"), details, StandardCharsets.UTF_8);
      network.writeTo(artifacts.resolve("network.txt"));
    } catch (IOException | RuntimeException ignored) {
      // Diagnostics are best effort and must not hide the original failure.
    }
  }
}
