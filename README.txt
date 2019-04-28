Description:
	This project is a simple implement of Web-Proxy.

How to run it:
	Compile it using "javac Main.java".
	Then use "java Main" to run.
	You will see "WEB PROXY SERVER IS LISTENING" when the program start.

Before you run it:
	You should set your browser proxy setting first. 
	If you are using Mac: Open Safari -> Preferences -> Advanced -> Proxies: Change Settings -> Check the HTTP option, Web Proxy Server is "127.0.0.1" and the port is "6666" (you can change this but should be the same as you set in code). Then it should be fine. You can try some http websites by directly typing the destination address with out localhost:port.

	If you are using Windows: It is similar to do it.

Tips: Some http websites may reorient to your request to its https port, which will return code 302. Your browser will handle this scenario instead of this program, since the previous setting only forward http request to this program. If you want to catch the 302 code, you should not set the browser as before. What you need to do is adding "localhost:<port you listening>" before destination address and modify the "parseRemoteDestAddress" function in the Main.java to handle the localhost:<port>. I have not added the code about this though.