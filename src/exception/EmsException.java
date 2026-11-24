package exception;

import model.EmployeeData;
import model.EmployeeType;
import model.FieldKeys;

public class EmsException extends RuntimeException {
    public EmsException(String message) { super(message); }
}


