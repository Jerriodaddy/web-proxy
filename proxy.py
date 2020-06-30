# Output formatting improvements w.r.t. original code from Mai
# Lines with $$$$$$$ are for debugging
# Proxy should not cache if response is 302 (redirection) or MOVED PERMANENTLY
# Renamed some sockets to be consistent with the pseudocode in project assignment
# Commented out the case of not GET 

import socket
import os, sys
from lib2to3.pgen2.tokenize import String
#from ctypes.wintypes import MSG

MAX_MESSAGE_LEN = 2048
HOST  = "localhost" #type in the command prompt ipconfig.
#Look for your IPv4 address. Put that in the quotes above
HTTP_PORT_REMOTE = 80
PORT = 5005 # Arbitrary non-privileged port
LOOKUP_RESULT_FOUND = 0
LOOKUP_RESULT_NOTFOUND = 1
HTTP_RESP_OK = "200 OK"
HTTP_RESP_NOT_FOUND = "404 Not Found"
TAG_CACHE_ADDRESS = "address"
TAG_CACHE_PATH = "path"
PATH_BASE="cache"

ADDRESS = "address"
PATH = "path"
MIME ="mime"
ENCODE = "encode"
CACHE_INDEX_FILENAME = "cache.index"
cache_index = [ ]
#http://localhost:5005/htmldog.com/examples/images1.html
#http://localhost:5005/www.google.com
#http://localhost:5005/assets.climatecentral.org/images/uploads/news/Earth.jpg
#http://localhost:5005/www.bing.com

def Lookup(address):
    #print "[LOOK UP THE CACHE] LOOK UP THE CACHE"
    #look up the cache
    for x in cache_index:
        addr = x.get(ADDRESS)
        if address == addr:
            path = x.get(PATH)
            if os.path.isfile(path):
                print "\n[LOOK UP THE CACHE]: FOUND IN THE CACHE: FILE =%s" %path
                return (LOOKUP_RESULT_FOUND, x)
            else:
                cache_index.remove(x)
                #updateCacheFile():
                return (LOOKUP_RESULT_NOTFOUND, 0)
        else:
            continue      
    return (LOOKUP_RESULT_NOTFOUND, 0)

def parseRemoteDestAddress(destAddress):
    #print "parseRemoteDestAddress"
    destAddress_split = destAddress.split('/')
    n = len(destAddress_split)
    hostname = destAddress_split[0]
    url = ""
    filename = ""
    if(n-1>0):
        url = destAddress[(len(hostname)+1):]
        filename = destAddress_split[n-1]
    if len(hostname) !=0:
        print "[PARSE REQUEST HEADER] HOSTNAME IS %s" % hostname #$$$$$$$$$$$$$$$$$$$$$$$$
    if len(url) != 0:
        print "[PARSE REQUEST HEADER] URL IS %s"%url #$$$$$$$$$$$$$$$$$$$$$$$$
    if len(filename) != 0:
        print "[PARSE REQUEST HEADER] FILENAME is %s"%filename
    return (hostname, url, filename)

def ProcessResponseHeader(resHeader):

    #print"ProcessResponseHeader"
    #print "RESPONSE HEADER FROM SERVER:" 
    #print resHeader #$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$
    header_split = resHeader.split('\r\n')
    content_len = 0
    code = ""
    contenttype = ""
    respCode = ""
    encoding =""
    for x in header_split:
        x_split = x.split(" ")
        
        if x_split[0].startswith(("HTTP/")):
            tmp = x_split[1]
            respCode = x
            if tmp == "200":
                code = HTTP_RESP_OK
                continue  #no need to parse the Cache-Control tag
            
            if tmp == "404": #From server: HTTP/1.1 301 Moved Permanently
                code = HTTP_RESP_NOT_FOUND
        if x_split[0].startswith("Content-Length:"):
            content_len = int(x_split[1])
            #print "content_len is %d" %content_len #$$$$$$$$$$$$$$$$$$$$$$$$$
        if x_split[0].startswith("Content-Type"):
            contenttype=x_split[1]
            respCode += "\r\n" + x
        if x_split[0].startswith("Content-Encoding"):
            encoding = x_split[1]
            if encoding == "gzip":
                respCode += "\r\n"+"Content-Encoding: gzip"
    return (code, respCode, content_len, contenttype, encoding)

def composeRequestMessageHeader(host, url, header):
    #print "composeRequestMessage"
    
    header_split = header.split("\r\n")
    count = 1
    reqMessage = ""
    for x in header_split:
        if count == 1:
            count += 1
            x_split=x.split(" ")
            
            reqMessage += x_split[0] + " " + "/" + url + " " + x_split[2] + "\r\n"
            continue
        else:
            if x.startswith("Host:"):
                x_split = x.split(" ")
                reqMessage += x_split[0] + " " + host + "\r\n"
            else:
                if x.startswith("Connection"):
                    x_split = x.split(" ")
                    reqMessage +=x_split[0] + " "+"close" + "\r\n"
                else:
                    reqMessage += x + "\r\n"
    reqMessage += "\r\n"  
    #print "reqMessage = %s" %reqMessage
    return reqMessage


def ParseRequest(reqHeader):  #Now, it's used to get the method, destAddress, and HTTPVersion
    headerList = reqHeader.split("\r\n")
    count = 1
    destAddress = ""
    method = ""
    HTTPVersion = ""
    
    for x in headerList:        #we now only parse the first line
        
        if count == 1:                  #method is always in the first line? 
            methodList = x.split(" ")   #example: "GET /www.google.com HTTP/1.1"
            method = methodList[0]
            if methodList[1].startswith('/'):
                d= methodList[1]
                destAddress = d[1:]
            else:
                destAddress = methodList[1]
                print "dest: %s" %destAddress
                
            HTTPVersion=methodList[2]
            if destAddress.endswith("/"):
                destAddress = destAddress[:-1]
        if x.startswith("Referer:"): #http://localhost:5005/www.bing.com
            x_split = x.split("/")
            t = destAddress.split("/")
            if t[0] != x_split[3]:   #the hostnambe may have been included in the url
                destAddress = x_split[3] +"/" + destAddress
        count += 1
    print "\n[PARSE MESSAGE HEADER]:\n METHOD = %s, DESTADDRESS = %s, HTTPVersion = %s" % (method,destAddress, HTTPVersion)
    #print "###########################" 
    return (method, destAddress, HTTPVersion)

# Returns cache_f
def OpenCacheFile(filename):
    if False == os.path.exists(PATH_BASE):
        os.mkdir(PATH_BASE)
    pathname = PATH_BASE +"/"+ filename
    print "[WRITE FILE INTO CACHE]:  %s" % pathname
    if False == os.path.isfile(pathname):
        cache_f= open(pathname, "a+b")
    else:
        os.remove(pathname)
        cache_f= open(pathname, "a+b")
    return cache_f


def ComposeResponse(rec):
    #print("COMPOSE RESPONSE FROM LOCAL FILE")
    print("\n")
    path = rec.get(PATH)
    filetype = rec.get(MIME)
    encoding = rec.get(ENCODE)
    if False == os.path.isfile(path):
        return ""
    else:
        
        cache_f= open(path, "rb")
        content = cache_f.read() #!!!!!!!!!!!!
        #print "Content read from cache: " + str(content)
        cache_f.close()
        n = len(content)
        header = "HTTP/1.1 200 OK\r\n" +"Content-Length: " + str(len(content))+"\r\n"
        if n ==0:
            return ""
        
        else:
            if len(filetype)!=0:
                header += "Content-Type: " + filetype + "\r\n"
           
            if encoding == "gzip":
                header += "Content-Encoding: gzip" + "\r\n"
        header += "\r\n"
    #print "RESPONSE HEADER FROM PROXY TO CLIENT: FROM CACHE FILE %s" %path #$$$$$$$$$$$$$$$$$$$$$$$$
    #print header #$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$
    response = header + content
    #print response
    #print "END OF HEADER" #$$$$$$$$$$$$$$$$$$$$$$$$$$$$
    return response


# Sends request to original server and returns response in (header, body) form
def SendRequest(method, destAddress, data): 
    #print "[SendRequest] FROM WEB PROXY TO REAL SERVER" #$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$
    try:
        serverSocket=socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    except socket.error as msg:
        print ("FAILED TO CREATE A SOCKET: %s"%msg)
        sys.exit()
    if len(destAddress) != 0:
        destHostname, url, filename = parseRemoteDestAddress(destAddress)
    else:
        print "NO DESTINATION HOSTNAME"
        return (HTTP_RESP_NOT_FOUND, "")
    
    try:
        remoteIP = socket.gethostbyname(destHostname)
    except socket.error as msg:
        print "REMOTE IP ERROR: %s"%msg
        return ("HTTP/1.1 400 Bad Request", "From Web Proxy:400 Bad Request")
    serverSocket.connect((remoteIP, HTTP_PORT_REMOTE))
    #compose the http GET or POST message
    data_split = data.split("\r\n\r\n")
    httpMsgHeader=composeRequestMessageHeader(destHostname, url, data_split[0])
    reqMsg = httpMsgHeader + data_split[1]
    print '\nREQUEST MESSAGE SENT TO ORIGINAL SERVER:'
    print reqMsg
    print 'END OF MESSAGE SENT TO ORIGINAL SERVER'
    #if method == "POST":
        #print "[POST HEADER FROM WEB PROXY]:"
        #print reqMsg
    try:
       
        serverSocket.sendall(reqMsg)
    except socket.error as msg:
        print "SOCKET SENDING ERROR: %s"%msg
        #how to response back?
    
    buf = serverSocket.recv(MAX_MESSAGE_LEN) #how to know if this is the end? parse the header first
    l = buf.split("\r\n\r\n")
    resHeader = l[0]
    print '\nRESPONSE HEADER FROM ORIGINAL SERVER:'
    #print 'HEADER, LENGTH IS ' + str(len(resHeader))
    print resHeader
    #print buf
    print 'END OF HEADER\n'
    
    if len(l)>1:
        resContent = l[1]
    else:
        resContent = ""
    contentlen = 0
    #print "Content length = " + str(len(resContent)) #^^^^^^^^^^^^^^^^^

    code, respCode, contentlen, contenttype, encoding = ProcessResponseHeader(resHeader) #get response code, expire, cache-control, size
    counter = 0
    #print "After call ProcessResponseHeader ###################"
    # DEBUG: Need to robustify the code for other cases of response, such as MOVED PERMANENTLY?
    if(code == HTTP_RESP_NOT_FOUND):
        if len(resContent)==0:
            resContent = "From Web Proxy Server: 404 Not Found"
        else:
            #print "[RESPONSE FROM SERVER] 404: THERE IS CONTENT FROM SERVER" #$$$$$$$$$$$$$$$$$$$$$$$$$$$$
            counter = len(resContent)
            n = counter
            #print resContent #$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$
            while n>0:
                c = serverSocket.recv(MAX_MESSAGE_LEN)
                n = len(c)
                counter += n
                resContent += c
                if contentlen >0:
                    if counter >= contentlen:
                        break
            #print "Content size counter = %d" % (counter)
            serverSocket.close()
            return (respCode, resContent)
    else:
        if(code == HTTP_RESP_OK):
	    #print "[RESPONSE FROM SERVER] 200 OK" #$$$$$$$$$$$$$$$$$$$$$
 	    if len(filename) != 0:
                if len(filename) > 40:
                    filename = filename[-20:]
                fd = OpenCacheFile(filename)
                
            else:
                filename = destAddress
                if len(filename) > 40:
                    filename = filename[-20:]
                fd = OpenCacheFile(filename)
            counter = len(resContent)
            fd.write(resContent)
            n = counter
            while n>0:
                c = serverSocket.recv(MAX_MESSAGE_LEN)
                #if not c: #^^^^^^^^ I added, but perhaps not needed
                    #break #^^^^^^
                n = len(c)
                counter += n
                resContent += c
                fd.write(c)
                if contentlen >0:
                    if counter >= contentlen:
                        break
            fd.close()
            #(destAddress, BASE_PATH+filename) add to the local variable
            cache_rec = {}
            cache_rec[ADDRESS] = destAddress
            cache_rec[MIME] = contenttype
            cache_rec[PATH] = PATH_BASE+ "/" + filename
            cache_rec[ENCODE] = encoding
            
            
            cache_index.append(cache_rec)
            serverSocket.close()
            #print 'BODY LENGTH IS ' + str(len(resContent))
            #print 'BODY LENGTH IS ' + str(counter)
            #print 'TOTAL MESSAGE LENGTH IS ' + str(len(resContent)+len(resHeader))
            return (respCode, resContent)
           
        else:  #204-POST
            counter = len(resContent)
            n = counter
            #print "[RESPONSE FROM SERVER]" #$$$$$$$$$$$$$$$$$$$$$$$$
            #print resContent #DEBUG2
            while n>0:
                c = serverSocket.recv(MAX_MESSAGE_LEN)
                n = len(c)
                counter += n
                resContent += c
                if contentlen >0:
                    if counter >= contentlen:
                        break
            serverSocket.close()
            #print 'BODY LENGTH IS ' + str(counter)
            #print 'TOTAL MESSAGE LENGTH IS ' + str(len(resContent)+len(resHeader))
            return (resHeader, resContent)
    

def ProcessRequest(conn, addr, data): 
    reqHeader = data.split("\r\n\r\n")[0]
    method, destAddress, HTTPVersion=ParseRequest(reqHeader) 
    if method == "GET":   #GET
        ret,rec = Lookup(destAddress)
        if ret==LOOKUP_RESULT_NOTFOUND:
            print "\n[LOOK UP IN THE CACHE]: NOT FOUND, BUILD REQUEST TO SEND TO ORIGINAL SERVER "
            code, content = SendRequest(method, destAddress, data) #code is the response header from destiny
            response= code + "\r\n\r\n" + content
        else: #send back to client
            response = ComposeResponse(rec)
### UNCOMMENT THIS
    else:   # Not GET    
        code, content = SendRequest(method, destAddress, data) #code is the response header from destiny
        print "[POST] THE RESPONSE HEADER TO CLIENT: "
        print code
        
        response= "HTTP/1.1 200 OK\r\n\r\n" + content
        response= code + "\r\n\r\n" + content
### END OF UNCOMMENT
            
    print "\nRESPONSE HEADER FROM PROXY TO CLIENT:"
    respHeader = response.split("\r\n\r\n")[0]
    #print "HEADER, LENGTH IS " + str(len(respHeader))
    print respHeader
    print "\nEND OF HEADER"
    #print 'TOTAL MESSAGE LENGTH IS ' + str(len(response))
    
    conn.sendall(response) # how if the buffer size is too large? 
    conn.close()
           
def Listening():
    s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
 
    try:
        s.bind((HOST, PORT))
    except socket.error as msg:
        print ('WEB PROXY SERVER BINDING FAIL. Error Code : ' + str(msg[0]) + ' MESSAGE ' + msg[1])
        sys.exit()
     
    s.listen(5)
    print ('\nWEB PROXY SERVER IS NOW LISTENING')
     
    #now keep talking with the client
    while 1:
        #wait to accept a connection - blocking call
        conn, addr = s.accept()
        print "\r\n\r\n"
        print ('WEB PROXY SERVER CONNECTED WITH  ' + addr[0] + ':' + str(addr[1]))
        data = c = conn.recv(MAX_MESSAGE_LEN)
        data_split = data.split("\r\n\r\n")
        if len(data_split) >1:
            count = len(data_split[1])
            #print (count) #$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$
            #print ('%%%%%%%%%%%%%%%%%% data_split[1]: ' + data_split[1] + '\n%%%%%%%%%%%%%%%') #$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$
        else:
            count = 0;
        if not data:
            conn.close()
            continue
        while len(c) == MAX_MESSAGE_LEN:
            
            c=conn.recv(MAX_MESSAGE_LEN)
            count += len(c)
            data += c
        
        print ("\nMESSAGE RECEIVED FROM CLIENT:\n" + str(data) + "END OF MESSAGE RECEIVED FROM CLIENT")##################
        ProcessRequest(conn, addr, data)      
    s.close()
#def __init__(self):
    #do something here?
if __name__ == "__main__":
    
    Listening()
    
        
