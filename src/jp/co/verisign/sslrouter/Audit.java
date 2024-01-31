/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package jp.co.verisign.sslrouter;

/**
 *
 * @author t-yamamoto
 */
public class Audit {

    // private String DateTime;
    private java.sql.Timestamp DateTime;
    private String AppServer;
    private String Log;

    public String getAppServer() {
        return AppServer;
    }

    public void setAppServer(String AppServer) {
        this.AppServer = AppServer;
    }

    public java.sql.Timestamp getDateTime() {
        return DateTime;
    }

    public void setDateTime(java.sql.Timestamp DateTime) {
        this.DateTime = DateTime;
    }

    public String getLog() {
        return Log;
    }

    public void setLog(String Log) {
        this.Log = Log;
    }
}
