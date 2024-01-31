/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package jp.co.verisign.sslrouter;

/**
 *
 * @author t-yamamoto
 */
public class Transaction {

    // private String DateTime;
    private java.sql.Timestamp DateTime;
    private String AppServer;
    private String ClientCertInfo;
    private Long ElapsedTime;
    private Integer LogLevel;
    private String Log;

    public String getClientCertInfo() {
        return ClientCertInfo;
    }

    public void setClientCertInfo(String ClientCertInfo) {
        this.ClientCertInfo = ClientCertInfo;
    }

    public Long getElapsedTime() {
        return ElapsedTime;
    }

    public void setElapsedTime(Long ElapsedTime) {
        this.ElapsedTime = ElapsedTime;
    }

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

    public Integer getLogLevel() {
        return LogLevel;
    }

    public void setLogLevel(Integer LogLevel) {
        this.LogLevel = LogLevel;
    }

}
