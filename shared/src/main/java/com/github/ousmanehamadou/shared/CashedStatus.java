package com.github.ousmanehamadou.shared;

import java.io.Serializable;

public record CashedStatus(Status status, boolean done) implements Serializable {}
