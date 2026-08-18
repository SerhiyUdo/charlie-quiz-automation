package com.charlie.quiz;

import java.time.Duration;
import java.util.Locale;

final class Env {
  static final String DEFAULT_URL =
      "https://stage.allright.com/uk/app/sign-up/long/charlie/age-range";

  private Env() {}

  static String value(String name, String fallback) {
    String value = System.getenv(name);
    return value == null || value.isBlank() ? fallback : value.trim();
  }

  static boolean flag(String name) {
    return "true".equals(value(name, "false").toLowerCase(Locale.ROOT));
  }

  static int integer(String name, int fallback) {
    return Integer.parseInt(value(name, Integer.toString(fallback)));
  }

  static Duration durationSeconds(String name, int fallback) {
    return Duration.ofSeconds(integer(name, fallback));
  }
}
