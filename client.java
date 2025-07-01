import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import javax.swing.*;

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
    private int handicap = 0; // ハンディキャップの値
    private String firstPlayerName = ""; // 先手プレイヤー名
    private String secondPlayerName = ""; // 後手プレイヤー名
    private int yourColor; // 自分の色 (1: 黒, 2: 白)
    private boolean resultAlreadyShown = false; // 結果画面を既に表示したか

    private boolean receivedDisconnectMessage = false;

    enum ClientState {
        IDLE, // 初期状態
        NICKNAME_ENTERED, // ニックネーム入力済み
        WAITING_FOR_HANDICAP, // ハンディキャップ選択待ち
        GAME_RUNNING, // ゲーム中
        GAME_OVER // ゲーム終了
    }

    private ClientState clientState = ClientState.IDLE;
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
        setSize(553, 655);
        setResizable(false);
        setLayout(new FlowLayout());
        setTitle("Othello");

        JPanel panel = new JPanel() {
            private Image backgroundImage = new ImageIcon("オセロ背景.png").getImage();

            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                g.drawImage(backgroundImage, 0, 0, getWidth(), getHeight(), this);
            }
        };
        setContentPane(panel);
        showIpInputDialog(panel); // IPアドレス入力欄を表示
        // Replace board setup with showBoard method
        setVisible(true);
    }

    private void showIpInputDialog(JPanel panel) {
        panel.removeAll(); // メインウィンドウの内容をクリア
        panel.setLayout(new GridBagLayout());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel titleLabel = new JLabel("Othello Game");
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
        titleLabel.setFont(new Font("Arial", Font.BOLD | Font.ITALIC, 48));
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2; // 横に2つ分のスペースを占有
        gbc.anchor = GridBagConstraints.CENTER; // 中央揃え
        panel.add(titleLabel, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 1;
        panel.add(label, gbc);

        gbc.gridx = 1;
        panel.add(ipField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        JPanel buttonPanel = new JPanel(new FlowLayout());
        buttonPanel.setOpaque(false);
        buttonPanel.add(okButton);
        buttonPanel.add(clearButton);
        panel.add(buttonPanel, gbc);

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
                showIpInputDialog((JPanel) getContentPane());
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

        if (animationTimer != null && animationTimer.isRunning()) {
            animationTimer.stop();
        }

        // 500msごとにメッセージを切り替えるアニメーション
        animationTimer = new Timer(500, new ActionListener() {
            int index = 0;

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
            String nickname = nicknameField.getText().trim(); // 入力されたニックネームを取得

            if (nickname.isEmpty() || nickname.contains(" ")) {
                JOptionPane.showMessageDialog(this,
                        "ニックネームには空白や空文字は使用できません。",
                        "無効なニックネーム",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }

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

        if (animationTimer != null && animationTimer.isRunning()) {
            animationTimer.stop();
        }

        animationTimer = new Timer(500, new ActionListener() {
            int index = 0;

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
            JLabel label = new JLabel("ハンディキャップを選択してください:");
            label.setFont(new Font("Dialog", Font.BOLD, 16));

            // ハンディキャップのラベルと値の対応
            String[] handicapLabels = {
                    "０ : ハンディなし",
                    "１ : 引き分け勝ち",
                    "２ : 1子局",
                    "３ : 2子局",
                    "４ : 3子局",
                    "５ : 4子局"
            };

            List<String> availableHandicapLabels = new ArrayList<>();
            for (int i = 0; i <= 5; i++) {
                if (HandicapChoices[i] == 0) {
                    availableHandicapLabels.add(handicapLabels[i]);
                }
            }

            JComboBox<String> handicapDropdown = new JComboBox<>(availableHandicapLabels.toArray(new String[0]));
            JButton okButton = new JButton("OK");

            okButton.addActionListener(_ -> {
                String selectedLabel = (String) handicapDropdown.getSelectedItem();
                try {
                    // ラベルから対応する数値を取得
                    int candidate = -1;
                    for (int i = 0; i < handicapLabels.length; i++) {
                        if (handicapLabels[i].equals(selectedLabel)) {
                            candidate = i;
                            break;
                        }
                    }

                    if (candidate != -1) {
                        objOut.writeObject(candidate); // サーバーに送信
                        objOut.flush();
                        showWaitingForApprovalScreen(); // 承認待ち画面を表示
                    }
                } catch (IOException ex) {
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

            if (animationTimer != null && animationTimer.isRunning()) {
                animationTimer.stop();
            }

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

        if (animationTimer != null && animationTimer.isRunning()) {
            animationTimer.stop();
        }
        animationTimer = new Timer(500, new ActionListener() {
            int index = 0;

            public void actionPerformed(ActionEvent e) {
                waitingLabel.setText(messages[index]);
                index = (index + 1) % messages.length;
            }
        });
        animationTimer.start();

    }

    // 盤面を表示するメソッド
    private void showGameScreen(String message) {

        if (animationTimer != null && animationTimer.isRunning()) {
            animationTimer.stop();
        }

        getContentPane().removeAll();
        setLayout(new BorderLayout());

        // 画面上部に先手と後手のプレイヤー名を表示 (例: "先手のプレイヤー vs 後手のプレイヤー")
        JPanel headerPanel = new JPanel(new FlowLayout());
        JLabel headerLabel = new JLabel(firstPlayerName + " vs " + secondPlayerName);
        headerLabel.setFont(new Font("Dialog", Font.BOLD, 18));
        headerPanel.add(headerLabel);
        add(headerPanel, BorderLayout.NORTH);

        // 1. 盤面を表示
        boardInstance.setClickCallback((row, col) -> {
            try {
                // サーバーに着手を送信
                objOut.writeObject(new int[] { row, col });
                objOut.flush();

                // 盤面を更新
                boardInstance.setLastPlacedPosition(row, col);

                // UIを更新
                boardInstance.placeStone(row, col, yourColor);
                boardInstance.updateBoard(boardInstance.getBoard(), validPoints);
                repaint();

            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        // 2. 盤面を更新
        boardInstance.setCurrentTurn(this.yourColor);
        boardInstance.updateBoard(this.board, this.validPoints);
        JComponent boardPanel = boardInstance.drawBoard();
        add(boardPanel, BorderLayout.CENTER);

        // 3. 手番メッセージと手数、及び色表示のパネルを作成
        turnMessageLabel.setText(message);
        turnMessageLabel.setFont(new Font("Dialog", Font.BOLD, 18));
        turnMessageLabel.setHorizontalAlignment(SwingConstants.CENTER);
        turnMessageLabel.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));

        JLabel moveCountLabel = new JLabel(getMoveCountText());
        moveCountLabel.setFont(new Font("Dialog", Font.BOLD, 18));
        moveCountLabel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // 自分の色に応じたメッセージ (先手は黒、後手は白) を表示
        String myColorMessage = (yourColor == 1) ? "あなたは黒です" : "あなたは白です";
        JLabel colorLabel = new JLabel(myColorMessage);
        colorLabel.setFont(new Font("Dialog", Font.BOLD, 18));
        colorLabel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel statusPanel = new JPanel(new BorderLayout());
        statusPanel.add(moveCountLabel, BorderLayout.WEST);
        statusPanel.add(turnMessageLabel, BorderLayout.CENTER);
        statusPanel.add(colorLabel, BorderLayout.EAST);
        add(statusPanel, BorderLayout.SOUTH);

        revalidate();
        repaint();
    }

    // 手数を計算してテキストとして返すヘルパーメソッド
    private String getMoveCountText() {
        int placedStones = 0;
        for (int[] row : board) {
            for (int cell : row) {
                if (cell != 0)
                    placedStones++;
            }
        }
        // ハンディキャップが 0 の場合は初期配置は 4 個、その他の場合は 4 + (handicap - 1)
        int initialStones = (handicap == 0) ? 4 : 4 + (handicap - 1);
        int moveNumber = placedStones - initialStones + 1;
        if (moveNumber < 1) {
            moveNumber = 1;
        }
        return moveNumber + "手目";
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
                System.err.println("通信エラー: " + e.getMessage());

                // 既にDISCONNECTEDメッセージを処理した場合は無視
                if (receivedDisconnectMessage || resultAlreadyShown || clientState == ClientState.GAME_OVER) {
                    System.out.println("既にDISCONNECTED処理済みのため、例外を無視します。");
                    return;
                }

                // messageQueue内にDISCONNECTEDがない場合のみput
                if (!messageQueue.contains("DISCONNECTED")) {
                    try {
                        messageQueue.put("DISCONNECTED");
                    } catch (InterruptedException ignored) {
                    }
                }
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

    private int countStones(int color) {
        int count = 0;
        for (int[] row : board) {
            for (int cell : row) {
                if (cell == color)
                    count++;
            }
        }
        return count;
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

            case "DISCONNECTED":
                if (receivedDisconnectMessage)
                    return;
                receivedDisconnectMessage = true;

                if (animationTimer != null && animationTimer.isRunning()) {
                    animationTimer.stop();
                }

                if (resultAlreadyShown) {
                    System.out.println("結果画面はすでに表示されたため、DISCONNECTED メッセージを無視します。");
                    return;
                }

                if (clientState != ClientState.GAME_OVER) {
                    clientState = ClientState.GAME_OVER;

                    int black = countStones(1);
                    int white = countStones(2);

                    JOptionPane.showMessageDialog(this,
                            "相手が接続を切断しました。\nゲームを終了します。",
                            "接続終了", JOptionPane.INFORMATION_MESSAGE);

                    showResultScreen(black, white, "相手が退出しました。\n勝敗はありません。");
                }
                return;

            case "DUPLICATE":
                JOptionPane.showMessageDialog(this, "ニックネームが重複しています。別のニックネームを入力してください。");
                SwingUtilities.invokeLater(this::showNicknameInputDialog);
                break;

            case "WAITING_FOR_OTHER":
                clientState = ClientState.NICKNAME_ENTERED;

                SwingUtilities.invokeLater(() -> {
                    showWaitingForOtherNicknameScreen();
                });

                break;

            case "WAIT_FOR_HANDICAP":
                SwingUtilities.invokeLater(() -> showHandicapSelectionDialog(false));
                break;

            case "OK":
                isWaitingForHandicap = false; // ハンディキャップ選択待ちフラグを解除
                break;

            case "SELECT_HANDICAP":
                SwingUtilities.invokeLater(() -> showHandicapSelectionDialog(true));
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
                                // ハンディキャップ番号から内容を取得
                                String[] handicapDescriptions = {
                                        "０ : ハンディなし",
                                        "１ : 引き分け勝ち",
                                        "２ : 1子局",
                                        "３ : 2子局",
                                        "４ : 3子局",
                                        "５ : 4子局"
                                };

                                // メッセージからハンディキャップ番号を抽出
                                int handicapNumber = -1;
                                try {
                                    String numberPart = msg.replaceAll("[^0-9]", ""); // 数字部分を抽出
                                    handicapNumber = Integer.parseInt(numberPart);
                                } catch (NumberFormatException e) {
                                    e.printStackTrace();
                                }

                                // ハンディキャップ内容を取得
                                String handicapDescription = (handicapNumber >= 0
                                        && handicapNumber < handicapDescriptions.length)
                                                ? handicapDescriptions[handicapNumber]
                                                : "不明なハンディキャップ";

                                // 承認ダイアログを表示
                                String fullMessage = msg + "\n" + "内容: " + handicapDescription;
                                int result = JOptionPane.showConfirmDialog(this, fullMessage, "ハンディキャップ承認",
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
        this.handicap = gameUpdate.handicap;
        this.firstPlayerName = gameUpdate.firstPlayerName;
        this.secondPlayerName = gameUpdate.secondPlayerName;
        this.yourColor = gameUpdate.yourColor;
        printBoard(board);

        if (gameUpdate.gameEnd) {
            clientState = ClientState.GAME_OVER; // ゲーム終了状態に遷移
            receivedDisconnectMessage = true;
            resultAlreadyShown = true;
        }

        SwingUtilities.invokeLater(() -> {
            if (animationTimer != null && animationTimer.isRunning()) {
                animationTimer.stop();
            }

            if (gameUpdate.gameEnd) {
                boardInstance.setCurrentTurn(this.yourColor);
                boardInstance.updateBoard(this.board, this.validPoints);
                showGameScreen(gameUpdate.message);

                // ゲーム終了後のメッセージを表示
                Timer delayTimer = new Timer(100, new ActionListener() {
                    public void actionPerformed(ActionEvent evt) {
                        int black = countStones(1);
                        int white = countStones(2);

                        String winner;
                        String personalResult;
                        boolean youAreBlack = (yourColor == 1);

                        if (black > white || (black == white && handicap == 1)) {
                            winner = "黒";
                        } else if (white > black) {
                            winner = "白";
                        } else {
                            winner = "引き分け";
                        }

                        if (winner.equals("引き分け")) {
                            personalResult = "引き分けです";
                        } else if ((winner.equals("黒") && youAreBlack) || (winner.equals("白") && !youAreBlack)) {
                            personalResult = "あなたの勝ちです";
                        } else {
                            personalResult = "あなたの負けです";
                        }

                        String result = (winner.equals("引き分け"))
                                ? "引き分けです！ (" + black + " 対 " + white + ")"
                                : winner + "の勝ち！ (" + black + " 対 " + white + ")";

                        String finalMessage = result + "\n" + personalResult;

                        JOptionPane.showMessageDialog(client.this,
                                "ゲームが終了しました。\n",
                                "ゲーム結果",
                                JOptionPane.INFORMATION_MESSAGE);

                        showResultScreen(black, white, finalMessage);
                    }
                });
                delayTimer.setRepeats(false);
                delayTimer.start();

                return;
            }
            boardInstance.setCurrentTurn(this.yourColor);
            boardInstance.updateBoard(this.board, this.validPoints);

            // 手番メッセージを更新
            if (!gameUpdate.myTurn && gameUpdate.message.contains("パス")) {
                JOptionPane.showMessageDialog(this,
                        gameUpdate.message,
                        "パス通知",
                        JOptionPane.INFORMATION_MESSAGE);
            }

            if (gameUpdate.message.contains("あなたの番です。") ||
                    gameUpdate.message.contains("相手の番です。")) {
                showGameScreen(gameUpdate.message);
            }

            if ((gameUpdate.message.contains("先手") || gameUpdate.message.contains("後手")) &&
                    !turnMessageShown && !gameUpdate.gameEnd) {
                JOptionPane.showMessageDialog(this,
                        gameUpdate.message,
                        "あなたの手番情報",
                        JOptionPane.INFORMATION_MESSAGE);
                turnMessageShown = true;
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
        JLabel result = new JLabel(resultText.split("\n")[0]); // ex : "黒の勝ち！ (34 対 30)"
        result.setFont(new Font("Dialog", Font.BOLD, 16));
        result.setHorizontalAlignment(SwingConstants.CENTER);
        add(result, gbc);

        // 個人結果ラベル
        gbc.gridy = 4;
        JLabel personalResultLabel = new JLabel(resultText.split("\n")[1]); // ex : "あなたの勝ちです"
        personalResultLabel.setFont(new Font("Dialog", Font.BOLD, 24));
        personalResultLabel.setForeground(new Color(0, 102, 204));
        personalResultLabel.setHorizontalAlignment(SwingConstants.CENTER);
        add(personalResultLabel, gbc);

        // ボタンパネル
        gbc.gridy = 5;
        JPanel buttonPanel = new JPanel(new FlowLayout());
        buttonPanel.setOpaque(false);
        JButton retryButton = new JButton("再試合");
        retryButton.addActionListener(_ -> {
            sendText("EXIT");
            try {
                if (socket != null)
                    socket.close();
                if (objIn != null)
                    objIn.close();
                if (objOut != null)
                    objOut.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            nicknameDialogShown = false;
            isWaitingForHandicap = false;
            board = new int[BOARD_SIZE][BOARD_SIZE];
            validPoints.clear();
            turnMessageShown = false;
            showIpInputDialog((JPanel) getContentPane());
        });

        JButton exitButton = new JButton("終了");
        exitButton.addActionListener(_ -> {
            sendText("EXIT");
            try {
                if (socket != null)
                    socket.close();
                if (objIn != null)
                    objIn.close();
                if (objOut != null)
                    objOut.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            System.exit(0);
        });
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
