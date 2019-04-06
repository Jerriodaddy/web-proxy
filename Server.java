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
 

    public ArrayList readMessage() throws Exception {
        ArrayList<String> msg_array = new ArrayList<String>();
        String msg;
        while ((msg = (String)in.readLine()) != null) {
            System.out.println(msg);
            if(msg == "\n")
                break;
            msg_array.add(msg);
            //if timer = 1 second stop connection 

        }
        return msg_array;
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

    public static boolean parseMessage(ArrayList msg) throws IOException{   
        System.out.println("\n * Response");
          
        for(int i = 0; i <= 10; i++)
        {
            if(msg.contains("GET"))
                return true;
        }
       
      /*while ((msg = (String)in.readLine()) != null) {
            
            String[] msgArr = msg.split(" ", 5); 
              
            for (String a : msgArr) 
                System.out.println(a);    
        }
        //if(msg.contains("GET"))
            
     //   else
      //    return false;*/
        return false;
    }
  /*  public String composeMessage(String[] string){

    }

    public void print_parsedMsg(String parsed_msg){

    }

    public boolean isThisInCashe(Object obj){

    }

    public void writeCache(Cache){
        
    }

    public String readCache(){

    }

    public void printFormate(){

    }

    public void printHeader(){

    }*/
    

    public static void listening() throws Exception{
        Server server=new Server();
        server.start(6666);
        while(true){
            System.out.println("WEB PROXY SERVER IS LISTENING’");

            ArrayList msg = server.readMessage();
            if (msg!=null) {
                for (Object s : msg) {
                    System.out.println(s);
                }
            }
            // String msg[] = server.readMessage();
            // boolean parsed_msg = parseMessage(msg);
            // if (parsed_msg){
            //     System.out.println("Has GET");
            //     if isThisInCashe() is false
            //     System.out.println(“Object not found in the cache”);
            //     Server server2 = new Server(); //Create a (New) serverSocket to send request to the original server
            //     composed_msg = composeMessage()
            //     sendMessage(composed_msg)
            //     printFormat()
            //     read message
            //     parse message to the New server
            //     if response is 200 OK
            //         writeCache()
            //     Close (New) serverSocket
            //     (Old) sendMessage
            //     print OK?
            // else
            //     Print “Object found in the cache” message
            //     composed_msg = composeMessage()
            //     sendMessage(composed_msg)
            //     Print the response header from the prox to the client
                
            // }else{
            //     Server server2 = new Server(); //Create a (New) serverSocket to send request to the original server
            //     composed_msg = composeMessage()
            //     sendMessage(composed_msg)
            //     printFormate()
            //     read message
            //     parse message to the New server
            //     Print the response header from the (new) server
            //     composed_msg = composeMessage()
            //     sendMessage(composed_msg) to client
            //     Print the response header from the proxy to the client
            //     Close (New) serverSocket
            // }
                


        // ArrayList msg = server.readMessage();
        // if (msg!=null) {
        //     for (Object s : msg) {
        //         System.out.println(s);
        //     }
        // }
        // server.sendMessage("Hello");

        }
        // server.stop();
    }
    public static void main(String[] args) throws Exception {
        listening();
    }
}