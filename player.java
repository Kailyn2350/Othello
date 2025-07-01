public class player {
    private String playerName;
    private int playerColor; // 1 が黒（先手）, 2 が白（後手）
    private boolean checkturn; //自分の番かどうか

    // コンストラクタ
    public player(String playerName, int playerColor) {
        this.playerName = playerName;
        this.playerColor = playerColor;
        this.checkturn = false; // 初期状態では自分の番ではない
    }
    // メソッド
    //プレイヤ名を受け付けるメソッド
    public void setName(String playerName) {
        this.playerName = playerName;
    }

    // プレイヤー名を取得するメソッド
    public String getPlayerName() {
        return playerName;
    }

    //先手後手情報を受け付けるメソッド
    public void setColor(int playerColor) {
        this.playerColor = playerColor;
    }

    // 先手後手情報を取得するメソッド
    public int getColor() {
        return playerColor;
    }

    // 自分の番かどうかを受け付けるメソッド
    public void setCheckturn(boolean checkturn) {
        this.checkturn = checkturn;
    }

    // 自分の番かどうかを取得するメソッド
    public boolean getCheckturn() {
        return checkturn;
    }

}
