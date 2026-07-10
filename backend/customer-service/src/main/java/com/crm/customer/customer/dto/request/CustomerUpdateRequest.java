package com.crm.customer.customer.dto.request;

import com.crm.customer.common.exception.MessageKeys;
import com.crm.customer.customer.entity.Gender;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CustomerUpdateRequest {

    // Same demographic validation as CustomerCreateRequest (FR-CUST-04).
    // VR-NAME: letters only (incl. Turkish), words separated by a single space.
    // Leading/trailing whitespace is trimmed pre-validation by JacksonConfig's
    // global string-trimming deserializer, not by this regex.
    private static final String NAME_REGEX = "^[A-Za-zÇĞİÖŞÜçğıöşü]+( [A-Za-zÇĞİÖŞÜçğıöşü]+)*$";
    // VR-NATID: exactly 11 digits.
    private static final String NATIONALITY_ID_REGEX = "^[0-9]{11}$";

    @NotBlank(message = MessageKeys.VAL_NAME)
    @Pattern(regexp = NAME_REGEX, message = MessageKeys.VAL_NAME)
    @Size(max = 50, message = MessageKeys.VAL_NAME)
    private String firstName;

    @Pattern(regexp = NAME_REGEX, message = MessageKeys.VAL_NAME)
    @Size(max = 50, message = MessageKeys.VAL_NAME)
    private String middleName;

    @NotBlank(message = MessageKeys.VAL_NAME)
    @Pattern(regexp = NAME_REGEX, message = MessageKeys.VAL_NAME)
    @Size(max = 50, message = MessageKeys.VAL_NAME)
    private String lastName;

    @Pattern(regexp = NAME_REGEX, message = MessageKeys.VAL_NAME)
    @Size(max = 50, message = MessageKeys.VAL_NAME)
    private String fatherName;

    @Pattern(regexp = NAME_REGEX, message = MessageKeys.VAL_NAME)
    @Size(max = 50, message = MessageKeys.VAL_NAME)
    private String motherName;

    @NotNull(message = MessageKeys.VAL_BIRTHDATE)
    private LocalDate birthDate;

    // No dedicated message key exists for gender in the given list; plain message is used.
    @NotNull(message = "gender is required")
    private Gender gender;

    @NotBlank(message = MessageKeys.VAL_NATID)
    @Pattern(regexp = NATIONALITY_ID_REGEX, message = MessageKeys.VAL_NATID)
    private String nationalityId;
}
