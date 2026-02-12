package com.github.ousmanehamadou.shared;

import java.io.Serializable;

public record Pair(Order first, MoneyOrder second) implements Serializable {}
