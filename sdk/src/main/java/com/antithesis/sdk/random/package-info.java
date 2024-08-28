/**
 * The random package provides methods that request both structured and unstructured randomness from the Antithesis environment.
 * <p>
 * These methods should not be used to seed a conventional PRNG, and should not have their return values stored and used to make a decision at a later time.
 * Doing either of these things makes it much harder for the Antithesis platform to control the history of your program's execution, and also makes it harder for Antithesis to learn which inputs provided at which times are most fruitful.
 * Instead, you should call a method from the random package every time your program or <a href="https://antithesis.com/docs/getting_started/workload.html" target="_blank">workload</a> needs to make a decision, at the moment that you need to make the decision.
 * <p>
 * These methods are also safe to call outside the Antithesis environment, where
 * they will fall back on the Java standard class library implementation.
 */
package com.antithesis.sdk.random;
