package com.tutorial.redis.module14.finance.domain.model;

/**
 * Risk alert raised by the fraud detection system.
 *
 * <p>Represents an alert generated when suspicious activity is detected
 * on an account. Alerts are published to a Redis Stream for asynchronous
 * consumption by downstream risk-processing services.</p>
 */
public class RiskAlert {

    private String alertId;
    private String accountId;
    private String alertType;
    private String description;
    private String severity;
    private long timestamp;

    public RiskAlert() {
    }

    public RiskAlert(String alertId, String accountId, String alertType,
                     String description, String severity, long timestamp) {
        this.alertId = alertId;
        this.accountId = accountId;
        this.alertType = alertType;
        this.description = description;
        this.severity = severity;
        this.timestamp = timestamp;
    }

    public String getAlertId() {
        return alertId;
    }

    public void setAlertId(String alertId) {
        this.alertId = alertId;
    }

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public String getAlertType() {
        return alertType;
    }

    public void setAlertType(String alertType) {
        this.alertType = alertType;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "RiskAlert{alertId='" + alertId + "', accountId='" + accountId
                + "', alertType='" + alertType + "', description='" + description
                + "', severity='" + severity + "', timestamp=" + timestamp + '}';
    }
}
