package cn.xiaofuge.deepseek.harness.cases.orchestration;

/**
 * Standard checked-exception boundary for case orchestration.
 */
public class CaseExecutionException extends RuntimeException {

    public CaseExecutionException(String message, Throwable cause) {
        super(message, cause);
    }
}
