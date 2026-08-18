package com.charlie.common;

import java.text.MessageFormat;
import java.time.Instant;
import java.util.UUID;

public record TestData(String email, String childName, String parentName, String phone) {
  public static TestData unique() {
    String suffix = MessageFormat.format(
        "{0}-{1}", Instant.now().toEpochMilli(), UUID.randomUUID().toString().substring(0, 8));
    String shortSuffix = suffix.substring(suffix.length() - 4);
    String domain = Env.value("CHARLIE_EMAIL_DOMAIN", "example.com");
    return new TestData(
        MessageFormat.format("charlie-smoke+{0}@{1}", suffix, domain),
        MessageFormat.format("Charlie {0}", shortSuffix),
        MessageFormat.format("QA Parent {0}", shortSuffix),
        Env.value("CHARLIE_PHONE", "+380501234567"));
  }
}
