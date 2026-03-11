package com.skortati.fluxtrade_pipeline.model;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * This class maintains a fixed-size circular buffer to store numerical values
 * and updates its state using an index and count to track the buffer's usage.
 * It is designed to facilitate computations such as moving averages or similar
 * rolling statistics.
 *
 * @param window an array to hold the numerical data within the rolling window.
 * @param index an atomic integer representing the current position in the circular buffer.
 * @param count an atomic integer tracking the number of elements added to the buffer.
 */
public record RollingStats(double[] window, AtomicInteger index, AtomicInteger count) {
    public RollingStats(int size) {
        this(new double[size], new AtomicInteger(0), new AtomicInteger(0));
    }
}
