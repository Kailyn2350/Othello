import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.awt.image.BufferedImage;

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
    private int cellSize = 60;
    // 黒と白の石の数
    private int blackCount = 0;
    private int whiteCount = 0;
    private ClickCallback clientClickCallback;

    private int lastPlacedRow = -1;
    private int lastPlacedCol = -1;

    private List<int[]> validPoints = new ArrayList<>();

    public interface ClickCallback {
        void onCellClicked(int row, int col);
    }

    public void setClickCallback(ClickCallback callback) {
        this.clientClickCallback = callback;
    }

    // 盤面を描画する
    public JComponent drawBoard() {
        JPanel boardPanel = new JPanel(new GridLayout(9, 9)); // 8x8 + label rows
        boardPanel.setBounds(0, 0, cellSize * 9, cellSize * 9);
        boardPanel.setPreferredSize(new Dimension(cellSize * 9, cellSize * 9));

        int buttonSize = cellSize;

        // 列ラベルのための空のラベルを追加
        boardPanel.add(new JLabel());

        // 列ラベル (A ~ H)
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
                    button.setBackground(new Color(144, 238, 144));
                } else {
                    button.setBackground(new Color(34, 139, 34));
                }

                updateButton(button, r, c);
                button.setBorder(BorderFactory.createLineBorder(Color.BLACK, 2));

                button.addActionListener(e -> {
                    for (int[] pos : validPoints) {
                        if (pos[0] == r && pos[1] == c) {
                            if (clientClickCallback != null) {
                                clientClickCallback.onCellClicked(r, c);
                            }
                        }
                    }
                });

                boardPanel.add(button);
            }
        }

        // 行ラベル (1 ~ 8)
        JPanel dotOverlay = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setColor(Color.BLACK);

                int[][] dotPoints = {
                        { 3, 3 },
                        { 3, 7 },
                        { 7, 3 },
                        { 7, 7 }
                };

                int dotSize = cellSize / 3;

                for (int[] point : dotPoints) {
                    int cx = point[1] * cellSize - dotSize / 2;
                    int cy = point[0] * cellSize - dotSize / 2;
                    g2d.fillOval(cx, cy, dotSize, dotSize);
                }

            }
        };
        dotOverlay.setOpaque(false);
        dotOverlay.setBounds(0, 0, cellSize * 9, cellSize * 9);

        // レイヤードペインを使用して、ドットを盤面の上に描画
        JLayeredPane layeredPane = new JLayeredPane();
        layeredPane.setPreferredSize(new Dimension(cellSize * 9, cellSize * 9));
        boardPanel.setBounds(0, 0, cellSize * 9, cellSize * 9);
        layeredPane.add(boardPanel, Integer.valueOf(0));
        layeredPane.add(dotOverlay, Integer.valueOf(1));
        return layeredPane;
    }

    // ボタンのテキストと色を更新する
    private void updateButton(JButton button, int row, int col) {
        button.setText(""); // テキストをクリア
        if (board[row][col] == 0) {
            if ((row == 2 && col == 2) || (row == 2 && col == 5) || (row == 5 && col == 2) || (row == 5 && col == 5)) {
                button.setIcon(createDotIcon());
            }
        }

        if (board[row][col] == 1) {
            if (row == lastPlacedRow && col == lastPlacedCol) {
                animatePlacement(button, Color.BLACK);
            } else {
                button.setIcon(createStoneIcon(Color.BLACK, 1.0f));
            }
        } else if (board[row][col] == 2) {
            if (row == lastPlacedRow && col == lastPlacedCol) {
                animatePlacement(button, Color.WHITE);
            } else {
                button.setIcon(createStoneIcon(Color.WHITE, 1.0f));
            }
        } else {
            boolean isValid = false;
            for (int[] pos : validPoints) {
                if (pos[0] == row && pos[1] == col) {
                    isValid = true;
                    break;
                }
            }

            if (isValid) {
                Color ghostColor = (currentTurn == 1) ? Color.BLACK : Color.WHITE;
                button.setIcon(createStoneIcon(ghostColor, 0.5f));
            } else {
                button.setIcon(null);
            }
        }

        button.setContentAreaFilled(false);
        button.setOpaque(true);
        button.setBackground(new Color(34, 139, 34)); // 濃い緑色
    }

    private Icon createStoneIcon(Color color, float alpha) {
        int size = (int) (cellSize * 0.8);
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = image.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
        g2d.setColor(color);
        g2d.fillOval(0, 0, size, size);

        g2d.dispose();
        return new ImageIcon(image);
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

    class ScalableBoardWrapper extends JPanel {
    private final JComponent boardContent;
    private final int baseSize;

    public ScalableBoardWrapper(JComponent boardContent, int baseSize) {
        this.boardContent = boardContent;
        this.baseSize = baseSize;
        setLayout(null); 
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g.create();

        int panelW = getWidth();
        int panelH = getHeight();
        float scale = Math.min(panelW, panelH) / (float) baseSize;

        g2d.scale(scale, scale);

        // 盤面を描画
        boardContent.paint(g2d);
        g2d.dispose();
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(baseSize, baseSize);
    }
}

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

    private void animatePlacement(JButton button, Color color) {
        Timer timer = new Timer(15, null);
        final int maxFrame = 10;
        final int[] frame = { 0 };

        timer.addActionListener(e -> {
            float progress = (float) frame[0] / maxFrame;

            // アニメーションのパラメータ
            float scale = 1.2f - 0.2f * progress;
            float alpha = 0.3f + 0.7f * progress; // 透明度
            button.setIcon(createAnimatedStoneIcon(color, alpha, scale));
            frame[0]++;
            if (frame[0] > maxFrame) {
                // アニメーション終了時に石を置く
                button.setIcon(createStoneIcon(color, 1.0f));
                ((Timer) e.getSource()).stop();
            }
        });
        timer.start();
    }

    private Icon createDotIcon() {
        BufferedImage image = new BufferedImage(cellSize, cellSize, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = image.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int dotSize = cellSize / 6;
        int offset = (cellSize - dotSize) / 2;

        g2d.setColor(Color.BLACK);
        g2d.fillOval(offset, offset, dotSize, dotSize);

        g2d.dispose();
        return new ImageIcon(image);
    }

    private Icon createAnimatedStoneIcon(Color color, float alpha, float scale) {
        int size = (int) (cellSize * scale);
        BufferedImage image = new BufferedImage(cellSize, cellSize, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = image.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
        g2d.setColor(color);

        int offset = (cellSize - size) / 2;
        g2d.fillOval(offset, offset, size, size);
        g2d.dispose();
        return new ImageIcon(image);
    }

    // クリック座標をセル座標に変換する
    public int[] convertToCell(int pixelX, int pixelY) {
        int x = pixelX / cellSize;
        int y = pixelY / cellSize;
        return new int[] { x, y };
    }

    // セル座標をクリックする
    public void setLastPlacedPosition(int row, int col) {
        this.lastPlacedRow = row;
        this.lastPlacedCol = col;
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
