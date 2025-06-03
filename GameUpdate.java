import java.io.Serializable;
import java.util.List;

public class GameUpdate implements Serializable {
    private static final long serialVersionUID = 1L;
    public boolean myTurn;
    public int[][] board;
    public int handicap;
    public String message;
    public String firstPlayerName;
    public String secondPlayerName;
    public List<int[]> validPoint;
    public boolean gameEnd;
    public int yourColor;

    public GameUpdate(
        boolean myTurn,
        int[][] board,
        int handicap,
        String message,
        String firstPlayerName,
        String secondPlayerName,
        List<int[]> validPoint,
        boolean gameEnd,
        int yourColor // 1 = 黒, 2 = 白
    ) {
        this.myTurn = myTurn;
        this.board = board;
        this.handicap = handicap;
        this.message = message;
        this.firstPlayerName = firstPlayerName;
        this.secondPlayerName = secondPlayerName;
        this.validPoint = validPoint;
        this.gameEnd = gameEnd;
        this.yourColor = yourColor;
    }
}


