## Othello Online Game (Java)

This is a **two-player online Othello game** implemented in Java. It features nickname registration, handicap proposal/approval, and real-time turn-based gameplay using a client-server architecture.

---

## Features

- Real-time two-player Othello over TCP/IP
- Nickname registration with duplicate name check
- Handicap system (0–5 levels) with proposal and confirmation
- Java Swing GUI for interactive game board
- Turn-based gameplay with move validation
- Automatic handling of pass and game-end conditions
- Result screen with game statistics and personal outcome

---

## Handicap System

- Players can choose one of six handicap levels:
  - `0`: No handicap
  - `1`: Draw counts as black win
  - `2`: 1 black stone at top-left
  - `3`: 2 black stones (top-left & bottom-right)
  - `4`: 3 black stones (plus top-right)
  - `5`: 4 black stones in all corners
- The first player proposes a handicap.
- The second player can accept or reject the proposal.
- If all handicaps are rejected, the game starts with no handicap.

---

## Technologies Used

- Java SE (JDK 8+)
- Java Socket API (TCP connection)
- Java Swing (JFrame, JPanel, JButton, JLabel)
- Multithreading with `BlockingQueue`
- Serializable data structure (`GameUpdate`)

---

##  Game Flow

1. Server waits for two client connections.
2. Both players register their nickname.
3. Player A proposes a handicap.
4. Player B accepts or rejects the proposal.
5. Game starts based on confirmed handicap setting.
6. Players take turns placing stones.
7. If neither player can make a move, the game ends.
8. Results are displayed with:
   - Total black/white count
   - Winner declaration
   - Personal result: "You Win", "You Lose", or "Draw"

---

## How to Run

### 1. Start the server

```bash
javac server.java
java server
```

### 2. Start the client (twice on separate terminals or machines)

```bash
javac client.java
java client
```

Input the server's IP address when prompted.

---

## 📝 Note

- Ensure the server is running before starting clients.
- This is a local-network game; port `10000` must be open.
- The game ends automatically if both players pass consecutively.

---
