package mailserver;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.Timer;
import java.util.TimerTask;
import org.xbill.DNS.*;
 
/**
 * The class for composing email
 */
public class EmailAgent extends Thread{
    private static final String HELO_DOMAIN = "localhost.com";

    private List<Email> mEmailList;

    public EmailAgent() {
        mEmailList = new ArrayList<Email>();
    }

    /**
     * Compose an email with given data
     */
    public String composeEmail(String source, 
                                String dest, 
                                String subject, 
                                String smtpServer, 
                                String delayTime,
                                String message) {
        // find the smtp of the recipient if not given
        if (smtpServer.equals("")) {
            smtpServer = getSmtpServer(dest);
        }
        // validate data
        String error = validateData(source, dest, subject, smtpServer, delayTime, message);

        // if there is error
        if (error != null) {
            Log.print("Error: " + error);
            return error;
        }

        // otherwise, compose the email
        Email email = new Email(source, dest, subject, smtpServer, Integer.parseInt(delayTime), message);
        // add to queue
        addToQueue(email);
        // set the timer to send email
        setSendingEmailTimer(email, true);

        return "The email will be sent in " + delayTime + " seconds";
    }

    /*!
     * Send confirmation email
     */
    private void composeConfirmationEmail(Email email, boolean success) {
        String message = null;
        String subject = null;
        if (success) {
            message = "Your email has been sent successfully !!!"; 
            subject = "Mail delivered successfully \"" + email.subject + "\"";
        } else {
            message = "Unable to send to the email !!!";
            subject = "Unable to deliver the email \"" + email.subject + "\"";
        }
        message += "\r\n\r\n" + email.toString();

        Email confirmEmail = new Email(
            "server@kth.se",
            email.source,
            subject,
            getSmtpServer(email.source),
            0,
            message);
        setSendingEmailTimer(confirmEmail, false);
    }

    /*!
     * Set the timer to send email
     */
    private void setSendingEmailTimer(Email email, boolean needConfirm) {
        Log.print("Creating a timer for: " + email.subject);
        ComposeEmailAction action = new ComposeEmailAction(email, needConfirm);

        Timer timer = new Timer();
        timer.schedule(action, 1000*email.delayTime);
    }

    /*!
     * Look up the mail server of the given email address
     */
    private String getSmtpServer(String email) {
        try {
            String domain = email.substring(email.indexOf("@")+1);
            Log.print("Looking up SMTP server for domain: " + domain);
            Record[] records = new Lookup(domain, Type.MX).run();

            int minPriority = Integer.MAX_VALUE;            
            String mailServer = "";
            for (int i = 0; i < records.length; i++) {
                MXRecord mx = (MXRecord) records[i];
                if (mx.getPriority() < minPriority) {
                    mailServer = mx.getTarget().toString();
                    minPriority = mx.getPriority();
                }
            }
            Log.print("SMTP server of: " + domain + " is: " + mailServer);
            return mailServer;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    /*!
     * Check if the given domain existed or not
     */
    public boolean isDomainExist(String domainName) {
        try {
            Lookup lookup = new Lookup(domainName, Type.A, DClass.IN);  
            lookup.run();  
            return (lookup.getResult() == Lookup.SUCCESSFUL);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Add an email to the queue
     */
    private synchronized void addToQueue(Email email) {
        mEmailList.add(email);
    }

    /**
     * Remove an email from the queue
     */
    private synchronized void removeFromQueue(Email email) {
        mEmailList.remove(email);
    }

    /*!
     * Return the email list
     */
    public List<Email> getEmailList() {
        return mEmailList;
    }

    /**
     * Validate data and return error if any
     */
    private String validateData(String source, 
                                String dest, 
                                String subject, 
                                String smtpServer, 
                                String delayTime,
                                String message) {
        if (!isValidEmail(source)) {
            return "Source email address is invalid";
        }
        if (!isValidEmail(dest)) {
            return "Destination email address is invalid";
        }
        if (!isInteger(delayTime)) {
            return "Delay time is invalid";
        }
        int time = Integer.parseInt(delayTime);
        if (time < 0) {
            return "Delay time is invalid";   
        }
        if (smtpServer == null || smtpServer.equals("")) {
            return "Unable to find the SMTP server corresponding to the recipient address";
        }
        if (!isDomainExist(smtpServer)) {
            return "SMTP server doesn't exist";
        }

        return null;
    }

    /**
     * Check if the given email is valid or not
     */
    private boolean isValidEmail(String email) {
        if (email == null) {
            return false;
        }
        String emailPattern = "^[_A-Za-z0-9-\\+]+(\\.[_A-Za-z0-9-]+)*@"
                        + "[A-Za-z0-9-]+(\\.[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})$";
        Pattern p = Pattern.compile(emailPattern);
        Matcher m = p.matcher(email);
        return m.matches();
    }

    /**
     * Check if the given string is an integer
     */
    private boolean isInteger(String s) {
        try { 
            Integer.parseInt(s); 
        } catch(NumberFormatException e) { 
            return false; 
        }
        return true;
    }

    public static String encodeSubject(String s) {
        byte[] b = null;
        try{
            b = s.getBytes("ISO-8859-15");
        } catch (UnsupportedEncodingException ex) {
                Log.print("Encoding Error");

        }
        String code = "";
        for (int i = 0; i < b.length; i++) {
            byte c = b[i];
            if(c == 13){
                code = code.concat("\n");
                continue;
            }         
            else if (c == 10){
                //do nothing
                continue;
            }
            code = code.concat("=" + Integer.toHexString(c & 255).toUpperCase());
            }
        // Log.print(code);
        return code;
    }

    public static String encodeMessage(String s) {
        byte[] b = null;
        try{
            b = s.getBytes("ISO-8859-15");
        } catch (UnsupportedEncodingException ex) {
                Log.print("Encoding Error");
        }
        String code = "";
        int wc = 0;
        for (int i = 0; i < b.length; i++) {
            byte c = b[i];
            if(c == 13){
                code = code.concat("\n");
                continue;
            }         
            else if (c == 10){
                //do nothing
                continue;
            }
            code = code.concat("=" + Integer.toHexString(c & 255).toUpperCase());
            wc += 3;
            if (wc >= 75) {
                code = code.concat("=\n");
                wc = 0;
            }
        }
        // Log.print(code);
        return code;
    }

    /**
     * A action listener used for sending email
     */
    class ComposeEmailAction extends TimerTask {
        Email mEmail;
        boolean mNeedConfirm;

        public ComposeEmailAction(Email email, boolean needConfirm) {
            mEmail = email;
            mNeedConfirm = needConfirm;
        }

        public void run() {
            Log.print("Start sending email: " + mEmail.subject);

            boolean success = true;

            try {
                // Establish a TCP connection with the mail server.
                Socket emailSocket = new Socket(mEmail.smtpServer, 25);

                // Create a BufferedReader to read a line at a time. 
                InputStream is = emailSocket.getInputStream(); 
                InputStreamReader isr = new InputStreamReader(is); 
                BufferedReader br = new BufferedReader(isr); 
                // Read greeting from the server. 
                String response = br.readLine(); 
                if (!response.startsWith("220")) { 
                    throw new Exception(response); 
                } 

                // Get a reference to the socket's output stream. 
                OutputStream os = emailSocket.getOutputStream(); 

                // Send HELO command and get server response. 
                String fullHeloCommand =  "HELO " + HELO_DOMAIN + "\r\n";
                os.write(fullHeloCommand.getBytes("US-ASCII")); 
                response = br.readLine(); 
                if (!response.startsWith("250")) { 
                    throw new Exception(response); 
                } 

                // Send MAIL FROM command.
                String mailFromCommand = "MAIL FROM:  <" + mEmail.source + ">\r\n";
                os.write(mailFromCommand.getBytes("US-ASCII"));
                response = br.readLine();
                if (!response.startsWith("250")) {
                    throw new Exception(response); 
                }

                // Send RCPT TO command.
                
                String fullAddress = "RCPT TO:  <" + mEmail.dest + ">\n";
                os.write(fullAddress.getBytes("US-ASCII"));
                response = br.readLine();
                if(!response.startsWith("250")) {
                    throw new Exception(response); 
                }

                // Send DATA command.  
                String dataString = "DATA\r\n";
                //Log.print(dataString);
                os.write(dataString.getBytes("US-ASCII"));
                response = br.readLine();
                if(!response.startsWith("354")) {
                    throw new Exception(response); 
                }
                //Log.print(response);

                // Send message data. 
                 String messageData = "From: " + "<" + mEmail.source + ">" + "\n"
                                    + "To: " + "<" + mEmail.dest + ">" + "\n"
                                    + "Subject: =?ISO-8859-15?Q?"
                                    + new EmailAgent().encodeSubject(mEmail.subject) + "?=\n"
                                    + "Date: " + mEmail.sendTime + "\n"
                                    //+ "Message-ID: " + cal.getTimeInMillis() + "@ik2213.lab\n"
                                    + "MIME-Version: 1.0\n"
                                    + "Content-Type:text/plain; charset=ISO-8859-15\n"
                                    + "Content-Transfer-Encoding: quoted-printable\n"
                                    + new EmailAgent().encodeMessage(mEmail.message)
                                    + "\r\n.\r\n";

                os.write(messageData.getBytes("US-ASCII"));
                response = br.readLine();
                if(!response.startsWith("250")) {
                    throw new Exception(response); 
                }
                 
                // Send QUIT command.
                String quitCommand = "QUIT";
                os.write(quitCommand.getBytes("US-ASCII"));
            } catch (Exception e) {
                e.printStackTrace();
                success = false;
            }

            // remove the email from queue
            removeFromQueue(mEmail);

            // log status
            if (success) {
                Log.print("The email has been sent successfully.");
            } else {
                Log.print("Unable to send the email.");
            }

            // send confirmation email if needed
            if (mNeedConfirm) {
                composeConfirmationEmail(mEmail, success);    
            }
        }
    }

}
