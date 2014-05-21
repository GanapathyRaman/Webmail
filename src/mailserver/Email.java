package mailserver;

import java.util.*;
import java.text.SimpleDateFormat;
import java.text.DateFormat;

public class Email {
    int delayTime; // delay time in second
    String source;
    String dest;
    String subject;
    String smtpServer;
    String message;
    String submittedTime;
    String sendTime;

    public Email(String source, 
                    String dest, 
                    String subject, 
                    String smtpServer, 
                    int delayTime,
                    String message) {
    	this.source = source;
    	this.dest = dest;
    	this.subject = subject;
    	this.smtpServer = smtpServer;
    	this.delayTime = delayTime;
    	this.message = message.trim();

    	// get current time
    	Calendar cal = Calendar.getInstance();
		//cal.add(Calendar.DATE, 1);
		Date date = cal.getTime(); 
		// get submitted time
    	DateFormat dateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss");
    	this.submittedTime = dateFormat.format(date);
    	// get send time
    	date.setTime(cal.getTimeInMillis() + delayTime*1000);
    	this.sendTime = dateFormat.format(date);
    }

    public String toString() {
    	return "From: " + source + "\r\n"
    			+ "To: " + dest + "\r\n"
    			+ "Subject: " + subject + "\r\n"
    			+ "Submitted time: " + submittedTime + "\r\n"
    			+ "Sent time: " + sendTime + "\r\n"
    			+ "\r\nContent: \r\n" + message + "\r\n";
    }
}