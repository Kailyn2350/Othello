# Othello Game

This is a classic Othello game implemented in Java with a graphical user interface (GUI) and client-server architecture for two-player gameplay over a network.

## Features

*   **Graphical User Interface:** A simple and intuitive GUI for playing the game.
*   **Client-Server Architecture:** Allows two players to play against each other over a network.
*   **Handicap System:** Supports various handicap options for players of different skill levels.
*   **Real-time Updates:** The game board and player turns are updated in real-time.

## How to Play

### Prerequisites

*   Java Development Kit (JDK) installed on your system.

### Compilation

1.  Open a terminal or command prompt.
2.  Navigate to the project directory (`PL_03-main`).
3.  Compile the Java source files using the following command:

    ```bash
    javac *.java
    ```

### Running the Game

1.  **Start the Server:**
    In the terminal, run the following command to start the game server:

    ```bash
    java server
    ```

2.  **Start the Clients:**
    Open two new terminal windows and run the following command in each to start the client applications:

    ```bash
    java client
    ```

3.  **Connect to the Server:**
    In each client window, enter the IP address of the server (e.g., `127.0.0.1` if running on the same machine) and click "OK".

4.  **Enter Nicknames and Play:**
    Follow the on-screen instructions to enter your nicknames, choose a handicap, and start playing Othello!

## Project Structure

*   `server.java`: The main class for the game server. It handles client connections, game logic, and communication between players.
*   `client.java`: The main class for the game client. It provides the GUI for the game and communicates with the server.
*   `Board.java`: Represents the Othello game board and handles its state and drawing.
*   `player.java`: Represents a player in the game, storing their name and color.
*   `GameUpdate.java`: A class used to send game state updates from the server to the clients.
*   `オセロ背景.png`: The background image for the game's GUI.
