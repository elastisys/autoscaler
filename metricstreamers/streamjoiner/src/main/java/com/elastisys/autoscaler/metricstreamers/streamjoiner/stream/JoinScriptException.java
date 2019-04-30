package com.elastisys.autoscaler.metricstreamers.streamjoiner.stream;

/**
 * Thrown by {@link JoiningMetricStream} to indicate a problem to execute a join
 * script.
 */
public class JoinScriptException extends Exception {

    private static final long serialVersionUID = 1L;

    public JoinScriptException() {
        super();
    }

    public JoinScriptException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public JoinScriptException(String message, Throwable cause) {
        super(message, cause);
    }

    public JoinScriptException(String message) {
        super(message);
    }

    public JoinScriptException(Throwable cause) {
        super(cause);
    }

}
