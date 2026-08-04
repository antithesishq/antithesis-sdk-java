package com.antithesis.sdk;

/**
 * A subclass of java.util.Random that draws from {@link com.antithesis.sdk.Random}.
 */
public class AntithesisRandom extends java.util.Random {
    private boolean constructed;

    public AntithesisRandom() {
        constructed = true;
    }

    @Override
    protected int next(int bits) {
        if (bits <= 0) {
            return 0;
        }

        if (bits > 32) {
            bits = 32;
        }

        return (int)(Random.getRandom() >>> (64 - bits));
    }

    @Override
    public void setSeed(long seed) {
        if (constructed) {
            throw new UnsupportedOperationException();
        }
    }

    /**
     * Draws a standard-normal value using the Marsaglia-Tsang ziggurat method.
     * 
     * Unlike {@link java.util.Random#nextGaussian}, this is stateless to ensure that draws after
     * the Antithesis fuzzer has reseeded randomness reflect that new entropy.
     */
    @Override
    public double nextGaussian() {
        int hz = nextInt();
        int iz = hz & 127;
        if (Math.abs((long) hz) < ZIG_K[iz]) {
            return hz * ZIG_W[iz];
        }
        return nextGaussianFallback(hz, iz);
    }

    private double nextGaussianFallback(int hz, int iz) {
        for (;;) {
            double x = hz * ZIG_W[iz];
            if (iz == 0) {
                // Base strip: sample from the tail beyond ZIG_R.
                double y;
                do {
                    x = -StrictMath.log(nextOpenUnit()) * ZIG_INV_R;
                    y = -StrictMath.log(nextOpenUnit());
                } while (y + y < x * x);
                return (hz > 0) ? ZIG_R + x : -ZIG_R - x;
            }
            // Wedge between layer iz and the density curve.
            if (ZIG_F[iz] + nextOpenUnit() * (ZIG_F[iz - 1] - ZIG_F[iz]) < StrictMath.exp(-0.5 * x * x)) {
                return x;
            }
            // Rejected: draw again and retry.
            hz = nextInt();
            iz = hz & 127;
            if (Math.abs((long) hz) < ZIG_K[iz]) {
                return hz * ZIG_W[iz];
            }
        }
    }

    /** A uniform value in the open interval (0, 1), so StrictMath.log stays finite and nonzero. */
    private double nextOpenUnit() {
        return ((nextLong() >>> 11) + 0.5) * 0x1.0p-53;
    }

    // Marsaglia & Tsang, "The Ziggurat Method for Generating Random Variables" (2000),
    // 128-layer tables. Computed once from the published recurrence rather than transcribed
    // as magic numbers. Immutable after class init, so shared safely across instances/threads.
    private static final double ZIG_R = 3.442619855899;   // right-tail boundary
    private static final double ZIG_INV_R = 1.0 / ZIG_R;
    private static final long[] ZIG_K = new long[128];
    private static final double[] ZIG_W = new double[128];
    private static final double[] ZIG_F = new double[128];
    static {
        final double m1 = 2147483648.0;          // 2^31: candidate values are 32-bit signed
        final double vn = 9.91256303526217e-3;   // area of each ziggurat layer
        double dn = ZIG_R, tn = ZIG_R;
        double q = vn / StrictMath.exp(-0.5 * dn * dn);
        ZIG_K[0] = (long) ((dn / q) * m1);
        ZIG_K[1] = 0;
        ZIG_W[0] = q / m1;
        ZIG_W[127] = dn / m1;
        ZIG_F[0] = 1.0;
        ZIG_F[127] = StrictMath.exp(-0.5 * dn * dn);
        for (int i = 126; i >= 1; i--) {
            dn = StrictMath.sqrt(-2.0 * StrictMath.log(vn / dn + StrictMath.exp(-0.5 * dn * dn)));
            ZIG_K[i + 1] = (long) ((dn / tn) * m1);
            tn = dn;
            ZIG_F[i] = StrictMath.exp(-0.5 * dn * dn);
            ZIG_W[i] = dn / m1;
        }
    }
}
