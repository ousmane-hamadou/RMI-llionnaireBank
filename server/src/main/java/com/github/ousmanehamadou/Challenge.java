package com.github.ousmanehamadou;

import java.io.Serializable;


public record Challenge(long v, IDGenerator generator) implements Serializable {}
