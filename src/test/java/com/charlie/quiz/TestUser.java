package com.charlie.quiz;

import java.time.Instant;
import java.text.MessageFormat;
import java.util.UUID;

record TestUser(String email, String childName, String parentName, String phone) {
  static TestUser unique() {
    String suffix = MessageFormat.format(
        "{0}-{1}", Instant.now().toEpochMilli(), UUID.randomUUID().toString().substring(0, 8));
    String domain = Env.value("CHARLIE_EMAIL_DOMAIN", "example.com");
    return new TestUser(
        MessageFormat.format("charlie-smoke+{0}@{1}", suffix, domain),
        MessageFormat.format("Charlie {0}", suffix.substring(suffix.length() - 4)),
        MessageFormat.format("QA Parent {0}", suffix.substring(suffix.length() - 4)),
        Env.value("CHARLIE_PHONE", "+380501234567"));
  }
}
