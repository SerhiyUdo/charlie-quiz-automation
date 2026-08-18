package com.charlie.steps;

import com.charlie.sections.BookingSection;

public final class BookingSteps {
  private final BookingSection booking;

  public BookingSteps(BookingSection booking) {
    this.booking = booking;
  }

  public void selectTimeOrBookLesson() {
    booking.selectTimeOrBook();
  }
}
