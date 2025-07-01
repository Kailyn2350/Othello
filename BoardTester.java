import java.util.ArrayList;
import java.util.List;
import javax.swing.*;

public class BoardTester {
    public static void main(String[] args) {
        // Boardインスタンス生成
        Board board = new Board();
        JFrame frame = new JFrame("Board Tester");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        // 初期盤面描画
        frame.getContentPane().add(board.drawBoard());
        frame.pack();
        frame.setVisible(true);

        // 盤面初期化テスト
        board.placeStone(3, 3, 2);
        board.placeStone(3, 4, 1);
        board.placeStone(4, 3, 1);
        board.placeStone(4, 4, 2);

        List<int[]> validPoints = new ArrayList<>();
        validPoints.add(new int[]{2, 3});
        validPoints.add(new int[]{5, 4});
        validPoints.add(new int[]{3, 2});
        validPoints.add(new int[]{4, 5});

        // 盤面状態を更新し、再描画
        board.updateBoard(board.getBoard(), validPoints);
        frame.getContentPane().removeAll();
        frame.getContentPane().add(board.drawBoard());
        frame.revalidate();
        frame.repaint();

        // 石の数を数えるテスト
        board.countStones();
        System.out.println("黒の数: " + board.getBlackCount());
        System.out.println("白の数: " + board.getWhiteCount());
    }
}
