package com.tutorial.redis.module08.domain.model;

import java.util.Objects;

/**
 * RPO (Recovery Point Objective) / RTO (Recovery Time Objective) analysis
 * for a given Redis persistence strategy.
 *
 * <p>Provides a qualitative assessment of each strategy's trade-offs:
 * <ul>
 *   <li>{@code strategy} — the persistence strategy name</li>
 *   <li>{@code rpoDescription} — how much data may be lost on failure</li>
 *   <li>{@code rtoDescription} — how long recovery takes</li>
 *   <li>{@code performanceImpact} — runtime performance cost</li>
 *   <li>{@code recommendedScenario} — ideal use case for this strategy</li>
 * </ul>
 *
 * <p>Non-final fields with getters/setters to support Jackson NON_FINAL default typing.</p>
 */
public class RpoRtoAnalysis {

    private String strategy;
    private String rpoDescription;
    private String rtoDescription;
    private String performanceImpact;
    private String recommendedScenario;

    /**
     * No-arg constructor for deserialization frameworks.
     */
    public RpoRtoAnalysis() {
    }

    /**
     * Creates an RpoRtoAnalysis with the specified values.
     *
     * @param strategy            the persistence strategy name
     * @param rpoDescription      qualitative RPO description
     * @param rtoDescription      qualitative RTO description
     * @param performanceImpact   performance impact level
     * @param recommendedScenario recommended use case scenario
     */
    public RpoRtoAnalysis(String strategy, String rpoDescription,
                          String rtoDescription, String performanceImpact,
                          String recommendedScenario) {
        this.strategy = Objects.requireNonNull(strategy, "strategy must not be null");
        this.rpoDescription = Objects.requireNonNull(rpoDescription, "rpoDescription must not be null");
        this.rtoDescription = Objects.requireNonNull(rtoDescription, "rtoDescription must not be null");
        this.performanceImpact = Objects.requireNonNull(performanceImpact, "performanceImpact must not be null");
        this.recommendedScenario = Objects.requireNonNull(recommendedScenario, "recommendedScenario must not be null");
    }

    public String getStrategy() {
        return strategy;
    }

    public void setStrategy(String strategy) {
        this.strategy = strategy;
    }

    public String getRpoDescription() {
        return rpoDescription;
    }

    public void setRpoDescription(String rpoDescription) {
        this.rpoDescription = rpoDescription;
    }

    public String getRtoDescription() {
        return rtoDescription;
    }

    public void setRtoDescription(String rtoDescription) {
        this.rtoDescription = rtoDescription;
    }

    public String getPerformanceImpact() {
        return performanceImpact;
    }

    public void setPerformanceImpact(String performanceImpact) {
        this.performanceImpact = performanceImpact;
    }

    public String getRecommendedScenario() {
        return recommendedScenario;
    }

    public void setRecommendedScenario(String recommendedScenario) {
        this.recommendedScenario = recommendedScenario;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RpoRtoAnalysis that)) return false;
        return Objects.equals(strategy, that.strategy);
    }

    @Override
    public int hashCode() {
        return Objects.hash(strategy);
    }

    @Override
    public String toString() {
        return "RpoRtoAnalysis{strategy='%s', rpo='%s', rto='%s', performance='%s', scenario='%s'}".formatted(
                strategy, rpoDescription, rtoDescription, performanceImpact, recommendedScenario);
    }
}
