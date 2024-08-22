package com.antithesis.sdk.random;

import com.antithesis.sdk.internal.Internal;

import java.util.Optional;

final public class Random {

    /**
     * Returns a long value chosen by Antithesis. You should not
     * store this value or use it to seed a PRNG, but should use it
     * immediately.
     *
     * @return Random value
     */
    public static long getRandom() {
        return Internal.dispatchRandom();
    }

    /**
     * Returns a randomly chosen item from a list of options. You
     * should not store this value, but should use it immediately.
     * <p>
     * This function is not purely for convenience. Signaling to
     * the Antithesis platform that you intend to use a random value
     * in a structured way enables it to provide more interesting
     * choices over time.
     *
     * @param array An array of items to select from
     * @param <T>   Type of the array member items
     * @return Selected item
     */
    public static <T> Optional<T> randomChoice(T[] array) {
        if (array.length == 0) {
            return Optional.empty();
        } else if (array.length == 1) {
            return Optional.of(array[0]);
        } else {
            // Safety: Result of modulo is always less than the divisor
            // and will always fit into an integer
            int idx = (int) Long.remainderUnsigned(getRandom(), array.length);
            return Optional.of(array[idx]);
        }
    }
}
