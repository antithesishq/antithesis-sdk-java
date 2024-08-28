package com.antithesis.sdk;

import com.antithesis.sdk.internal.Internal;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * The Lifecycle class contains methods which inform the Antithesis
 * environment that particular test phases or milestones have been reached.
 */
final public class Lifecycle {

    /**
     * Default constructor
     */
    public Lifecycle() {
    }

    /**
     * Indicates to Antithesis that setup has completed. Call this function when
     * your system and workload are fully initialized.
     * After this function is called, Antithesis will take a snapshot of your system
     * and begin <a href="https://antithesis.com/docs/applications/reliability/fault_injection.html" target="_blank">injecting faults</a>.
     * <p>
     * Calling this function multiple times or from multiple processes will have no effect.
     * Antithesis will treat the first time any process called this function as
     * the moment that the setup was completed.
     *
     * @param details   additional details that provide greater context 
     *                  for system setup.  Evaluated at runtime.
     */
    public static void setupComplete(final ObjectNode details) {
        ObjectMapper mapper = new ObjectMapper();

        ObjectNode antithesisSetup = mapper.createObjectNode();
        antithesisSetup.put("status", "complete");
        antithesisSetup.put("details", details);

        ObjectNode setup_complete_data = mapper.createObjectNode();
        setup_complete_data.put("antithesis_setup", antithesisSetup);

        Internal.dispatchOutput(setup_complete_data);
    }

    /**
     * Indicates to Antithesis that a certain event has been reached. It sends a
     * structured log message to Antithesis that you may later use to aid debugging.
     * <p>
     * In addition to <code>details</code>, you also provide <code>rawName</code>,
     * which is the name of the event that you are logging.
     *
     * @param name the name of the event that is being logged
     * @param details   additional details that provide greater context 
     *                  for the lifecycle event.  Evaluated at runtime.
     */
    public static void sendEvent(final String name, final ObjectNode details) {
        String thisName = name.trim();
        if (thisName.isEmpty()) {
            thisName = "anonymous";
        }

        ObjectMapper mapper = new ObjectMapper();
        ObjectNode eventJson = mapper.createObjectNode();
        eventJson.put(thisName, details);

        Internal.dispatchOutput(eventJson);
    }

}
