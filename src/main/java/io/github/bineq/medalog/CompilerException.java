package io.github.bineq.medalog;

/**
 * Unchecked exception for MeDaLog compiler errors.
 * Includes the input line number where the error was detected.
 */
public class CompilerException extends RuntimeException {

    private final int lineNumber;

    public CompilerException(String message, int lineNumber) {
        super(formatMessage(message, lineNumber));
        this.lineNumber = lineNumber;
    }

    public CompilerException(String message, int lineNumber, Throwable cause) {
        super(formatMessage(message, lineNumber), cause);
        this.lineNumber = lineNumber;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    private static String formatMessage(String message, int lineNumber) {
        return "Line " + lineNumber + ": " + message;
    }
}
