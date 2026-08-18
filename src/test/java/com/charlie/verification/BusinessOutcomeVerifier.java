package com.charlie.verification;

import com.charlie.common.TestData;

public interface BusinessOutcomeVerifier {
  void observe();

  boolean complete();

  String status();

  void assertComplete(TestData user);
}
