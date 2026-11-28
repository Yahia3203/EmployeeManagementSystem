package config;

import factory.EmployeeFactory;
import factory.FullTimeEmployeeCreator;
import factory.PartTimeEmployeeCreator;
import factory.RegistryEmployeeFactory;
import persistence.CsvDepartmentRepository;
import persistence.CsvEmployeeRepository;
import persistence.CsvFileStore;
import persistence.DepartmentCsvMapper;
import persistence.DepartmentRepository;
import persistence.EmployeeCsvMapper;
import persistence.EmployeeRepository;
import service.AuthService;
import service.DepartmentService;
import service.DepartmentServiceImpl;
import service.EmployeeService;
import service.EmployeeServiceImpl;
import service.FixedCredentialsAuthService;
import validation.CommonFieldsValidator;
import validation.EmployeeValidator;
import validation.FullTimeSalaryValidator;
import validation.PartTimeSalaryValidator;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;

/**
 * Composition root: the one place that creates the application's objects and connects them.
 *
 * <p>Nothing else uses {@code new} on a repository, service, factory or validator. To support a
 * new employee type, add its creator and validator to the two lists below.
 *
 * <p>Construction loads the CSV files, so it can throw
 * {@link persistence.PersistenceException} (missing permissions, corrupt file, unknown department).
 * The caller should show the message and exit rather than continue without data.
 */
public final class AppConfig {

    public static final String DEFAULT_DATA_DIR = "data";

    private final AuthService authService;
    private final DepartmentService departmentService;
    private final EmployeeService employeeService;

    /** Uses the default {@value #DEFAULT_DATA_DIR} directory. */
    public AppConfig() {
        this(Paths.get(DEFAULT_DATA_DIR));
    }

    /**
     * @param dataDir directory holding {@code departments.csv} and {@code employees.csv};
     *                created on the first save if it does not exist
     * @throws persistence.PersistenceException if a data file cannot be read or is invalid
     */
    public AppConfig(Path dataDir) {
        Objects.requireNonNull(dataDir, "dataDir");

        EmployeeFactory employeeFactory = new RegistryEmployeeFactory(
                List.of(new FullTimeEmployeeCreator(), new PartTimeEmployeeCreator()));

        List<EmployeeValidator> validators = List.of(
                new CommonFieldsValidator(),
                new FullTimeSalaryValidator(),
                new PartTimeSalaryValidator());

        // Departments first: employees.csv refers to them by ID.
        CsvFileStore deptStore = new CsvFileStore(
                dataDir.resolve("departments.csv").toString(), DepartmentCsvMapper.header());
        CsvFileStore empStore = new CsvFileStore(
                dataDir.resolve("employees.csv").toString(), EmployeeCsvMapper.header());

        DepartmentRepository departmentRepository =
                new CsvDepartmentRepository(deptStore, new DepartmentCsvMapper());
        EmployeeRepository employeeRepository = new CsvEmployeeRepository(
                empStore, new EmployeeCsvMapper(employeeFactory::create, departmentRepository));

        this.authService = new FixedCredentialsAuthService();
        this.departmentService = new DepartmentServiceImpl(departmentRepository, employeeRepository);
        this.employeeService = new EmployeeServiceImpl(
                employeeRepository, departmentService, employeeFactory, validators);
    }

    public AuthService authService() {
        return authService;
    }

    public DepartmentService departmentService() {
        return departmentService;
    }

    public EmployeeService employeeService() {
        return employeeService;
    }
}
