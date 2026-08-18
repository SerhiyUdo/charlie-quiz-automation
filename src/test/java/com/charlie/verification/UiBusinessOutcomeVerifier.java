package com.charlie.verification;

import com.charlie.common.TestData;
import com.charlie.pages.DashboardPage;
import com.charlie.pages.QuizPage;
import java.text.MessageFormat;
import org.testng.Assert;

public final class UiBusinessOutcomeVerifier implements BusinessOutcomeVerifier {
  private final QuizPage quizPage;
  private final DashboardPage dashboardPage;
  private boolean bookingObserved;
  private boolean accountObserved;

  public UiBusinessOutcomeVerifier(QuizPage quizPage, DashboardPage dashboardPage) {
    this.quizPage = quizPage;
    this.dashboardPage = dashboardPage;
  }

  @Override
  public void observe() {
    bookingObserved |= quizPage.bookingConfirmationVisible();
    accountObserved |= dashboardPage.isOpen();
  }

  @Override
  public boolean complete() {
    return bookingObserved && accountObserved;
  }

  @Override
  public String status() {
    return MessageFormat.format(
        "bookingObserved={0}, accountObserved={1}", bookingObserved, accountObserved);
  }

  @Override
  public void assertComplete(TestData user) {
    observe();
    Assert.assertTrue(bookingObserved, "Trial booking was not observed for the generated test user");
    Assert.assertTrue(accountObserved, "Authenticated dashboard was not reached for the generated test user");
  }

}
