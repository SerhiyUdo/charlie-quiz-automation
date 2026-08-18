package com.charlie.tests;

import com.charlie.common.Env;
import com.charlie.common.TestData;
import com.charlie.listeners.NetworkEvidenceCollector;
import com.charlie.pages.DashboardPage;
import com.charlie.pages.QuizPage;
import com.charlie.steps.BookingSteps;
import com.charlie.steps.QuizSteps;
import com.charlie.steps.RegistrationSteps;
import com.charlie.verification.BusinessOutcomeVerifier;
import com.charlie.verification.UiBusinessOutcomeVerifier;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.time.Instant;
import org.testng.ITestResult;
import org.testng.SkipException;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;

public abstract class BaseUiTest {
  protected QuizSteps quizSteps;
  protected BusinessOutcomeVerifier businessOutcomeVerifier;

  private Playwright playwright;
  private Browser browser;
  private BrowserContext context;
  private QuizPage quizPage;
  private NetworkEvidenceCollector networkEvidence;

  @BeforeMethod(alwaysRun = true)
  public void setUpBrowser() {
    if (!Env.flag("RUN_SIDE_EFFECT_E2E")) {
      throw new SkipException("Creates real stage entities; set RUN_SIDE_EFFECT_E2E=true to opt in");
    }

    playwright = Playwright.create();
    browser = playwright.chromium().launch(
        new BrowserType.LaunchOptions().setHeadless(!Env.flag("CHARLIE_HEADED")));
    context = browser.newContext();
    Page page = context.newPage();

    quizPage = new QuizPage(page);
    DashboardPage dashboardPage = new DashboardPage(page);
    networkEvidence = new NetworkEvidenceCollector(page);
    businessOutcomeVerifier = new UiBusinessOutcomeVerifier(quizPage, dashboardPage);
    RegistrationSteps registrationSteps = new RegistrationSteps(quizPage.currentStep());
    BookingSteps bookingSteps = new BookingSteps(quizPage.booking());
    quizSteps = new QuizSteps(
        quizPage, registrationSteps, bookingSteps, businessOutcomeVerifier, networkEvidence);
  }

  @AfterMethod(alwaysRun = true)
  public void collectFailureEvidenceAndClose(ITestResult result) {
    if (result.getStatus() == ITestResult.FAILURE && quizPage != null) {
      persistDiagnostics();
    }
    if (context != null) {
      context.close();
    }
    if (browser != null) {
      browser.close();
    }
    if (playwright != null) {
      playwright.close();
    }
  }

  private void persistDiagnostics() {
    Path artifacts = Path.of(
        "test-results", MessageFormat.format("charlie-{0}", Instant.now().toEpochMilli()));
    try {
      Files.createDirectories(artifacts);
      quizPage.screenshot(artifacts.resolve("failure.png"));
    } catch (IOException | RuntimeException ignored) {
      // Diagnostics are best effort and must not hide the original failure.
    }

    try {
      TestData user = quizSteps.testData();
      String details = MessageFormat.format(
          "URL: {0}{1}Step: {2}{1}Active step HTML:{1}{3}",
          quizPage.url(),
          System.lineSeparator(),
          quizPage.diagnosticStepName(),
          user == null ? "<test data unavailable>" : quizPage.shortenedActiveStepHtml(user));
      Files.writeString(artifacts.resolve("failure.txt"), details, StandardCharsets.UTF_8);
      networkEvidence.writeTo(artifacts.resolve("network.txt"));
    } catch (IOException | RuntimeException ignored) {
      // Diagnostics are best effort and must not hide the original failure.
    }
  }
}
