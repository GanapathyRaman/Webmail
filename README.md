#
# Readme.txt
#
#  Project -  WebMail
#
#  Preferred platform for running the server - Any Linux Machine
#  Dependencies - 
#           * For the Server - openjdk-7-jdk or any available Java Development Kit packages
#           * For the Client - Any HTML supported web browser (Chrome, Firefox or IE) 
#
#  +----------+----------------------+-------------------------------------------+
#  | Authors  |  Tien Thanh Bui      | Ganapathy Raman Madanagopal               |
#  +----------+----------------------+-------------------------------------------+
#  |  Email   |    <ttbu@kth.se>     |               <grma@kth.se>               |
#  +---------+-----------------------+-------------------------------------------+
#
#  The following step can be use to run the Webmail Server
#  1. The src consist of "mailserver" folder and a unix shell script "runserver".
#  2. The mailserver folder consist of following file:
#          * Four Java Source files - HttpServer.java, EmailAgent.java, Email.java, Log.java
#          * Jar file for supporting DNS functionality - org.xbill.dns_2.1.6.jar
#          * Folder HTML
#              * Containing four HTML files - 404.htm  index.htm  status_begin.htm  status_end.htm
#  3. In order to run the server, go to src folder and use the command “sh runserver” or “./runserver”
#  4. Now the script will compile the java files and start will the Webserver automatically
#  5. At this point you should be able a line “The HTTP Server is running…” on the terminal
#  6. For the clients, open any web browser and type “http://<ip-address-of-the-server>”, a web page consisting of form for sending emails will appear on your web browser.
#  7. To see the status page, open any web browser and type “http://<ip-address-of-the-server>/status”
