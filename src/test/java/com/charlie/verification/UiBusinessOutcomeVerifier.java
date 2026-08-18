package com.charlie.verification;

import com.charlie.common.TestData;
import com.charlie.pages.DashboardPage;
import com.charlie.pages.QuizPage;
import org.testng.Assert;

import java.text.MessageFormat;

public final class UiBusinessOutcomeVerifier implements BusinessOutcomeVerifier {

    private final QuizPage quizPage;
    private final DashboardPage dashboardPage;

    private boolean bookingObserved;
    private boolean accountObserved;

    public UiBusinessOutcomeVerifier(
            QuizPage quizPage,
            DashboardPage dashboardPage
    ) {
        this.quizPage = quizPage;
        this.dashboardPage = dashboardPage;
    }

    @Override
    public void observe() {
        boolean dashboardOpen = dashboardPage.isOpen();
        boolean loginConfirmation = quizPage.loginBookingConfirmationVisible();
        boolean bookingConfirmation = quizPage.bookingConfirmationVisible();

        bookingObserved |=
                bookingConfirmation
                        || loginConfirmation;

        accountObserved |=
                dashboardOpen
                        || loginConfirmation;
    }

    @Override
    public boolean complete() {
        return bookingObserved && accountObserved;
    }

    @Override
    public String status() {
        return MessageFormat.format(
                "bookingObserved={0}, accountObserved={1}",
                bookingObserved,
                accountObserved
        );
    }

    @Override
    public void assertComplete(TestData user) {
        observe();

        Assert.assertTrue(
                bookingObserved,
                "Trial booking was not observed for the generated test user"
        );

        Assert.assertTrue(
                accountObserved,
                "Created account was not confirmed for the generated test user"
        );
    }
}
