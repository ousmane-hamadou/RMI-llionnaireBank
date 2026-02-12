package com.github.ousmanehamadou;

import com.github.ousmanehamadou.shared.MoneyOrder;
import com.github.ousmanehamadou.shared.Order;
import lombok.Builder;

@Builder(toBuilder = true)
public record RemoteOrder(Order order, MoneyOrder provider) {}

