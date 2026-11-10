package com.antithesis.sdk;

import com.antithesis.sdk.internal.Internal;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * The Lifecycle class contains methods which inform the Antithesis
 * environment that particular test phases or milestones have been reached.
 */
final public class Lifecycle {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * Default constructor
     */
    public Lifecycle() {
    }

    /**
     * Indicates to Antithesis that setup has completed. Call this function when
     * your system and workload are fully initialized.
     * After this function is called, Antithesis will take a snapshot of your system
     * and begin <a href="https://antithesis.com/docs/environment/fault_injection/" target="_blank">injecting faults</a>.
     * <p>
     * Calling this function multiple times or from multiple processes will have no effect.
     * Antithesis will treat the first time any process called this function as
     * the moment that the setup was completed.
     *
     * A null details argument omits details; an explicit empty object is preserved.
     * Serialization failures report an {@code antithesis_error} and still emit setup
     * completion without details.
     *
     * @param details   additional details that provide greater context
     *                  for system setup. Evaluated at runtime.
     */
    public static void setupComplete(final ObjectNode details) {

        ObjectNode antithesisSetup = MAPPER.createObjectNode();
        antithesisSetup.put("status", "complete");

        ObjectNode setup_complete_data = MAPPER.createObjectNode();
        setup_complete_data.put("antithesis_setup", antithesisSetup);

        Internal.dispatchWithDetails(setup_complete_data, antithesisSetup, "details",
                details, "setup_complete", false);
    }

    /**
     * Indicates to Antithesis that a certain event has been reached. It sends a
     * structured log message to Antithesis that you may later use to aid debugging.
     * <p>
     * In addition to <code>details</code>, you also provide <code>rawName</code>,
     * which is the name of the event that you are logging.
     *
     * A null details argument produces an empty object payload. Serialization
     * failures report an {@code antithesis_error} and still emit the event with
     * an empty object payload.
     *
     * @param name the name of the event that is being logged
     * @param details   additional details that provide greater context 
     *                  for the lifecycle event.  Evaluated at runtime.
     */
    public static void sendEvent(final String name, final ObjectNode details) {
        // The name passes through verbatim — no trimming or substitution —
        // so every SDK emits the same packet for the same call.
        ObjectNode eventJson = MAPPER.createObjectNode();
        Internal.dispatchWithDetails(eventJson, eventJson, name, details, name, true);
    }

}
