package com.crm.customer.customer.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Stored in the database as the enum name (MALE/FEMALE, see {@code @Enumerated(STRING)}
 * on {@link Individual}), but serialized/deserialized over the API as exactly
 * "Male"/"Female" per FR-CUST-03.
 */
public enum Gender {
    MALE("Male"),
    FEMALE("Female");

    private final String apiValue;

    Gender(String apiValue) {
        this.apiValue = apiValue;
    }

    @JsonValue
    public String getApiValue() {
        return apiValue;
    }

    @JsonCreator
    public static Gender fromApiValue(String value) {
        for (Gender gender : values()) {
            if (gender.apiValue.equalsIgnoreCase(value)) {
                return gender;
            }
        }
        throw new IllegalArgumentException("Invalid gender value: " + value);
    }
}
