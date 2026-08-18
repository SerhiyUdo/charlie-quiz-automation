package com.charlie.quiz;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.PlaywrightException;
import com.microsoft.playwright.options.AriaRole;
import java.text.MessageFormat;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

final class QuizStepDriver {
  private static final Set<String> PROGRESS_ONLY = Set.of(
      "control-schedule", "child-device-advice", "speaking-clubs-info", "repeat-material",
      "telegram-bot");
  private static final Pattern PARENT = Pattern.compile(
      "(?i).*(parent|guardian|adult|mother|father|бать|дорос|мам|тат).*"
  );
  private static final Pattern TIME = Pattern.compile("^\\s*([01]?\\d|2[0-3]):[0-5]\\d\\s*$");

  private final Page page;
  private final TestUser user;
  private final int childAge;
  private final Set<String> selectedHobbies = new HashSet<>();

  QuizStepDriver(Page page, TestUser user) {
    this.page = page;
    this.user = user;
    this.childAge = Env.integer("CHARLIE_CHILD_AGE", 7);
  }

  /** Performs exactly one decision iteration and no more than one click. */
  void advanceOneIteration() {
    if (handleQuestionnaireOwnerModal()) {
      return;
    }

    Locator step = visibleStep();
    String name = step.getAttribute("data-step-name");
    if (name == null || name.isBlank()) {
      throw unsupported("Visible step has no data-step-name", step);
    }

    switch (name) {
      case "age-range" -> chooseAge(step);
      case "child-name" -> submitNamedForm(step, "input[name='name']", user.childName());
      case "user-info-name" -> submitNamedForm(step, "input[name='name']", user.parentName());
      case "user-info-phone" -> submitPhone(step);
      case "user-info-email" -> submitEmail(step);
      case "child-hobby" -> handleHobbies(step);
      case "lesson-time-select" -> handleBooking(step);
      default -> {
        if (PROGRESS_ONLY.contains(name)) {
          clickProgressCta(step);
        } else if (isSingleChoice(step)) {
          clickFirstEnabledChoice(step);
        } else {
          throw unsupported(MessageFormat.format("Unknown interaction primitive for step ''{0}''", name), step);
        }
      }
    }
  }

  String currentStepName() {
    Locator steps = page.locator("[data-step-name]:visible");
    return steps.count() == 0 ? "<none>" : String.valueOf(steps.first().getAttribute("data-step-name"));
  }

  String shortenedActiveStepHtml() {
    Locator steps = page.locator("[data-step-name]:visible");
    if (steps.count() == 0) {
      return "<no visible data-step-name>";
    }
    String html = redactUserData((String) steps.first().evaluate("element => element.outerHTML"));
    return html.length() <= 4_000 ? html : MessageFormat.format("{0}…", html.substring(0, 4_000));
  }

  private boolean handleQuestionnaireOwnerModal() {
    Locator dialog = page.getByRole(AriaRole.DIALOG).filter(
        new Locator.FilterOptions().setHas(page.getByText(Pattern.compile("(?i).*(questionnaire|анкет).*"))));
    if (dialog.count() == 0 || !dialog.first().isVisible()) {
      return false;
    }
    Locator choices = dialog.first().getByRole(AriaRole.BUTTON);
    for (int i = 0; i < choices.count(); i++) {
      Locator choice = choices.nth(i);
      if (choice.isVisible() && choice.isEnabled() && PARENT.matcher(choice.innerText()).matches()) {
        choice.click();
        return true;
      }
    }
    throw unsupported("Owner modal is visible but no semantic parent/guardian choice was found", dialog.first());
  }

  private Locator visibleStep() {
    Locator steps = page.locator("[data-step-name]:visible");
    if (steps.count() == 0) {
      throw new IllegalStateException(MessageFormat.format("No visible [data-step-name] at {0}", page.url()));
    }
    return steps.first();
  }

  private void chooseAge(Locator step) {
    Locator choices = enabledButtons(step);
    Pattern age = Pattern.compile(MessageFormat.format("(^|\\D){0}(\\D|$)", childAge));
    for (int i = 0; i < choices.count(); i++) {
      Locator choice = choices.nth(i);
      String text = choice.innerText();
      String value = String.valueOf(choice.getAttribute("data-value"));
      if (age.matcher(text).find() || Integer.toString(childAge).equals(value)) {
        choice.click();
        return;
      }
    }
    throw unsupported(MessageFormat.format(
        "No enabled age choice matched CHARLIE_CHILD_AGE={0}", childAge), step);
  }

  private void submitNamedForm(Locator step, String selector, String value) {
    Locator input = step.locator(MessageFormat.format("{0}:visible", selector));
    requireOne(input, MessageFormat.format("expected visible field {0}", selector), step).fill(value);
    clickSubmit(step);
  }

  private void submitPhone(Locator step) {
    Locator input = step.locator("input[type='tel']:visible");
    if (input.count() == 0) {
      input = step.locator("input[name*='phone' i]:visible, input[autocomplete='tel']:visible");
    }
    requireOne(input, "expected visible phone field", step).fill(user.phone());
    clickSubmit(step);
  }

  private void submitEmail(Locator step) {
    Locator input = step.locator("input[name='email']:visible, input[autocomplete='email']:visible, input[type='email']:visible");
    requireOne(input, "expected visible email field", step).fill(user.email());
    clickSubmit(step);
  }

  private void handleHobbies(Locator step) {
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
    throw unsupported("No new hobby option and continue CTA is still disabled", step);
  }

  private void handleBooking(Locator step) {
    Locator booking = step.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions()
        .setName(Pattern.compile("(?i).*(book|reserve|заброн|брон).*")));
    if (booking.count() > 0 && booking.first().isVisible() && booking.first().isEnabled()
        && hasSelectedEnabledTime(step)) {
      booking.first().click();
      return;
    }

    Locator buttons = enabledButtons(step);
    for (int i = 0; i < buttons.count(); i++) {
      Locator candidate = buttons.nth(i);
      if (TIME.matcher(candidate.innerText()).matches()) {
        candidate.click();
        return;
      }
    }
    throw unsupported("Booking step has no enabled HH:mm slot or enabled booking CTA", step);
  }

  private boolean hasSelectedEnabledTime(Locator step) {
    Locator selected = step.locator("button[aria-pressed='true']:not([disabled]), button[aria-selected='true']:not([disabled]), button[data-selected='true']:not([disabled]), button.selected:not([disabled]), button[class*='active' i]:not([disabled]), button[class*='selected' i]:not([disabled])");
    for (int i = 0; i < selected.count(); i++) {
      if (selected.nth(i).isVisible() && TIME.matcher(selected.nth(i).innerText()).matches()) {
        return true;
      }
    }
    return false;
  }

  private void clickProgressCta(Locator step) {
    Locator submit = findSubmit(step);
    if (submit.count() == 0 || !submit.first().isEnabled()) {
      throw unsupported("Progress screen has no enabled scoped CTA", step);
    }
    submit.first().click();
  }

  private boolean isSingleChoice(Locator step) {
    return step.locator("input:visible, textarea:visible, select:visible").count() == 0
        && enabledButtons(step).count() >= 2;
  }

  private void clickFirstEnabledChoice(Locator step) {
    Locator buttons = enabledButtons(step);
    for (int i = 0; i < buttons.count(); i++) {
      Locator button = buttons.nth(i);
      if (!looksLikeSubmit(button)) {
        button.click();
        return;
      }
    }
    throw unsupported("Choice screen has no enabled answer button", step);
  }

  private void clickSubmit(Locator step) {
    Locator submit = findSubmit(step);
    if (submit.count() == 0 || !submit.first().isEnabled()) {
      throw unsupported("Form has no enabled submit/continue button", step);
    }
    submit.first().click();
  }

  private Locator findSubmit(Locator step) {
    Locator explicit = step.locator("button[type='submit']:visible:not([disabled])");
    if (explicit.count() > 0) {
      return explicit;
    }
    return step.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions()
        .setName(Pattern.compile("(?i).*(continue|next|submit|далі|продовж).*")));
  }

  private Locator enabledButtons(Locator step) {
    return step.locator("button[type='button']:visible:not([disabled]), button:not([type]):visible:not([disabled])");
  }

  private boolean looksLikeSubmit(Locator button) {
    String text = button.innerText().toLowerCase(Locale.ROOT);
    return text.matches(".*(continue|next|submit|далі|продовж|book|reserve|заброн).*" );
  }

  private String stableKey(Locator locator, int index) {
    String value = locator.getAttribute("data-value");
    return value != null ? value : MessageFormat.format("{0}:{1}", index, locator.innerText().strip());
  }

  private String redactUserData(String text) {
    return text.replace(user.email(), "[REDACTED_EMAIL]").replace(user.phone(), "[REDACTED_PHONE]");
  }

  private Locator requireOne(Locator locator, String message, Locator step) {
    if (locator.count() == 0) {
      throw unsupported(message, step);
    }
    return locator.first();
  }

  private IllegalStateException unsupported(String message, Locator scope) {
    String html;
    try {
      html = (String) scope.evaluate("element => element.outerHTML");
    } catch (PlaywrightException ignored) {
      html = "<unavailable>";
    }
    if (html.length() > 1_500) {
      html = MessageFormat.format("{0}…", html.substring(0, 1_500));
    }
    return new IllegalStateException(MessageFormat.format("{0} at {1}\n{2}", message, page.url(), html));
  }
}
