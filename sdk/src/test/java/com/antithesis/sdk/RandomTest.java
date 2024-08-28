package com.antithesis.sdk;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class RandomTest {
    @Test
    void randomNoChoice() {
        String[] arr = {};
        assertEquals(Random.randomChoice(arr), Optional.empty());
    }

    @Test
    void randomOneChoice() {
        String[] arr = {"ABc"};
        assertEquals(Random.randomChoice(arr), Optional.of("ABc"));
    }

    @Test
    void randomFewChoices() {
        // For each map key, the value is the count of the number of
        // random_choice responses received matching that key
        HashMap<Character, Integer> counted_items = new HashMap<Character, Integer>();
        counted_items.put('a', 0);
        counted_items.put('b', 0);
        counted_items.put('c', 0);
        counted_items.put('d', 0);

        Character[] all_keys = counted_items.keySet().toArray(new Character[0]);
        assertEquals(counted_items.size(), all_keys.length);
        for (int i = 0; i < 25; i++) {
            Optional<Character> rc = Random.randomChoice(all_keys);
            if (rc.isPresent()) {
                Character choice = rc.get();
                counted_items.computeIfPresent(choice, (key, val) -> val + 1);
            }
        }
        counted_items.forEach((key, val) -> assertNotEquals(val, 0, String.format("Did not produce the choice: %c", key)));
    }

    @Test
    void getRandom10k() {
        HashSet<Long> random_numbers = new HashSet<Long>();
        for (int i = 0; i < 10_000; i++) {
            long rn = Random.getRandom();
            assertFalse(random_numbers.contains(rn));
            random_numbers.add(rn);
        }
    }
}
