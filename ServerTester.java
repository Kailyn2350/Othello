import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.*;

public class ServerTester {
    private static final int BOARD_SIZE = 8;
    public static void main(String[] args) throws Exception {
        Scanner scanner = new Scanner(System.in);
        System.out.print("Enter server IP : ");
        String serverIp = scanner.nextLine().trim();
        // 2つのボットの手順（nullならランダム）
        int[][][] movesA = {
            // 例: {{2,3},{2,2},...} 形式。nullならランダム
            null
        };
        int[][][] movesB = {
            null
        };
        Thread tA = new Thread(() -> runBot("BotA", serverIp, movesA[0]));
        Thread tB = new Thread(() -> runBot("BotB", serverIp, movesB[0]));
        tA.start();
        tB.start();
        tA.join();
        tB.join();
    }

    static void runBot(String nickname, String serverIp, int[][] moves) {
        try {
            Socket socket = new Socket(serverIp, 10000);
            System.out.println(nickname + "Connected to server: " + serverIp);
            ObjectOutputStream objOut = new ObjectOutputStream(socket.getOutputStream());
            objOut.flush();
            ObjectInputStream objIn = new ObjectInputStream(socket.getInputStream());
            Random rand = new Random();
            int moveIdx = 0;

            while (true) {
                Object obj = objIn.readObject();
                if (obj instanceof String) {
                    String msg = (String) obj;
                    if (msg.equals("READY_FOR_NICKNAME")) {
                        objOut.writeObject(nickname);
                        objOut.flush();
                        System.out.println(nickname + "Sent Nickname: " + nickname);
                    } else if (msg.equals("WAIT_FOR_HANDICAP")) {
                        objOut.writeObject("yes");
                        objOut.flush();
                        System.out.println(nickname + "Handicap accepted");
                    } else if (msg.startsWith("対戦相手がハンディキャップ")) {
                        objOut.writeObject("yes");
                        objOut.flush();
                        System.out.println(nickname + "Opponent's handicap accepted");
                    } else if (msg.equals("SELECT_HANDICAP") || msg.equals("RESELECT_HANDICAP")) {
                        Object next = objIn.readObject();
                        if (next instanceof List) {
                            List<?> options = (List<?>) next;
                            int idx = rand.nextInt(options.size());
                            objOut.writeObject(options.get(idx));
                            objOut.flush();
                            System.out.println(nickname + "Handicap selected: " + options.get(idx));
                        }
                    }
                } else if (obj.getClass().getSimpleName().equals("GameUpdate")) {
                    GameUpdate update = (GameUpdate) obj;
                    List<int[]> validPoints = update.validPoint;
                    if (update.gameEnd) {
                        int black = 0, white = 0;
                        String color_number_map[] = {"", "Black", "White"};
                        for (int i = 0; i < BOARD_SIZE; i++) {
                            for (int j = 0; j < BOARD_SIZE; j++) {
                                if (update.board[i][j] == 1)
                                    black++;
                                if (update.board[i][j] == 2)
                                    white++;
                            }
                        }
                        if (update.yourColor == 1) {//先手だけ表示(二重に表示されてしまうので)
                            System.out.println("Game Over");
                            System.out.println("Black: " + black + ", White: " + white);
                            if (black == white) {
                                System.out.println("Draw");
                            }
                        }
                        if (black > white && update.yourColor == 1) {
                            System.out.println(nickname +"("+color_number_map[update.yourColor]+")" +" wins!");
                        } else if (black < white && update.yourColor == 2) {
                            System.out.println(nickname +"("+color_number_map[update.yourColor]+")" +" wins!");
                        }
                        break;
                    }
                    if (update.myTurn && validPoints != null && !validPoints.isEmpty()) {
                        int[] move;
                        if (moves != null && moveIdx < moves.length) {
                            // 指定手順
                            move = moves[moveIdx++];
                            // 指定手が有効な手でなければランダム
                            boolean found = false;
                            for (int[] vp : validPoints) {
                                if (Arrays.equals(vp, move)) {
                                    found = true;
                                    break;
                                }
                            }
                            if (!found) {
                                move = validPoints.get(rand.nextInt(validPoints.size()));
                            }
                        } else {
                            // ランダム
                            move = validPoints.get(rand.nextInt(validPoints.size()));
                        }
                        objOut.writeObject(move);
                        objOut.flush();
                        System.out.println(nickname + " move: [" +Integer.toString(move[0]+1) + "," + "ABCDEFGH".toCharArray()[move[1]] +"]");
                    }
                }
            }
            objIn.close();
            objOut.close();
            socket.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}