package com.charlie.pages;

import com.charlie.common.Env;
import com.charlie.common.TestData;
import com.charlie.sections.BookingSection;
import com.charlie.sections.QuizModalSection;
import com.charlie.sections.QuizStepSection;
import com.microsoft.playwright.Page;

import java.nio.file.Path;

public final class QuizPage {

    private final Page page;
    private final QuizStepSection currentStep;
    private final QuizModalSection modal;
    private final BookingSection booking;

    public QuizPage(Page page) {
        this.page = page;
        this.currentStep = new QuizStepSection(page);
        this.modal = new QuizModalSection(page);
        this.booking = new BookingSection(page);
    }

    public void open() {
        page.navigate(
                Env.value(
                        "CHARLIE_QUIZ_URL",
                        Env.DEFAULT_URL
                )
        );
    }

    public QuizStepSection currentStep() {
        return currentStep;
    }

    public QuizModalSection modal() {
        return modal;
    }

    public BookingSection booking() {
        return booking;
    }

    public boolean bookingConfirmationVisible() {
        return page.locator(
                "[data-step-name='telegram-bot']:visible"
        ).count() > 0
                && page.locator(
                "[data-test-lesson-date]:visible"
        ).count() > 0;
    }

    public boolean loginBookingConfirmationVisible() {
        if (!page.url().contains("/app/login")) {
            return false;
        }

        return page.getByText(
                "заявка на заняття вже надіслана",
                new Page.GetByTextOptions()
                        .setExact(false)
        ).count() > 0;
    }

    public boolean hasVisibleStep() {
        return page.locator("[data-step-name]:visible").count() > 0;
    }

    public void waitForStepTransition(String previousStep) {
        try {
            page.waitForFunction(
                    """
                    previous => {
                      const visible =
                        [...document.querySelectorAll('[data-step-name]')]
                          .find(e => {
                            const s = getComputedStyle(e);
                            return s.visibility !== 'hidden'
                              && s.display !== 'none';
                          });

                      return visible
                        && visible.dataset.stepName !== previous;
                    }
                    """,
                    previousStep,
                    new Page.WaitForFunctionOptions()
                            .setTimeout(5_000)
            );
        } catch (RuntimeException ignored) {
            // Multi-select and booking can legitimately
            // remain on the same step.
        }

        page.waitForTimeout(250);
    }

    public void waitForInteractionToSettle() {
        page.waitForTimeout(250);
    }

    public String url() {
        return page.url();
    }

    public String currentStepName() {
        return currentStep.name();
    }

    public String diagnosticStepName() {
        LocatorResult result = visibleStep();

        return result.exists()
                ? result.name()
                : "<none>";
    }

    public String shortenedActiveStepHtml(TestData user) {
        return currentStep.shortenedHtml(user);
    }

    public void screenshot(Path target) {
        page.screenshot(
                new Page.ScreenshotOptions()
                        .setPath(target)
                        .setFullPage(true)
        );
    }

    private LocatorResult visibleStep() {
        var steps = page.locator("[data-step-name]:visible");

        if (steps.count() == 0) {
            return new LocatorResult(false, null);
        }

        return new LocatorResult(
                true,
                String.valueOf(
                        steps.first().getAttribute("data-step-name")
                )
        );
    }

    private record LocatorResult(
            boolean exists,
            String name
    ) {
    }

    public boolean isQuizRoute() {
        return page.url().contains("/app/sign-up/");
    }
}
