import java.net.*;
import java.util.*;
import java.io.*;

public class Client{
	
	private Socket clientSocket;
    private PrintWriter out;
    private BufferedReader in;
 
    public void startConnection(String ip, int port) throws Exception{
        clientSocket = new Socket(ip, port);
        out = new PrintWriter(clientSocket.getOutputStream(), true);
        in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
    }
 
    public String sendMessage(String msg) throws Exception{
        out.println(msg);
        System.out.println("sending msg");
        String resp = in.readLine();
        return resp;
    }

    public void stopConnection() throws Exception {
        in.close();
        out.close();
        clientSocket.close();
    }

    public static void main(String[] args) {
        try{
            Client client = new Client();
            client.startConnection("127.0.0.1", 6666);
            String resp = client.sendMessage("Hello");
            System.out.println(resp);
            client.stopConnection();
        }catch(Exception e){
            e.printStackTrace();
        }
        
    }
}

