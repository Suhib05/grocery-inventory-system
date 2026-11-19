package com.grocery.support;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;

/** A clock tests can move forward by hand. */
public class MutableClock extends Clock {

    private Instant now;

    public MutableClock(LocalDate date) {
        this.now = date.atTime(12, 0).toInstant(ZoneOffset.UTC);
    }

    public void advance(Duration d) { now = now.plus(d); }

    @Override public ZoneId getZone() { return ZoneOffset.UTC; }
    @Override public Clock withZone(ZoneId zone) { return this; }
    @Override public Instant instant() { return now; }
}
