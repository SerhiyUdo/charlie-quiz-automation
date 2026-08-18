package com.charlie.steps;

import com.charlie.common.Env;
import com.charlie.common.TestData;
import com.charlie.sections.QuizStepSection;

/** Business-specific registration interactions; ordinary quiz steps remain generic. */
public final class RegistrationSteps {
  private final QuizStepSection currentStep;

  public RegistrationSteps(QuizStepSection currentStep) {
    this.currentStep = currentStep;
  }

  public void chooseConfiguredAge() {
    currentStep.chooseAge(Env.integer("CHARLIE_CHILD_AGE", 7));
  }

  public void submitChildName(TestData user) {
    currentStep.submitChildName(user.childName());
  }

  public void submitParentName(TestData user) {
    currentStep.submitParentName(user.parentName());
  }

  public void submitPhone(TestData user) {
    currentStep.submitPhone(user.phone());
  }

  public void submitEmail(TestData user) {
    currentStep.submitEmail(user.email());
  }

  public void selectHobbyOrContinue() {
    currentStep.selectHobbyOrContinue();
  }
}
