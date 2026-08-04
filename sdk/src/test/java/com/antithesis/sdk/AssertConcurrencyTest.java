package com.antithesis.sdk;

import com.antithesis.sdk.internal.CaptureSupport;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The de-duplication trackers use ConcurrentHashMap + AtomicInteger.  Hitting a
 * single assertion id concurrently from many threads must still emit exactly one
 * "pass" and one "fail".
 */
public class AssertConcurrencyTest {

    private final ObjectMapper mapper = new ObjectMapper();
    private CaptureSupport capture;

    @BeforeEach
    void setUp() {
        capture = CaptureSupport.install();
    }

    @AfterEach
    void cleanUp() {
        CaptureSupport.uninstall();
    }

    //TODO: port to Hegel and expand this simple test once Hegel is in use for SDK tests
    //Ref: https://github.corp.antithesis.com/antithesis/star/pull/2379#discussion_r5355
    @Test
    void concurrentPassesEmitExactlyOnce() throws InterruptedException {
        final int threads = 16;
        final int perThread = 100;
        final CountDownLatch start = new CountDownLatch(1);
        final CountDownLatch done = new CountDownLatch(threads);

        for (int t = 0; t < threads; t++) {
            new Thread(() -> {
                try {
                    start.await();
                    for (int i = 0; i < perThread; i++) {
                        Assert.always(true, "concurrent-pass", mapper.createObjectNode());
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            }).start();
        }

        start.countDown();
        done.await();

        assertEquals(1, capture.assertionsFor("concurrent-pass").size(),
                "a single passing id must be emitted exactly once even under concurrency");
    }

    @Test
    void concurrentPassAndFailEmitExactlyOncePerOutcome() throws InterruptedException {
        final int threads = 16;
        final int perThread = 100;
        final CountDownLatch start = new CountDownLatch(1);
        final CountDownLatch done = new CountDownLatch(threads);

        for (int t = 0; t < threads; t++) {
            final boolean condition = (t % 2 == 0);
            new Thread(() -> {
                try {
                    start.await();
                    for (int i = 0; i < perThread; i++) {
                        Assert.always(condition, "concurrent-mixed", mapper.createObjectNode());
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            }).start();
        }

        start.countDown();
        done.await();

        assertEquals(2, capture.assertionsFor("concurrent-mixed").size(),
                "one emission for the first pass and one for the first fail");
    }
}
