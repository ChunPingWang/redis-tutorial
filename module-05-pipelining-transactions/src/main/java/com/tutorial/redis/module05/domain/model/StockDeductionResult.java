package com.tutorial.redis.module05.domain.model;

/**
 * Represents the outcome of an atomic stock deduction executed via a Lua script.
 * The Lua script performs an atomic check-and-deduct to prevent overselling.
 */
public enum StockDeductionResult {

    /** Stock was successfully deducted. */
    SUCCESS,

    /** Deduction failed because the current stock is less than the requested quantity. */
    INSUFFICIENT_STOCK,

    /** Deduction failed because the stock key does not exist in Redis. */
    KEY_NOT_FOUND
}
