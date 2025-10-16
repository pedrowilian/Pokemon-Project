package backend.network.protocol;

import java.io.Serializable;

/**
 * Base request class for client-server communication
 * PREPARED FOR FUTURE SOCKET IMPLEMENTATION
 *
 * When implementing sockets:
 * 1. Create specific request types (LoginRequest, BattleActionRequest, etc.)
 * 2. Serialize/deserialize using ObjectOutputStream/ObjectInputStream
 * 3. Send via Socket connection
 */
public abstract class Request implements Serializable {
    private static final long serialVersionUID = 1L;

    private String requestType;
    private String sessionToken;
    private long timestamp;

    public Request(String requestType) {
        this.requestType = requestType;
        this.timestamp = System.currentTimeMillis();
    }

    public String getRequestType() {
        return requestType;
    }

    public String getSessionToken() {
        return sessionToken;
    }

    public void setSessionToken(String sessionToken) {
        this.sessionToken = sessionToken;
    }

    public long getTimestamp() {
        return timestamp;
    }
}
