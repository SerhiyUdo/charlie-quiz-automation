package com.charlie.common;

import java.time.Duration;
import java.util.Locale;

public final class Env {
  public static final String DEFAULT_URL =
      "https://stage.allright.com/uk/app/sign-up/long/charlie/age-range";

  private Env() {}

  public static String value(String name, String fallback) {
    String value = System.getenv(name);
    return value == null || value.isBlank() ? fallback : value.trim();
  }

  public static boolean flag(String name) {
    return "true".equals(value(name, "false").toLowerCase(Locale.ROOT));
  }

  public static int integer(String name, int fallback) {
    return Integer.parseInt(value(name, Integer.toString(fallback)));
  }

  public static Duration durationSeconds(String name, int fallback) {
    return Duration.ofSeconds(integer(name, fallback));
  }
}
