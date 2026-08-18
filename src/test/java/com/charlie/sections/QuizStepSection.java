package com.charlie.sections;

import com.charlie.common.TestData;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.PlaywrightException;
import com.microsoft.playwright.options.AriaRole;
import java.text.MessageFormat;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/** The capability surface for whichever [data-step-name] is currently visible. */
public final class QuizStepSection {
  private static final Pattern SUBMIT_LABEL =
      Pattern.compile("(?i).*(continue|next|submit|далі|продовж).*" );

  private final Page page;
  private final Set<String> selectedHobbies = new HashSet<>();

  public QuizStepSection(Page page) {
    this.page = page;
  }

  public String name() {
    Locator step = activeStep();
    String name = step.getAttribute("data-step-name");
    if (name == null || name.isBlank()) {
      throw unsupported("Visible step has no data-step-name");
    }
    return name;
  }

  public String shortenedHtml(TestData user) {
    Locator steps = page.locator("[data-step-name]:visible");
    if (steps.count() == 0) {
      return "<no visible data-step-name>";
    }
    String html = (String) steps.first().evaluate("element => element.outerHTML");
    html = html.replace(user.email(), "[REDACTED_EMAIL]").replace(user.phone(), "[REDACTED_PHONE]");
    return html.length() <= 4_000
        ? html
        : MessageFormat.format("{0}…", html.substring(0, 4_000));
  }

  public boolean isGenericChoice() {
    Locator step = activeStep();
    return step.locator("input:visible, textarea:visible, select:visible").count() == 0
        && enabledButtons(step).count() >= 2;
  }

  public void chooseGenericOption() {
    Locator step = activeStep();
    Locator buttons = enabledButtons(step);
    for (int i = 0; i < buttons.count(); i++) {
      Locator button = buttons.nth(i);
      if (!looksLikeSubmit(button)) {
        button.click();
        return;
      }
    }
    throw unsupported("Choice screen has no enabled answer button");
  }

  public void continueProgress() {
    Locator submit = findSubmit(activeStep());
    if (submit.count() == 0 || !submit.first().isEnabled()) {
      throw unsupported("Progress screen has no enabled scoped CTA");
    }
    submit.first().click();
  }

  public void chooseAge(int childAge) {
    Locator step = activeStep();
    Locator choices = enabledButtons(step);
    Pattern age = Pattern.compile(MessageFormat.format("(^|\\D){0}(\\D|$)", childAge));
    for (int i = 0; i < choices.count(); i++) {
      Locator choice = choices.nth(i);
      String value = String.valueOf(choice.getAttribute("data-value"));
      if (age.matcher(choice.innerText()).find() || Integer.toString(childAge).equals(value)) {
        choice.click();
        return;
      }
    }
    throw unsupported(MessageFormat.format(
        "No enabled age choice matched CHARLIE_CHILD_AGE={0}", childAge));
  }

  public void submitChildName(String childName) {
    fillAndSubmit("input[name='name']", childName);
  }

  public void submitParentName(String parentName) {
    fillAndSubmit("input[name='name']", parentName);
  }

  public void submitPhone(String phone) {
    Locator step = activeStep();
    Locator input = step.locator("input[type='tel']:visible");
    if (input.count() == 0) {
      input = step.locator("input[name*='phone' i]:visible, input[autocomplete='tel']:visible");
    }
    requireOne(input, "expected visible phone field").fill(phone);
    clickSubmit(step);
  }

  public void submitEmail(String email) {
    Locator input = activeStep().locator(
        "input[name='email']:visible, input[autocomplete='email']:visible, input[type='email']:visible");
    requireOne(input, "expected visible email field").fill(email);
    clickSubmit(activeStep());
  }

  public void selectHobbyOrContinue() {
    Locator step = activeStep();
    Locator continueButton = findSubmit(step);
    if (continueButton.count() > 0 && continueButton.first().isEnabled()) {
      continueButton.first().click();
      return;
    }

    Locator buttons = enabledButtons(step);
    for (int i = 0; i < buttons.count(); i++) {
      Locator option = buttons.nth(i);
      String key = stableKey(option, i);
      if (!selectedHobbies.contains(key) && !looksLikeSubmit(option)) {
        selectedHobbies.add(key);
        option.click();
        return;
      }
    }
    throw unsupported("No new hobby option and continue CTA is still disabled");
  }

  public IllegalStateException unsupported(String message) {
    String html;
    try {
      html = (String) activeStep().evaluate("element => element.outerHTML");
    } catch (PlaywrightException ignored) {
      html = "<unavailable>";
    }
    if (html.length() > 1_500) {
      html = MessageFormat.format("{0}…", html.substring(0, 1_500));
    }
    return new IllegalStateException(MessageFormat.format("{0} at {1}\n{2}", message, page.url(), html));
  }

  private void fillAndSubmit(String selector, String value) {
    Locator step = activeStep();
    Locator input = step.locator(MessageFormat.format("{0}:visible", selector));
    requireOne(input, MessageFormat.format("expected visible field {0}", selector)).fill(value);
    clickSubmit(step);
  }

  private void clickSubmit(Locator step) {
    Locator submit = findSubmit(step);
    if (submit.count() == 0 || !submit.first().isEnabled()) {
      throw unsupported("Form has no enabled submit/continue button");
    }
    submit.first().click();
  }

  private Locator findSubmit(Locator step) {
    Locator explicit = step.locator("button[type='submit']:visible:not([disabled])");
    return explicit.count() > 0
        ? explicit
        : step.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName(SUBMIT_LABEL));
  }

  private Locator enabledButtons(Locator step) {
    return step.locator(
        "button[type='button']:visible:not([disabled]), button:not([type]):visible:not([disabled])");
  }

  private boolean looksLikeSubmit(Locator button) {
    return SUBMIT_LABEL.matcher(button.innerText().toLowerCase(Locale.ROOT)).matches()
        || button.innerText().toLowerCase(Locale.ROOT).matches(".*(book|reserve|заброн).*" );
  }

  private String stableKey(Locator locator, int index) {
    String value = locator.getAttribute("data-value");
    return value != null ? value : MessageFormat.format("{0}:{1}", index, locator.innerText().strip());
  }

  private Locator requireOne(Locator locator, String message) {
    if (locator.count() == 0) {
      throw unsupported(message);
    }
    return locator.first();
  }

  private Locator activeStep() {
    Locator steps = page.locator("[data-step-name]:visible");
    if (steps.count() == 0) {
      throw new IllegalStateException(MessageFormat.format(
          "No visible [data-step-name] at {0}", page.url()));
    }
    return steps.first();
  }
}
