
public class PlayerSample1 {

	private String myName = ""; //プレイヤ名
	private String myColor = ""; //先手後手情報(白黒)

	// メソッド
	public void setName(String name){ // プレイヤ名を受付
		myName = name;
	}
	public String getName(){	// プレイヤ名を取得
		return myName;
	}
	public void setColor(String c){ // 先手後手情報の受付
		myColor = c;
	}
	public String getColor(){ // 先手後手情報の取得
		return myColor;
	}
}