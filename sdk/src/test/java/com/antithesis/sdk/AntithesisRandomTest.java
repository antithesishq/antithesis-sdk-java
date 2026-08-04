package com.antithesis.sdk;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class AntithesisRandomTest {
    @Test
    void setSeedThrows() {
        AntithesisRandom random = new AntithesisRandom();
        assertThrows(UnsupportedOperationException.class, () -> random.setSeed(42L));
    }

    @Test
    void defaultConstructorDoesNotThrow() {
        assertDoesNotThrow(() -> new AntithesisRandom());
    }

    @Test
    void nextGaussianIsStandardNormal() {
        final int samples = 200_000;
        AntithesisRandom random = new AntithesisRandom();

        double sum = 0.0;
        double sumSquares = 0.0;
        int within1 = 0;
        int within2 = 0;
        int within3 = 0;
        int positive = 0;
        int beyondTail = 0; // |x| > 3.442..., forcing the ziggurat fallback/tail path

        for (int i = 0; i < samples; i++) {
            double x = random.nextGaussian();
            assertFalse(Double.isNaN(x), "nextGaussian returned NaN");
            assertFalse(Double.isInfinite(x), "nextGaussian returned an infinite value");

            sum += x;
            sumSquares += x * x;
            double abs = Math.abs(x);
            if (abs <= 1.0) within1++;
            if (abs <= 2.0) within2++;
            if (abs <= 3.0) within3++;
            if (x > 0.0) positive++;
            if (abs > 3.442619855899) beyondTail++;
        }

        double mean = sum / samples;
        double variance = sumSquares / samples - mean * mean;
        double stdDev = Math.sqrt(variance);

        // The following assertions are probabilistic and therefore have some degree of inescapable flakiness. The
        // probability of a spurious failure ("p(F)"") is very low and noted next to each assertion (calculations
        // assume 200,000 samples). If you see failures more often than those probabilities indicate, there is probably
        // an issue with the random source or the implementation!

        // With 200k samples the standard error of the mean is ~0.0022
        assertEquals(0.0, mean, 0.05, "mean should be approximately 0"); // p(F): ~10^-110
        assertEquals(1.0, stdDev, 0.05, "standard deviation should be approximately 1"); // p(F): ~10^-219

        // Empirical rule for a standard normal: ~68.27% / ~95.45% / ~99.73%.
        assertEquals(0.6827, (double) within1 / samples, 0.02, "fraction within 1 stddev"); // p(F): ~10^-82
        assertEquals(0.9545, (double) within2 / samples, 0.02, "fraction within 2 stddev"); // p(F): ~10^-402
        assertEquals(0.9973, (double) within3 / samples, 0.01, "fraction within 3 stddev"); // p(F): ~10^-1615

        // Distribution should be roughly symmetric about 0.
        assertEquals(0.5, (double) positive / samples, 0.02, "fraction of positive values"); // p(F): ~10^-71

        // This is a better fit for an Antithesis sometimes assertion, but we wouldn't want that to show up in customer triage reports when they use the SDK
        assertTrue(beyondTail > 0, "expected some samples in the far tail to exercise the fallback path"); // p(F): ~10^-50
    }
}
