package com.github.ousmanehamadou.shared;

import java.io.Serializable;

import lombok.Builder;
import lombok.With;

@Builder(toBuilder = true)
public record Order(int ref, String by, String to, Status status, int amount)
    implements Serializable {}
