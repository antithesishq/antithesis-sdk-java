package com.antithesis.sdk.assertions;

import com.fasterxml.jackson.databind.node.ObjectNode;

final public class Assertions {

    /**
     * Assert that <code>condition</code> is true every time this function is called, <i>and</i> that it is
     * called at least once. The corresponding test property will be viewable in the <code>Antithesis SDK: Always</code> group of your triage report.
     *
     * @param condition the result of evaluating the assertion at runtime
     * @param message the unique text associated with the assertion
     * @param details additional values describing the program state when the assertion was evaluated at runtime
     */
    public static void always(boolean condition, String message, ObjectNode details) {
        Assertion assertion = new Assertion("always", "Always", condition, message, details, true, true);
    }

    /**
     * Assert that <code>condition</code> is true every time this function is called. The corresponding 
     * test property will pass even if the assertion is never encountered.

     * The corresponding test property will be viewable in the <code>Antithesis SDK: Always</code> group of your triage report.
     *
     * @param condition the result of evaluating the assertion at runtime
     * @param message the unique text associated with the assertion
     * @param details additional values describing the program state when the assertion was evaluated at runtime
     */
    public static void alwaysOrUnreachable(boolean condition, String message, ObjectNode details) {
        Assertion assertion = new Assertion("always", "AlwaysOrUnreachable", condition, message, details, true, true);
    }

    /**
     * Assert that <code>condition</code> is true at least one time that this function was called.
     * (If the assertion is never encountered, the test property will therefore fail.)
     * This test property will be viewable in the <code>Antithesis SDK: Sometimes</code> group.
     *
     * @param condition the result of evaluating the assertion at runtime
     * @param message the unique text associated with the assertion
     * @param details additional values describing the program state when the assertion was evaluated at runtime
     */ 
    public static void sometimes(boolean condition, String message, ObjectNode details) {
        Assertion assertion = new Assertion("sometimes", "Sometimes", condition, message, details, true, true);
    }

    /**
     * Assert that a line of code is never reached.
     * The corresponding test property will fail if this method is ever called.
     * (If it is never called the test property will therefore pass.)
     * This test property will be viewable in the <code>Antithesis SDK: Reachability assertions</code> group.
     *
     * @param message the unique text associated with the assertion
     * @param details additional values describing the program state when the assertion was evaluated at runtime
     */ 
    public static void unreachable(String message, ObjectNode details) {
        Assertion assertion = new Assertion("reachability", "Unreachable", true, message, details, true, false);
    }

    /**
     * Assert that a line of code is reached at least once.
     * The corresponding test property will pass if this method is ever called.
     * (If it is never called the test property will therefore fail.)
     * This test property will be viewable in the <code>Antithesis SDK: Reachability assertions</code> group.
     *
     * @param message the unique text associated with the assertion
     * @param details additional values describing the program state when the assertion was evaluated at runtime
     */ 
    public static void reachable(String message, ObjectNode details) {
        Assertion assertion = new Assertion("reachability", "Reachable", true, message, details, true, true);
    }

    /**
     * This is a low-level method designed to be used by third-party frameworks.
     * Regular users of the assertions package should not call it.
     *
     * This is primarily intended for use by adapters from other
     * diagnostic tools that intend to output Antithesis-style
     * assertions.
     * 
     * Be certain to provide an assertion catalog entry
     * for each assertion issued with <code>assert_raw()</code>.  Assertion catalog
     * entries are also created using <code>assert_raw()</code>, by setting the value
     * of the <code>hit</code> parameter to false.
     *
     * Please refer to the general Antithesis documentation regarding the
     * use of the <a href="https://antithesis.com/docs/using_antithesis/sdk/fallback/assert.html" target="_blank">Fallback SDK</a>
     * for additional information.
     *
     * @param assertType must be one of "always", "sometimes", "reachability" 
     * @param displayType one of "Always", "AlwaysOrUnreachable", "Sometimes", "Reachable", "Unreachable"
     * @param className the name of the package and class containing this assertion
     * @param functionName the name of the method containing this assertion
     * @param fileName the name of the source file containing this assertion
     * @param begin_line the source line number where the assertion is located
     * @param begin_column the source column number where the assertion is located
     * @param id the unique text associated with the assertion
     * @param condition the result of evaluating the assertion at runtime
     * @param hit true if the assertion has been evaluated, false if the assertion is being added to the assertion catalog
     * @param message the unique text associated with the assertion
     * @param details additional values describing the program state when the assertion was evaluated at runtime
     * @param hit true if the assertion has been evaluated, false if the assertion is being added to the assertion catalog
     * @param mustHit true if the assertion is expected to be evaluated at least once, otherwise false
     */ 
    public static void rawAssert(
        String assertType,
        String displayType,
        String className,
        String functionName,
        String fileName,
        int begin_line,
        int begin_column,
        String id,
        boolean condition,
        String message,
        ObjectNode details,
        boolean hit,
        boolean mustHit
    ) {
      Assertion assertion = new Assertion(
          assertType,
          displayType,
          className,
          functionName,
          fileName,
          begin_line,
          begin_column,
          id,
          condition,
          message,
          details,
          hit,
          mustHit
      );
    } 
}
