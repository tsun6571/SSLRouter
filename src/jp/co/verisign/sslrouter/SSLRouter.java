package jp.co.verisign.sslrouter;

import java.io.*;
import java.net.*;
import javax.net.ServerSocketFactory;
import javax.net.ssl.*;
import java.util.Properties;
import java.security.*;
import java.util.concurrent.*;
import java.util.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.apache.log4j.*;

public class SSLRouter {

    public static Log logger = LogFactory.getLog(SSLRouter.class);
    public static TransactionLog tlog;
    public static AuditLog alog;
    public static RouteInfoQuery riq;
    public static int loglevel = 8;
    public static String myName = "";
    public static String myAddr = "";
    public static String noLogIP="";
    public static boolean mode=false;

    public static void main(String[] args) {
        try {
            // Load Property
            Properties prop = new Properties();
            prop.load(new FileInputStream("conf" + File.separator + "SSLRouter.properties"));
            //tlog = new TransactionLog("conf" + File.separator +"SqlMapConfig.xml");
            //alog = new AuditLog("conf" + File.separator +"SqlMapConfig.xml");
            //riq = new RouteInfoQuery("conf" + File.separator +"SqlMapConfig.xml");
            tlog = new TransactionLog("jp/co/verisign/sslrouter/SqlMapConfig.xml");
            alog = new AuditLog("jp/co/verisign/sslrouter/SqlMapConfig.xml");
            riq = new RouteInfoQuery("jp/co/verisign/sslrouter/SqlMapConfig.xml");

            // SSL Debug Flag
            String sslDebug = prop.getProperty("ssl.debug");
            if (Boolean.valueOf(sslDebug).booleanValue()) {
                System.setProperty("javax.net.debug", "ssl:handshake,session,sslctx");
            }

            // Router Mode Flag
            String sslMode = prop.getProperty("ssl.mode");
            if (sslMode != null ) {
                mode = Boolean.valueOf(sslMode).booleanValue();
            }

            // Using for log4j due to HTTPClient requires
            String logmode = prop.getProperty("log.mode");
            PropertyConfigurator.configure(logmode);

            // Set port number
            int port = Integer.parseInt(prop.getProperty("server.port"));

            // Set thread pool number
            int pool = 50;
            try { pool = Integer.parseInt(prop.getProperty("thread.pool")); }
            catch(Exception e) {}

            // Set Server Name
            myName = prop.getProperty("server.name");
            myLog.eventLog(1, "My name is " + myName);

            // Set Server Name
            myAddr = prop.getProperty("server.address");
            myLog.eventLog(1, "Bind Address is " + myAddr);

            // No log heart beat
            noLogIP = prop.getProperty("server.nologip");
            myLog.eventLog(1, "No log SSL Init Error IP is " + noLogIP);

            // Set Server Key Store
            String keyStore = prop.getProperty("ssl.keyStore");
            myLog.eventLog(1, "Key Store is " + keyStore);

            // Load KeyStore
            KeyStore ks = KeyStore.getInstance("JKS");
            char[] keystorePass = prop.getProperty("ssl.keyStore.password").toCharArray();
            ks.load(new FileInputStream(keyStore), keystorePass);
            KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
            kmf.init(ks, keystorePass);

            // Set Trust Store
            String trustStore = prop.getProperty("ssl.trustStore");
            System.setProperty("javax.net.ssl.trustStore", trustStore);
            System.setProperty("javax.net.ssl.trustStorePassword", prop.getProperty("ssl.trustStore.password"));
            myLog.eventLog(1, "Trust Store is " + trustStore);

            // Create Server Socket
            // Bind to address, if nessearry
            InetAddress iad = InetAddress.getByName(myAddr);
            //InetSocketAddress isa = new InetSocketAddress(iad,port);
            SSLContext context = SSLContext.getInstance("TLS");
            context.init(kmf.getKeyManagers(), null, null);
            SSLServerSocketFactory ssf = context.getServerSocketFactory();
            SSLServerSocket ssoc = null;

            if(myAddr==null) {
                ssoc = (SSLServerSocket) ssf.createServerSocket(port);
            } else {
                ssoc = (SSLServerSocket) ssf.createServerSocket(port, -1, iad);
            }

            // Set Client Auth required
            ssoc.setNeedClientAuth(true);
            myLog.eventLog(1,"Ready for Service");
            ExecutorService Exec = Executors.newFixedThreadPool( pool );

            while (true) {
                try {
                    //myLog.eventLog(1, "Waiting for connection from client");
                    // Waiting for Client Connection
                    SSLSocket soc = (SSLSocket) ssoc.accept();

                    //myLog.eventLog(1, "receive client!");
                    // Fork to Connect()
                    //new Connect(soc);
                    Exec.submit(new Connect(soc));
                } catch (Exception e) {
                    myLog.eventLog(3, e.getLocalizedMessage());
                    myLog.eventLog(5, myLog.getStackTrace(e));
                }
            } // End of while

        } catch (Exception e) {
            myLog.eventLog(3, e.getLocalizedMessage());
            myLog.eventLog(5, myLog.getStackTrace(e));
        }
    }
}
