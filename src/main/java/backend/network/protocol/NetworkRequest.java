package backend.network.protocol;

import java.io.Serializable;
import java.util.Map;
import java.util.UUID;

/**
 * Representa uma requisição de rede do cliente para o servidor
 * Formato: JSON sobre TCP Socket
 */
public class NetworkRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    private String requestId;
    private String service;  // "UserService", "PokemonService", "TeamService"
    private String method;   // Nome do método a ser invocado
    private Map<String, Object> params; // Parâmetros do método

    public NetworkRequest() {
        this.requestId = UUID.randomUUID().toString();
    }

    public NetworkRequest(String service, String method, Map<String, Object> params) {
        this.requestId = UUID.randomUUID().toString();
        this.service = service;
        this.method = method;
        this.params = params;
    }

    // Getters and Setters
    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getService() {
        return service;
    }

    public void setService(String service) {
        this.service = service;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public Map<String, Object> getParams() {
        return params;
    }

    public void setParams(Map<String, Object> params) {
        this.params = params;
    }

    @Override
    public String toString() {
        return "NetworkRequest{" +
                "requestId='" + requestId + '\'' +
                ", service='" + service + '\'' +
                ", method='" + method + '\'' +
                ", params=" + params +
                '}';
    }
}
