package com.charlie.tests;

import com.charlie.common.TestData;
import org.testng.annotations.Test;

public class QuizBusinessOutcomeTest extends BaseUiTest {

  @Test(groups = "side-effect-e2e")
  public void quizCreatesAccountAndBooksTrial() {
    TestData user = TestData.unique();

    quizSteps.openQuiz();
    quizSteps.completeQuiz(user);

    businessOutcomeVerifier.assertComplete(user);
  }
}
