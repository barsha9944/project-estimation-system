package com.projectestimation.backend.common.util;

import com.projectestimation.backend.common.enums.CurrencyCode;
import com.projectestimation.backend.common.exception.BadRequestException;

public final class CurrencyFormatter {

    private CurrencyFormatter() {
    }

    public static CurrencyCode requireCurrency(CurrencyCode currency) {
        if (currency == null) {
            throw new BadRequestException("Currency is required");
        }
        return currency;
    }

    public static double calculateEstimatedCost(double totalEffortHours, double hourlyRate) {
        return totalEffortHours * hourlyRate;
    }

    public static String formatAmount(double amount, CurrencyCode currency) {
        return requireCurrency(currency).name() + " " + String.format("%.2f", amount);
    }
}
