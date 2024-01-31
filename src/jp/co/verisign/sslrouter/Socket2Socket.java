/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package jp.co.verisign.sslrouter;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;

/**
 *
 * @author t-yamamoto
 */
public class Socket2Socket extends Thread {

    private Socket s_from;
    private Socket s_to;
    private String fl;

    public Socket2Socket(Socket s1, Socket s2, String s) {
        s_from = s1;
        s_to = s2;
        fl = s;

        start();
    }

    @Override
    public void run() {
        try{
            InputStream is = s_from.getInputStream();
            OutputStream os = s_to.getOutputStream();
            BufferedInputStream bis = new BufferedInputStream(is);
            BufferedOutputStream bos = new BufferedOutputStream(os);
            if(fl!=null) {
                //System.out.print(fl);
                bos.write(fl.getBytes());
            }

            while(true){
                int data = bis.read();
                if(data < 0) {
                    return;
                }
                bos.write(data);
                bos.flush();

                //System.out.printf("%c", data);
            }
        }
        catch(java.io.IOException ex){

        }

    }

}
