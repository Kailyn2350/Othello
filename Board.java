import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class Board {
    // 盤面状態を保持する配列 (0: 空, 1: 黒, 2: 白)
    private int[][] board = new int[8][8];
    // 置ける場所を保持する配列 (1: 置ける, 0: 置けない)
    private int[][] validPositions = new int[8][8];
    // 現在の手番 (1: 黒, 2: 白)
    private int currentTurn = 1;
    // 選択中のセルの座標
    private int selectedX = -1;
    private int selectedY = -1;
    // セルのサイズ
    private int cellSize = 50;
    // 黒と白の石の数
    private int blackCount = 0;
    private int whiteCount = 0;
    private ClickCallback clientClickCallback;

    private List<int[]> validPoints = new ArrayList<>();

    public interface ClickCallback {
        void onCellClicked(int row, int col);
    }

    public void setClickCallback(ClickCallback callback) {
        this.clientClickCallback = callback;
    }
    
    // 盤面を描画する
    public JPanel drawBoard() {
        JPanel boardPanel = new JPanel(new GridLayout(8 + 1, 8 + 1)); // 8x8の盤面 + ラベル行/列
        boardPanel.setPreferredSize(new Dimension(cellSize * 8, cellSize * 8));

        // ボタンサイズを計算
        int buttonSize = cellSize;

        // 空のコーナーセルを追加
        boardPanel.add(new JLabel());

        // 列ラベル (A-H) を追加
        for (int col = 0; col < 8; col++) {
            JLabel colLabel = new JLabel(String.valueOf((char) ('A' + col)), SwingConstants.CENTER);
            colLabel.setPreferredSize(new Dimension(buttonSize, buttonSize));
            boardPanel.add(colLabel);
        }

        for (int row = 0; row < 8; row++) {
            JLabel rowLabel = new JLabel(String.valueOf(row + 1), SwingConstants.CENTER);
            rowLabel.setPreferredSize(new Dimension(buttonSize, buttonSize));
            boardPanel.add(rowLabel);
        
            for (int col = 0; col < 8; col++) {
                final int r = row;
                final int c = col;
        
                JButton button = new JButton();
                button.setMinimumSize(new Dimension(buttonSize, buttonSize));
                button.setFont(new Font("Arial", Font.BOLD, buttonSize / 2));
        
                boolean isValid = false;
                for (int[] pt : validPoints) {
                    if (pt[0] == r && pt[1] == c) {
                        isValid = true;
                        break;
                    }
                }
        
                if (isValid) {
                    button.setBackground(new Color(144, 238, 144)); // 연두색
                } else {
                    button.setBackground(new Color(34, 139, 34)); // 기본 초록색
                }
        
                updateButton(button, r, c);
                button.setBorder(BorderFactory.createLineBorder(Color.BLACK, 2));
        
                button.addActionListener(e -> {
                    for (int[] pos : validPoints) {
                        if (pos[0] == r && pos[1] == c) {
                            if (clientClickCallback != null) {
                                clientClickCallback.onCellClicked(r, c);  // 콜백 호출
                            }
                        }
                    }
                });
        
                boardPanel.add(button);
            }
        }
        
        return boardPanel;
    }

    // ボタンのテキストと色を更新する
    private void updateButton(JButton button, int row, int col) {
        if (board[row][col] == 1) {
            button.setText("●");
            button.setForeground(Color.BLACK);
        } else if (board[row][col] == 2) {
            button.setText("●");
            button.setForeground(Color.WHITE);
        } else {
            // 착수 가능 위치 표시
            for (int[] pos : validPoints) {
                if (pos[0] == row && pos[1] == col) {
                    button.setText("○");
                    button.setForeground(Color.GRAY);
                    break;
                }
            }
        }
    }

    public void setValidPoints(List<int[]> validPoints) {
        this.validPoints = validPoints;
    }

    // 指定した座標に石を置く
    public void placeStone(int x, int y, int color) {
        if (x >= 0 && x < 8 && y >= 0 && y < 8) {
            board[x][y] = color;
        }
    }

    // 盤面を更新する
    public void updateBoard(int[][] boardState, List<int[]> validPoints) {
        for (int x = 0; x < 8; x++) {
            for (int y = 0; y < 8; y++) {
                board[x][y] = boardState[x][y];
            }
        }
        this.validPoints = validPoints;
    }

    // // 置ける場所を表示する
    // public void showValidPositions(List<int[]> positions) {
    // // まず、validPositions配列をリセット
    // for (int x = 0; x < 8; x++) {
    // for (int y = 0; y < 8; y++) {
    // validPositions[x][y] = 0;
    // }
    // }

    // // List<int[]>の情報をvalidPositions配列に反映
    // for (int[] position : positions) {
    // int x = position[0];
    // int y = position[1];
    // if (x >= 0 && x < 8 && y >= 0 && y < 8) {
    // validPositions[x][y] = 1; // 置ける場所を1に設定
    // }
    // }
    // }

    // 石の数を数える
    public void countStones() {
        blackCount = 0;
        whiteCount = 0;
        for (int x = 0; x < 8; x++) {
            for (int y = 0; y < 8; y++) {
                if (board[x][y] == 1) {
                    blackCount++;
                } else if (board[x][y] == 2) {
                    whiteCount++;
                }
            }
        }
    }

    // クリック座標をセル座標に変換する
    public int[] convertToCell(int pixelX, int pixelY) {
        int x = pixelX / cellSize;
        int y = pixelY / cellSize;
        return new int[] { x, y };
    }

    // 指定した座標に着手可能かを判定する
    public boolean canPlaceStone(int x, int y) {
        return x >= 0 && x < 8 && y >= 0 && y < 8 && validPositions[x][y] == 1;
    }

    // 選択状態をリセットする
    public void resetSelection() {
        selectedX = -1;
        selectedY = -1;
    }

    // 選択中のセルのX座標を取得する
    public int getSelectedX() {
        return selectedX;
    }

    // 選択中のセルのY座標を取得する
    public int getSelectedY() {
        return selectedY;
    }

    // GetterとSetter（必要に応じて追加）
    public int[][] getBoard() {
        return board;
    }

    public int[][] getValidPositions() {
        return validPositions;
    }

    public int getCurrentTurn() {
        return currentTurn;
    }

    public void setCurrentTurn(int currentTurn) {
        this.currentTurn = currentTurn;
    }

    public int getBlackCount() {
        return blackCount;
    }

    public int getWhiteCount() {
        return whiteCount;
    }

    public int getCellSize() {
        return cellSize;
    }

    public void setCellSize(int cellSize) {
        this.cellSize = cellSize;
    }
}
