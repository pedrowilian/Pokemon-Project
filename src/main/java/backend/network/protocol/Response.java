package backend.network.protocol;

import java.io.Serializable;

/**
 * Base response class for server-client communication
 * PREPARED FOR FUTURE SOCKET IMPLEMENTATION
 */
public abstract class Response implements Serializable {
    private static final long serialVersionUID = 1L;

    private boolean success;
    private String message;
    private long timestamp;

    public Response(boolean success, String message) {
        this.success = success;
        this.message = message;
        this.timestamp = System.currentTimeMillis();
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public long getTimestamp() {
        return timestamp;
    }
}
