package com.antithesis.sdk;

import com.antithesis.sdk.internal.Internal;

import java.util.Optional;

/**
 * The Random class provides methods that request both structured and unstructured randomness from the Antithesis environment.
 * <p>
 * These methods should not be used to seed a conventional PRNG, and should not have their return values stored and used to make a decision at a later time.
 * Doing either of these things makes it much harder for the Antithesis platform to control the history of your program's execution, and also makes it harder for Antithesis to learn which inputs provided at which times are most fruitful.
 * Instead, you should call a method from the random class every time your program or <a href="https://antithesis.com/docs/getting_started/workload.html" target="_blank">workload</a> needs to make a decision, at the moment that you need to make the decision.
 * <p>
 * These methods are also safe to call outside the Antithesis environment, where
 * they will fall back on the Java standard class library implementation.
 */
final public class Random {

    /**
     * Default constructor
     */
    public Random() {
    }

    /**
     * Returns a value chosen by Antithesis. You should not
     * store this value or use it to seed a PRNG, but should use it
     * immediately.
     *
     * @return Random long integer
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
     * @return Randomly selected item from the provided array.
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
