package activesupport.database.credential;

public enum DatabaseCredentialType {
    USERNAME ("dbUsername"),
    PASSWORD ("dbPassword");

    private final String name;

    DatabaseCredentialType(String name) {
        this.name = name;
    }

    public boolean equalsName(String otherName) {
        return name.equals(otherName);
    }

    @Override
    public String toString() {
        return this.name;
    }
}
