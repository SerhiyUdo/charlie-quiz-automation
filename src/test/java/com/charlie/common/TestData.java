package com.charlie.common;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public record TestData(
        String email,
        String childName,
        String parentName,
        String phone
) {

    public static TestData unique() {
        String timestamp = String.valueOf(
                Instant.now().toEpochMilli()
        );

        String random = UUID.randomUUID()
                .toString()
                .replace("-", "")
                .substring(0, 6);

        String domain = Env.value(
                "CHARLIE_EMAIL_DOMAIN",
                "gmail.com"
        );

        String email =
                "charlieqa"
                        + timestamp
                        + random
                        + "@"
                        + domain;

        return new TestData(
                email,
                "Charlie",
                "Parent",
                Env.value(
                        "CHARLIE_PHONE",
                        generateAlbanianPhone()
                )
        );
    }

    private static String generateAlbanianPhone() {
        int subscriberNumber =
                ThreadLocalRandom.current()
                        .nextInt(
                                1_000_000,
                                10_000_000
                        );

        return "+35569" + subscriberNumber;
    }
}