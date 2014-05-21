package mailserver;

import java.net.*; 
import java.io.*; 
import java.util.*; 
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.swing.Timer;
import java.text.SimpleDateFormat;
import java.text.DateFormat;

class HttpRequest implements Runnable{ 
	private static final String HTML_FOLDER = "mailserver/html/";
	private static final String HTML_INDEX = HTML_FOLDER + "index.htm";
	private static final String HTML_404 = HTML_FOLDER + "404.htm";
	private static final String HTML_STATUS_BEGIN = HTML_FOLDER + "status_begin.htm";
	private static final String HTML_STATUS_END = HTML_FOLDER + "status_end.htm";

	private static final String STATUS_PAGE_ACTION = "status";
	private static final String COMPOSE_EMAIL_ACTION = "composeEmail";
	private static final String SOURCE_FIELD = "from";
	private static final String DEST_FIELD = "to";
	private static final String SUBJECT_FIELD = "subject";
	private static final String DELAY_FIELD = "delayTime";
	private static final String SMTP_SERVER_FIELD = "smtpServer";
	private static final String MESSAGE_FIELD = "message";

	private Socket mClientConn; 
	private EmailAgent mEmailAgent;

	public HttpRequest(Socket clientConn, EmailAgent emailAgent) throws Exception { 
		this.mClientConn = clientConn; 
		this.mEmailAgent = emailAgent;
	} 

	public void run() { 
		try {
			BufferedReader in = new BufferedReader(new InputStreamReader(mClientConn.getInputStream()));
			BufferedOutputStream out = new BufferedOutputStream(mClientConn.getOutputStream()); 

			String request = in.readLine().trim(); 
			Log.print("Request: " + request);

			StringTokenizer st = new StringTokenizer(request); 
			String requestType = st.nextToken(); 
			String action = st.nextToken().substring(1); 

			// return index.html
			if (requestType.equals("GET") && action.equals("")) { 
				returnHTMLFile(HTML_INDEX, 200, out);

			// return status page
			} else if (requestType.equals("GET") && action.equals(STATUS_PAGE_ACTION)) {
				returnStatusPage(out);
			// compose email
			} else if (requestType.equals("POST") && action.equals(COMPOSE_EMAIL_ACTION)) {
				composeEmail(in, out);

			// action undefined
			} else {
				returnHTMLFile(HTML_404, 404, out);
			}

			out.close(); 
		} catch (Exception e) {
			e.printStackTrace();
		}

		// close connection
		try {
			mClientConn.close(); 	
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	} 

	/**
	 * Parse the POST data from the request
	 */ 
	private Map<String, String> parsePOSTData(BufferedReader in){
		Map<String, String> data = null;

		try {
			String line = "";
	        // looks for post data
	        int postDataI = -1;
	        while ((line = in.readLine()) != null && (line.length() != 0)) {
	            //Log.out("HTTP-HEADER: " + line);
	            if (line.indexOf("Content-Length:") > -1) {
	                postDataI = new Integer(
	                        line.substring(
	                                line.indexOf("Content-Length:") + 16,
	                                line.length())).intValue();
	            }
	        }

	        String postDataStr = "";
	        // read the post data
	        if (postDataI > 0) {
	        	data = new HashMap<String, String>();	

	            char[] charArray = new char[postDataI];
	            in.read(charArray, 0, postDataI);
	            postDataStr = new String(charArray);
	            Log.print(postDataStr);

	            String[] fields = postDataStr.split("&");
	            for (String field : fields) {
	            	field = URLDecoder.decode(field, "ISO-8859-15");
	            	String[] tmp = (field + " ").split("=", 2);
	            	if (tmp.length < 2) {
	            		continue;
	            	}
	        		data.put(tmp[0], tmp[1].trim());
	            }
	        }

	    	return data;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * Compose an email with data from user
	 */
	private void composeEmail(BufferedReader in, 
									BufferedOutputStream out) throws Exception{
		Log.print("Receiving a sending email request...");
		String message = null;
		int statusCode = 200;

		try {
			Map<String, String> data = parsePOSTData(in);
			
			if (data == null) {
				message = "Invalid data";
			} else {
				for (Map.Entry<String, String> entry : data.entrySet()) {
					//Log.print(entry.getKey() + ": " + entry.getValue());
				}

				message = mEmailAgent.composeEmail(
					data.get(SOURCE_FIELD),
					data.get(DEST_FIELD),
					data.get(SUBJECT_FIELD),
					data.get(SMTP_SERVER_FIELD),
					data.get(DELAY_FIELD),
					data.get(MESSAGE_FIELD)
				);
			}
		} catch (Exception e) {
			e.printStackTrace();
			message = "Invalid data";
		}

		String resHeader = createHeader(statusCode, 5);
		String resData = "<html> <body> <h2>" + message + "</h2> <p>"
							+ "<h3> To see the list of pending emails </h3>"
							+ "<a href='status'>Click Here"
							+ "</a> </p></body> </html>";
		out.write(resHeader.getBytes());
		out.write(resData.getBytes());
	}

	/**
	 * Return the status page to client
	 */
	private void returnStatusPage(BufferedOutputStream out) {
		try {
			List<Email> emailList = mEmailAgent.getEmailList();
			// write header
			String resHeader = createHeader(200, 5);
			out.write(resHeader.getBytes());

			parseHTMLFile(HTML_STATUS_BEGIN, out);
			if (emailList.size() == 0) {
				out.write("<tr> <td colspan='6'>No pending emails!</td></tr>".getBytes());		
			} else {
				for (int i = 0; i < emailList.size(); i++) {
					Email email = emailList.get(i);

					out.write("<tr>".getBytes());
					out.write(("<td>" + (i+1) + "</td>").getBytes());
					out.write(("<td>" + email.source + "</td>").getBytes());
					out.write(("<td>" + email.dest + "</td>").getBytes());
					out.write(("<td>" + email.subject + "</td>").getBytes());
					out.write(("<td>" + email.submittedTime + "</td>").getBytes());
					out.write(("<td>" + email.sendTime + "</td>").getBytes());
					out.write("</tr>".getBytes());
				}
			}
			parseHTMLFile(HTML_STATUS_END, out);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Return a HTML file to client
	 */
	private void returnHTMLFile(String fileName, int statusCode, 
								BufferedOutputStream out) {
		try{
			// write header
			String resHeader = createHeader(statusCode, 5);
			out.write(resHeader.getBytes());

			// write the html file
			parseHTMLFile(fileName, out);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/*!
	 * Parse a HTML file
	 */
	private void parseHTMLFile(String fileName, BufferedOutputStream out) {
		try{
			FileInputStream fin = null; 

			try { 
				fin = new FileInputStream(fileName); 
			} catch(Exception e) {
				e.printStackTrace();
				return;
			}

			int temp = 0; 
			byte[] buffer = new byte[1024]; 
			int bytes = 0; 
			while ((bytes = fin.read(buffer)) != -1 ) { 
				out.write(buffer, 0, bytes); 
				for(int iCount = 0; iCount < bytes; iCount++) { 
					temp = buffer[iCount]; 
				} 
			} 
			fin.close(); 
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/***
	 * Make the HTTP header for the response
	 */
	private String createHeader(int return_code, int file_type) {
	    String s = "HTTP/1.0 ";
	    //you probably have seen these if you have been surfing the web a while
	    switch (return_code) {
	      case 200:
	        s = s + "200 OK";
	        break;
	      case 400:
	        s = s + "400 Bad Request";
	        break;
	      case 403:
	        s = s + "403 Forbidden";
	        break;
	      case 404:
	        s = s + "404 Not Found";
	        break;
	      case 500:
	        s = s + "500 Internal Server Error";
	        break;
	      case 501:
	        s = s + "501 Not Implemented";
	        break;
	    }

	    s = s + "\r\n"; //other header fields,
	    s = s + "Connection: close\r\n"; //we can't handle persistent connections
	    s = s + "Server: Simple HTTP Server \r\n"; //server name

	    //Construct the right Content-Type for the header.
	    switch (file_type) {
	      //plenty of types for you to fill in
	      case 0:
	        break;
	      case 1:
	        s = s + "Content-Type: image/jpeg\r\n";
	        break;
	      case 2:
	        s = s + "Content-Type: image/gif\r\n";
	      case 3:
	        s = s + "Content-Type: application/x-zip-compressed\r\n";
	      default:
	        s = s + "Content-Type: text/html\r\n";
	        break;
	    }

	    s = s + "\r\n"; //this marks the end of the httpheader

	    return s;
    }
} 

class HttpServer { 
	public static void main(String args[]) throws Exception { 
		// create a HTTP server
		ServerSocket serverSocket = new ServerSocket(80); 
		// create a thread pool to handle client connections
		ExecutorService threadPool = Executors.newFixedThreadPool(10);
		// create an email agent handling sending emails
		EmailAgent emailAgent = new EmailAgent();

		Log.print("The HTTP Server is running...");

		while(true) { 
			Socket inSocket = serverSocket.accept(); 
			HttpRequest request = new HttpRequest(inSocket, emailAgent); 
			// processing the request
			threadPool.execute(request);
		} 
	} 
} 


