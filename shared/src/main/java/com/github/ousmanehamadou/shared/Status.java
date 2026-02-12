package com.github.ousmanehamadou.shared;

import java.io.Serializable;

public sealed interface Status extends Serializable
    permits Status.Cancelled, Status.Cashed, Status.AwaitingPayout {
  record Cashed(String msg) implements Status {}

  record Cancelled(String msg) implements Status {}

  record AwaitingPayout(String msg) implements Status {}
}
