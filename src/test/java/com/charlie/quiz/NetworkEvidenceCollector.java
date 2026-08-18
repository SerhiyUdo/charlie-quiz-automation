package com.charlie.quiz;

import com.microsoft.playwright.Page;
import com.microsoft.playwright.Response;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Locale;
import java.util.Set;

final class NetworkEvidenceCollector {
  private static final int MAX_ENTRIES = 80;
  private static final Set<String> INTERESTING_TYPES = Set.of("xhr", "fetch");

  private final Deque<String> evidence = new ArrayDeque<>();
  private final TestUser user;

  NetworkEvidenceCollector(Page page, TestUser user) {
    this.user = user;
    page.onResponse(this::record);
  }

  private synchronized void record(Response response) {
    String resourceType = response.request().resourceType().toLowerCase(Locale.ROOT);
    if (!INTERESTING_TYPES.contains(resourceType)) {
      return;
    }
    String line = "%s %s %d %s".formatted(
        resourceType.toUpperCase(Locale.ROOT),
        response.request().method(),
        response.status(),
        redact(response.url()));
    if (evidence.size() == MAX_ENTRIES) {
      evidence.removeFirst();
    }
    evidence.addLast(line);
  }

  synchronized void writeTo(Path file) throws IOException {
    Files.writeString(file, String.join(System.lineSeparator(), evidence), StandardCharsets.UTF_8);
  }

  private String redact(String text) {
    String redacted = text.replace(user.email(), "[REDACTED_EMAIL]").replace(user.phone(), "[REDACTED_PHONE]");
    redacted = redacted.replaceAll("(?i)(email=)[^&]+", "$1[REDACTED]");
    return redacted.replaceAll("(?i)((?:phone|tel)=)[^&]+", "$1[REDACTED]");
  }
}
