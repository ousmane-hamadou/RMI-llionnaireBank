package com.github.ousmanehamadou;

import com.github.ousmanehamadou.shared.Order;
import java.util.concurrent.ConcurrentSkipListSet;

public record ActivityLog(ConcurrentSkipListSet<Order> orders) {}
