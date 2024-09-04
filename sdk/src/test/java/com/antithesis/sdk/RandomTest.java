package com.antithesis.sdk;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Vector;

import static org.junit.jupiter.api.Assertions.*;

public class RandomTest {
    @Test
    void randomNoChoice() {
        List<String> list = new ArrayList<>();
        assertNull(Random.randomChoice(list));
    }

    @Test
    void randomOneChoice() {
        List<String> list = new LinkedList<String>();
        list.add("ABc");
        assertEquals(Random.randomChoice(list), "ABc");
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

        List<Character> all_keys = new Vector<>(counted_items.keySet());
        assertEquals(counted_items.size(), all_keys.size());
        for (int i = 0; i < 25; i++) {
            Character choice = Random.randomChoice(all_keys);
            if (choice != null) {
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
