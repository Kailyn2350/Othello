import java.net.*;
import java.io.*;

public class EchoServer {
    static int port = 10000;
    
    public static void main(String[] args) {
	try {
	    ServerSocket server = new ServerSocket(port);
	    Socket sock = null;
	    System.out.println("サーバが起動しました");
	    while(true) {
		try {
		    sock = server.accept(); // クライアントからの接続を待つ

		    System.out.println("クライアントと接続しました");
		    BufferedReader in = new BufferedReader(
                        new InputStreamReader(sock.getInputStream()));
		    PrintWriter out = new PrintWriter(sock.getOutputStream());
		    String s;
		    while((s = in.readLine()) != null) { // 一行受信
			out.print(s + "\r\n"); // 一行送信
			out.flush();
			System.out.println(s);
		    }
		    sock.close(); // クライアントからの接続を切断
		    System.out.println("切断しました");
		} catch (IOException e) {
		    System.err.println(e);
		}
	    }
	} catch (IOException e) {
	    System.err.println(e);
	}
    }
}