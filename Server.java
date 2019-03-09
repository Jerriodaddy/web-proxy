import java.net.*;
import java.util.*;
import java.io.*;

public class Server {
    private ServerSocket serverSocket;
    private Socket clientSocket;
    private PrintWriter out;
    private static BufferedReader in;
 
    public void start(int port) throws Exception {
        serverSocket = new ServerSocket(port);
        clientSocket = serverSocket.accept();
        out = new PrintWriter(clientSocket.getOutputStream(), true);
        in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
    }
 
    public String readMessage() throws Exception {
        String msg = in.readLine();
        // while (in.hasNextLine){
        //  print
        // }
        return msg;
    }

    public void sendMessage(String msg) throws Exception{
        out.println(msg);
        System.out.println("sending msg");
    }

    public void stop() throws Exception {
        in.close();
        out.close();
        clientSocket.close();
        serverSocket.close();
    }

    public static String parseMessage(String msg) throws IOException{
        
        System.out.println("\n * Response");
        while ((msg = in.readLine()) != null) {
            System.out.println(msg);
        }
        return msg;
    }
  /*  public String composeMessage(String[] string){

    }

    public void print_parsedMsg(String parsed_msg){

    }

    public boolean isThisInCashe(Object obj){

    }

    public void writeCache(){

    }

    public String readCache(){

    }

    public void printFormate(){

    }

    public void printHeader(){

    }*/
    

    public static void main(String[] args) throws Exception{
        Server server=new Server();
        server.start(80);
        boolean run = true;
        while(run){
            server.readMessage();
            System.out.println("Connected"); //print connecgtion msg
            String msg = server.readMessage();
            String parsed_msg = parseMessage(msg);
            System.out.println(parsed_msg);
            if (parsed_msg.contains("GET")){
                
                //if isThisInCashe() is false
                //System.out.println(“Object not found in the cache”);
                //Server server2 = new Server(); Create a (New) serverSocket to send request to the original server
                //composed_msg = composeMessage()
                //sendMessage(composed_msg)
                //printFormat()
                //read message
                //parse message to the New server
                //if response is 200 OK
                    //writeCache()
                //Close (New) serverSocket
                //(Old) sendMessage
                //print OK?
            //else
                //Print “Object found in the cache” message
                //composed_msg = composeMessage()
                //sendMessage(composed_msg)
                //Print the response header from the prox to the client
                
            }
                
            //else
                //Create a (New) serverSocket to send request to the original server
                //composed_msg = composeMessage()
                //sendMessage(composed_msg)
                //printFormate()
                //read message
                //parse message to the New server
                //Print the response header from the (new) server
                //composed_msg = composeMessage()
                //sendMessage(composed_msg) to client
                //Print the response header from the proxy to the client
                //Close (New) serverSocket
            
        }
        server.stop();
    }
    
}