package com.antithesis.sdk;

import java.util.Map;

import com.antithesis.sdk.internal.Assertion;
import com.antithesis.sdk.internal.Guidance;
import com.antithesis.sdk.internal.LocationInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * The Assert class enables defining <a href="https://antithesis.com/docs/using_antithesis/properties/" target="_blank">test properties</a>
 * about your program or <a href="https://antithesis.com/docs/getting_started/first_test/" target="_blank">workload</a>.
 * <p>
 * Each static method in this class takes a parameter called <code>message</code>, which is
 * a string literal identifier used to aggregate assertions.
 * Antithesis generates one test property per unique <code>message</code>.  This test property will be named <code>message</code> in the <a href="https://antithesis.com/docs/reports/triage/" target="_blank">triage report</a>.
 * <p>
 * Each static method also takes a parameter called <code>details</code>, which is an <code>ObjectNode</code> reference of optional additional information provided by the user to add context for assertion failures.
 * The information that is logged will appear in the <code>logs</code> section of a <a href="https://antithesis.com/docs/reports/triage/" target="_blank">triage report</a>.
 * Normally the values in <code>details</code> are evaluated at runtime.
 */
final public class Assert {

    /**
     * Default constructor
     */
    public Assert() {
    }

    /**
     * Assert that <code>condition</code> is true every time this function is called, <i>and</i> that it is
     * called at least once. The corresponding test property will be viewable in the <code>Antithesis SDK: Always</code> group of your triage report.
     *
     * @param condition the condition being asserted.  Evaluated at runtime.
     * @param message   a unique string identifier of the assertion. 
     *                  Provides context for assertion success/failure 
     *                  and is intended to be human-readable.  Must be 
     *                  provided as a string literal.
     * @param details   additional details that provide greater context 
     *                  for assertion success/failure.  Evaluated at runtime.
     */
    public static void always(final boolean condition, final String message, final ObjectNode details) {
        alwaysHelper(condition, message, details);
    }

    private static void alwaysHelper(final boolean condition, final String message, final ObjectNode details) {
        Assertion.builder()
                .assertType(AssertType.Always)
                .condition(condition)
                .details(details)
                .displayType("Always")
                .hit(true)
                .id(message)
                .location(Assertion.getLocationInfo(message))
                .message(message)
                .mustHit(true)
                .build()
                .trackEntry();
    }

    /**
     * Assert that <code>condition</code> is true every time this function is called. The corresponding
     * test property will pass even if the assertion is never encountered.
     * <p>
     * The corresponding test property will be viewable in the <code>Antithesis SDK: Always</code> group of your triage report.
     *
     * @param condition the condition being asserted.  Evaluated at runtime.
     * @param message   a unique string identifier of the assertion. 
     *                  Provides context for assertion success/failure 
     *                  and is intended to be human-readable.  Must be 
     *                  provided as a string literal.
     * @param details   additional details that provide greater context 
     *                  for assertion success/failure.  Evaluated at runtime.
     */
    public static void alwaysOrUnreachable(final boolean condition, final String message, final ObjectNode details) {
        Assertion.builder()
                .assertType(AssertType.Always)
                .condition(condition)
                .details(details)
                .displayType("AlwaysOrUnreachable")
                .hit(true)
                .id(message)
                .location(Assertion.getLocationInfo(message))
                .message(message)
                .mustHit(false)
                .build()
                .trackEntry();
    }

    /**
     * Assert that <code>condition</code> is true at least one time that this function was called.
     * (If the assertion is never encountered, the test property will therefore fail.)
     * This test property will be viewable in the <code>Antithesis SDK: Sometimes</code> group.
     *
     * @param condition the condition being asserted.  Evaluated at runtime.
     * @param message   a unique string identifier of the assertion. 
     *                  Provides context for assertion success/failure 
     *                  and is intended to be human-readable.  Must be 
     *                  provided as a string literal.
     * @param details   additional details that provide greater context 
     *                  for assertion success/failure.  Evaluated at runtime.
     */
    public static void sometimes(final boolean condition, final String message, final ObjectNode details) {
        sometimesHelper(condition, message, details);
    }

    private static void sometimesHelper(final boolean condition, final String message, final ObjectNode details) {
        Assertion.builder()
                .assertType(AssertType.Sometimes)
                .condition(condition)
                .details(details)
                .displayType("Sometimes")
                .hit(true)
                .id(message)
                .location(Assertion.getLocationInfo(message))
                .message(message)
                .mustHit(true)
                .build()
                .trackEntry();
    }

    /**
     * Assert that a line of code is never reached.
     * The corresponding test property will fail if this method is ever called.
     * (If it is never called the test property will therefore pass.)
     * This test property will be viewable in the <code>Antithesis SDK: Reachability assertions</code> group.
     *
     * @param message   a unique string identifier of the assertion. 
     *                  Provides context for assertion success/failure 
     *                  and is intended to be human-readable.  Must be 
     *                  provided as a string literal.
     * @param details   additional details that provide greater context 
     *                  for assertion success/failure.  Evaluated at runtime.
     */
    public static void unreachable(final String message, final ObjectNode details) {
        Assertion.builder()
                .assertType(AssertType.Reachability)
                .condition(false)
                .details(details)
                .displayType("Unreachable")
                .hit(true)
                .id(message)
                .location(Assertion.getLocationInfo(message))
                .message(message)
                .mustHit(false)
                .build()
                .trackEntry();
    }

    /**
     * Assert that a line of code is reached at least once.
     * The corresponding test property will pass if this method is ever called.
     * (If it is never called the test property will therefore fail.)
     * This test property will be viewable in the <code>Antithesis SDK: Reachability assertions</code> group.
     *
     * @param message   a unique string identifier of the assertion. 
     *                  Provides context for assertion success/failure 
     *                  and is intended to be human-readable.  Must be 
     *                  provided as a string literal.
     * @param details   additional details that provide greater context 
     *                  for assertion success/failure.  Evaluated at runtime.
     */
    public static void reachable(final String message, final ObjectNode details) {
        Assertion.builder()
                .assertType(AssertType.Reachability)
                .condition(true)
                .details(details)
                .displayType("Reachable")
                .hit(true)
                .id(message)
                .location(Assertion.getLocationInfo(message))
                .message(message)
                .mustHit(true)
                .build()
                .trackEntry();
    }

    /**
     * This is a low-level method designed to be used by third-party frameworks.
     * Regular users of the assertions class should not call it.
     * <p>
     * This is primarily intended for use by adapters from other
     * diagnostic tools that intend to output Antithesis-style
     * assertions.
     * <p>
     * Be certain to provide an assertion catalog entry
     * for each assertion issued with <code>rawAssert()</code>.  Assertion catalog
     * entries are also created using <code>rawAssert()</code>, by setting the value
     * of the <code>hit</code> parameter to false.
     * <p>
     * Please refer to the general Antithesis documentation regarding the
     * use of the <a href="https://antithesis.com/docs/using_antithesis/sdk/fallback/assert/" target="_blank">Fallback SDK</a>
     * for additional information.
     *
     * @param assertType   must be a valid Assertion.AssertType value
     * @param displayType  one of "Always", "AlwaysOrUnreachable", "Sometimes", "Reachable", "Unreachable"
     * @param className    the name of the package and class containing this assertion
     * @param functionName the name of the method containing this assertion
     * @param fileName     the name of the source file containing this assertion
     * @param beginLine    the source line number where the assertion is located
     * @param beginColumn  the source column number where the assertion is located
     * @param id           the unique text associated with the assertion
     * @param condition    the condition being asserted.  Evaluated at runtime.
     * @param message   a unique string identifier of the assertion. 
     *                  Provides context for assertion success/failure 
     *                  and is intended to be human-readable.
     * @param details   additional details that provide greater context 
     *                  for assertion success/failure.  Evaluated at runtime.
     * @param hit          true if the assertion has been evaluated, false if the assertion is being added to the assertion catalog
     * @param mustHit      true if the assertion is expected to be evaluated at least once, otherwise false
     */
    public static void rawAssert(
            final AssertType assertType,
            final String displayType,
            final String className,
            final String functionName,
            final String fileName,
            final int beginLine,
            final int beginColumn,
            final String id,
            final boolean condition,
            final String message,
            final ObjectNode details,
            final boolean hit,
            final boolean mustHit
    ) {
        LocationInfo locationInfo = LocationInfo.builder()
                .beginColumn(beginColumn)
                .beginLine(beginLine)
                .className(className)
                .fileName(fileName)
                .functionName(functionName)
                .build();

        Assertion.builder()
                .assertType(assertType)
                .condition(condition)
                .details(details)
                .displayType(displayType)
                .hit(hit)
                .id(id)
                .location(locationInfo)
                .message(message)
                .mustHit(mustHit)
                .build()
                .trackEntry();
    }

    /**
     * Types of assertions available
     */
    public enum AssertType {
        /**
         * Condition must always be true
         */
        Always,
        /**
         * Condition must be true at least once
         */
        Sometimes,
        /**
         * Indicates if an assertion should be encountered
         */
        Reachability
    }

    /**
     * @hidden
     */
    public enum GuidanceType {
        Numeric,
        Boolean,
        Json
    }

    /**
     * @hidden
     */
    public static void rawGuidance(
        final GuidanceType guidanceType,
        final ObjectNode guidanceData,
        final boolean maximize,
        final String className,
        final String functionName,
        final String fileName,
        final int beginLine,
        final int beginColumn,
        final String id,
        final String message,
        final boolean hit
    ) {
        LocationInfo location = LocationInfo.builder()
                .beginColumn(beginColumn)
                .beginLine(beginLine)
                .className(className)
                .fileName(fileName)
                .functionName(functionName)
                .build();
        Guidance.builder()
                .guidanceType(guidanceType)
                .id(id)
                .message(message)
                .location(location)
                .maximize(maximize)
                .data(guidanceData)
                .hit(hit)
                .build()
                .trackEntry();
    }

    private static void guidanceHelper(
        final GuidanceType guidanceType,
        final ObjectNode guidanceData,
        final boolean maximize,
        final String message
    ) {
        Guidance.builder()
                .guidanceType(guidanceType)
                .id(message)
                .message(message)
                .location(Assertion.getLocationInfo(message))
                .maximize(maximize)
                .data(guidanceData)
                .hit(true)
                .build()
                .trackEntry();
    }

    /**
     * {@code alwaysGreaterThan(x, y, ...)} is mostly equivalent to {@code always(x > y, ...)}.
     * Additionally Antithesis has more visibility to the value of {@code x} and {@code y},
     * and the assertion details would be merged with {@code {"left": x, "right": y}}.
     *
     * @param <T>      the numeric type that we are comparing in.
     * @param left     the left-hand-side of the comparison.
     * @param right    the right-hand-side of the comparison.
     * @param message  a unique string identifier of the assertion.
     *                 Provides context for assertion success/failure
     *                 and is intended to be human-readable.  Must be
     *                 provided as a string literal.
     * @param details  additional details that provide greater context
     *                 for assertion success/failure.  Evaluated at runtime.
     * @see Assert#always always
     */
    public static <T extends Number> void alwaysGreaterThan(final T left, final T right, final String message, final ObjectNode details) {
        double leftValue = left.doubleValue();
        double rightValue = right.doubleValue();
        ObjectNode guidanceData = new ObjectMapper().createObjectNode().put("left", leftValue).put("right", rightValue);
        ObjectNode detailsExtended = (ObjectNode) details.setAll(guidanceData);
        alwaysHelper(leftValue > rightValue, message, detailsExtended);
        guidanceHelper(GuidanceType.Numeric, guidanceData, false, message);
    }

    /**
     * {@code alwaysGreaterThanOrEqualTo(x, y, ...)} is mostly equivalent to {@code always(x >= y, ...)}.
     * Additionally Antithesis has more visibility to the value of {@code x} and {@code y},
     * and the assertion details would be merged with {@code {"left": x, "right": y}}.
     *
     * @param <T>      the numeric type that we are comparing in.
     * @param left     the left-hand-side of the comparison.
     * @param right    the right-hand-side of the comparison.
     * @param message  a unique string identifier of the assertion.
     *                 Provides context for assertion success/failure
     *                 and is intended to be human-readable.  Must be
     *                 provided as a string literal.
     * @param details  additional details that provide greater context
     *                 for assertion success/failure.  Evaluated at runtime.
     * @see Assert#always always
     */
    public static <T extends Number> void alwaysGreaterThanOrEqualTo(final T left, final T right, final String message, final ObjectNode details) {
        double leftValue = left.doubleValue();
        double rightValue = right.doubleValue();
        ObjectNode guidanceData = new ObjectMapper().createObjectNode().put("left", leftValue).put("right", rightValue);
        ObjectNode detailsExtended = (ObjectNode) details.setAll(guidanceData);
        alwaysHelper(leftValue >= rightValue, message, detailsExtended);
        guidanceHelper(GuidanceType.Numeric, guidanceData, false, message);
    }

    /**
     * {@code alwaysLessThan(x, y, ...)} is mostly equivalent to {@code always(x < y, ...)}.
     * Additionally Antithesis has more visibility to the value of {@code x} and {@code y},
     * and the assertion details would be merged with {@code {"left": x, "right": y}}.
     *
     * @param <T>      the numeric type that we are comparing in.
     * @param left     the left-hand-side of the comparison.
     * @param right    the right-hand-side of the comparison.
     * @param message  a unique string identifier of the assertion.
     *                 Provides context for assertion success/failure
     *                 and is intended to be human-readable.  Must be
     *                 provided as a string literal.
     * @param details  additional details that provide greater context
     *                 for assertion success/failure.  Evaluated at runtime.
     * @see Assert#always always
     */
    public static <T extends Number> void alwaysLessThan(final T left, final T right, final String message, final ObjectNode details) {
        double leftValue = left.doubleValue();
        double rightValue = right.doubleValue();
        ObjectNode guidanceData = new ObjectMapper().createObjectNode().put("left", leftValue).put("right", rightValue);
        ObjectNode detailsExtended = (ObjectNode) details.setAll(guidanceData);
        alwaysHelper(leftValue < rightValue, message, detailsExtended);
        guidanceHelper(GuidanceType.Numeric, guidanceData, true, message);
    }

    /**
     * {@code alwaysLessThanOrEqualTo(x, y, ...)} is mostly equivalent to {@code always(x <= y, ...)}.
     * Additionally Antithesis has more visibility to the value of {@code x} and {@code y},
     * and the assertion details would be merged with {@code {"left": x, "right": y}}.
     *
     * @param <T>      the numeric type that we are comparing in.
     * @param left     the left-hand-side of the comparison.
     * @param right    the right-hand-side of the comparison.
     * @param message  a unique string identifier of the assertion.
     *                 Provides context for assertion success/failure
     *                 and is intended to be human-readable.  Must be
     *                 provided as a string literal.
     * @param details  additional details that provide greater context
     *                 for assertion success/failure.  Evaluated at runtime.
     * @see Assert#always always
     */
    public static <T extends Number> void alwaysLessThanOrEqualTo(final T left, final T right, final String message, final ObjectNode details) {
        double leftValue = left.doubleValue();
        double rightValue = right.doubleValue();
        ObjectNode guidanceData = new ObjectMapper().createObjectNode().put("left", leftValue).put("right", rightValue);
        ObjectNode detailsExtended = (ObjectNode) details.setAll(guidanceData);
        alwaysHelper(leftValue <= rightValue, message, detailsExtended);
        guidanceHelper(GuidanceType.Numeric, guidanceData, true, message);
    }

    /**
     * {@code sometimesGreaterThan(x, y, ...)} is mostly equivalent to {@code sometimes(x > y, ...)}.
     * Additionally Antithesis has more visibility to the value of {@code x} and {@code y},
     * and the assertion details would be merged with {@code {"left": x, "right": y}}.
     *
     * @param <T>      the numeric type that we are comparing in.
     * @param left     the left-hand-side of the comparison.
     * @param right    the right-hand-side of the comparison.
     * @param message  a unique string identifier of the assertion.
     *                 Provides context for assertion success/failure
     *                 and is intended to be human-readable.  Must be
     *                 provided as a string literal.
     * @param details  additional details that provide greater context
     *                 for assertion success/failure.  Evaluated at runtime.
     * @see Assert#sometimes sometimes
     */
    public static <T extends Number> void sometimesGreaterThan(final T left, final T right, final String message, final ObjectNode details) {
        double leftValue = left.doubleValue();
        double rightValue = right.doubleValue();
        ObjectNode guidanceData = new ObjectMapper().createObjectNode().put("left", leftValue).put("right", rightValue);
        ObjectNode detailsExtended = (ObjectNode) details.setAll(guidanceData);
        sometimesHelper(leftValue > rightValue, message, detailsExtended);
        guidanceHelper(GuidanceType.Numeric, guidanceData, false, message);
    }

    /**
     * {@code sometimesGreaterThanOrEqualTo(x, y, ...)} is mostly equivalent to {@code sometimes(x >= y, ...)}.
     * Additionally Antithesis has more visibility to the value of {@code x} and {@code y},
     * and the assertion details would be merged with {@code {"left": x, "right": y}}.
     *
     * @param <T>      the numeric type that we are comparing in.
     * @param left     the left-hand-side of the comparison.
     * @param right    the right-hand-side of the comparison.
     * @param message  a unique string identifier of the assertion.
     *                 Provides context for assertion success/failure
     *                 and is intended to be human-readable.  Must be
     *                 provided as a string literal.
     * @param details  additional details that provide greater context
     *                 for assertion success/failure.  Evaluated at runtime.
     * @see Assert#sometimes sometimes
     */
    public static <T extends Number> void sometimesGreaterThanOrEqualTo(final T left, final T right, final String message, final ObjectNode details) {
        double leftValue = left.doubleValue();
        double rightValue = right.doubleValue();
        ObjectNode guidanceData = new ObjectMapper().createObjectNode().put("left", leftValue).put("right", rightValue);
        ObjectNode detailsExtended = (ObjectNode) details.setAll(guidanceData);
        sometimesHelper(leftValue >= rightValue, message, detailsExtended);
        guidanceHelper(GuidanceType.Numeric, guidanceData, false, message);
    }

    /**
     * {@code sometimesLessThan(x, y, ...)} is mostly equivalent to {@code sometimes(x < y, ...)}.
     * Additionally Antithesis has more visibility to the value of {@code x} and {@code y},
     * and the assertion details would be merged with {@code {"left": x, "right": y}}.
     *
     * @param <T>      the numeric type that we are comparing in.
     * @param left     the left-hand-side of the comparison.
     * @param right    the right-hand-side of the comparison.
     * @param message  a unique string identifier of the assertion.
     *                 Provides context for assertion success/failure
     *                 and is intended to be human-readable.  Must be
     *                 provided as a string literal.
     * @param details  additional details that provide greater context
     *                 for assertion success/failure.  Evaluated at runtime.
     * @see Assert#sometimes sometimes
     */
    public static <T extends Number> void sometimesLessThan(final T left, final T right, final String message, final ObjectNode details) {
        double leftValue = left.doubleValue();
        double rightValue = right.doubleValue();
        ObjectNode guidanceData = new ObjectMapper().createObjectNode().put("left", leftValue).put("right", rightValue);
        ObjectNode detailsExtended = (ObjectNode) details.setAll(guidanceData);
        sometimesHelper(leftValue < rightValue, message, detailsExtended);
        guidanceHelper(GuidanceType.Numeric, guidanceData, true, message);
    }

    /**
     * {@code sometimesLessThanOrEqualTo(x, y, ...)} is mostly equivalent to {@code sometimes(x <= y, ...)}.
     * Additionally Antithesis has more visibility to the value of {@code x} and {@code y},
     * and the assertion details would be merged with {@code {"left": x, "right": y}}.
     *
     * @param <T>      the numeric type that we are comparing in.
     * @param left     the left-hand-side of the comparison.
     * @param right    the right-hand-side of the comparison.
     * @param message  a unique string identifier of the assertion.
     *                 Provides context for assertion success/failure
     *                 and is intended to be human-readable.  Must be
     *                 provided as a string literal.
     * @param details  additional details that provide greater context
     *                 for assertion success/failure.  Evaluated at runtime.
     * @see Assert#sometimes sometimes
     */
    public static <T extends Number> void sometimesLessThanOrEqualTo(final T left, final T right, final String message, final ObjectNode details) {
        double leftValue = left.doubleValue();
        double rightValue = right.doubleValue();
        ObjectNode guidanceData = new ObjectMapper().createObjectNode().put("left", leftValue).put("right", rightValue);
        ObjectNode detailsExtended = (ObjectNode) details.setAll(guidanceData);
        sometimesHelper(leftValue <= rightValue, message, detailsExtended);
        guidanceHelper(GuidanceType.Numeric, guidanceData, true, message);
    }

    /**
     * {@code alwaysSome(Map.of("a", x, "b", y, ...), ...)} is similar to {@code always(x || y || ..., ...)}.
     * Additionally:
     * <ul>
     *   <li>Antithesis has more visibility to the individual propositions.</li>
     *   <li>There is no short-circuiting, so all of {@code x}, {@code y}, ... would be evaluated.</li>
     *   <li>The assertion details would be merged with {@code {"a": x, "b": y, ...}}.</li>
     * </ul>
     *
     * @param conditions  the collection of conditions to-be disjuncted,
     *                    represented as a map of booleans indexed by strings.
     * @param message     a unique string identifier of the assertion.
     *                    Provides context for assertion success/failure
     *                    and is intended to be human-readable.  Must be
     *                    provided as a string literal.
     * @param details     additional details that provide greater context
     *                    for assertion success/failure.  Evaluated at runtime.
     * @see Assert#always always
    */
    public static void alwaysSome(final Map<String, Boolean> conditions, final String message, final ObjectNode details) {
        ObjectNode guidanceData = new ObjectMapper().createObjectNode();
        conditions.forEach(guidanceData::put);
        ObjectNode detailsExtended = (ObjectNode) details.setAll(guidanceData);
        alwaysHelper(conditions.containsValue(true), message, detailsExtended);
        guidanceHelper(GuidanceType.Boolean, guidanceData, false, message);
    }

    /**
     * {@code sometimesAll(Map.of("a", x, "b", y, ...), ...)} is similar to {@code sometimes(x && y && ..., ...)}.
     * Additionally:
     * <ul>
     *   <li>Antithesis has more visibility to the individual propositions.</li>
     *   <li>There is no short-circuiting, so all of {@code x}, {@code y}, ... would be evaluated.</li>
     *   <li>The assertion details would be merged with {@code {"a": x, "b": y, ...}}.</li>
     * </ul>
     *
     * @param conditions  the collection of conditions to-be conjuncted,
     *                    represented as a map of booleans indexed by strings.
     * @param message     a unique string identifier of the assertion.
     *                    Provides context for assertion success/failure
     *                    and is intended to be human-readable.  Must be
     *                    provided as a string literal.
     * @param details     additional details that provide greater context
     *                    for assertion success/failure.  Evaluated at runtime.
     * @see Assert#sometimes sometimes
    */
    public static void sometimesAll(final Map<String, Boolean> conditions, final String message, final ObjectNode details) {
        ObjectNode guidanceData = new ObjectMapper().createObjectNode();
        conditions.forEach(guidanceData::put);
        ObjectNode detailsExtended = (ObjectNode) details.setAll(guidanceData);
        sometimesHelper(!conditions.containsValue(false), message, detailsExtended);
        guidanceHelper(GuidanceType.Boolean, guidanceData, true, message);
    }
}
