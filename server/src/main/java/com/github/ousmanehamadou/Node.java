package com.github.ousmanehamadou;

import lombok.Builder;

@Builder(toBuilder = true)
public record Node(String ip, int port, String name) {}
