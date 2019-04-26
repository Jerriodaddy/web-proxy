import java.net.*;
import java.util.*;
import java.io.*;

public class Main {
    
    static int PORT = 6666; //Local Port
    static int HTTP_PORT_REMOTE = 80; //Remote Port
    static int MAX_MESSAGE_LEN = 1024; //8196
    static String HTTP_RESP_OK = "200 OK";
    static String HTTP_RESP_NOT_FOUND = "404 Not Found";

    // http://assets.climatecentral.org/images/uploads/news/Earth.jpg

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

    public static String[] ParseResponseHeader(String resHeader){
        //Can edit respond content in this function.
        String[] resHeader_split = resHeader.split("\r\n");
        String code = "";
        String respCode = "";
        String conType = "";
        String length = "";
        for (String s:resHeader_split) {
            String[] s_split = s.split(" ");
            if(s_split[0].startsWith(("HTTP/"))){
                respCode = s;
                if(s_split[1].equals("200"))
                    code = HTTP_RESP_OK;
                if(s_split[1].equals("400"))
                    code = HTTP_RESP_NOT_FOUND;
            }
            if(s_split[0].startsWith(("Content-Type:"))){
                conType = s_split[1];
                respCode += "\r\n" + s;
            }
            if(s_split[0].startsWith(("Content-Length:"))){
                length = s_split[1];
                // respCode += "\r\n" + s;
            }
            if(s_split[0].startsWith(("Content-Encoding"))) {//maybe not need
                respCode += "\r\n" + s;
            }
        }
        return new String[]{code, respCode, length, conType};
    }

    public static String composeRequestMessageHeader(String host, String url, String header){
        String[] header_split = header.split("\r\n");
        String reqMsg = "";
        int count = 0;
        for (String x : header_split) {
            String[] x_split = x.split(" ");
            if(count == 0){
                count+=1;
                reqMsg += x_split[0]+" /"+url+" "+x_split[2]+"\r\n";
            }else{
                if(x_split[0].equals("Host:")){
                    reqMsg += x_split[0] + " " + host + "\r\n";
                }else{
                    if (x_split[0].equals("Connection:")){
                        x_split = x.split(" ");
                        reqMsg += x_split[0] + " "+"close" + "\r\n";
                    }else{
                        reqMsg += x + "\r\n";
                    }
                }
            }
        }
        return reqMsg;
    }

    public static String[] ParseRequest(String reqHeader){
        String[] reqHeader_split = reqHeader.split("\r\n");
        String[] s = reqHeader_split[0].split(" "); //only parse the first line.
        // String method = s[0];
        // String destAddress = s[1];
        // String httpVersion = s[2];
        System.out.println("\n[PARSE MESSAGE HEADER]:\n METHOD = "+s[0]+", DESTADDRESS = "+s[1]+", HTTPVersion = "+s[2]);
        return s;
    }

    //Sends request to original server and returns response as {header, body}
    public static byte[] SendRequest(String method, String destAddress, String data){
        // Socket serverSocket = null;
        // PrintWriter out;
        // BufferedReader in;
        String[] s = parseRemoteDestAddress(destAddress);//{hostname, url, filename}
        InetAddress remoteAdd = null;
        try{
            remoteAdd = InetAddress.getByName(s[0]);
        }catch(UnknownHostException e){
            e.printStackTrace();
            return null;
            // return ("HTTP/1.1 400 Bad Request", "From Web Proxy:400 Bad Request");
        }
        // String remoteIP = remoteAdd.getHostAddress();
        // System.out.println(remoteIP);
        try{
            Socket serverSocket = new Socket(remoteAdd, HTTP_PORT_REMOTE);//Connect to remote Server
            PrintWriter out = new PrintWriter(serverSocket.getOutputStream(), true); // Bad code here, I should use 
                                                                                     // OutputStream instead of PrintWriter.

            // in = new BufferedReader(new InputStreamReader(serverSocket.getInputStream()));
            InputStream in = serverSocket.getInputStream(); //Must be bytes.
            String[] data_split = data.split("\r\n\r\n");
            String httpMsgHeader=composeRequestMessageHeader(s[0], s[1], data_split[0]);
            String reqMsg = httpMsgHeader;
            if (data_split.length >1) {
                System.out.println("data_split="+data_split);
                reqMsg= httpMsgHeader + data_split[1];
            }
            System.out.println("\nREQUEST MESSAGE SENT TO ORIGINAL SERVER:\n"
                                +reqMsg
                                +"\nEND OF MESSAGE SENT TO ORIGINAL SERVER\n");
            //Send and resive Msg
            out.println(reqMsg);
            out.flush();

            byte[] resMsgBytes = {};
            String resHeader = "";
            String resContent = "";
            /*** Method 1 ***///Can't work
            // char[] buf = new char[MAX_MESSAGE_LEN];
            byte[] buf = new byte[MAX_MESSAGE_LEN];
            int len; //May over flow?
            while((len = in.read(buf)) != -1){
                //Handle buffer overflow
                int resMsgBytes_len = resMsgBytes.length;
                resMsgBytes = Arrays.copyOf(resMsgBytes, resMsgBytes.length+len); //extend length of resMsgBytes
                System.arraycopy(buf, 0, resMsgBytes, resMsgBytes_len, len); //copy buf to resMsg
                // resMsg += new String(buf, 0, len); //have not split

                /* 
                We can not add the following code since len < MAX_MESSAGE_LEN only means 
                we received one packet, but there may be multiple packet need to transmit. 
                */
                // System.out.println("*****The len ="+len);
                // if (len<MAX_MESSAGE_LEN) {
                //     break;
                // }
            }

            FileOutputStream fos = new FileOutputStream("record");
            fos.write(resMsgBytes, 0, resMsgBytes.length);

            String resMsg = new String(resMsgBytes, 0, resMsgBytes.length);
            // System.out.println(resMsg);
            // for (byte b : resMsg ) {
            //     System.out.print((char)b);
            // }
            System.out.println("&&&&&The len ="+len);
            String[] resMsg_split = resMsg.toString().split("\r\n\r\n");
            resHeader = resMsg_split[0];
            for(int i=1; i<resMsg_split.length; i++){
                resContent += resMsg_split[i];
            }
            
            /*** Method 2 ***/
            // String line;
            // if((line = in.readLine()) != null){
            //     //Handle buffer overflow
            //     System.out.println("---"+line);
            //     resMsg += line;
            // }
            // String[] resMsg_split = resMsg.split("\r\n\r\n");
            // resHeader = resMsg_split[0];



            System.out.println("\nRESPONSE HEADER FROM ORIGINAL SERVER:\n"
                            + resHeader
                            + "\nEND OF HEADER\n");
            

            String[] parsedHeader = ParseResponseHeader(resHeader); //return {code, respCode, length, conType}
            String code = parsedHeader[0]; 
            if(code.equals(HTTP_RESP_NOT_FOUND)){
                //return
            }else if (code.equals(HTTP_RESP_OK)) {
                // OpenCacheFile(s[2]); //Write into cache. s[2] is filename.
                // return
            }else{//POST
                //return
            }
            // return new String[]{parsedHeader[1], resContent};
            // return new String[]{parsedHeader[1], resMsg};
            return resMsgBytes;
        }
        catch(IOException e){
            System.out.println("Error: Remote Connecting Error.");
            e.printStackTrace();
            return null;
        }

    }

    public static void ProcessRequest(Socket clientSocket, OutputStream out, BufferedReader in, String request) throws Exception{
        //Create a new socket for connecting to destination server
        String header = request.split("\r\n\r\n")[0];
        // System.out.println("HHHH:\n"+header);
        String[] headerList= ParseRequest(header); //return {method,destAddress,httpVersion}
        String[] result = null;
        String response = "";
        byte[] responseBytes=null;
        // Socket socket2 = new Socket("localhost", 80);
        // OutputStream outgoingOS = socket2.getOutputStream();
        // outgoingOS.write(data, 0, len);
        if (headerList[0].equals("GET")) {
            //Create a new socket for connecting to destination server
            //is in cache?
            // if(isInChash(destAddress)){

            // }else{}
                System.out.println("\n[LOOK UP IN THE CACHE]: NOT FOUND, BUILD REQUEST TO SEND TO ORIGINAL SERVER");
                // result = SendRequest(headerList[0], headerList[1], request); //{method,destAddress,data} return {respCode, content}
                responseBytes = SendRequest(headerList[0], headerList[1], request);
                // response = result[0]+"\r\n\r\n"+result[1];
        }else{
            System.out.println("*** Not GET ***");
            return;
            //TODO
            // SendRequest(headerList[0], headerList[1], request);
        }
        // System.out.println("\nRESPONSE HEADER FROM PROXY TO CLIENT:\n"
        //                     +result[0]
        //                     +"\nEND OF HEADER\n");

        // System.out.println("\n%%%%%%%%%%%%%%%%%%%%%%%%%%\n");
        // for (byte b : response.getBytes() ) {
        //     System.out.print((char)b);
        // }
        // System.out.println("\n%%%%%%%%%%%%%%%%%%%%%%%%%%\n");
        // out.println(response);
    

        // char[] responseChars = new char[responseBytes.length];
        // int count = 0;
        // for (byte b:responseBytes ) {
        //     System.out.print((char)b);
        //     count++;
        // }
        out.write(responseBytes);
        // System.out.println("responseBytes.length="+responseBytes.length+" count="+count);
        // out.println(new String(responseBytes, 0 ,responseBytes.length)); // Can not sent byte[].
        // out.flush();

        try{
            clientSocket.close();
        }catch(IOException e){
            e.printStackTrace();
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
            // PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
            OutputStream out = clientSocket.getOutputStream();
            BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            // InputStream in = clientSocket.getInputStream();
            System.out.println("==========================================");
            System.out.println("WEB PROXY SERVER CONNECTED WITH " + clientSocket.getInetAddress() + ":" + clientSocket.getPort());

            // byte[] buf = new byte[MAX_MESSAGE_LEN];
            char[] buf = new char[MAX_MESSAGE_LEN]; //8196 is the default max size for GET requests in Apache
            int len; //May over flow? //The actural length readed & Write data into buf
            if((len = in.read(buf)) != -1){
                //Handle buffer overflow
                System.out.println("*****The len ="+len);
            }
            if(len!=-1){
                String request = new String(buf, 0, len);
                System.out.println("\nMESSAGE RECEIVED FROM CLIENT:\n"+request+"END OF MESSAGE RECEIVED FROM CLIENT");
                // System.out.println(buf.length);
                // ProcessRequest(buf,len);
                // for (byte b:buf) {
                //     if ((char)b=='\r')
                //         System.out.print("GET");
                // }
                ProcessRequest(clientSocket, out, in, request);
            }
        }
        // serverSocket.close();        
    }
    public static void main(String[] args) throws Exception {
        listening();
    }
}