package backend.network.protocol;

import java.io.Serializable;

/**
 * Representa uma resposta de rede do servidor para o cliente
 * Formato: JSON sobre TCP Socket
 */
public class NetworkResponse implements Serializable {
    private static final long serialVersionUID = 1L;

    private String requestId;  // Mesmo ID da requisição
    private boolean success;   // true = sucesso, false = erro
    private Object data;       // Dados de retorno (pode ser DTO, List, etc)
    private String error;      // Mensagem de erro (se success = false)

    public NetworkResponse() {
    }

    public NetworkResponse(String requestId, boolean success, Object data, String error) {
        this.requestId = requestId;
        this.success = success;
        this.data = data;
        this.error = error;
    }

    // Factory methods para sucesso/erro
    public static NetworkResponse success(String requestId, Object data) {
        return new NetworkResponse(requestId, true, data, null);
    }

    public static NetworkResponse error(String requestId, String errorMessage) {
        return new NetworkResponse(requestId, false, null, errorMessage);
    }

    // Getters and Setters
    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    @Override
    public String toString() {
        return "NetworkResponse{" +
                "requestId='" + requestId + '\'' +
                ", success=" + success +
                ", data=" + data +
                ", error='" + error + '\'' +
                '}';
    }
}
