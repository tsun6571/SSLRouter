/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package jp.co.verisign.sslrouter;

import java.io.*;
import java.util.Date;

/**
 *
 * @author tsunoda
 */
public class myLog {
    static final String EOL = new String("" + ((char) 13) + ((char) 10));

    public static synchronized void eventLog(int level, String msg) {
        long tm = new Date().getTime();
        String info = "";
        switch (level) {
            case 1:
                info = "INFO: ";
                break;
            case 2:
                info = "WARN: ";
                break;
            case 3:
                info = "ERROR: ";
                break;
            case 4:
                info = "FATAL: ";
                break;
            case 5:
                info = "DEBUG: ";
                break;
            case 6:
                info = "TRACE: ";
                break;
            default:
                info = "INFO: ";
        }
        info += msg;
        if(level==3||level==4||SSLRouter.loglevel==8) {
            System.out.println(info);
        }
        if(info.length()>3000) {
            info = info.substring(0,3000);
        }
        //if(SSLRouter.loglevel<5 && level >4) { return; }
        if(SSLRouter.loglevel==0 && level==1) { return; }
        if(SSLRouter.loglevel==0 && level==2) { return; }
        if(SSLRouter.loglevel==0 && !(level==3 || level ==4)) { return; }
        //if(SSLRouter.loglevel>level) { return; }
        Audit a = new Audit();
        a.setDateTime(new java.sql.Timestamp(tm));
        a.setAppServer(SSLRouter.myName);
        a.setLog(info);
        try {
            SSLRouter.alog.insert(a);
        } catch (java.sql.SQLException ex) {
            //SSLRouter.logger.info("Eception in logger!!", ex);
            ex.printStackTrace();
        }
    }

    public static synchronized void writeLog(long stm, long etm,
            String certinfo,
            int loglvl,
            String host, int port,
            String imsg, String omsg) {

        if(SSLRouter.loglevel < loglvl) {
            return;
        }

        Transaction t = new Transaction();

        t.setDateTime(new java.sql.Timestamp(stm));
        t.setAppServer(host+":"+String.valueOf(port));
        t.setElapsedTime(etm);

        t.setClientCertInfo(certinfo);

        t.setLogLevel(loglvl);
        int ip = imsg.indexOf(EOL);
        if (ip < 0) { ip = 0; }
        int op = omsg.indexOf(EOL);
        if (op < 0) { op = 0; }
        String shortLog = imsg.substring(0, ip) + " -> "
                + omsg.substring(0, op);
        String longLog = imsg + omsg;
        if (longLog.length() > 3000) {
            longLog = longLog.substring(0, 3000);
        }
        if (SSLRouter.loglevel == 0) {
            t.setLog(shortLog);
        } else {
            t.setLog(longLog);
        }
        try {
            SSLRouter.tlog.insert(t);
        } catch (java.sql.SQLException ex) {
            SSLRouter.logger.info("Eception in logger!!", ex);
        }
    }

    public static String getStackTrace(Exception ex) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        ex.printStackTrace(pw);
        return sw.toString();
    }

}
