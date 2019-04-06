import java.net.*;
import java.util.*;
import java.io.*;

public class Main {
    
    static int PORT = 6666; //local
    static int HTTP_PORT_REMOTE = 80; //remote

    public static String[] parseRemoteDestAddress(String destAddress){
        String[] destAddress_split = destAddress.split("//|/");
        String hostname = destAddress_split[1];
        String url = "";
        String filename = destAddress_split[destAddress_split.length-1];
        for(int i=2; i<destAddress_split.length-1;i++){
            url = url+destAddress_split[i]+"/";
        }
        url = url+filename;
        System.out.println("[PARSE REQUEST HEADER] HOSTNAME IS "+hostname);
        System.out.println("[PARSE REQUEST HEADER] URL IS "+url);
        System.out.println("[PARSE REQUEST HEADER] FILENAME IS "+filename);
        return new String[]{hostname, url, filename};
    }

    public static String composeRequestMessageHeader(String host, String url, String header){
        String s = "";
        return s;
    }

    public static String[] ParseRequest(String reqHeader){
        String[] s = reqHeader.split(" ");
        // String method = s[0];
        // String destAddress = s[1];
        // String httpVersion = s[2];
        System.out.println("\n[PARSE MESSAGE HEADER]:\n METHOD = "+s[0]+", DESTADDRESS = "+s[1]+", HTTPVersion = "+s[2]);
        return s;
    }

    public static void SendRequest(String method, String destAddress, String data){
        Socket serverSocket = null;
        String[] s = parseRemoteDestAddress(destAddress);//{hostname, url, filename}
        InetAddress remoteAdd = null;
        try{
            remoteAdd = InetAddress.getByName(s[0]);
        }catch(UnknownHostException e){
            e.printStackTrace();
            // return ("HTTP/1.1 400 Bad Request", "From Web Proxy:400 Bad Request");
        }
        // String remoteIP = remoteAdd.getHostAddress();
        // System.out.println(remoteIP);
        try{
            serverSocket = new Socket(remoteAdd, HTTP_PORT_REMOTE);//Connect to remote Server
        }
        catch(IOException e){
            System.out.println("Remote Connecting Error");
            System.exit(-1);
        }

        String[] data_split = data.split("\r\n\r\n");
        String httpMsgHeader=composeRequestMessageHeader(s[0], s[1], data_split[0]);
        String reqMsg = httpMsgHeader + data_split[1];
        System.out.println("\nREQUEST MESSAGE SENT TO ORIGINAL SERVER:"
                            +reqMsg
                            +"END OF MESSAGE SENT TO ORIGINAL SERVER");

    }

    public static void ProcessRequest(String request){
        //Create a new socket for connecting to destination server
        String header = request.split("\n")[0];
        String[] headerList= ParseRequest(header); //{method,destAddress,httpVersion}

        // Socket socket2 = new Socket("localhost", 80);
        // OutputStream outgoingOS = socket2.getOutputStream();
        // outgoingOS.write(data, 0, len);
        if (headerList[0].equals("GET")) {
            //Create a new socket for connecting to destination server
            //is in cache?
            System.out.println("\n[LOOK UP IN THE CACHE]: NOT FOUND, BUILD REQUEST TO SEND TO ORIGINAL SERVER");
            SendRequest(headerList[0], headerList[1], request); //{method,destAddress,data}
        }else{
            System.out.println("Not GET");
        }

    }

    public static void listening() throws Exception{
        ServerSocket serverSocket = null;
        try{
            serverSocket = new ServerSocket(PORT);
        }catch(IOException e){
            System.out.println("Port Error");
            System.exit(-1);
        }
        System.out.println("WEB PROXY SERVER IS LISTENING");
        while(true){
            Socket clientSocket = serverSocket.accept();
            PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
            // BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            InputStream in = clientSocket.getInputStream();

            System.out.println("WEB PROXY SERVER CONNECTED WITH " + clientSocket.getInetAddress() + ":" + clientSocket.getPort());

            byte[] buf = new byte[8196];
            // char[] buf = new char[8196]; //8196 is the default max size for GET requests in Apache
            int len = in.read(buf); //The actural length readed & Write data into buf
            // System.out.print((char)buf[0]);
            if(len!=-1){
                String request = new String(buf, 0, len);
                System.out.println("\nMESSAGE RECEIVED FROM CLIENT:\n"+request+"END OF MESSAGE RECEIVED FROM CLIENT");
                // System.out.println(buf.length);
                // ProcessRequest(buf,len);
                ProcessRequest(request);
            }
        }
        // serverSocket.close();        
    }
    public static void main(String[] args) throws Exception {
        listening();
    }
}