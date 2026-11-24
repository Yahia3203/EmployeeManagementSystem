package model;

import java.util.Map;

public class PartTimeEmployee extends Employee {
    private double hourlyRate;
    private int hoursWorked;

    public PartTimeEmployee(int id, String fullName, int age, Department department, String position , double hourlyRate , int hoursWorked) {
        super(id, fullName, age, department, position);
        this.hourlyRate = hourlyRate;
        this.hoursWorked = hoursWorked;

    }

    @Override
    public double calculateSalary() {
        return hoursWorked * hourlyRate;
    }

    @Override
    public EmployeeType getType() {
        return EmployeeType.PART_TIME;
    }

    @Override
    public Map<String, String> getTypeFields() {
        return Map.of(FieldKeys.HOURLY_RATE , String.valueOf(hourlyRate) , FieldKeys.HOURS_WORKED , String> );
    }

    public double getHourlyRate() {
        return hourlyRate;
    }

    public void setHourlyRate(double hourlyRate) {
        this.hourlyRate = hourlyRate;
    }

    public int getHoursWorked() {
        return hoursWorked;
    }

    public void setHoursWorked(int hoursWorked) {
        this.hoursWorked = hoursWorked;
    }
}
