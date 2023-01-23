package activesupport.database;

import org.jetbrains.annotations.NotNull;

public enum Driver {
    MYSQL ("com.mysql.cj.jdbc.Driver");

    private final String name;

    Driver(@NotNull String name) {
        this.name = name;
    }

    @Override
    public String toString () {
        return this.name;
    }
}
