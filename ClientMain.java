import java.net.*;
import java.util.*;
import java.io.*;

public class ClientMain{

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