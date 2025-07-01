import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.IOException;

public class server {
    private int port; // サーバの待ち受けポート
    private String PlayerAName;
    private String PlayerBName;
    private Map<String, Boolean> SenteGoteInfo; // 例: <PlayerAName, true(先手)>
    private int handicap;
    /*
     * ０ : ハンディなし、
     * １：引き分け勝ち：石の数が同数の場合に黒の勝ちとする
     * ２：1子局：左上隅に黒石を置いて対局を開始する
     * ３：2子局：左上隅と右下隅に黒石を置いて対局を開始する
     * ４：3子局：左上隅、右下隅、右上隅に黒石を置いて対局を開始する
     * ５：4子局：４か所全ての隅に黒石を置いて対局を開始する
     */
    private int HandicapChoices[] = new int[6]; // ハンディキャップの選択肢
    private int[][] boardState = new int[8][8]; // ボードの初期状態を0で初期化 (0：空き、１：黒、２：白)

    private Socket socketA;
    private Socket socketB;

    private boolean isConnectedA = false; // Aプレイヤーの接続状態
    private boolean isConnectedB = false; // Bプレイヤーの接続状態

    private ObjectOutputStream objOutA;
    private ObjectOutputStream objOutB;

    private ObjectInputStream objInA;
    private ObjectInputStream objInB;

    private int currentTurn = 1; // １＝黒（先手）、 ２＝白（後手）

    volatile boolean nicknameThreadFailed = false;

    private Thread nickAThread;
    private Thread nickBThread;

    private boolean exitRequestedA = false;
    private boolean exitRequestedB = false;

    // コンストラクタ
    public server(int port) { // 待ち受けポート番号を引数に受け取る
        this.port = port;
        this.SenteGoteInfo = new HashMap<>();
    }

    // メソッド

    // クライアントの接続・ニックネーム登録
    public void acceptClient() {
        try (ServerSocket ss = new ServerSocket(port)) {
            System.out.println("サーバが起動しました。");

            // 1. クライアントの接続を待機
            while (!isConnectedA || !isConnectedB) {
                Socket socket = ss.accept();
                System.out.println("新しいクライアントが接続しました。");
                ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
                out.flush(); // フラッシュしてバッファをクリア
                ObjectInputStream in = new ObjectInputStream(socket.getInputStream());

                synchronized (this) {
                    if (!isConnectedA) {
                        socketA = socket;
                        objOutA = out;
                        objInA = in;
                        isConnectedA = true;
                        System.out.println("クライアントAが接続されました。");
                    } else if (!isConnectedB) {
                        socketB = socket;
                        objOutB = out;
                        objInB = in;
                        isConnectedB = true;
                        System.out.println("クライアントBが接続されました。");
                    }
                }
            }

            // 2. クライアントにニックネーム登録のメッセージを送信
            objOutA.writeObject("READY_FOR_NICKNAME");
            objOutA.flush();
            objOutB.writeObject("READY_FOR_NICKNAME");
            objOutB.flush();

            // 3. クライアントからニックネームを受信
            nickAThread = new Thread(() -> handleNicknameInput(true));
            nickBThread = new Thread(() -> handleNicknameInput(false));
            nickAThread.start();
            nickBThread.start();
            nickAThread.join();
            nickBThread.join();
            if (nicknameThreadFailed)
                return;

            objOutB.writeObject("WAIT_FOR_HANDICAP");
            objOutB.flush();

            // 4. ハンディキャップの選択肢を初期化
            List<int[]> emptyList = new ArrayList<>();
            GameUpdate updateA = new GameUpdate(
                    true,
                    boardState,
                    handicap,
                    "あなたは先手です",
                    PlayerAName,
                    PlayerBName,
                    emptyList,
                    false,
                    1);
            GameUpdate updateB = new GameUpdate(
                    false,
                    boardState,
                    handicap,
                    "あなたは後手です",
                    PlayerBName,
                    PlayerAName,
                    emptyList,
                    false,
                    2);
            objOutA.writeObject(updateA);
            objOutA.flush();
            objOutB.writeObject(updateB);
            objOutB.flush();

            System.out.println("GameUpdate をクライアントに送信しました。");

        } catch (Exception e) {
            System.err.println("サーバ接続処理中にエラー: " + e.getMessage());
        }
    }

    private void handleNicknameInput(boolean isPlayerA) {
        try {
            ObjectOutputStream out = isPlayerA ? objOutA : objOutB;
            out.flush(); // フラッシュしてバッファをクリア
            ObjectInputStream in = isPlayerA ? objInA : objInB;

            while (!Thread.currentThread().isInterrupted()) {
                String nickname = (String) in.readObject();
                synchronized (this) {
                    // ニックネームの重複チェック
                    if (nickname.equals(PlayerAName) || nickname.equals(PlayerBName)) {
                        out.writeObject("DUPLICATE");
                        out.flush();
                    } else {
                        // ニックネームを登録
                        if (isPlayerA) {
                            PlayerAName = nickname;
                            SenteGoteInfo.put(nickname, true);
                        } else {
                            PlayerBName = nickname;
                            SenteGoteInfo.put(nickname, false);
                        }

                        // 相手のニックネームを待機
                        if ((isPlayerA && PlayerBName == null) || (!isPlayerA && PlayerAName == null)) {
                            out.writeObject("WAITING_FOR_OTHER");
                            out.flush();

                            // 相手のニックネームが登録されるまで待機
                            while ((PlayerAName == null) || (PlayerBName == null)) {
                                wait();
                            }
                        } else {
                            // 相手のニックネームが登録された場合、通知
                            notifyAll(); // 他のスレッドに通知
                        }

                        // ニックネーム登録完了のメッセージを送信
                        if (isPlayerA) {
                            out.writeObject("WAITING_FOR_OTHER");
                        } else {
                            if (handicap == 0) {
                                // ハンディキャップなしならメッセージ送信しない
                            } else {
                                out.writeObject("WAIT_FOR_HANDICAP");
                            }
                        }
                        out.flush();

                        break;
                    }
                }
            }
        } catch (Exception e) {
            nicknameThreadFailed = true;
            synchronized (this) {
                notifyAll();
            }
            System.err.println("ニックネーム入力中にエラー: " + e.getMessage());
            handleDisconnection();
            return;
        }
    }

    public void receiveCandidateAndApproval() {
        try {
            objOutA.writeObject("SELECT_HANDICAP");
            objOutA.flush();

            while (true) {
                sendPossibleHandicapOptions();

                // クライアントAからハンディキャップ候補を受信
                Integer candidate = (Integer) objInA.readObject();
                proposeHandicap(candidate);

                if (candidate == 0) {
                    handicap = 0;
                    System.out.println("ハンディキャップ : " + handicap + " が確定されました。");
                    break;
                }

                // クライアントBに提案メッセージ送信の前に UI トリガーを送る
                objOutB.writeObject("WAIT_FOR_HANDICAP");
                objOutB.flush();

                // クライアントBに提案メッセージ送信
                String proposalMessage = "対戦相手がハンディキャップ " + candidate + " を提案しました。承認しますか？";
                objOutB.writeObject(proposalMessage);
                objOutB.flush();

                // クライアントBからの応答を受信
                String response = (String) objInB.readObject();
                boolean approved = response.trim().equalsIgnoreCase("yes");

                handleHandicapApproval(approved);

                if (approved) {
                    System.out.println("ハンディキャップ : " + handicap + " が確定されました。");
                    break;

                } else if (allOnes(HandicapChoices)) {
                    handicap = 0;
                    System.out.println("全て拒否されたのでハンディ無しに決定します。");
                    break;

                } else {
                    boolean allRejected = true;
                    for (int i = 1; i <= 5; i++) {
                        if (HandicapChoices[i] == 0) {
                            allRejected = false;
                            break;
                        }
                    }

                    if (allRejected) {
                        handicap = 0;
                        System.out.println("全て拒否されたのでハンディ無しに決定します。");
                        break;
                    }
                    // 拒否されたので再選択をクライアントAに要求
                    objOutA.writeObject("RESELECT_HANDICAP");
                    objOutA.flush();

                    sendPossibleHandicapOptionsTo(objOutA);
                }
            }

        } catch (IOException | ClassNotFoundException | NumberFormatException e) {
            System.err.println("ハンディキャップ提案中にエラー: " + e.getMessage());
            handleDisconnection();
            return;
        }
    }

    private void sendPossibleHandicapOptionsTo(ObjectOutputStream out) {
        List<Integer> availableHandicapOptions = new ArrayList<>();
        for (int i = 0; i <= 5; i++) {
            if (HandicapChoices[i] == 0) {
                availableHandicapOptions.add(i);
            }
        }

        try {
            out.writeObject(availableHandicapOptions);
            out.flush();
        } catch (IOException e) {
            System.err.println("ハンディキャップ候補の送信エラー: " + e.getMessage());
        }
    }

    // クライアントに可能なハンディキャップ候補を送信するメソッド
    public void sendPossibleHandicapOptions() {
        List<Integer> availableHandicapOptions = new ArrayList<>();
        for (int i = 0; i <= 5; i++) {
            if (HandicapChoices[i] == 0) { // 選択肢が残っている場合
                availableHandicapOptions.add(i);
            }
        }

        try {
            // クライアントAとクライアントBにハンディキャップ候補を送信
            objOutA.writeObject(availableHandicapOptions);
            objOutA.flush();

            objOutB.writeObject(availableHandicapOptions);
            objOutB.flush();

        } catch (IOException e) {
            System.err.println("ハンディキャップ候補の送信中にエラーが発生しました: " + e.getMessage());
        }
    }

    public void proposeHandicap(int candidate) {
        if (candidate != 0) {
            HandicapChoices[candidate] = 1;
        }
        handicap = candidate;
    }

    public void handleHandicapApproval(boolean approval) { // プレイヤーからのハンディキャップ承認を処理するメソッド
        if (approval) { // 承認された場合
            System.out.println("ハンディキャップが承認されました。");
            java.util.Arrays.fill(HandicapChoices, 0); // ハンディキャップの選択肢をリセット
            // この部分でハンディキャップを適用する処理を行う
        } else { // 拒否された場合
            System.out.println("ハンディキャップが拒否されました。");
            if (allOnes(HandicapChoices)) { // すべての選択肢が拒否された場合
                System.out.println("全て拒否されたのでハンディ無しに決定します。");
                handicap = 0; // リセットしないで新しい選択肢を提供する
                java.util.Arrays.fill(HandicapChoices, 0); // ハンディキャップの選択肢をリセット
            }
        }
    }

    // 配列内の値がすべて1であるか確認するメソッド
    public static boolean allOnes(int[] array) {
        for (int num : array) {
            if (num != 1) {
                return false;
            }
        }
        return true;
    }

    public void startGame() {
        initializeBoard(); // ボードの初期化
        applyHandicap(); // ハンディキャップを適用するメソッド

        // ゲーム開始時にボードを送信
        GameUpdate updateA = new GameUpdate(
                true,
                deepCopyBoard(boardState),
                handicap,
                "あなたの番です。",
                PlayerAName,
                PlayerBName,
                new ArrayList<>(),
                false,
                1);

        GameUpdate updateB = new GameUpdate(
                false,
                deepCopyBoard(boardState),
                handicap,
                "相手の番です。",
                PlayerBName,
                PlayerAName,
                new ArrayList<>(),
                false,
                2);

        try {
            printBoardState("startGame - 初期状態送信直前");
            objOutA.writeObject(updateA); // クライアントAにゲームスタートメッセージ
            objOutA.flush();
            objOutB.writeObject(updateB); // クライアントBにゲームスタートメッセージ
            objOutB.flush();
            System.out.println("GameUpdate をクライアントに送信しました。");
        } catch (IOException e) {
            System.err.println("GameUpdate 送信を失敗しました: " + e.getMessage());
        }
    }

    // ハンディキャップを適用するメソッド
    public void applyHandicap() {
        System.out.println("ハンディキャップ " + handicap + " が適用されました。");
        sendHandicapNotice("ハンディキャップ " + handicap + " が適用されました。");

        // ハンディキャップに応じて初期配置を設定
        boardState[3][3] = 2;
        boardState[3][4] = 1;
        boardState[4][4] = 2;
        boardState[4][3] = 1;

        if (handicap >= 2)
            boardState[0][0] = 1; // 左上隅
        if (handicap >= 3)
            boardState[7][7] = 1; // 右下隅
        if (handicap >= 4)
            boardState[0][7] = 1; // 右上隅
        if (handicap == 5)
            boardState[7][0] = 1; // 左下隅
    }

    private void sendHandicapNotice(String message) {
        try {
            objOutA.writeObject(message);
            objOutA.flush();
            objOutB.writeObject(message);
            objOutB.flush();
        } catch (IOException e) {
            System.err.println("ハンディキャップ通知の送信エラー: " + e.getMessage());
        }
    }

    // ボードの初期化メソッド
    public void initializeBoard() {
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                boardState[i][j] = 0; // ボードを0で初期化
            }
        }
    }

    public void gameLoop() {
        while (true) {
            List<int[]> emptyList = new ArrayList<>();
            List<int[]> validPoint = calculationValidPoint();

            if (validPoint.isEmpty()) {
                System.out.println("プレイヤー " + currentTurn + " は置ける場所がないため、パスします。");

                // プレイヤーが置ける場所がない場合、次のプレイヤーにターンを移す
                int nextTurn = (currentTurn == 1) ? 2 : 1;
                currentTurn = nextTurn; // ターンを移す
                List<int[]> nextValid = calculationValidPoint();

                if (nextValid.isEmpty()) {
                    // 両プレイヤーが置ける場所がない場合
                    System.out.println("両プレイヤーが置ける場所がないため、ゲーム終了します。");
                    try {
                        GameUpdate updateA = new GameUpdate(
                                false, deepCopyBoard(boardState), handicap,
                                "ゲームが終了しました。",
                                PlayerAName, PlayerBName, emptyList, true, 1);
                        GameUpdate updateB = new GameUpdate(
                                false, deepCopyBoard(boardState), handicap,
                                "ゲームが終了しました。",
                                PlayerBName, PlayerAName, emptyList, true, 2);
                        printBoardState("両プレイヤーが置く場所がない時");
                        objOutA.writeObject(updateA);
                        objOutA.flush();
                        objOutB.writeObject(updateB);
                        objOutB.flush();
                    } catch (IOException e) {
                        System.err.println("ゲーム更新の送信中にエラー: " + e.getMessage());
                        handleDisconnection();
                        break;
                    }
                    waitForClientExit();
                    break;
                } else {
                    // 片方だけ置けない場合
                    try {
                        GameUpdate updateA, updateB;
                        if (nextTurn == 2) { // 次のターンがプレイヤーBの場合
                            updateA = new GameUpdate(false, deepCopyBoard(boardState), handicap,
                                    "置く場所がないためパスになりました。",
                                    PlayerAName, PlayerBName, emptyList, false, 1);
                            updateB = new GameUpdate(true, deepCopyBoard(boardState), handicap,
                                    "相手が置く場所がないためパスになりました。",
                                    PlayerBName, PlayerAName, emptyList, false, 2);
                        } else {
                            updateA = new GameUpdate(true, deepCopyBoard(boardState), handicap,
                                    "相手が置く場所がないためパスになりました。",
                                    PlayerAName, PlayerBName, emptyList, false, 1);
                            updateB = new GameUpdate(false, deepCopyBoard(boardState), handicap,
                                    "置く場所がないためパスになりました。",
                                    PlayerBName, PlayerAName, emptyList, false, 2);
                        }
                        printBoardState("片方だけ置けない時");
                        objOutA.writeObject(updateA);
                        objOutA.flush();
                        objOutB.writeObject(updateB);
                        objOutB.flush();
                    } catch (IOException e) {
                        System.err.println("ゲーム更新の送信中にエラー: " + e.getMessage());
                        handleDisconnection();
                        break;
                    }
                    // プレイヤーのターンを移す
                    continue;
                }
            }

            // プレイヤーの手番を更新
            try {
                GameUpdate updateA = new GameUpdate(
                        currentTurn == 1, deepCopyBoard(boardState), handicap,
                        (currentTurn == 1) ? "あなたの番です。" : "相手の番です。",
                        PlayerAName, PlayerBName,
                        (currentTurn == 1) ? validPoint : new ArrayList<>(), false, 1);
                GameUpdate updateB = new GameUpdate(
                        currentTurn == 2, deepCopyBoard(boardState), handicap,
                        (currentTurn == 2) ? "あなたの番です。" : "相手の番です。",
                        PlayerBName, PlayerAName,
                        (currentTurn == 2) ? validPoint : new ArrayList<>(), false, 2);
                printBoardState("通常手番処理");
                objOutA.writeObject(updateA);
                objOutA.flush();
                objOutB.writeObject(updateB);
                objOutB.flush();

                int[] point = waitForMove();
                if (point != null) {
                    applyBoard(point);
                    currentTurn = (currentTurn == 1) ? 2 : 1;
                }
            } catch (IOException e) {
                System.err.println("ゲーム更新の送信中にエラー: " + e.getMessage());
                handleDisconnection();
                break;
            }
        }
    }

    // クライアント切断時の処理
    private void handleDisconnection() {
        System.out.println("クライアントとの接続が切断されました。ゲームを強制終了します。");

        try {
            if (nickAThread != null)
                nickAThread.interrupt();
            if (nickBThread != null)
                nickBThread.interrupt();
        } finally {
            try {
                if (objOutA != null && !socketA.isClosed()) {
                    objOutA.writeObject("DISCONNECTED");
                    objOutA.flush();
                }
                if (objOutB != null && !socketB.isClosed()) {
                    objOutB.writeObject("DISCONNECTED");
                    objOutB.flush();
                }
            } catch (IOException e) {
                System.err.println("DISCONNECTED メッセージ送信失敗: " + e.getMessage());
            }
            endGame();
        }
    }

    public int[] waitForMove() {
        try {
            ObjectInputStream objIn = (currentTurn == 1) ? objInA : objInB;
            Object obj = objIn.readObject();
            if (obj instanceof int[]) {
                return (int[]) obj;
            } else {
                System.err.println("受信したデータが int[] ではありません。: " + obj.getClass().getName());
            }
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("着手の受信中にエラー: " + e.getMessage());
        }
        return null;
    }

    public void applyBoard(int[] point) {
        int row = point[0];
        int col = point[1];
        int playerColor = currentTurn;
        int opponentColor = (playerColor == 1) ? 2 : 1;

        boardState[row][col] = playerColor; // ボードの情報変更

        // 石をおいたときに覆される石を探すための方向ベクトル
        int[] dRow = { -1, -1, -1, 0, 1, 1, 1, 0 };
        int[] dCol = { -1, 0, 1, 1, 1, 0, -1, -1 };

        // 全方向探索
        for (int d = 0; d < 8; d++) {
            int nRow = row + dRow[d];
            int nCol = col + dCol[d];
            List<int[]> flipList = new ArrayList<>(); // 覆される可能性がある石の情報入れるリスト

            while (nRow >= 0 && nRow < 8 && nCol >= 0 && nCol < 8 && // ボードの外に出る条件
                    boardState[nRow][nCol] == opponentColor) { // 現在の方向に相手の石が置いてある条件
                flipList.add(new int[] { nRow, nCol }); // リストに追加
                // その方向にもっと進
                nRow += dRow[d];
                nCol += dCol[d];
            }

            if (nRow >= 0 && nRow < 8 && nCol >= 0 && nCol < 8 && // ボードの外に出る条件
                    boardState[nRow][nCol] == playerColor) { // 探索している所に自分の石が置いてある条件
                for (int[] flip : flipList) { // リストの全ての石を覆す。
                    boardState[flip[0]][flip[1]] = playerColor; // 相手の石を自分の石に変更。
                }
            }
        }
    }

    // 現在の手番のプレイヤーの色を使って、置ける場所を計算
    public List<int[]> calculationValidPoint() {
        List<int[]> validPoint = new ArrayList<>();

        int playerColor = currentTurn; // 現在の順番のカラーの確認１：黒、２：白
        int opponentColor = (playerColor == 1) ? 2 : 1; // 相手のカラーを確認

        int[] dRow = { -1, -1, -1, 0, 1, 1, 1, 0 }; // 探索のためのベクトルの列スカラー
        int[] dCol = { -1, 0, 1, 1, 1, 0, -1, -1 }; // 探索のためのベクトルの行スカラー

        for (int row = 0; row < 8; row++) { // ボードの全ての座標を確認。
            for (int col = 0; col < 8; col++) {
                if (boardState[row][col] != 0)
                    continue; // 既に石が置いている時次の座標に移動

                for (int d = 0; d < 8; d++) { // 石がない座標の全方向に石が存在するか確認。
                    int nRow = row + dRow[d];
                    int nCol = col + dCol[d];
                    boolean foundOpponent = false; // 相手の石が1つでも見つかったかを記録する変数

                    while (nRow >= 0 && nRow < 8 && nCol >= 0 && nCol < 8 &&
                            boardState[nRow][nCol] == opponentColor) { // 相手の石が存在するとき
                        // その方向にずっと進
                        nRow += dRow[d];
                        nCol += dCol[d];
                        foundOpponent = true;
                    }
                    // ボードの外に出たり、相手の石ではないときに。
                    if (foundOpponent && nRow >= 0 && nRow < 8 && nCol >= 0 && nCol < 8 &&
                            boardState[nRow][nCol] == playerColor) { // 相手の石が続いてその終わりに自分の石がある時
                        validPoint.add(new int[] { row, col }); // おける場所追加
                        break;
                    }
                }
            }
        }

        return validPoint;
    }

    // endGameメソッド
    private void endGame() {
        System.out.println("ゲームが強制終了されました。ソケットを閉じます。");
        try {
            if (objOutA != null)
                objOutA.writeObject("DISCONNECTED");
            if (objOutB != null)
                objOutB.writeObject("DISCONNECTED");
        } catch (IOException e) {
            System.err.println("DISCONNECTED 送信失敗: " + e.getMessage());
        }

        try {
            if (socketA != null)
                socketA.close();
            if (socketB != null)
                socketB.close();
        } catch (IOException e) {
            System.err.println("ソケットのクローズ中にエラー: " + e.getMessage());
        }
    }

    public void waitForClientExit() {
        Thread exitWaiterA = new Thread(() -> {
            try {
                while (true) {
                    Object obj = objInA.readObject();
                    if (obj instanceof String && obj.equals("EXIT")) {
                        handleExitMessage(true);
                        break;
                    }
                }
            } catch (IOException | ClassNotFoundException e) {
                System.err.println("クライアントAのEXIT待機中にエラー: " + e.getMessage());
            }
        });

        Thread exitWaiterB = new Thread(() -> {
            try {
                while (true) {
                    Object obj = objInB.readObject();
                    if (obj instanceof String && obj.equals("EXIT")) {
                        handleExitMessage(false);
                        break;
                    }
                }
            } catch (IOException | ClassNotFoundException e) {
                System.err.println("クライアントBのEXIT待機中にエラー: " + e.getMessage());
            }
        });

        exitWaiterA.start();
        exitWaiterB.start();
    }

    private void handleExitMessage(boolean fromPlayerA) {
        System.out.println((fromPlayerA ? "クライアントA" : "クライアントB") + " から EXIT を受信しました。");

        if (fromPlayerA) {
            exitRequestedA = true;
            closeSocketSafely(socketA);
        } else {
            exitRequestedB = true;
            closeSocketSafely(socketB);
        }

        // 両方のクライアントが終了した場合、正常終了処理を呼び出す
        if ((socketA == null || socketA.isClosed()) && (socketB == null || socketB.isClosed())) {
            gracefulShutdown();
        }
    }

    private void closeSocketSafely(Socket socket) {
        try {
            if (socket != null && !socket.isClosed())
                socket.close();
        } catch (IOException e) {
            System.err.println("ソケット終了時エラー: " + e.getMessage());
        }
    }

    public void resetServerState() {
        PlayerAName = null;
        PlayerBName = null;
        isConnectedA = false;
        isConnectedB = false;
        boardState = new int[8][8];
        currentTurn = 1;
        SenteGoteInfo.clear();
        handicap = 0; // ハンディキャップのリセット
        for (int i = 0; i < HandicapChoices.length; i++) {
            HandicapChoices[i] = 0; // ハンディキャップの選択肢をリセット
        }
        nicknameThreadFailed = false; // ニックネームスレッドの失敗フラグをリセット
    }

    // 正常終了メソッド
    public void gracefulShutdown() {
        System.out.println("ゲームが正常に終了しました。ソケットを閉じます。");

        try {
            if (socketA != null && !socketA.isClosed())
                socketA.close();
            if (socketB != null && !socketB.isClosed())
                socketB.close();
        } catch (IOException e) {
            System.err.println("gracefulShutdown中にエラー: " + e.getMessage());
        }
    }

    private void printBoardState(String context) {
        System.out.println("=== [" + context + "] 現在のボード状態 ===");
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                System.out.print(boardState[i][j] + " ");
            }
            System.out.println();
        }
        System.out.println("========================================");
    }

    private int[][] deepCopyBoard(int[][] original) {
        int[][] copy = new int[original.length][];
        for (int i = 0; i < original.length; i++) {
            copy[i] = original[i].clone();
        }
        return copy;
    }

    public static void main(String[] args) { // メイン関数
        server server = new server(10000); // ポート10000番でサーバオブジェクトを作成
        while (true) {
            server.acceptClient(); // クライアント接続＋ユーザー名登録
            if (server.nicknameThreadFailed) {
                server.resetServerState();
                continue;
            }
            server.receiveCandidateAndApproval();
            server.startGame(); // ハンディキャップ適用＋スタート
            server.gameLoop(); // ゲームの中継
            server.resetServerState(); // 状態初期化
            System.out.println("次のゲームの準備ができました。新しいプレイヤーの接続を待ちます。\n");
        }
    }

}
