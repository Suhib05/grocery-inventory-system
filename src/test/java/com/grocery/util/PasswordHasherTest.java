package com.grocery.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordHasherTest {

    // Low iteration count keeps the tests fast; production uses DEFAULT_ITERATIONS.
    private final PasswordHasher hasher = new PasswordHasher(1_000);

    @Test
    void correctPasswordVerifies() {
        assertTrue(hasher.verify("Secret#42", hasher.hash("Secret#42")));
    }

    @Test
    void wrongPasswordFails() {
        assertFalse(hasher.verify("secret#42", hasher.hash("Secret#42")));
    }

    @Test
    void samePasswordGetsDifferentSalts() {
        assertNotEquals(hasher.hash("Secret#42"), hasher.hash("Secret#42"));
    }

    @Test
    void storedIterationCountIsUsedWhenVerifying() {
        String strongHash = new PasswordHasher(2_000).hash("pw");
        assertTrue(hasher.verify("pw", strongHash));
    }

    @Test
    void malformedOrMissingHashesNeverMatch() {
        assertFalse(hasher.verify("pw", null));
        assertFalse(hasher.verify(null, hasher.hash("pw")));
        assertFalse(hasher.verify("pw", "not-a-hash"));
        assertFalse(hasher.verify("pw", "pbkdf2_sha256$abc$xx$yy"));
        assertFalse(hasher.verify("pw", "bcrypt$1000$AAAA$BBBB"));
    }

    @Test
    void rejectsNonPositiveIterations() {
        assertThrows(IllegalArgumentException.class, () -> new PasswordHasher(0));
    }
}
