package com.example.demo.eval;

/**
 * One field's expected-vs-actual comparison from an eval run. {@code match} is only
 * meaningful for the objectively-comparable fields (numbers, dates, booleans,
 * contractType) - for the two free-text fields (supplier/plan name) it's a best-effort
 * case-insensitive hint, not authoritative, since "Eesti Energia" and "Eesti Energia AS"
 * are the same answer but wouldn't match exactly. The admin is expected to eyeball the
 * diff, not treat this as a pass/fail gate - see the conversation that scoped this
 * feature for why an automated text-field grader wasn't worth building for v1.
 */
public record ContractEvalFieldResult(String field, String expected, String actual, boolean match) {
}
