import java.io.Serializable;
import java.util.List;


public class GameUpdate implements Serializable {
    private static final long serialVersionUID = 1L;
    public boolean isYourTurn;
    public int[][] board;
    public int handicap;
    public String message;
    public String yourName;
    public String opponentName;
    public List<int[]> validPoint;
    public boolean gameEnd;
    public int yourColor;

    public GameUpdate(
        boolean isYourTurn,
        int[][] board,
        int handicap,
        String message,
        String yourName,
        String opponentName,
        List<int[]> validPoint,
        boolean gameEnd,
        int yourColor // 1 = 黒, 2 = 白
    ) {
        this.isYourTurn = isYourTurn;
        this.board = board;
        this.handicap = handicap;
        this.message = message;
        this.yourName = yourName;
        this.opponentName = opponentName;
        this.validPoint = validPoint;
        this.gameEnd = gameEnd;
        this.yourColor = yourColor;
    }
    
}


