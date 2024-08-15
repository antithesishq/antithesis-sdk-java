package com.antithesis.sdk.lifecycle;

import com.antithesis.sdk.internal.Internal;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

final public class Lifecycle {

    /**
     * Indicates to Antithesis that setup has completed. Call this function when
     * your system and workload are fully initialized.
     * After this function is called, Antithesis will take a snapshot of your system
     * and begin <a href="https://antithesis.com/docs/applications/reliability/fault_injection.html" target="_blank">injecting faults</a>.
     *
     * Calling this function multiple times or from multiple processes will have no effect.
     * Antithesis will treat the first time any process called this function as 
     * the moment that the setup was completed.
     *
     * @param details additional values describing the program state 
     * when the setupComplete was evaluated
     */
    public static void setupComplete(final ObjectNode details) {
        ObjectMapper mapper = new ObjectMapper();

        ObjectNode antithesis_setup = mapper.createObjectNode();
        antithesis_setup.put("status", "complete");
        antithesis_setup.put("details", details);

        ObjectNode setup_complete_data = mapper.createObjectNode();
        setup_complete_data.put("antithesis_setup", antithesis_setup);

        Internal.dispatchOutput(setup_complete_data);
    }

    /**
     * Indicates to Antithesis that a certain event has been reached. It sends a 
     * structured log message to Antithesis that you may later use to aid debugging.
     *
     * In addition to <code>details</code>, you also provide <code>rawName</code>, 
     * which is the name of the event that you are logging.
     *
     * @param rawName the name of the event that is being logged
     * @param details event values
     */
    public static void sendEvent(final String rawName, final ObjectNode details) {
        String name = rawName.trim();
        if (name.isEmpty()) {
            name = "anonymous";
        }


        ObjectMapper mapper = new ObjectMapper();
        ObjectNode json_event = mapper.createObjectNode();
        json_event.put(name, details);

        Internal.dispatchOutput(json_event);
    }

}
