/*
[Wait to Do List]
- Cache
- Receive msg from client should also using byte stream.
*/

// Example: http://assets.climatecentral.org/images/uploads/news/Earth.jpg
import java.net.*;
import java.util.*;
import java.io.*;

public class Main {
    
    static int PORT = 6666; //Local Port
    static int HTTP_PORT_REMOTE = 80; //Remote Port
    static int MAX_MESSAGE_LEN = 1024; //8196
    static String HTTP_RESP_OK = "200 OK";
    static String HTTP_RESP_NOT_FOUND = "404 Not Found";
    static String PATH = "mycache";
    static Map<String,Cache> cacheMap = new HashMap<String,Cache>();
    
    static private class Cache{
        // String address;
        String contentType;
        String path;
        String encoding;

        public Cache(String type, String path, String encoding){
            // this.address = add;
            this.contentType = type;
            this.path = path;
            this.encoding = encoding;
        }
    }

    private static byte[] byteMergerAll(byte[]... values) {
        int length_byte = 0;
        for (int i = 0; i < values.length; i++) {
            length_byte += values[i].length;
        }
        byte[] all_byte = new byte[length_byte];
        int countLength = 0;
        for (int i = 0; i < values.length; i++) {
            byte[] b = values[i];
            System.arraycopy(b, 0, all_byte, countLength, b.length);
            countLength += b.length;
        }
        return all_byte;
    }

    public static String[] parseRemoteDestAddress(String destAddress){
        String[] destAddress_split = destAddress.split("//|/");
        String hostname = "";
        String url = "";
        String filename = destAddress_split[destAddress_split.length-1];
        if (destAddress_split.length==1) {
            hostname = destAddress_split[0];
        }else{
            hostname = destAddress_split[1];

        }

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
        String length = "";
        String conType = "";
        String noCache = "false"; //may don't require cache. Add in this logic in the future.
        String encoding = "";
        for (String s:resHeader_split) {
            String[] s_split = s.split(" ");
            if(s_split[0].startsWith(("HTTP/"))){
                respCode = s;
                if(s_split[1].equals("200"))
                    code = HTTP_RESP_OK;
                if(s_split[1].equals("404"))
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
                encoding = s_split[1];
                respCode += "\r\n" + s;
            }
        }
        return new String[]{code, respCode, length, conType, encoding};
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

    public static File OpenCacheFile(String filename){ //Should consider the same name issue
        File dir = new File(PATH);
        if (!dir.exists()) {
            dir.mkdir();
        }
        String pathName = PATH+"/"+filename;
        File file = new File(pathName);
        if(file.isFile()){
            //already exist, delete it
            file.delete();
        }
        return file;
    }

    public static Cache FindInChash(String address){
        return cacheMap.get(address);
    }

    public static byte[] ComposeFromCache(Cache cache){
        System.out.println("\n[LOOK UP THE CACHE]: FOUND IN THE CACHE: FILE = "+cache.path);
        try{
            byte[] response = {};
            File file = new File(cache.path);
            if(!file.isFile())
                return null;
            else{
                FileInputStream fis = new FileInputStream(file);
                byte[] buf = new byte[MAX_MESSAGE_LEN];
                int len;
                while((len = fis.read(buf)) != -1){
                    response = byteMergerAll(response, buf);
                }
                //response is the content now.
                fis.close();
                String header = "HTTP/1.1 200 OK\r\n"
                            +"Content-Length: " + response.length + "\r\n"
                            +"Content-Type: " + cache.contentType + "\r\n"
                            +"Content-Encoding: " + cache.encoding +"\r\n\r\n";

                System.out.println("\nRESPONSE HEADER FROM PROXY TO CLIENT:\n"
                            + header
                            + "\nEND OF HEADER\n");

                response = byteMergerAll(header.getBytes(), response);
                return response;
            }

        }catch(Exception e){
            e.printStackTrace();
        }
        return null;
    }

    //Sends request to original server and returns response as {header, body}
    public static byte[] SendRequest(String method, String destAddress, String data){

        String[] s = parseRemoteDestAddress(destAddress);//{hostname, url, filename}
        String hostname = s[0];
        String url = s[1];
        String filename = s[2];
        InetAddress remoteAdd = null; 
        try{
            remoteAdd = InetAddress.getByName(hostname); //get IP address.
        }catch(UnknownHostException e){
            e.printStackTrace();
            String error = "HTTP/1.1 400 Bad Request From Web Proxy:400 Bad Request";
            return error.getBytes();
        }
        // String remoteIP = remoteAdd.getHostAddress();
        // System.out.println(remoteIP);
        try{
            Socket serverSocket = new Socket(remoteAdd, HTTP_PORT_REMOTE);//Connect to remote Server
            PrintWriter out = new PrintWriter(serverSocket.getOutputStream(), true); // Bad code here, I should use 
                                                                                     // OutputStream instead of PrintWriter.
            // in = new BufferedReader(new InputStreamReader(serverSocket.getInputStream()));
            InputStream in = serverSocket.getInputStream(); //Must be bytes.
            String[] data_split = data.split("\r\n\r\n"); //Header
            String httpMsgHeader=composeRequestMessageHeader(hostname, url, data_split[0]); //new Header
            String reqMsg = httpMsgHeader;
            if (data_split.length >1) { //sent content
                System.out.println("data_split="+data_split);
                reqMsg= httpMsgHeader + data_split[1];
            }
            System.out.println("\nREQUEST MESSAGE SENT TO ORIGINAL SERVER:\n"
                                +reqMsg
                                +"\nEND OF MESSAGE SENT TO ORIGINAL SERVER\n");
            //Send and resive Msg
            out.println(reqMsg);
            out.flush();

            byte[] resMsgBytes = {}; // Increase dynamically
            /*** Method 1 ***/
            // char[] buf = new char[MAX_MESSAGE_LEN];
            byte[] buf = new byte[MAX_MESSAGE_LEN];
            int len; //May over flow?
            while((len = in.read(buf)) != -1){
                //Handle buffer overflow
                int resMsgBytes_len = resMsgBytes.length;
                resMsgBytes = Arrays.copyOf(resMsgBytes, resMsgBytes.length+len); //extend length of resMsgBytes
                System.arraycopy(buf, 0, resMsgBytes, resMsgBytes_len, len); //copy buf to resMsgBytes

                /* 
                We can not add the following code since len < MAX_MESSAGE_LEN only means 
                we received one packet, but there may be multiple packet need to transmit. 
                */
                // System.out.println("*****The len ="+len);

                // if (len<MAX_MESSAGE_LEN) {
                //     break;
                // }
            }

            // FileOutputStream fos = new FileOutputStream("record.jpg");
            // fos.write(resMsgBytes, 0, resMsgBytes.length);

            // String resMsg = Base64.getEncoder().encodeToString(resMsgBytes); //Not the String we want
            String resMsg = new String(resMsgBytes);
            // System.out.println(resMsg);
            // System.out.println("&&&&&The len ="+len);
            String[] resMsg_split = resMsg.toString().split("\r\n\r\n");
            String resHeader = resMsg_split[0];
            /* 
            String resContent = resMsg_split[1]; 
            byte[] resContentBytes = Base64.getDecoder().decode(resContent); //image encode using base64.

            PS: Can not conver the image to String and decode back to binary 
            stream using base64 in this way. The reason may be New String(byte[]) 
            uses different encode method so that it makes the string been invalid 
            Base64 scheme.
            */

            byte[] resHeaderBytes = resMsg_split[0].getBytes();
            /* Subtract the head length from the original message.
            resHeaderBytes.length+4 because we count the "\r\n\r\n" following the header.*/
            byte[] resContentBytes = Arrays.copyOfRange(resMsgBytes, resHeaderBytes.length+4, resMsgBytes.length);
            
            /*** Method 2 ***///Wrong
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
            
            String[] parsedHeader = ParseResponseHeader(resHeader); //return {code, respCode, length, conType, encoding}
            String code = parsedHeader[0]; 
            String respCode = parsedHeader[1];
            String conType = parsedHeader[3];
            String encoding = parsedHeader[4];

            // System.out.println(code+" ***"+code.equals(HTTP_RESP_NOT_FOUND));
            if(code.equals(HTTP_RESP_NOT_FOUND)){
                String resContent = "From Web Proxy Server: 404 Not Found";
                resContentBytes = resContent.getBytes();
                //return
            }else if (code.equals(HTTP_RESP_OK)) {
                if (filename.length()>0) {
                    File cacheFile = OpenCacheFile(filename); //open file.
                    FileOutputStream fos = new FileOutputStream(cacheFile);
                    System.out.println("[WRITE FILE INTO CACHE]: " +  cacheFile.getPath());
                    fos.write(resContentBytes);
                    fos.flush();
                    fos.close();

                    Cache cache = new Cache(conType, cacheFile.getPath(), encoding);//(String add, String conType, String path, String encoding)
                    cacheMap.put(destAddress,cache);
                }
                
            }else{//POST
                //DO Nothing
                //return
            }

            System.out.println("\nRESPONSE HEADER FROM PROXY TO CLIENT:\n"
                            + respCode
                            + "\nEND OF HEADER\n");

            respCode += "\r\n\r\n"; //add "\r\n\r\n" back.
            byte[] respCodeBytes = respCode.getBytes(); //respCode
            byte[] mergeBytes = byteMergerAll(respCodeBytes, resContentBytes);

            serverSocket.close();
            return mergeBytes;
        }
        catch(Exception e){
            System.out.println("Error: Remote Connecting Error.");
            e.printStackTrace();
            return null;
        }

    }

    /* Should use thread here. */
    public static void ProcessRequest(Socket clientSocket, OutputStream out, BufferedReader in, String request){
        try{
            //Create a new socket for connecting to destination server
            String header = request.split("\r\n\r\n")[0];
            // System.out.println("HHHH:\n"+header);
            String[] headerList= ParseRequest(header); //return {method,destAddress,httpVersion}
            String[] result = null;
            String response = "";
            byte[] responseBytes = null;
            // Socket socket2 = new Socket("localhost", 80);
            // OutputStream outgoingOS = socket2.getOutputStream();
            // outgoingOS.write(data, 0, len);
            if (headerList[0].equals("GET")) {
                //Create a new socket for connecting to destination server
                //is in cache?
                Cache cache = FindInChash(headerList[1]);
                if(cache != null){
                    responseBytes = ComposeFromCache(cache);
                    // System.out.println("\n[LOOK UP THE CACHE]: FOUND IN THE CACHE: FILE = "+cache.path);

                }else{
                    System.out.println("\n[LOOK UP THE CACHE]: NOT FOUND, BUILD REQUEST TO SEND TO ORIGINAL SERVER");
                    // result = SendRequest(headerList[0], headerList[1], request); //{method,destAddress,data} return byte[]
                    responseBytes = SendRequest(headerList[0], headerList[1], request);
                    if(responseBytes == null)
                        return;
                }
            }else{
                System.out.println("*** Not GET ***");
                responseBytes = SendRequest(headerList[0], headerList[1], request);
                if(responseBytes == null)
                    responseBytes = (new String(" ")).getBytes();
                //TODO
                // SendRequest(headerList[0], headerList[1], request);
            }

            out.write(responseBytes);
            out.flush();
            clientSocket.close();

        }catch(IOException e){
            e.printStackTrace();
        }
    }

    public static void Listening() throws Exception{
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
        Listening();
    }
}