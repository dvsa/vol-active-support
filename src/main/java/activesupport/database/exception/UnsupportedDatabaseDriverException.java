package activesupport.database.exception;

import org.jetbrains.annotations.NotNull;

public class UnsupportedDatabaseDriverException extends Exception {
    public UnsupportedDatabaseDriverException() {
    }

    public UnsupportedDatabaseDriverException(@NotNull String message) {
        super(message);
    }

    public UnsupportedDatabaseDriverException(@NotNull String message, @NotNull Throwable cause) {
        super(message, cause);
    }

    public UnsupportedDatabaseDriverException(@NotNull Throwable cause) {
        super(cause);
    }
}
