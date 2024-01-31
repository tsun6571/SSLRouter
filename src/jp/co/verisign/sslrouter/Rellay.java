/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package jp.co.verisign.sslrouter;

import java.io.*;
import java.net.*;

/**
 *
 * @author tsunoda
 * Created 2010/07/18.12:27:23
 */
public class Rellay extends Thread {
    private static Socket cs;
    private static String thost;
    private static int tport;
    private static String s;

    Rellay(Socket sock, String host, int port, String firstLine) {
        cs = sock;
        thost = host;
        tport = port;
        s = firstLine;
        this.start();
    }
    
    @Override
    public void run() {
        try {
                final InputStream cis = cs.getInputStream();
                final OutputStream cos = cs.getOutputStream();
                final Socket ts = new Socket(thost, tport);
                final InputStream tis = ts.getInputStream();
                final OutputStream tos = ts.getOutputStream();
                tos.write(s.getBytes());
            while (true) {

                new Thread() {

                    @Override
                    public void run() {
                        byte[] buf;
                        int r;
                        try {
                            buf = new byte[cs.getReceiveBufferSize()];
                        } catch (SocketException se) {
                            System.err.println(se.getMessage());
                            return;
                        }
                        do {
                            try {
                                r = cis.read(buf);
                                if (r > 0) {
                                    tos.write(buf, 0, r);
                                }
                            } catch (IOException ix) {
                                System.err.println(ix.getMessage());
                                r = -1;
                            }
                        } while (r > 0);
                        try {
                            cis.close();
                            cos.close();
                            cs.close();
                            tis.close();
                            tos.close();
                            ts.close();
                            //System.out.println("External client " + cs + " has disconnected successfully");
                        } catch (IOException ix) {
                            System.err.println(ix.getMessage());
                        }
                        //System.out.println("External thread for the client " + cs + " has terminated");
                    }
                }.start();

                new Thread() {
                    @Override
                    public void run() {
                        byte[] buf;
                        int r;
                        try {
                            buf = new byte[ts.getReceiveBufferSize()];
                        } catch (SocketException se) {
                            System.err.println(se.getMessage());
                            return;
                        }
                        do {
                            try {
                                r = tis.read(buf);
                                if (r > 0) {
                                    cos.write(buf, 0, r);
                                }
                            } catch (IOException ix) {
                                System.err.println(ix.getMessage());
                                r = -1;
                            }
                        } while (r > 0);
                        try {
                            tis.close();
                            tos.close();
                            ts.close();
                            cis.close();
                            cos.close();
                            cs.close();
                            //System.out.println("Internal client " + ts + " has disconnected successfully");
                        } catch (IOException ix) {
                            System.err.println(ix.getMessage());
                        }
                        //System.out.println("Internal thread for the client " + ts + " has terminated");
                    }
                }.start();
            }
        } catch (IOException z) {
            System.err.println(z.getMessage());
            //System.exit(3);
        }
    }
}
