package com.charlie.pages;

import com.microsoft.playwright.Page;
import java.util.regex.Pattern;

public final class DashboardPage {
  private static final Pattern DASHBOARD = Pattern.compile("/app/dashboard(?:[/?#]|$)");

  private final Page page;

  public DashboardPage(Page page) {
    this.page = page;
  }

  public boolean isOpen() {
    return DASHBOARD.matcher(page.url()).find();
  }
}
