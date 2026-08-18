# Charlie Registration Quiz — Manual Testing Approach

## 1. Scope and Goal

The main purpose of manual testing is to verify that a user can complete the Charlie registration quiz successfully despite different A/B variants and that the flow produces the expected business result:

1. the user account is created;
2. a trial lesson is booked successfully.

Because the quiz is actively A/B tested, the exact number, order, wording, or visual layout of steps should not be treated as a fixed contract. The focus should be on the behavior of each interaction type, data validation, resilience, security, and the final business outcome.

---

## 2. Main Functional Areas

### 2.1 Entry and Navigation

Verify that:

- the quiz opens from the provided entry URL;
- the first interactive screen loads successfully;
- browser Back/Forward navigation does not corrupt the quiz state;
- refreshing a step either restores the current state or restarts the flow in a predictable way;
- no step results in an unexpected blank page or endless loading state;
- the user cannot become stuck without a valid way to continue.

Any blank page should be treated as a reliability failure rather than a normal experiment variation. The observed email-step example is covered in the Email section and the real-test-run notes below.

### 2.2 Single-Choice Quiz Steps

For screens where the user selects one answer, verify that:

- all expected choices are visible and clickable;
- disabled options cannot be selected;
- selecting an option records the choice;
- auto-advance happens only once;
- a double click does not accidentally answer the next question;
- the selected answer is preserved where product behavior requires it;
- different valid answers can lead to different A/B branches without breaking the flow.

The exact text or number of these screens should not be asserted globally because those elements are expected to change during experiments.

### 2.3 Informational / Progress Steps

For screens that explain something and provide a Continue/Next action, verify that:

- content renders successfully;
- the progress CTA is visible;
- the CTA is enabled at the correct time;
- one click moves the user forward;
- repeated clicks do not skip additional screens.

During testing, a previously unseen `age-range-info` variant appeared. This is a good example of why interaction type is more stable than an exact screen-name whitelist.

### 2.4 Multi-Select Steps

For screens such as child interests or hobbies, verify that:

- one or multiple options can be selected;
- selected options have a clear selected state;
- clicking an already selected option follows the intended toggle behavior;
- Continue remains disabled until the minimum selection requirement is satisfied;
- the user can continue once the requirement is met;
- selecting many or all available choices does not break layout or validation.

### 2.5 Modal / Overlay Behavior

Verify overlays independently from the underlying quiz screen:

- the modal appears at the correct point;
- the underlying page cannot accidentally receive clicks while the modal is open;
- all available choices work;
- selecting the parent/guardian path continues to the correct contact-data flow;
- the modal closes correctly after selection;
- keyboard interaction and focus do not remain trapped after closing;
- the modal does not repeatedly reappear after a valid choice.

During the real flow, both a parent/guardian questionnaire modal and a separate `Завершити бронювання` action were observed, so modal handling should not depend on one exact dialog only.

### 2.6 Child and Parent Name Validation

Check:

- valid alphabetic names;
- minimum and maximum supported length;
- spaces and compound names;
- Ukrainian and Latin characters where supported;
- empty value;
- digits;
- special characters;
- leading and trailing spaces;
- copy/paste;
- validation message behavior;
- Continue button enabled/disabled state.

The real flow showed that names containing digits were rejected, so this should be explicitly covered.

### 2.7 Phone Number

Verify:

- a valid phone number for the selected country;
- country selector and country code behavior;
- changing the country;
- national number with and without the country prefix;
- too short or too long number;
- letters and special characters;
- empty number;
- already registered number;
- formatting after input;
- validation on blur and submit;
- repeated registration attempts.

Because the flow creates actual stage entities, dedicated test numbers or dynamically generated valid test numbers should be used.

### 2.8 Email

Verify:

- valid email;
- uppercase/lowercase;
- subdomains;
- plus-addressing if supported by the product;
- dots, hyphens and other valid characters;
- missing `@`;
- missing domain;
- spaces;
- empty email;
- already registered email;
- very long email;
- validation on blur;
- validation on submit.

The email step itself should also be checked for reliable rendering across active experiment variants.

During execution, both a normally rendered email step and a variant that occasionally produced a blank page were observed. This is a real reliability risk rather than a theoretical case.

### 2.9 Lesson Scheduling

Verify the booking screen for:

- available days;
- different dayparts;
- available time slots;
- unavailable or disabled slots;
- changing the selected slot;
- preselected slot behavior;
- booking when a time is already selected;
- booking after manually selecting a time;
- a slot becoming unavailable between selection and confirmation;
- no available slots;
- repeated booking attempts;
- timezone and date correctness.

A particular date or time should not be hardcoded in manual or automated checks because availability is dynamic.

### 2.10 Successful Completion

After completing the quiz, verify that:

- trial booking is confirmed;
- booked date/time is shown correctly where applicable;
- the user reaches the expected authenticated state or dashboard;
- the booked lesson is visible in the user account;
- refreshing the dashboard does not lose the booking;
- logging out and logging back in shows the same user and booking.

UI confirmation alone is not enough for a high-confidence check. If a supported admin/API/database read interface is available, both the account and booking should also be verified as persisted in the backend.

---

## 3. Negative and Interruption Scenarios

Additional coverage should include:

- refresh on every important registration step;
- closing and reopening the browser;
- slow network;
- failed API request;
- API timeout;
- temporary 5xx response;
- repeated clicking;
- navigating backward after entering personal data;
- expired session;
- duplicated email;
- duplicated phone;
- booking conflict;
- no lesson availability;
- unexpected modal;
- newly introduced A/B step;
- blank/loading screen;
- browser resize and mobile viewport.

For network failures, the UI should either recover or present a meaningful retry/error state. It should not leave the user on an unexplained white screen.

---

## 4. Security and Abuse Scenarios

Because this flow creates an account, processes personal data, and books a real lesson, focused security and abuse testing should be included.

### 4.1 Session, Cookies and Client-Side State

Verify that:

- authentication/session cookies use appropriate `Secure`, `HttpOnly` and `SameSite` attributes;
- session identifiers are rotated after authentication where applicable;
- deleting or modifying non-security cookies does not incorrectly grant access or skip mandatory registration steps;
- changing experiment/A/B cookies does not expose an invalid or unauthorized state;
- changing client-side values in `localStorage` or `sessionStorage` cannot mark registration or booking as completed without server confirmation;
- copying session cookies into another browser context behaves according to the intended session policy;
- an expired or invalid session is rejected and does not expose another user's data;
- logout invalidates the authenticated session as expected.

For A/B experiments specifically, experiment cookies or feature flags should be modified manually where practical to switch between known variants and verify that every reachable variant still enforces the same backend business rules.

### 4.2 Request and Parameter Tampering

Using browser DevTools or an intercepting proxy, inspect registration requests and attempt controlled tampering of user-controlled fields such as:

- email;
- phone;
- child/parent name;
- selected age;
- quiz answers;
- lesson date/time;
- booking identifiers;
- user/account identifiers if exposed.

Example:

```text
UI email:
sergii-test-1@example.com

Modified request:
sergii-test-2@example.com
```

The server should consistently accept or reject the actual submitted value according to backend rules. The UI must not show one identity while the backend creates another without a clear reason.

Most importantly, changing identifiers must never allow a request to update, access or book something for another user.

### 4.3 Email and Phone Ownership / Duplicate Handling

Check:

- changing the email in the outgoing request after UI validation;
- changing a unique email to an already registered email;
- changing the phone to another existing account's phone;
- submitting email or phone values that were not validated by the UI;
- email case normalization;
- leading/trailing spaces;
- Unicode look-alike characters where relevant;
- duplicate requests sent in parallel;
- retrying the same registration request.

The backend must enforce uniqueness and validation independently from the browser. Client-side validation must never be the only protection.

### 4.4 Authorization / IDOR

If API requests expose identifiers such as:

```text
userId
childId
bookingId
lessonId
```

attempt replacing them with another valid-looking identifier.

Expected behavior is a safe rejection such as `403`, `404`, or the product's equivalent authorization response—never access to or modification of another user's account or booking.

This is particularly important after registration when the user reaches the authenticated dashboard.

### 4.5 Booking Manipulation

For lesson booking, test:

- changing selected lesson/time identifiers in the outgoing request;
- booking a disabled or unavailable slot;
- replaying a successful booking request;
- sending two identical booking requests concurrently;
- sending two different slots concurrently;
- modifying timezone/date values;
- modifying price or product-related fields if any are client controlled;
- attempting booking without completing required registration state.

The backend should remain the source of truth for availability and should prevent duplicate or conflicting bookings.

### 4.6 Replay and Duplicate Submission

Registration and booking endpoints should be checked for accidental duplicate side effects.

Examples:

```text
POST booking
POST booking
```

or two simultaneous requests.

A double click, browser refresh, timeout retry, or network retry must not unexpectedly create two trial bookings.

Where applicable, verify whether the API uses idempotency keys or another mechanism to safely detect duplicate operations.

### 4.7 Input Security

For text fields, include lightweight injection checks such as:

```text
<script>alert(1)</script>
"><img src=x onerror=alert(1)>
' OR '1'='1
${7*7}
```

The purpose is not to perform a full penetration test, but to verify that user input is treated as data, safely validated/encoded, and never reflected unsafely later on the dashboard, CRM/admin interface, or emails.

Stored XSS deserves particular attention because names entered during registration may later be rendered in multiple systems.

### 4.8 Sensitive Data Exposure

Using DevTools/network logs, verify that:

- passwords, tokens or session secrets are not placed in URLs;
- PII is not unnecessarily included in query strings;
- email/phone are not leaked to third-party analytics without an approved reason;
- API responses do not expose internal fields or unrelated user data;
- authentication tokens are not written to console logs;
- error messages do not expose stack traces, SQL errors, internal service names or secrets.

### 4.9 Rate Limiting and Automation Abuse

The registration flow should also be checked against simple abuse:

- many registration attempts from one client;
- repeated OTP/phone verification requests if present;
- rapid account creation;
- repeated trial booking;
- automated replay of registration requests.

Expected controls could include rate limiting, duplicate detection, CAPTCHA/risk checks, or backend throttling depending on product requirements.

---

## 5. A/B Testing Strategy

Variant coverage should be separated from business-flow coverage.

For every run, capture the experiment or variant ID if it is available through a cookie, response, DOM attribute, feature-flag payload, or analytics event.

Track results by variant, for example:

```text
Variant A -> completed -> account created -> trial booked
Variant B -> completed -> account created -> trial booked
Variant C -> ...
```

One successful run does not prove that every A/B branch works.

For newly introduced screens, first determine which interaction primitive they belong to:

- single choice;
- multi-select;
- information/progress;
- form;
- modal;
- scheduling;
- new/unknown interaction.

A new screen using an existing primitive normally requires additional exploratory coverage but should not require redesigning the whole test strategy.

---

## 6. Prioritization

### P0

- user cannot complete registration;
- account is not created;
- trial lesson is not booked;
- wrong user or booking is created;
- blank page or unrecoverable step;
- duplicate or unintended booking;
- authorization bypass;
- another user's data is accessible;
- session manipulation results in unauthorized access;
- request replay creates duplicate bookings.

### P1

- validation prevents legitimate users from continuing;
- an available lesson cannot be selected;
- modal blocks the journey;
- state is lost after refresh/back navigation;
- one A/B variant cannot complete;
- important security controls rely only on client-side validation.

### P2

- cosmetic differences;
- copy issues;
- minor layout problems;
- non-blocking visual inconsistencies.

This keeps testing focused on business impact instead of treating every quiz-text change as a regression.

---

## 7. Test Data and Side Effects

Because this flow creates real stage users and bookings, test data must be controlled.

Use:

- unique email per execution;
- dedicated or generated valid phone numbers;
- recognizable test-user naming;
- a test/automation marker where supported;
- cleanup API/job or TTL where available.

Repeatedly using the same account should be avoided because duplicate-user behavior can hide registration defects and make booking results ambiguous.

---

## 8. Exit Criteria

The flow can be considered ready when:

- all P0 scenarios pass;
- core validation and booking P1 scenarios pass;
- every currently active A/B variant observed during testing can reach the expected business outcome;
- no blocking blank/loading states remain unexplained;
- account creation and lesson booking are confirmed;
- no critical security or authorization issue is open;
- known issues are documented with severity and reproduction information.

---

## 9. Notes from the Real Test Run

During hands-on validation of the stage environment, several behaviors influenced the test strategy:

- an additional `age-range-info` informational A/B step appeared;
- the email step had more than one observed route/variant;
- one email-step variant occasionally rendered as a blank page and required a bounded reload recovery;
- a parent/guardian questionnaire modal appeared outside the normal step container;
- a separate `Завершити бронювання` modal could block the underlying email submit;
- booking could already have a valid time preselected;
- successful completion was observed through the final booking/dashboard flow.

These observations are why the automation implementation is capability-based and business-outcome-focused rather than tied to one fixed sequence of quiz screens.
