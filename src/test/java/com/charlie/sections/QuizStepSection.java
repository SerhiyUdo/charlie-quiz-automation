package com.charlie.sections;

import com.charlie.common.TestData;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.PlaywrightException;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.WaitForSelectorState;
import com.microsoft.playwright.options.WaitUntilState;

import java.net.URI;
import java.text.MessageFormat;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * The capability surface for whichever quiz step is currently visible.
 * Prefers [data-step-name], but can fall back to a visible form and URL-derived step name.
 */
public final class QuizStepSection {

    private static final Pattern SUBMIT_LABEL =
            Pattern.compile(
                    ".*(continue|next|submit|далі|продовж).*",
                    Pattern.CASE_INSENSITIVE
            );

    private final Page page;
    private final Set<String> selectedHobbies = new HashSet<>();

    public QuizStepSection(Page page) {
        this.page = page;
        this.page.setDefaultTimeout(10_000);
    }

    public String name() {
        Locator steps = page.locator("[data-step-name]:visible");

        if (steps.count() > 0) {
            String name = steps.first().getAttribute("data-step-name");

            if (name != null && !name.isBlank()) {
                return name;
            }
        }

        String path = URI.create(page.url()).getPath();
        String[] parts = path.split("/");

        for (int i = parts.length - 1; i >= 0; i--) {
            if (!parts[i].isBlank()) {
                return parts[i];
            }
        }

        throw new IllegalStateException(
                MessageFormat.format(
                        "Unable to determine current quiz step at {0}",
                        page.url()
                )
        );
    }

    public String shortenedHtml(TestData user) {
        Locator step = activeStep();

        String html = (String) step.evaluate(
                "element => element.outerHTML"
        );

        html = html
                .replace(user.email(), "[REDACTED_EMAIL]")
                .replace(user.phone(), "[REDACTED_PHONE]");

        return html.length() <= 4_000
                ? html
                : MessageFormat.format(
                "{0}…",
                html.substring(0, 4_000)
        );
    }

    public boolean isGenericChoice() {
        Locator step = activeStep();

        return step.locator(
                "input:visible, textarea:visible, select:visible"
        ).count() == 0
                && enabledButtons(step).count() >= 2;
    }

    public boolean hasEnabledProgressControl() {
        Locator progressControl = findSubmit(activeStep());

        return progressControl.count() > 0
                && progressControl.first().isVisible()
                && progressControl.first().isEnabled();
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

        throw unsupported(
                "Choice screen has no enabled answer button"
        );
    }

    public void continueProgress() {
        Locator submit = findSubmit(activeStep());

        if (submit.count() == 0
                || !submit.first().isVisible()
                || !submit.first().isEnabled()) {

            throw unsupported(
                    "Progress screen has no enabled scoped CTA"
            );
        }

        submit.first().click();
    }

    public void chooseAge(int childAge) {
        Locator step = activeStep();
        Locator choices = enabledButtons(step);

        Pattern age = Pattern.compile(
                MessageFormat.format(
                        "(^|\\D){0}(\\D|$)",
                        childAge
                )
        );

        for (int i = 0; i < choices.count(); i++) {
            Locator choice = choices.nth(i);

            String value = String.valueOf(
                    choice.getAttribute("data-value")
            );

            if (age.matcher(choice.innerText()).find()
                    || Integer.toString(childAge).equals(value)) {

                choice.click();
                return;
            }
        }

        throw unsupported(
                MessageFormat.format(
                        "No enabled age choice matched CHARLIE_CHILD_AGE={0}",
                        childAge
                )
        );
    }

    public void submitChildName(String childName) {
        fillAndSubmit(
                "input[name='name']",
                childName
        );
    }

    public void submitParentName(String parentName) {
        fillAndSubmit(
                "input[name='name']",
                parentName
        );
    }

    public void submitPhone(String phone) {
        Locator step = activeStep();

        Locator input = step.locator(
                "input[type='tel']:visible"
        );

        if (input.count() == 0) {
            input = step.locator(
                    "input[name*='phone' i]:visible, " +
                            "input[autocomplete='tel']:visible"
            );
        }

        Locator phoneInput = requireOne(
                input,
                "expected visible phone field"
        );

        phoneInput.click();
        phoneInput.fill("");

        String nationalPhone = phone.startsWith("+355")
                ? phone.substring(4)
                : phone;

        phoneInput.pressSequentially(
                nationalPhone,
                new Locator.PressSequentiallyOptions()
                        .setDelay(50)
        );

        phoneInput.press("Tab");

        Locator submit = findSubmit(step);

        page.waitForCondition(
                () -> submit.count() > 0
                        && submit.first().isVisible()
                        && submit.first().isEnabled()
        );

        submit.first().click();
    }

    public void submitEmail(String email) {
        Locator input = emailInput();

        if (!waitForEmailInput(input, 15_000)) {
            page.reload(
                    new Page.ReloadOptions()
                            .setWaitUntil(WaitUntilState.DOMCONTENTLOADED)
            );

            input = emailInput();

            if (!waitForEmailInput(input, 20_000)) {
                throw new IllegalStateException(
                        "Email step remained blank after one reload at " + page.url()
                );
            }
        }

        Locator emailInput = input.first();

        emailInput.fill(email);
        emailInput.press("Tab");

        page.waitForTimeout(300);

        Locator finishBooking = page.getByRole(
                AriaRole.BUTTON,
                new Page.GetByRoleOptions()
                        .setName(
                                Pattern.compile(
                                        ".*(завершити бронювання|finish.*booking|complete.*booking).*",
                                        Pattern.CASE_INSENSITIVE
                                )
                        )
        );

        if (finishBooking.count() > 0
                && finishBooking.first().isVisible()
                && finishBooking.first().isEnabled()) {

            finishBooking.first().click();
            return;
        }

        Locator submit = page.locator(
                "button[type='submit']:visible"
        );

        if (submit.count() == 0) {
            submit = page.getByRole(
                    AriaRole.BUTTON,
                    new Page.GetByRoleOptions()
                            .setName(SUBMIT_LABEL)
            );
        }

        Locator finalSubmit = submit;

        page.waitForCondition(
                () -> finalSubmit.count() > 0
                        && finalSubmit.first().isVisible()
                        && finalSubmit.first().isEnabled(),
                new Page.WaitForConditionOptions()
                        .setTimeout(30_000)
        );

        finalSubmit.first().click();
    }

    private Locator emailInput() {
        return page.locator(
                "input[name='email']:visible, " +
                        "input[autocomplete='email']:visible, " +
                        "input[type='email']:visible, " +
                        "input[placeholder*='mail' i]:visible"
        );
    }

    private boolean waitForEmailInput(
            Locator input,
            double timeoutMs
    ) {
        try {
            input.first().waitFor(
                    new Locator.WaitForOptions()
                            .setState(WaitForSelectorState.VISIBLE)
                            .setTimeout(timeoutMs)
            );

            return true;
        } catch (PlaywrightException ignored) {
            return false;
        }
    }

    public void selectHobbyOrContinue() {
        Locator step = activeStep();
        Locator continueButton = findSubmit(step);

        if (continueButton.count() > 0
                && continueButton.first().isEnabled()) {

            continueButton.first().click();
            return;
        }

        Locator buttons = enabledButtons(step);

        for (int i = 0; i < buttons.count(); i++) {
            Locator option = buttons.nth(i);
            String key = stableKey(option, i);

            if (!selectedHobbies.contains(key)
                    && !looksLikeSubmit(option)) {

                selectedHobbies.add(key);
                option.click();
                return;
            }
        }

        throw unsupported(
                "No new hobby option and continue CTA is still disabled"
        );
    }

    public IllegalStateException unsupported(String message) {
        String html;

        try {
            html = (String) activeStep().evaluate(
                    "element => element.outerHTML"
            );
        } catch (PlaywrightException ignored) {
            html = "<unavailable>";
        }

        if (html.length() > 1_500) {
            html = MessageFormat.format(
                    "{0}…",
                    html.substring(0, 1_500)
            );
        }

        return new IllegalStateException(
                MessageFormat.format(
                        "{0} at {1}\n{2}",
                        message,
                        page.url(),
                        html
                )
        );
    }

    private void fillAndSubmit(
            String selector,
            String value
    ) {
        Locator step = activeStep();

        Locator input = step.locator(
                MessageFormat.format(
                        "{0}:visible",
                        selector
                )
        );

        requireOne(
                input,
                MessageFormat.format(
                        "expected visible field {0}",
                        selector
                )
        ).fill(value);

        clickSubmit(step);
    }

    private void clickSubmit(Locator step) {
        Locator submit = findSubmit(step);

        if (submit.count() == 0
                || !submit.first().isEnabled()) {

            throw unsupported(
                    "Form has no enabled submit/continue button"
            );
        }

        submit.first().click();
    }

    private Locator findSubmit(Locator step) {
        Locator explicit = step.locator(
                "button[type='submit']:visible"
        );

        return explicit.count() > 0
                ? explicit
                : step.getByRole(
                AriaRole.BUTTON,
                new Locator.GetByRoleOptions()
                        .setName(SUBMIT_LABEL)
        );
    }

    private Locator enabledButtons(Locator step) {
        return step.locator(
                "button[type='button']:visible:not([disabled]), " +
                        "button:not([type]):visible:not([disabled])"
        );
    }

    private boolean looksLikeSubmit(Locator button) {
        String text = button.innerText()
                .toLowerCase(Locale.ROOT);

        return SUBMIT_LABEL.matcher(text).matches()
                || text.matches(
                ".*(book|reserve|заброн).*"
        );
    }

    private String stableKey(
            Locator locator,
            int index
    ) {
        String value = locator.getAttribute(
                "data-value"
        );

        return value != null
                ? value
                : MessageFormat.format(
                "{0}:{1}",
                index,
                locator.innerText().strip()
        );
    }

    private Locator requireOne(
            Locator locator,
            String message
    ) {
        if (locator.count() == 0) {
            throw unsupported(message);
        }

        return locator.first();
    }

    private Locator activeStep() {
        Locator steps = page.locator(
                "[data-step-name]:visible"
        );

        if (steps.count() > 0) {
            return steps.first();
        }

        Locator form = page.locator(
                "form:visible"
        );

        if (form.count() > 0) {
            return form.first();
        }

        throw new IllegalStateException(
                MessageFormat.format(
                        "No visible quiz step or form at {0}",
                        page.url()
                )
        );
    }
}
