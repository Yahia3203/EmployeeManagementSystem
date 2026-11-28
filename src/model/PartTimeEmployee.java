package model;

import validation.Guard;

import java.util.Map;

public class PartTimeEmployee extends Employee {
    private double hourlyRate;
    private double hoursWorked;

    public PartTimeEmployee(int id, String fullName, int age, Department department,
                            String position, double hourlyRate, double hoursWorked) {
        super(id, fullName, age, department, position);
        setHourlyRate(hourlyRate);
        setHoursWorked(hoursWorked);
    }

    @Override
    public double calculateSalary() {
        return hourlyRate * hoursWorked;
    }

    @Override
    public EmployeeType getType() {
        return EmployeeType.PART_TIME;
    }

    @Override
    public Map<String, String> getTypeFields() {
        return Map.of(
                FieldKeys.HOURLY_RATE, plain(hourlyRate),
                FieldKeys.HOURS_WORKED, plain(hoursWorked));
    }

    public double getHourlyRate() {
        return hourlyRate;
    }

    public final void setHourlyRate(double hourlyRate) {
        this.hourlyRate = Guard.requireNonNegative(hourlyRate, "Hourly rate");
    }

    public double getHoursWorked() {
        return hoursWorked;
    }

    public final void setHoursWorked(double hoursWorked) {
        this.hoursWorked = Guard.requireNonNegative(hoursWorked, "Hours worked");
    }
}
