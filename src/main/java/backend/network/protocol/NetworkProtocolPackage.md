# Network Protocol Package

This package is **prepared for future client-server socket implementation**.

## Architecture for Socket Integration

### Server Side
```java
// Future implementation example
ServerSocket serverSocket = new ServerSocket(8080);
while (true) {
    Socket clientSocket = serverSocket.accept();
    // Create new thread for each client
    new Thread(new ClientHandler(clientSocket, services)).start();
}
```

### Client Side
```java
// Future implementation example
Socket socket = new Socket("localhost", 8080);
ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
ObjectInputStream in = new ObjectInputStream(socket.getInputStream());

// Send request
LoginRequest request = new LoginRequest(username, password);
out.writeObject(request);

// Receive response
LoginResponse response = (LoginResponse) in.readObject();
```

## Request/Response Types to Implement

### Authentication
- `LoginRequest / LoginResponse`
- `RegisterRequest / RegisterResponse`
- `LogoutRequest / LogoutResponse`

### Pokemon Operations
- `GetPokemonListRequest / PokemonListResponse`
- `SearchPokemonRequest / PokemonListResponse`
- `GetPokemonByIdRequest / PokemonResponse`

### Battle Operations
- `StartBattleRequest / BattleStateResponse`
- `ExecuteMoveRequest / BattleResultResponse`
- `SwitchPokemonRequest / BattleStateResponse`
- `EndBattleRequest / BattleEndResponse`

### Admin Operations
- `GetUsersRequest / UserListResponse`
- `CreateUserRequest / UserResponse`
- `UpdateUserRequest / UserResponse`
- `DeleteUserRequest / Response`

## Thread Safety Considerations

When implementing sockets:
1. Each client connection should run in its own thread
2. Use `synchronized` blocks for shared resources
3. Use `ConcurrentHashMap` for session management
4. Implement proper connection pooling for database access
5. Handle thread interruption and cleanup gracefully

## Example Implementation

```java
public class ClientHandler implements Runnable {
    private Socket socket;
    private UserService userService;
    private PokemonService pokemonService;
    private BattleService battleService;

    @Override
    public void run() {
        try (ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
             ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream())) {

            while (true) {
                Request request = (Request) in.readObject();
                Response response = handleRequest(request);
                out.writeObject(response);
                out.flush();
            }
        } catch (IOException | ClassNotFoundException e) {
            // Handle connection errors
        }
    }

    private Response handleRequest(Request request) {
        return switch (request.getRequestType()) {
            case "LOGIN" -> handleLogin((LoginRequest) request);
            case "GET_POKEMON" -> handleGetPokemon((GetPokemonRequest) request);
            case "BATTLE_ACTION" -> handleBattleAction((BattleActionRequest) request);
            default -> new ErrorResponse("Unknown request type");
        };
    }
}
```

## Current Status

✅ DTOs created (UserDTO, PokemonDTO, BattleStateDTO)
✅ Base Request/Response classes defined
✅ Services layer ready for network integration
⏳ Awaiting specific request/response implementations
⏳ Awaiting server socket handler
⏳ Awaiting client socket handler
