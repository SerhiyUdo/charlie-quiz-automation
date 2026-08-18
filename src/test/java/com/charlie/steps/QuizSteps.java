package com.charlie.steps;

import com.charlie.common.Env;
import com.charlie.common.TestData;
import com.charlie.listeners.NetworkEvidenceCollector;
import com.charlie.pages.QuizPage;
import com.charlie.verification.BusinessOutcomeVerifier;
import java.text.MessageFormat;
import java.time.Duration;

public final class QuizSteps {
  private final QuizPage quizPage;
  private final RegistrationSteps registrationSteps;
  private final BookingSteps bookingSteps;
  private final BusinessOutcomeVerifier verifier;
  private final NetworkEvidenceCollector networkEvidence;
  private TestData testData;

  public QuizSteps(
      QuizPage quizPage,
      RegistrationSteps registrationSteps,
      BookingSteps bookingSteps,
      BusinessOutcomeVerifier verifier,
      NetworkEvidenceCollector networkEvidence) {
    this.quizPage = quizPage;
    this.registrationSteps = registrationSteps;
    this.bookingSteps = bookingSteps;
    this.verifier = verifier;
    this.networkEvidence = networkEvidence;
  }

  public void openQuiz() {
    quizPage.open();
  }

  public void completeQuiz(TestData user) {
    testData = user;
    networkEvidence.redact(user);
    Duration timeout = Env.durationSeconds("CHARLIE_FLOW_TIMEOUT_SECONDS", 300);
    long deadline = System.nanoTime() + timeout.toNanos();
    int iterations = 0;

    while (System.nanoTime() < deadline && iterations++ < 100) {
      verifier.observe();
      if (verifier.complete()) {
        return;
      }

      if (quizPage.modal().chooseParentIfVisible()) {
        quizPage.waitForInteractionToSettle();
        continue;
      }

      String beforeStep = quizPage.currentStepName();
      advanceOneIteration(user, beforeStep);
      quizPage.waitForStepTransition(beforeStep);
    }

    verifier.observe();
    if (!verifier.complete()) {
      throw new AssertionError(MessageFormat.format(
          "Business outcome incomplete after {0} iterations; {1}, url={2}, step={3}",
          iterations,
          verifier.status(),
          quizPage.url(),
          quizPage.currentStepName()));
    }
  }

  public TestData testData() {
    return testData;
  }

  /** Performs one capability decision and no more than one click. */
  private void advanceOneIteration(TestData user, String stepName) {
    switch (stepName) {
      case "age-range" -> registrationSteps.chooseConfiguredAge();
      case "child-name" -> registrationSteps.submitChildName(user);
      case "user-info-name" -> registrationSteps.submitParentName(user);
      case "user-info-phone" -> registrationSteps.submitPhone(user);
      case "user-info-email" -> registrationSteps.submitEmail(user);
      case "child-hobby" -> registrationSteps.selectHobbyOrContinue();
      case "lesson-time-select" -> bookingSteps.selectTimeOrBookLesson();
      default -> advanceGenericStep(stepName);
    }
  }

  private void advanceGenericStep(String stepName) {
    if (quizPage.currentStep().isGenericChoice()) {
      quizPage.currentStep().chooseGenericOption();
    } else if (quizPage.currentStep().hasEnabledProgressControl()) {
      quizPage.currentStep().continueProgress();
    } else {
      throw quizPage.currentStep().unsupported(
          MessageFormat.format("Unknown interaction primitive for step ''{0}''", stepName));
    }
  }
}
