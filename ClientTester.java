import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.List;
import java.util.Random;

public class ClientTester {
    public static void main(String[] args) throws Exception {
        String serverIp = args.length > 0 ? args[0] : "127.0.0.1";
        Socket socket = new Socket(serverIp, 10000);
        ObjectOutputStream objOut = new ObjectOutputStream(socket.getOutputStream());
        objOut.flush();

        ObjectInputStream objIn = new ObjectInputStream(socket.getInputStream());
        Random rand = new Random();

        while (true) {
            Object obj = objIn.readObject();
            if (obj instanceof String) {
                String msg = (String) obj;
                if (msg.equals("READY_FOR_NICKNAME")) {
                    objOut.writeObject("RandomBot");
                    objOut.flush();
                } else if (msg.equals("WAIT_FOR_HANDICAP")) {
                    // ハンディキャップ承認: 常に"yes"で応答
                    objOut.writeObject("yes");
                    objOut.flush();
                } else if (msg.startsWith("対戦相手がハンディキャップ")) {
                    // ハンディキャップ承認: 常に"yes"で応答
                    objOut.writeObject("yes");
                    objOut.flush();
                } else if (msg.equals("SELECT_HANDICAP") || msg.equals("RESELECT_HANDICAP")) {
                    // ハンディキャップ候補リストを待つ
                    Object next = objIn.readObject();
                    if (next instanceof List) {
                        List<?> options = (List<?>) next;
                        int idx = rand.nextInt(options.size());
                        objOut.writeObject(options.get(idx));
                        objOut.flush();
                    }
                }
            } else if (obj.getClass().getSimpleName().equals("GameUpdate")) {
                // GameUpdate型の受信
                GameUpdate update = (GameUpdate) obj;
                List<int[]> validPoints = update.validPoint;
                if (update.gameEnd) {
                    break;
                }
                if (update.myTurn && validPoints != null && !validPoints.isEmpty()) {
                    int[] move = validPoints.get(rand.nextInt(validPoints.size()));
                    objOut.writeObject(move);
                    objOut.flush();
                }
            }
        }
        objIn.close();
        objOut.close();
        socket.close();
    }
}