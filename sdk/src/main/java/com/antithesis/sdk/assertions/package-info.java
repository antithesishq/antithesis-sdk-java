/**
 * The assertions package enables defining <a href="https://antithesis.com/docs/using_antithesis/properties.html" target="_blank">test properties</a>
 * about your program or <a href="https://antithesis.com/docs/getting_started/workload.html" target="_blank">workload</a>.
 * <p>
 * Each static method in this package takes a parameter called <code>message</code>, which is
 * a string literal identifier used to aggregate assertions.
 * Antithesis generates one test property per unique <code>message</code> This test property will be named <code>message</code> in the <a href="https://antithesis.com/docs/reports/triage.html" target="_blank">triage report</a>.
 * <p>
 * Each static method also takes a parameter called <code>details</code>, which is an <code>ObjectNode</code> reference of optional additional information provided by the user to add context for assertion failures.
 * The information that is logged will appear in the <code>logs</code> section of a <a href="https://antithesis.com/docs/reports/triage.html" target="_blank">triage report</a>.
 * Normally the values in <code>details</code> are evaluated at runtime.
 */
package com.antithesis.sdk.assertions;

