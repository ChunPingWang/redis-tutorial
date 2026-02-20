package com.tutorial.redis.module14.finance.domain.service;

import com.tutorial.redis.module14.finance.domain.model.Transaction;

/**
 * Pure domain service for risk assessment logic.
 *
 * <p>Evaluates financial transactions against configurable thresholds
 * to determine risk levels. This service contains no infrastructure
 * dependencies and encapsulates core business rules for fraud detection.</p>
 *
 * <p>Severity thresholds:</p>
 * <ul>
 *   <li>{@code amount <= 1000} — LOW</li>
 *   <li>{@code 1000 < amount <= 5000} — MEDIUM</li>
 *   <li>{@code 5000 < amount <= 10000} — HIGH</li>
 *   <li>{@code amount > 10000} — CRITICAL</li>
 * </ul>
 */
public class RiskAssessmentService {

    private static final double HIGH_RISK_THRESHOLD = 10000.0;
    private static final double CRITICAL_THRESHOLD = 10000.0;
    private static final double HIGH_THRESHOLD = 5000.0;
    private static final double MEDIUM_THRESHOLD = 1000.0;

    /**
     * Determines whether a transaction is high-risk based on amount.
     *
     * @param tx the transaction to evaluate
     * @return {@code true} if the transaction amount exceeds the high-risk threshold (10000)
     */
    public boolean isHighRiskTransaction(Transaction tx) {
        return tx.getAmount() > HIGH_RISK_THRESHOLD;
    }

    /**
     * Determines the severity level based on the transaction amount.
     *
     * @param amount the transaction amount
     * @return one of "LOW", "MEDIUM", "HIGH", or "CRITICAL"
     */
    public String determineSeverity(double amount) {
        if (amount > CRITICAL_THRESHOLD) {
            return "CRITICAL";
        } else if (amount > HIGH_THRESHOLD) {
            return "HIGH";
        } else if (amount > MEDIUM_THRESHOLD) {
            return "MEDIUM";
        } else {
            return "LOW";
        }
    }
}
