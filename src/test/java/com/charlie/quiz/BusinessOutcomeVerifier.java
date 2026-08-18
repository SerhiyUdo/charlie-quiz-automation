package com.charlie.quiz;

interface BusinessOutcomeVerifier {
  void observe();

  boolean bookingObserved();

  boolean accountObserved();

  default boolean complete() {
    return bookingObserved() && accountObserved();
  }
}
