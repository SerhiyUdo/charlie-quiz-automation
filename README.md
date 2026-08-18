# Charlie registration quiz automation

A deliberately small, product-level smoke for the A/B-tested Charlie registration quiz. It uses Java 21, Playwright for Java, TestNG, and Gradle 8.14. The smoke proves, in one browser session, that an account reaches the authenticated dashboard and that a trial lesson booking was visibly confirmed.

## Part A — Manual testing approach

The detailed manual test strategy, including functional coverage, A/B testing, negative scenarios, security/abuse checks, prioritization, test data strategy, and exit criteria, is documented in [MANUAL_TEST_APPROACH.md](MANUAL_TEST_APPROACH.md).

## Part B — Automation implementation

### Automation strategy

The deterministic core is intentionally narrow: the starting URL, age (default `7`), generated user data, supported interaction capabilities, and two business-outcome signals. The driver does not freeze screen count, order, copy, dates, or times. Generic single-choice screens intentionally accept any valid enabled answer because the route through the funnel is not the subject of this smoke.

AI-driven interaction is not used in the blocking smoke. An AI classifier could help discover novel variants in a non-blocking canary, but allowing it to guess production interactions would make failures hard to reproduce and could create unintended stage entities. Visual layout and marketing copy are also intentionally unfrozen; those belong in focused visual/content checks, not this business-outcome smoke.

### A/B resilience

`QuizStepSection` is scoped to the currently visible `[data-step-name]` and exposes capabilities rather than modeling every quiz screen as a Page Object. `QuizSteps` owns the resilient flow loop, while `RegistrationSteps` and `BookingSteps` describe only genuinely special business interactions. Each loop iteration performs at most one click. That matters for answer cards that auto-advance: after a click, the flow returns to observation instead of blindly clicking a CTA from stale assumptions.

The page/section layer has explicit form, multi-select, progress, modal, single-choice, and booking capabilities. Unknown primitives fail with the URL and scoped HTML rather than falling back to an arbitrary visible button. The owner modal is handled before the underlying step. Hobby selections are remembered so the same pill is not toggled off. Booking chooses the first enabled `HH:mm` slot only when needed and books on a later iteration.

### Architecture

The architecture is JDI-Light-inspired without using JDI Light: `BaseUiTest` owns lifecycle and diagnostics, pages represent stable product surfaces, sections encapsulate Playwright locators, steps express the journey, and the verification layer owns the stateful product oracle. Playwright remains the only browser engine and the test itself stays intentionally thin.

A real stage run exposed a previously unseen informational `age-range-info` variant. Because progress screens are recognized by an enabled scoped progress control rather than a step-name whitelist, variants like this can flow through without adding screen-specific code.

### Business oracle

Booking requires an observed booking-specific UI signal: either simultaneous visibility of `data-step-name="telegram-bot"` and `[data-test-lesson-date]`, or the observed login-page confirmation that the lesson request was sent. The account signal includes the same browser session reaching `/app/dashboard`. The verifier remembers booking evidence across later navigation; the dashboard URL alone is never treated as proof of booking.

This is a product-level UI oracle because the assignment exposes no supported backend lookup. A read-only backend or system-of-record query would be stronger for independently verifying the persisted account and booking, and should replace or supplement these signals if the product team provides one. No undocumented API is invented here.

### CI/CD strategy

The GitHub Actions workflow uses Java 21 and Gradle 8.14, installs Playwright Chromium, and runs only through explicit manual dispatch with destructive opt-in. There is no scheduled trigger, so CI does not automatically create stage accounts or bookings. The destructive job is serialized to reduce collisions, and failure evidence is retained for seven days. The test itself still requires `RUN_SIDE_EFFECT_E2E=true`, so a plain local or CI `gradle test` safely skips it.

### Risks and limitations

- The smoke creates real stage accounts and bookings; stage needs a cleanup/TTL policy.
- A UI success screen can disagree with persistence, hence the recommendation for a supported read-only oracle.
- The generated email domain or Albanian phone format may be rejected by a future validation rule; both are configurable.
- A materially new interaction primitive correctly fails until its contract is reviewed and added.
- Localization can change semantic modal/CTA labels. Stable test hooks for primitive type and actions would further reduce copy coupling.
- Stage availability, phone policy, capacity, and rate limiting can cause non-product failures.

### Why Option 1

Option 1 tests the highest-value user journey and exercises the unstable quiz without encoding its current choreography. It gives a concise signal that the funnel still produces the two outcomes the business cares about.

### Why Java + Playwright

Java 21 matches the assignment and is common in mature QA stacks. Playwright supplies auto-waiting, isolated browser contexts, strong locators, network observation, and first-class Chromium support without a separate WebDriver service. TestNG keeps the opt-in skip and assertions straightforward.

### Run locally

Prerequisites: JDK 21. The Gradle wrapper is committed.

```bash
./gradlew test
```

The command above is safe and reports the smoke as skipped. Install Chromium and explicitly opt in to real stage writes:

Install Chromium once if needed:

```bash
./gradlew playwrightInstall
```

Run the destructive smoke explicitly.

PowerShell:

```powershell
$env:RUN_SIDE_EFFECT_E2E = "true"
./gradlew.bat test --tests "com.charlie.tests.QuizBusinessOutcomeTest"
```

Bash:

```bash
RUN_SIDE_EFFECT_E2E=true ./gradlew test --tests "com.charlie.tests.QuizBusinessOutcomeTest"
```

Configuration:

| Variable | Default | Purpose |
| --- | --- | --- |
| `RUN_SIDE_EFFECT_E2E` | `false` | Mandatory destructive-test opt-in |
| `CHARLIE_QUIZ_URL` | stage URL | Override target URL |
| `CHARLIE_CHILD_AGE` | `7` | Deterministic age choice |
| `CHARLIE_PHONE` | generated `+35569…` number | Override registration phone |
| `CHARLIE_EMAIL_DOMAIN` | `gmail.com` | Domain for unique generated email |
| `CHARLIE_FLOW_TIMEOUT_SECONDS` | `300` | End-to-end timeout |
| `CHARLIE_HEADED` | `false` | Show browser locally |

Failures write a screenshot, current URL, current step name, shortened active-step HTML, and recent XHR/fetch metadata under `test-results/`. Network evidence contains no bodies and redacts the generated email and configured phone from URLs.

### Assumptions

- `[data-step-name]` remains the stable screen boundary.
- `[data-test-lesson-date]` remains the stable booking confirmation hook.
- The dashboard redirect occurs in the registration browser session.
- Stage accepts unique generated emails for the configured domain.
- The supplied or generated Albanian phone is safe and valid for test use.

### With more time

I would add contract-level unit tests against small saved DOM fixtures for each interaction primitive, obtain a dedicated phone/email policy and cleanup mechanism, add stable `data-test` hooks for modal roles and CTAs, and integrate a supported read-only account/booking oracle. I would also baseline stage latency and tune retries only around demonstrated infrastructure failure modes.
