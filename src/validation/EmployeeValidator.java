package validation;

import model.EmployeeData;
import model.EmployeeType;

public interface EmployeeValidator {

    /** Returns true if this validator applies to the given employee type. */
    boolean supports(EmployeeType type);

    /**
     * Checks the raw form data.
     *
     * @throws exception.ValidationException if any value is invalid
     */
    void validate(EmployeeData data);
}

