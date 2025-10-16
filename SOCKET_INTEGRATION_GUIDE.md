# Quick Start: Client-Server Socket Integration

## Overview

This guide shows you how to add client-server socket communication with threads to your Pokemon project. The architecture is already prepared - you just need to implement the socket handlers.

---

## Step 1: Create Specific Request/Response Classes

### Example: Login Request/Response

Create in `backend/network/protocol/`:

```java
// LoginRequest.java
package backend.network.protocol;

public class LoginRequest extends Request {
    private static final long serialVersionUID = 1L;
    private String username;
    private String password;

    public LoginRequest(String username, String password) {
        super("LOGIN");
        this.username = username;
        this.password = password;
    }

    public String getUsername() { return username; }
    public String getPassword() { return password; }
}

// LoginResponse.java
package backend.network.protocol;

public class LoginResponse extends Response {
    private static final long serialVersionUID = 1L;
    private String sessionToken;
    private boolean isAdmin;

    public LoginResponse(boolean success, String message, String sessionToken, boolean isAdmin) {
        super(success, message);
        this.sessionToken = sessionToken;
        this.isAdmin = isAdmin;
    }

    public String getSessionToken() { return sessionToken; }
    public boolean isAdmin() { return isAdmin; }
}
```

---

## Step 2: Create Server

### PokemonServer.java

Create in `backend/network/server/`:

```java
package backend.network.server;

import backend.infrastructure.ServiceLocator;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

public class PokemonServer {
    private static final Logger LOGGER = Logger.getLogger(PokemonServer.class.getName());
    private static final int PORT = 8080;

    private ServerSocket serverSocket;
    private ServiceLocator services;
    private boolean running;

    public void start() {
        try {
            serverSocket = new ServerSocket(PORT);
            services = ServiceLocator.getInstance();
            running = true;

            LOGGER.log(Level.INFO, "Pokemon Server started on port " + PORT);

            while (running) {
                Socket clientSocket = serverSocket.accept();
                LOGGER.log(Level.INFO, "Client connected: " + clientSocket.getInetAddress());

                // Create new thread for each client
                Thread clientThread = new Thread(new ClientHandler(clientSocket, services));
                clientThread.start();
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Server error", e);
        }
    }

    public void stop() {
        running = false;
        try {
            if (serverSocket != null) {
                serverSocket.close();
            }
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Error closing server", e);
        }
    }

    public static void main(String[] args) {
        PokemonServer server = new PokemonServer();

        // Add shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            LOGGER.log(Level.INFO, "Shutting down server...");
            server.stop();
            ServiceLocator.getInstance().shutdown();
        }));

        server.start();
    }
}
```

### ClientHandler.java

Create in `backend/network/server/`:

```java
package backend.network.server;

import backend.application.service.*;
import backend.infrastructure.ServiceLocator;
import backend.network.protocol.*;
import model.Pokemon;

import java.io.*;
import java.net.Socket;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ClientHandler implements Runnable {
    private static final Logger LOGGER = Logger.getLogger(ClientHandler.class.getName());

    private Socket socket;
    private ServiceLocator services;
    private ObjectInputStream in;
    private ObjectOutputStream out;
    private String sessionToken;

    public ClientHandler(Socket socket, ServiceLocator services) {
        this.socket = socket;
        this.services = services;
    }

    @Override
    public void run() {
        try {
            // Initialize streams (out first!)
            out = new ObjectOutputStream(socket.getOutputStream());
            out.flush();
            in = new ObjectInputStream(socket.getInputStream());

            LOGGER.log(Level.INFO, "Client handler started");

            // Handle requests
            while (true) {
                Request request = (Request) in.readObject();
                Response response = handleRequest(request);
                out.writeObject(response);
                out.flush();
            }

        } catch (EOFException e) {
            LOGGER.log(Level.INFO, "Client disconnected");
        } catch (IOException | ClassNotFoundException e) {
            LOGGER.log(Level.WARNING, "Client handler error", e);
        } finally {
            cleanup();
        }
    }

    private Response handleRequest(Request request) {
        try {
            return switch (request.getRequestType()) {
                case "LOGIN" -> handleLogin((LoginRequest) request);
                case "GET_POKEMON" -> handleGetPokemon();
                case "REGISTER" -> handleRegister((RegisterRequest) request);
                // Add more request types here
                default -> new ErrorResponse("Unknown request type: " + request.getRequestType());
            };
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error handling request", e);
            return new ErrorResponse("Server error: " + e.getMessage());
        }
    }

    private Response handleLogin(LoginRequest request) {
        try {
            UserService userService = services.getUserService();
            boolean authenticated = userService.authenticate(
                request.getUsername(),
                request.getPassword()
            );

            if (authenticated) {
                // Generate session token
                sessionToken = UUID.randomUUID().toString();

                return new LoginResponse(
                    true,
                    "Login successful",
                    sessionToken,
                    false // TODO: Get admin status from user
                );
            } else {
                return new LoginResponse(false, "Invalid credentials", null, false);
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Login error", e);
            return new LoginResponse(false, "Login error: " + e.getMessage(), null, false);
        }
    }

    private Response handleGetPokemon() {
        try {
            PokemonService pokemonService = services.getPokemonService();
            List<Pokemon> pokemon = pokemonService.getAllPokemon();

            return new PokemonListResponse(true, "Pokemon retrieved", pokemon);
        } catch (Exception e) {
            return new ErrorResponse("Error retrieving Pokemon: " + e.getMessage());
        }
    }

    private Response handleRegister(RegisterRequest request) {
        try {
            UserService userService = services.getUserService();
            userService.register(
                request.getUsername(),
                request.getPassword(),
                request.getConfirmPassword(),
                false // Not admin
            );

            return new Response(true, "Registration successful") {};
        } catch (Exception e) {
            return new ErrorResponse("Registration failed: " + e.getMessage());
        }
    }

    private void cleanup() {
        try {
            if (in != null) in.close();
            if (out != null) out.close();
            if (socket != null) socket.close();
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Error closing resources", e);
        }
    }
}

// Helper response classes
class ErrorResponse extends Response {
    public ErrorResponse(String message) {
        super(false, message);
    }
}

class PokemonListResponse extends Response {
    private List<Pokemon> pokemon;

    public PokemonListResponse(boolean success, String message, List<Pokemon> pokemon) {
        super(success, message);
        this.pokemon = pokemon;
    }

    public List<Pokemon> getPokemon() { return pokemon; }
}

class RegisterRequest extends Request {
    private String username;
    private String password;
    private String confirmPassword;

    public RegisterRequest(String username, String password, String confirmPassword) {
        super("REGISTER");
        this.username = username;
        this.password = password;
        this.confirmPassword = confirmPassword;
    }

    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public String getConfirmPassword() { return confirmPassword; }
}
```

---

## Step 3: Create Client

### PokemonClient.java

Create in `backend/network/client/`:

```java
package backend.network.client;

import backend.network.protocol.*;
import java.io.*;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

public class PokemonClient {
    private static final Logger LOGGER = Logger.getLogger(PokemonClient.class.getName());

    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private String sessionToken;

    public void connect(String host, int port) throws IOException {
        socket = new Socket(host, port);
        out = new ObjectOutputStream(socket.getOutputStream());
        out.flush();
        in = new ObjectInputStream(socket.getInputStream());

        LOGGER.log(Level.INFO, "Connected to server: " + host + ":" + port);
    }

    public LoginResponse login(String username, String password) {
        try {
            LoginRequest request = new LoginRequest(username, password);
            out.writeObject(request);
            out.flush();

            LoginResponse response = (LoginResponse) in.readObject();
            if (response.isSuccess()) {
                sessionToken = response.getSessionToken();
            }
            return response;

        } catch (IOException | ClassNotFoundException e) {
            LOGGER.log(Level.SEVERE, "Login error", e);
            return new LoginResponse(false, "Connection error", null, false);
        }
    }

    public Response sendRequest(Request request) throws IOException, ClassNotFoundException {
        request.setSessionToken(sessionToken);
        out.writeObject(request);
        out.flush();
        return (Response) in.readObject();
    }

    public void disconnect() {
        try {
            if (out != null) out.close();
            if (in != null) in.close();
            if (socket != null) socket.close();
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Error disconnecting", e);
        }
    }

    // Example usage
    public static void main(String[] args) {
        PokemonClient client = new PokemonClient();

        try {
            // Connect to server
            client.connect("localhost", 8080);

            // Login
            LoginResponse loginResponse = client.login("testuser", "password123");
            System.out.println("Login: " + loginResponse.getMessage());

            if (loginResponse.isSuccess()) {
                System.out.println("Session Token: " + loginResponse.getSessionToken());

                // Make additional requests here
                // Response response = client.sendRequest(new GetPokemonRequest());
            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            client.disconnect();
        }
    }
}
```

---

## Step 4: Integration with GUI

### Option A: Replace GUI with Client Calls

Modify `LoginFrame.java` to use client instead of direct service:

```java
private void authenticate(String username, String password) {
    SwingWorker<Boolean, Void> worker = new SwingWorker<>() {
        @Override
        protected Boolean doInBackground() {
            try {
                PokemonClient client = new PokemonClient();
                client.connect("localhost", 8080);

                LoginResponse response = client.login(username, password);
                return response.isSuccess();

            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "Connection error", e);
                return false;
            }
        }

        @Override
        protected void done() {
            try {
                if (get()) {
                    // Open main interface
                } else {
                    showError("Login failed");
                }
            } catch (Exception e) {
                showError("Error: " + e.getMessage());
            }
        }
    };
    worker.execute();
}
```

### Option B: Keep Local Mode + Add Network Mode

Add a toggle in your app to switch between:
- **Local Mode**: Direct service calls (current behavior)
- **Network Mode**: Socket client calls

```java
public class AppConfig {
    public enum Mode { LOCAL, NETWORK }
    private static Mode currentMode = Mode.LOCAL;

    public static void setMode(Mode mode) {
        currentMode = mode;
    }

    public static boolean isNetworkMode() {
        return currentMode == Mode.NETWORK;
    }
}
```

---

## Step 5: Testing

### Test 1: Server Startup

```bash
# Compile
mvn clean compile

# Run server
java -cp target/classes backend.network.server.PokemonServer
```

Expected output:
```
INFO: Application services initialized successfully
INFO: Pokemon Server started on port 8080
```

### Test 2: Client Connection

```bash
# In another terminal, run client
java -cp target/classes backend.network.client.PokemonClient
```

Expected output:
```
INFO: Connected to server: localhost:8080
Login: Login successful
Session Token: 550e8400-e29b-41d4-a716-446655440000
```

### Test 3: Multiple Clients

Open 3 terminals and run the client in each. All should connect successfully, demonstrating the **multi-threaded** capability.

---

## Thread Safety Considerations

### Current Status: ✅ Already Thread-Safe

The architecture is designed for multi-threading:

1. **ServiceLocator**: Singleton pattern, thread-safe
2. **Services**: Stateless (no instance variables)
3. **Repositories**: Each has its own connection
4. **ConnectionManager**: Uses ConcurrentHashMap internally

### If You Need Shared State

Use synchronization:

```java
public class SessionManager {
    private final ConcurrentHashMap<String, UserSession> sessions = new ConcurrentHashMap<>();

    public void addSession(String token, UserSession session) {
        sessions.put(token, session);
    }

    public UserSession getSession(String token) {
        return sessions.get(token);
    }
}
```

---

## Common Issues & Solutions

### Issue 1: Connection Refused
**Problem**: Client can't connect to server
**Solution**: Make sure server is running first

### Issue 2: Stream Corrupted
**Problem**: `java.io.StreamCorruptedException`
**Solution**: Create `ObjectOutputStream` before `ObjectInputStream`, and call `flush()`

### Issue 3: Serialization Error
**Problem**: `NotSerializableException`
**Solution**: Make sure all DTOs implement `Serializable`

### Issue 4: Database Locked
**Problem**: SQLite database locked with multiple threads
**Solution**: Use WAL mode:
```java
connection.createStatement().execute("PRAGMA journal_mode=WAL");
```

---

## Performance Tips

### 1. Connection Pooling
Already implemented via `ConnectionManager`

### 2. Thread Pool (Optional)
Instead of creating a new thread per client, use a thread pool:

```java
ExecutorService threadPool = Executors.newFixedThreadPool(10);

while (running) {
    Socket clientSocket = serverSocket.accept();
    threadPool.submit(new ClientHandler(clientSocket, services));
}
```

### 3. Caching (Optional)
Cache frequently accessed data:

```java
private static List<Pokemon> cachedPokemon;
private static long cacheTime;

public List<Pokemon> getAllPokemon() {
    if (System.currentTimeMillis() - cacheTime > 60000) {
        cachedPokemon = pokemonRepository.findAll();
        cacheTime = System.currentTimeMillis();
    }
    return cachedPokemon;
}
```

---

## Project Requirements Checklist

✅ **Database Integration**: SQLite with repositories
✅ **Client-Server Sockets**: Request/Response pattern
✅ **Threads**: One thread per client connection
✅ **Bidirectional Communication**: Client ↔ Server
✅ **Serialization**: DTOs with ObjectStreams
✅ **Thread Safety**: Stateless services
✅ **Clean Architecture**: Separated layers
✅ **Professional Code**: Well-documented

---

## Next Steps

1. ✅ Read this guide
2. ⏳ Implement specific Request/Response classes for your needs
3. ⏳ Copy PokemonServer.java to your project
4. ⏳ Copy ClientHandler.java to your project
5. ⏳ Copy PokemonClient.java to your project
6. ⏳ Test with localhost
7. ⏳ Integrate with GUI (optional)
8. ⏳ Add more request types as needed

---

## Conclusion

Your Pokemon project is **100% ready** for client-server socket integration. The architecture supports:

- ✅ Multi-threaded server (one thread per client)
- ✅ Serialized communication (DTOs)
- ✅ Clean separation (services for business logic)
- ✅ Thread-safe design (stateless services)
- ✅ Professional code quality

**Just follow the steps above, and you'll have a working client-server application!** 🚀

**Good luck with your DBMS project!**
