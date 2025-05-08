import java.awt.*;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import javax.swing.*;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

public class client extends JFrame {
    private static final int BOARD_SIZE = 8;
    private int board[][] = new int[BOARD_SIZE][BOARD_SIZE]; // 0: 空, 1: 黒, 2: 白
    private String serverIpAddress; // サーバーのIPアドレス
    private ObjectOutputStream objOut;
    private ObjectInputStream objIn;
    private Timer animationTimer;
    private boolean nicknameDialogShown = false;
    private int[] HandicapChoices = new int[6];
    private BlockingQueue<Object> messageQueue = new LinkedBlockingQueue<>();
    private Thread receiverThread, processorThread;
    private Board boardInstance = new Board();
    private boolean isWaitingForHandicap = false; // ハンディキャップ選択待ちフラグ
    private JLabel turnMessageLabel = new JLabel("", SwingConstants.CENTER);
    private List<int[]> validPoints = new ArrayList<>();
    private boolean turnMessageShown = false; // 手番メッセージ表示フラグ

    /*
     * ０ : ハンディなし、
     * １：引き分け勝ち：石の数が同数の場合に黒の勝ちとする
     * ２：1子局：左上隅に黒石を置いて対局を開始する
     * ３：2子局：左上隅と右下隅に黒石を置いて対局を開始する
     * ４：3子局：左上隅、右下隅、右上隅に黒石を置いて対局を開始する
     * ５：4子局：４か所全ての隅に黒石を置いて対局を開始する
     */

    Socket socket; // ソケット通信のインスタンス

    public client() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(600, 600);
        setLayout(new FlowLayout());
        setTitle("Othello");
        showIpInputDialog(); // IPアドレス入力欄を表示
        // Replace board setup with showBoard method
        setVisible(true);
    }

    private void showIpInputDialog() {
        getContentPane().removeAll(); // メインウィンドウの内容をクリア
        setLayout(new GridBagLayout());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel label = new JLabel("サーバーのIPアドレス:");
        JTextField ipField = new JTextField(15);
        JButton okButton = new JButton("OK");
        JButton clearButton = new JButton("クリア");

        okButton.addActionListener(_ -> {
            serverIpAddress = ipField.getText(); // 入力されたIPアドレスを保存
            connectToServer(serverIpAddress); // サーバーに接続
        });

        // Action for Clear button
        clearButton.addActionListener(_ -> {
            ipField.setText(""); // 入力フィールドをクリア
        });

        // 配置
        gbc.gridx = 0;
        gbc.gridy = 0;
        add(label, gbc);

        gbc.gridx = 1;
        gbc.gridy = 0;
        add(ipField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        JPanel buttonPanel = new JPanel(new FlowLayout());
        buttonPanel.add(okButton);
        buttonPanel.add(clearButton);
        add(buttonPanel, gbc);

        revalidate();
        repaint();
    }

    // サーバーに接続するメソッド
    private void connectToServer(String ipAddress) {
        try {
            socket = new Socket(ipAddress, 10000);
            System.out.println("サーバーに接続しました: " + ipAddress);

            objOut = new ObjectOutputStream(socket.getOutputStream());
            objOut.flush();
            objIn = new ObjectInputStream(socket.getInputStream());

            SwingUtilities.invokeLater(() -> showWaitingForOpponentScreen());

            startUnifiedReceiverThread();

        } catch (Exception e) {
            System.err.println("サーバーへの接続に失敗しました: " + e.getMessage());
            JOptionPane.showMessageDialog(this, "Failed to connect to the server. Please try again.",
                    "Connection Error", JOptionPane.ERROR_MESSAGE);
            SwingUtilities.invokeLater(() -> {
                getContentPane().removeAll();
                showIpInputDialog();
            });
        }
    }

    private void showWaitingForOpponentScreen() {
        getContentPane().removeAll();
        setLayout(new GridBagLayout());

        JLabel label = new JLabel("対戦相手の接続を待っています.");
        label.setFont(new Font("Dialog", Font.PLAIN, 16));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(20, 20, 20, 20);
        gbc.gridx = 0;
        gbc.gridy = 0;
        add(label, gbc);

        revalidate();
        repaint();

        String[] messages = {
                "対戦相手の接続を待っています.",
                "対戦相手の接続を待っています..",
                "対戦相手の接続を待っています...",
                "対戦相手の接続を待っています...."
        };

        // 500msごとにメッセージを切り替えるアニメーション
        Timer animationTimer = new Timer(500, new ActionListener() {
            int index = 0;

            @Override
            public void actionPerformed(ActionEvent e) {
                label.setText(messages[index]);
                index = (index + 1) % messages.length;
            }
        });
        animationTimer.start();

    }

    private void showNicknameInputDialog() {
        getContentPane().removeAll();
        setLayout(new GridBagLayout());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel label = new JLabel("ニックネーム:");
        JTextField nicknameField = new JTextField(15);
        JButton okButton = new JButton("OK");

        okButton.addActionListener(_ -> {
            String nickname = nicknameField.getText();
            System.out.println("ニックネーム: " + nickname);
            try {
                // サーバーにニックネームを送信
                objOut.writeObject(nickname);
                objOut.flush();
            } catch (IOException ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(this, "通信中にエラーが発生しました。", "通信エラー", JOptionPane.ERROR_MESSAGE);
            }
        });

        gbc.gridx = 0;
        gbc.gridy = 0;
        add(label, gbc);

        gbc.gridx = 1;
        gbc.gridy = 0;
        add(nicknameField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        add(okButton, gbc);

        revalidate();
        repaint();
    }

    private void showWaitingForOtherNicknameScreen() {
        getContentPane().removeAll();
        setLayout(new GridBagLayout());

        JLabel label = new JLabel("相手がニックネームを入力中です...");
        label.setFont(new Font("Dialog", Font.PLAIN, 16));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(20, 20, 20, 20);
        gbc.gridx = 0;
        gbc.gridy = 0;
        add(label, gbc);

        revalidate();
        repaint();

        String[] messages = {
                "相手がニックネームを入力中です.",
                "相手がニックネームを入力中です..",
                "相手がニックネームを入力中です...",
                "相手がニックネームを入力中です...."
        };

        // 500ms ごとにメッセージを切り替えるアニメーション
        animationTimer = new Timer(500, new ActionListener() {
            int index = 0;

            @Override
            public void actionPerformed(ActionEvent e) {
                label.setText(messages[index]);
                index = (index + 1) % messages.length;
            }
        });
        animationTimer.start();
    }

    // ハンディキャップ選択画面を表示するメソッド
    private void showHandicapSelectionDialog(boolean isFirstPlayer) {
        getContentPane().removeAll();
        setLayout(new GridBagLayout());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        if (isFirstPlayer) {
            JLabel label = new JLabel("Please select a handicap:");
            label.setFont(new Font("Dialog", Font.BOLD, 16));

            List<String> handicapOptionsList = new ArrayList<>();
            for (int i = 0; i <= 5; i++) {
                if (HandicapChoices[i] == 0) {
                    handicapOptionsList.add(String.valueOf(i));
                }
            }

            String[] handicapOptions = handicapOptionsList.toArray(new String[0]);
            JComboBox<String> handicapDropdown = new JComboBox<>(handicapOptions);
            JButton okButton = new JButton("OK");

            okButton.addActionListener(_ -> {
                String selectedHandicap = (String) handicapDropdown.getSelectedItem();
                try {
                    int candidate = Integer.parseInt(selectedHandicap);
                    objOut.writeObject(candidate); // サーバーに送信
                    objOut.flush();

                    showWaitingForApprovalScreen(); // 承認待ち画面のみ表示
                    // animationTimer.stop(); // アニメーションを停止

                } catch (IOException | NumberFormatException ex) {
                    ex.printStackTrace();
                }
            });

            gbc.gridx = 0;
            gbc.gridy = 0;
            add(label, gbc);

            gbc.gridx = 1;
            gbc.gridy = 0;
            add(handicapDropdown, gbc);

            gbc.gridx = 0;
            gbc.gridy = 1;
            gbc.gridwidth = 2;
            gbc.anchor = GridBagConstraints.CENTER;
            add(okButton, gbc);

        } else {
            JLabel waitingLabel = new JLabel("対戦相手がハンディキャップを選択しています.");
            waitingLabel.setFont(new Font("Dialog", Font.BOLD, 16));

            gbc.gridx = 0;
            gbc.gridy = 0;
            add(waitingLabel, gbc);

            String[] messages = {
                    "対戦相手がハンディキャップを選択しています.",
                    "対戦相手がハンディキャップを選択しています..",
                    "対戦相手がハンディキャップを選択しています...",
                    "対戦相手がハンディキャップを選択しています...."
            };

            Timer animationTimer = new Timer(500, new ActionListener() {
                int index = 0;

                @Override
                public void actionPerformed(ActionEvent e) {
                    waitingLabel.setText(messages[index]);
                    index = (index + 1) % messages.length;
                }
            });
            animationTimer.start();
        }

        revalidate();
        repaint();
    }

    private void showWaitingForApprovalScreen() {
        getContentPane().removeAll();
        setLayout(new GridBagLayout());

        JLabel waitingLabel = new JLabel("対戦相手がハンディキャップを承認するのを待っています.");
        waitingLabel.setFont(new Font("Dialog", Font.BOLD, 16));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(20, 20, 20, 20);
        gbc.gridx = 0;
        gbc.gridy = 0;
        add(waitingLabel, gbc);

        revalidate();
        repaint();

        String[] messages = {
                "対戦相手がハンディキャップを承認するのを待っています.",
                "対戦相手がハンディキャップを承認するのを待っています..",
                "対戦相手がハンディキャップを承認するのを待っています...",
                "対戦相手がハンディキャップを承認するのを待っています...."
        };

        Timer animationTimer = new Timer(500, new ActionListener() {
            int index = 0;

            @Override
            public void actionPerformed(ActionEvent e) {
                waitingLabel.setText(messages[index]);
                index = (index + 1) % messages.length;
            }
        });
        animationTimer.start();

    }

    // 盤面を表示するメソッド
    private void showGameScreen(String message) {
        getContentPane().removeAll();
        setLayout(new BorderLayout());

        // 1. 盤面を表示
        boardInstance.setClickCallback((row, col) -> {
            try {
                System.out.println("Clicked cell: " + row + ", " + col);
                objOut.writeObject(new int[] { row, col }); 
                objOut.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        // 2. 盤面を更新
        boardInstance.updateBoard(this.board, this.validPoints);
        JPanel boardPanel = boardInstance.drawBoard();
        add(boardPanel, BorderLayout.CENTER);

        // 3. ステータスラベルを表示
        turnMessageLabel.setText(message);
        turnMessageLabel.setFont(new Font("Dialog", Font.BOLD, 18));
        turnMessageLabel.setHorizontalAlignment(SwingConstants.CENTER);
        turnMessageLabel.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
        add(turnMessageLabel, BorderLayout.SOUTH);

        revalidate();
        repaint();
    }

    // 統一された受信スレッドを開始するメソッド
    private void startUnifiedReceiverThread() {
        receiverThread = new Thread(() -> {
            try {
                while (true) {
                    Object obj = objIn.readObject();
                    messageQueue.put(obj); 
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        processorThread = new Thread(() -> {
            try {
                while (true) {
                    Object obj = messageQueue.take(); // BlockingQueueからオブジェクトを取得
                    SwingUtilities.invokeLater(() -> processReceivedObject(obj));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        receiverThread.start();
        processorThread.start();
    }

    private void processReceivedObject(Object obj) {
        if (obj instanceof String) {
            handleServerMessage((String) obj);
        } else if (obj instanceof GameUpdate) {
            handleGameUpdate((GameUpdate) obj);
        } else if (obj instanceof List<?>) {
            handleHandicapCandidates((List<?>) obj);
        } else {
            System.out.println("未知のオブジェクトを受信: " + obj.getClass().getName());
        }
    }

    private void handleServerMessage(String msg) {
        System.out.println("サーバーからのメッセージ: " + msg);

        switch (msg) {
            case "READY_FOR_NICKNAME":
                if (!nicknameDialogShown) {
                    nicknameDialogShown = true;
                    SwingUtilities.invokeLater(this::showNicknameInputDialog);
                }
                break;

            case "DUPLICATE":
                JOptionPane.showMessageDialog(this, "ニックネームが重複しています。別のニックネームを入力してください。");
                SwingUtilities.invokeLater(this::showNicknameInputDialog);
                break;

            case "WAITING_FOR_OTHER":
                SwingUtilities.invokeLater(this::showWaitingForOtherNicknameScreen);
                break;

            case "WAIT_FOR_HANDICAP":
                // 対戦相手がハンディキャップを選択中
                if (!turnMessageShown) {
                    isWaitingForHandicap = true;
                    SwingUtilities.invokeLater(() -> {
                        showHandicapSelectionDialog(false);
                    });
                }
                break;

            case "OK":
                SwingUtilities.invokeLater(() -> {
                    showWaitingForOpponentScreen();
                });
                break;

            case "SELECT_HANDICAP":
                SwingUtilities.invokeLater(() -> {
                    showHandicapSelectionDialog(true);
                });
                break;

            case "RESELECT_HANDICAP":
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(this, "対戦相手がハンディキャップを拒否しました。再選択してください。",
                            "再選択", JOptionPane.INFORMATION_MESSAGE);
                    showHandicapSelectionDialog(true);
                });
                break;

            default:
                if (msg.contains("承認しますか？")) {
                    new Thread(() -> {
                        try {
                            SwingUtilities.invokeAndWait(() -> {
                                int result = JOptionPane.showConfirmDialog(this, msg, "ハンディキャップ承認",
                                        JOptionPane.YES_NO_OPTION);
                                sendText(result == JOptionPane.YES_OPTION ? "yes" : "no");
                                isWaitingForHandicap = false;
                            });
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }).start();
                } else {
                    SwingUtilities.invokeLater(
                            () -> JOptionPane.showMessageDialog(this, msg, "情報", JOptionPane.INFORMATION_MESSAGE));
                }
        }
    }

    private void handleGameUpdate(GameUpdate gameUpdate) {
        if (gameUpdate == null || gameUpdate.board == null)
            return;

        this.board = gameUpdate.board;
        this.validPoints = gameUpdate.validPoint != null ? gameUpdate.validPoint : new ArrayList<>();
        printBoard(board);

        if (isWaitingForHandicap) {
            return;
        }

        SwingUtilities.invokeLater(() -> {
            boardInstance.updateBoard(this.board, this.validPoints);
            showGameScreen(gameUpdate.message);
            // 手番メッセージを表示
            if ((gameUpdate.message.contains("先手") || gameUpdate.message.contains("後手")) &&
                    !turnMessageShown && !gameUpdate.gameEnd) {
                JOptionPane.showMessageDialog(this,
                        gameUpdate.message,
                        "あなたの手番情報",
                        JOptionPane.INFORMATION_MESSAGE);
                turnMessageShown = true;
            }
            // ゲーム終了時の処理
            if (gameUpdate.gameEnd) {
                int black = 0, white = 0;
                for (int i = 0; i < BOARD_SIZE; i++) {
                    for (int j = 0; j < BOARD_SIZE; j++) {
                        if (board[i][j] == 1)
                            black++;
                        if (board[i][j] == 2)
                            white++;
                    }
                }

                int handicap = gameUpdate.handicap;
                String winner;
                String personalResult;
                int yourColor = gameUpdate.yourColor;
                boolean youAreBlack = (yourColor == 1);

                // 勝者の判定
                if (black > white || (black == white && handicap == 1)) {
                    winner = "黒";
                } else if (white > black) {
                    winner = "白";
                } else {
                    winner = "引き分け";
                }

                // 個人結果の判定
                if (winner.equals("引き分け")) {
                    personalResult = "引き分けです";
                } else if ((winner.equals("黒") && youAreBlack) || (winner.equals("白") && !youAreBlack)) {
                    personalResult = "あなたの勝ちです";
                } else {
                    personalResult = "あなたの負けです";
                }

                // 結果メッセージの作成
                String result;
                if (winner.equals("黒")) {
                    result = "黒の勝ち！ (" + black + " 対 " + white + ")";
                } else if (winner.equals("白")) {
                    result = "白の勝ち！ (" + white + " 対 " + black + ")";
                } else {
                    result = "引き分けです！ (" + black + " 対 " + white + ")";
                }

                // 最終メッセージの作成
                String finalMessage = result + "\n" + personalResult;

                JOptionPane.showMessageDialog(this,
                        "ゲームが終了しました。\n",
                        "ゲーム結果",
                        JOptionPane.INFORMATION_MESSAGE);

                showResultScreen(black, white, finalMessage);

            }
        });
    }

    private void showResultScreen(int black, int white, String resultText) {
        getContentPane().removeAll();
        setLayout(new GridBagLayout());
    
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(15, 15, 15, 15);
        gbc.fill = GridBagConstraints.HORIZONTAL;
    
        JLabel title = new JLabel("ゲーム結果");
        title.setFont(new Font("Dialog", Font.BOLD, 20));
        title.setHorizontalAlignment(SwingConstants.CENTER);
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        add(title, gbc);
    
        gbc.gridwidth = 1;
        gbc.gridy = 1;
        add(new JLabel("黒の石:"), gbc);
        gbc.gridx = 1;
        add(new JLabel(String.valueOf(black)), gbc);
    
        gbc.gridx = 0;
        gbc.gridy = 2;
        add(new JLabel("白の石:"), gbc);
        gbc.gridx = 1;
        add(new JLabel(String.valueOf(white)), gbc);
    
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 2;
        JLabel result = new JLabel(resultText.split("\n")[0]);  // ex : "黒の勝ち！ (34 対 30)"
        result.setFont(new Font("Dialog", Font.BOLD, 16));
        result.setHorizontalAlignment(SwingConstants.CENTER);
        add(result, gbc);
    
        // 個人結果ラベル
        gbc.gridy = 4;
        JLabel personalResultLabel = new JLabel(resultText.split("\n")[1]);  // ex : "あなたの勝ちです"
        personalResultLabel.setFont(new Font("Dialog", Font.BOLD, 24));
        personalResultLabel.setForeground(new Color(0, 102, 204)); 
        personalResultLabel.setHorizontalAlignment(SwingConstants.CENTER);
        add(personalResultLabel, gbc);
    
        // ボタンパネル
        gbc.gridy = 5;
        JPanel buttonPanel = new JPanel(new FlowLayout());
        JButton retryButton = new JButton("再試合");
        retryButton.addActionListener(_ -> {
            try {
                if (socket != null) socket.close();
                if (objIn != null) objIn.close();
                if (objOut != null) objOut.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            nicknameDialogShown = false;
            isWaitingForHandicap = false;
            board = new int[BOARD_SIZE][BOARD_SIZE];
            validPoints.clear();
            turnMessageShown = false;
            showIpInputDialog();
        });
    
        JButton exitButton = new JButton("終了");
        exitButton.addActionListener(_ -> System.exit(0));
        buttonPanel.add(retryButton);
        buttonPanel.add(exitButton);
        add(buttonPanel, gbc);
    
        revalidate();
        repaint();
    }
    

    private void handleHandicapCandidates(List<?> rawList) {
        List<Integer> handicapCandidates = new ArrayList<>();
        for (Object obj : rawList) {
            if (obj instanceof Integer) {
                handicapCandidates.add((Integer) obj);
            }
        }

        for (int i = 0; i <= 5; i++) {
            HandicapChoices[i] = handicapCandidates.contains(i) ? 0 : 1;
        }
        System.out.println("ハンディキャップ候補を受信しました: " + handicapCandidates);
    }

    // サーバーにメッセージを送信するメソッド
    private void sendText(String msg) {
        try {
            objOut.writeObject(msg);
            objOut.flush();
            System.out.println("サーバーへ送信: " + msg);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void printBoard(int[][] board) {
        System.out.println("=== クライアントで受信したボード状態 ===");
        for (int i = 0; i < board.length; i++) {
            for (int j = 0; j < board[i].length; j++) {
                System.out.print(board[i][j] + " ");
            }
            System.out.println();
        }
        System.out.println("===================================");
    }

    public static void main(String[] args) {
        javax.swing.SwingUtilities.invokeLater(() -> new client());
    }

}