package com.charlie.sections;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;

import java.util.regex.Pattern;

public final class BookingSection {

    private static final Pattern TIME =
            Pattern.compile(
                    "^\\s*([01]?\\d|2[0-3]):[0-5]\\d\\s*$");

    private static final Pattern BOOK =
            Pattern.compile(
                    ".*(book|reserve|заброн|брон).*",
                    Pattern.CASE_INSENSITIVE
            );

    private final Page page;

    public BookingSection(Page page) {
        this.page = page;
    }

    public void selectTimeOrBook() {
        Locator step = bookingStep();

        Locator booking = step.getByRole(
                AriaRole.BUTTON,
                new Locator.GetByRoleOptions().setName(BOOK)
        );

        // If booking CTA is enabled, the application already considers
        // date/time selection valid.
        if (booking.count() > 0
                && booking.first().isVisible()
                && booking.first().isEnabled()) {
            booking.first().click();
            return;
        }

        Locator buttons = step.locator(
                "button[type='button']:visible:not([disabled]), " +
                        "button:not([type]):visible:not([disabled])"
        );

        for (int i = 0; i < buttons.count(); i++) {
            Locator candidate = buttons.nth(i);

            if (TIME.matcher(candidate.innerText()).matches()) {
                candidate.click();
                return;
            }
        }

        throw new IllegalStateException(
                "Booking step has no enabled HH:mm slot or enabled booking CTA"
        );
    }


    private Locator bookingStep() {
        Locator step = page.locator("[data-step-name='lesson-time-select']:visible");
        if (step.count() == 0) {
            throw new IllegalStateException("The lesson-time-select step is not visible");
        }
        return step.first();
    }
}
