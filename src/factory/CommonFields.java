package factory;

import model.Department;

/**
 * The fields that every employee has, already parsed into their final Java types.
 *
 * <p>{@link RegistryEmployeeFactory} builds this once, so each {@link EmployeeCreator}
 * receives ready-to-use values and only has to deal with the fields of its own type.
 *
 * <p>{@code fullName} and {@code position} are passed through as received. The
 * {@code Employee} constructor trims and validates them, so the rule lives in one place.
 *
 * @param id         positive employee ID
 * @param fullName   raw full name; validated by the {@code Employee} constructor
 * @param age        positive age
 * @param department the real department object; never null
 * @param position   raw position; validated by the {@code Employee} constructor
 */
public record CommonFields(int id, String fullName, int age, Department department, String position) {
}
