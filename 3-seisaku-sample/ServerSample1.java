import java.net.ServerSocket;
import java.net.Socket;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.PrintWriter;
import java.io.IOException;

public class ServerSample1{
	private int port; // サーバの待ち受けポート
	private PrintWriter out; //データ送信用オブジェクト
	private Receiver receiver; //データ受信用オブジェクト

	//コンストラクタ
	public ServerSample1(int port) { //待ち受けポートを引数とする
		this.port = port; //待ち受けポートを渡す
	}

	// データ受信用スレッド(内部クラス)
	class Receiver extends Thread {
		private InputStreamReader sisr; //受信データ用文字ストリーム
		private BufferedReader br; //文字ストリーム用のバッファ

		// 内部クラスReceiverのコンストラクタ
		Receiver (Socket socket){
			try{
				sisr = new InputStreamReader(socket.getInputStream());
				br = new BufferedReader(sisr);
			} catch (IOException e) {
				System.err.println("データ受信時にエラーが発生しました: " + e);
			}
		}
		// 内部クラス Receiverのメソッド
		public void run(){
			try{
				while(true) {// データを受信し続ける
					String inputLine = br.readLine();//データを一行分読み込む
					if (inputLine != null){ //データを受信したら
						System.out.println("サーバからメッセージ " + inputLine + " が届きました．そのまま返信します．");
						out.println(inputLine);//受信データをバッファに書き出す
						out.flush();//受信データをそのまま返信する
					}
				}
			} catch (IOException e){ // 接続が切れたとき
				System.err.println("クライアントとの接続が切れました．");
			}
		}
	}

	// メソッド

	public void acceptClient(){ //クライアントの接続(サーバの起動)
		try {
			System.out.println("サーバが起動しました．");
			ServerSocket ss = new ServerSocket(port); //サーバソケットを用意
			while (true) {
				Socket socket = ss.accept(); //新規接続を受け付ける
				System.out.println("クライアントと接続しました．"); //テスト用出力
					out = new PrintWriter(socket.getOutputStream(), true);//データ送信オブジェクトを用意
					receiver = new Receiver(socket);//データ受信オブジェクト(スレッド)を用意
					receiver.start();//データ送信オブジェクト(スレッド)を起動
			}
		} catch (Exception e) {
			System.err.println("ソケット作成時にエラーが発生しました: " + e);
		}
	}

	public static void main(String[] args){ //main
		ServerSample1 server = new ServerSample1(10000); //待ち受けポート10000番でサーバオブジェクトを準備
		server.acceptClient(); //クライアント受け入れを開始
	}
}