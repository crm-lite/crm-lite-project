package com.crm.customer.common.exception;

public final class MessageKeys {

    private MessageKeys() {
    }

    public static final String CUST_NOT_FOUND = "MSG-CUST-NOT-FOUND";
    public static final String CUST_DUP_NATID = "MSG-CUST-DUP-NATID";
    public static final String VAL_NATID = "MSG-VAL-NATID";
    public static final String VAL_BIRTHDATE = "MSG-VAL-BIRTHDATE";
    public static final String VAL_AGE_MIN = "MSG-VAL-AGE-MIN";
    public static final String VAL_NAME = "MSG-VAL-NAME";
    public static final String CUST_HAS_PRODUCTS = "MSG-CUST-HAS-PRODUCTS";
    public static final String FEATURE_NOT_IMPLEMENTED = "MSG-FEATURE-NOT-IMPLEMENTED";
    public static final String SEARCH_CRITERIA_REQUIRED = "MSG-SEARCH-CRITERIA-REQUIRED";

    // Not in the original message key list: needed as catch-alls so framework-level
    // failures (bean validation, malformed JSON, unexpected errors) still return the
    // same ErrorResponse shape instead of a raw stack trace / default Spring error body.
    public static final String VALIDATION_ERROR = "MSG-VALIDATION-ERROR";
    public static final String INTERNAL_ERROR = "MSG-INTERNAL-ERROR";
}
