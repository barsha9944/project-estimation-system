package com.projectestimation.backend.common.util;

import com.projectestimation.backend.common.exception.BadRequestException;

public final class CurrencyFormatter {

    private CurrencyFormatter() {
    }

    public static String normalizeCurrency(String currency) {
        if (currency == null || currency.isBlank()) {
            throw new BadRequestException("Currency is required");
        }
        String normalized = currency.trim().toUpperCase();
        if (normalized.length() != 3 || !normalized.matches("[A-Z]{3}")) {
            throw new BadRequestException("Currency must be a valid 3-letter ISO code");
        }
        return normalized;
    }

    public static double calculateEstimatedCost(double totalEffortHours, double hourlyRate) {
        return totalEffortHours * hourlyRate;
    }

    public static String formatAmount(double amount, String currency) {
        return normalizeCurrency(currency) + " " + String.format("%.2f", amount);
    }
}
