package io.github.bineq.medalog;

/**
 * Unchecked exception thrown when an annotation processor detects an error in the input program.
 * The {@link #getLineNumber()} method returns the source line where the error was found.
 */
public class AnnotationProcessorException extends RuntimeException {

    private final int lineNumber;

    public AnnotationProcessorException(String message, int lineNumber) {
        super("Line " + lineNumber + ": " + message);
        this.lineNumber = lineNumber;
    }

    public AnnotationProcessorException(String message, int lineNumber, Throwable cause) {
        super("Line " + lineNumber + ": " + message, cause);
        this.lineNumber = lineNumber;
    }

    public int getLineNumber() {
        return lineNumber;
    }
}
