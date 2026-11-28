package service;

public class FixedCredentialsAuthService implements AuthService {
    private static final String USERNAME = "admin";
    private static final String PASSWORD = "admin123";

    @Override
    public boolean login(String username, String password) {
        return USERNAME.equals(username) && PASSWORD.equals(password);
    }
}
