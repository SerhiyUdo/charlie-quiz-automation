package com.charlie.quiz;

import com.microsoft.playwright.BrowserType;

final class BrowserTypeLaunchOptionsFactory {
  private BrowserTypeLaunchOptionsFactory() {}

  static BrowserType.LaunchOptions options() {
    return new BrowserType.LaunchOptions().setHeadless(!Env.flag("CHARLIE_HEADED"));
  }
}
