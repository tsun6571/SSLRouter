package jp.co.verisign.sslrouter;

import java.io.*;
import java.net.*;
import java.util.Date;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.List;

import javax.net.ssl.*;

import org.apache.commons.httpclient.*;
import org.apache.commons.httpclient.methods.*;
import org.apache.commons.httpclient.params.HttpMethodParams;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


//public class Connect extends Thread {
public class Connect implements Runnable {

    private SSLSocket soc;
    private GetMethod ginfo = new GetMethod();
    private PostMethod pinfo = new PostMethod();
    private HttpClient client = new HttpClient();
    private boolean closedByFDS = false;

    private final String CR = new String("" + ((char) 13));
    private final String LF = new String("" + ((char) 10));
    private final String EOL = CR+LF;
    private final String EOT = EOL + EOL;
    private final String EndChunk = "0" + EOL;
    private final String FDSID = "FDSID - "; // FDS OU Identifire
    static final int MAX_HEADER_SIZE = 16384;
    static final Pattern COMMAND = Pattern.compile("^(\\w+)\\s+(.+?)\\s+HTTP/([\\d.]+)$");
    static final Pattern FQDN = Pattern.compile("http://pilot\\-fds\\-fe01:8080");
    static final String rFQDN = "https://pilot-fds-service01.verisign.co.jp";


    public Connect(SSLSocket soc) throws Exception {
        this.soc = soc;
        this.soc.setKeepAlive(true);
        this.soc.setReuseAddress(true);
        this.soc.setReceiveBufferSize(32768);
        // Thread Start
        //this.start();
    }


    @Override
    public void run() {
        InputStream sin = null;
        OutputStream sout = null;
        String cinfo = null;
        String targetHost = null;
        int targetPort = 0;
        //HttpClient client = null;

        byte[] ibuf = null;

        try {
            sout = soc.getOutputStream();
            sin = soc.getInputStream();
        } catch (IOException ioe) {
            myLog.eventLog(1, ioe.getMessage());
            try {
                sout.close();
                sin.close();
                soc.close();
            } catch (Exception soex) {
                myLog.eventLog(5, myLog.getStackTrace(soex));
            }
            return;
        }

        /*
         * Client Auth Stage
         */

        // Read Command Line
        String htmlCommand = null;
        String htmlResponse = null;
        String[] request;
        readHttpLine rhttp = new readHttpLine();
        RouteInfo ri=null;
        try {
            //htmlCommand = in.readLine();
            htmlCommand = rhttp.readHttpLine(sin);
            if (htmlCommand == null) {
                return;
            }
        } catch (Exception ex) {
            if (SSLRouter.loglevel == 8 && !(soc.getInetAddress().getHostAddress().equals(SSLRouter.noLogIP))) {
                System.out.println(soc.getInetAddress().getHostAddress());
                myLog.eventLog(3, "SSL Initial Connection Error : " + 
                                   soc.getInetAddress().getHostAddress() + " : " +
                                   ex.getLocalizedMessage());
            }
            try {
                //if (sin != null) {
                    sin.close();
                //}
                //if (sout != null) {
                    sout.close();
                //}
            } catch (Exception e) {
                myLog.eventLog(1, e.getMessage());
            } finally {
                try {
                    soc.close();
                    if (SSLRouter.loglevel == 8 && !(soc.getInetAddress().getHostAddress().equals(SSLRouter.noLogIP))) {
                        myLog.eventLog(1, "SSL Socket closed");
                    }
                } catch (Exception e) {
                }
                return;
            }
        }

        request = htmlCommand.split(" ");
        htmlCommand += EOL;
        long stm = new Date().getTime();

        // Parse client certificate when initial connection
        if (targetPort == 0) {
            Certificate[] c = null;
            // get client certificate information
            try {
                c = ((SSLSocket) soc).getSession().getPeerCertificates();
            } catch (Exception PKIEX) {
                myLog.eventLog(5, myLog.getStackTrace(PKIEX));
                try {
                    sin.close();
                    sout.close();
                    soc.close();
                } catch (Exception soex) {
                    myLog.eventLog(5, myLog.getStackTrace(soex));
                    return;
                }
            }
            X509Certificate x509 = (X509Certificate) c[0];
            List oulist = X509Util.getOUs(x509.getSubjectX500Principal());
            if (oulist.isEmpty()) {
                try {
                    sin.close();
                    sout.close();
                    soc.close();
                } catch (Exception soex) {
                    myLog.eventLog(5, myLog.getStackTrace(soex));
                    return;
                }
            }

            for (Object ou : oulist) {
                String sou = ou.toString();
                myLog.eventLog(1, "OU list : " + sou);
                if (sou.indexOf(FDSID) == 0) {
                    cinfo = sou.substring(FDSID.length());
                    myLog.eventLog(1, "Client info : " + cinfo);
                } else {
                    continue;
                }
                if (cinfo == null) {
                    try {
                        sout.close();
                        soc.close();
                        sin.close();
                    } catch (Exception e) {
                        myLog.eventLog(1, e.getMessage());
                    } finally {
                        return;
                    }
                }
            }

            if (SSLRouter.mode) {
                String tmpHost = null;
                int tmpPort = 0;
                try {
                    ri = SSLRouter.riq.ByClientCertInfoAndRouteFromRouter(cinfo, SSLRouter.myName);
                    tmpHost = ri.getRoute_to_host();
                    tmpPort = ri.getRoute_to_port();
                } catch (Exception e) {
                    myLog.eventLog(4, "DB Query Error");
                    myLog.eventLog(5, myLog.getStackTrace(e));
                    try {
                        sin.close();
                        sout.close();
                        soc.close();
                    } catch (Exception ex) {
                        myLog.eventLog(4, ex.getMessage());
                    } finally {
                        return;
                    }
                }

                Socket socket2FDS = null;

                try {
                    //System.out.println(tmpHost + ":" + tmpPort);
                    socket2FDS = new Socket(tmpHost, tmpPort);
                    Thread t1 = new Socket2Socket(soc, socket2FDS, htmlCommand);
                    Thread t2 = new Socket2Socket(socket2FDS, soc, null);
                    SSLRouter.loglevel=0;
                    t2.join();
/*
                    while (true) {
                        if (t1.isAlive() == false || t2.isAlive() == false) {
                            socket2FDS.close();
                            soc.close();
                            return;
                        }
                        Thread.sleep(100);
                    }
*/
                } catch (Exception ex) {
                    return;
                } finally {
                    try {
                        soc.close();
                        socket2FDS.close();

                    } catch (Exception ex) {
                    }
                }
            }



            /**** Process Main Loop ****/
            try {
                while (true) {
                    String tmpHost=null;
                    int tmpPort=0;
                    ri = SSLRouter.riq.ByClientCertInfoAndRouteFromRouter(cinfo, SSLRouter.myName);
                    try{
                        tmpHost = ri.getRoute_to_host();
                        tmpPort = ri.getRoute_to_port();
                    }catch(Exception e){
                        myLog.eventLog(4, "DB Query Error");
                        myLog.eventLog(5, myLog.getStackTrace(e));
                        tmpHost=null;
                    }

                    if (tmpHost == null) {
                        try {
                            sin.close();
                            sout.close();
                            soc.close();
                        } catch (Exception e) {
                            myLog.eventLog(4, e.getMessage());
                        } finally {
                            return;
                        }
                    }

                    if(!tmpHost.equalsIgnoreCase(targetHost) || tmpPort !=targetPort) {
                        client = null;
                        client = new HttpClient();
                        targetHost = tmpHost;
                        targetPort = tmpPort;
                        client.getHostConfiguration().setHost(targetHost, targetPort, "http");
                    }

                    SSLRouter.loglevel = ri.getLog_level();
                    myLog.eventLog(1, "targetHost : " + targetHost);
                    myLog.eventLog(1, "targetPort : " + String.valueOf(targetPort));
                    myLog.eventLog(1, "loglvl     : " + String.valueOf(SSLRouter.loglevel));


                    /*
                     * ============= doMethod ==============
                     */
                    String[] ret = null;
                    myLog.eventLog(5, htmlCommand);
                    if (request[0].equalsIgnoreCase("POST")) {
                        ret = doPostMethod(request, sin, sout, client);
                    } else if (request[0].equalsIgnoreCase("GET")) {
                        ret = doGetMethod( request, sin, sout, client);
                    } else {
                        ret[0] = "Unsupported Command Recieved\n";
                        ret[1] = request[2] + " 501 Not Implemented" + EOL;
                        sout.write(ret[1].getBytes()); // Shoule be quiet?
                    }
                    if (ret != null) {
                        htmlCommand += ret[0];
                        htmlResponse = ret[1];
                        if (SSLRouter.loglevel >= 7) {
                            System.out.println("================================================");
                            System.out.println(htmlCommand);
                            System.out.println("------------------------------------------------");
                            System.out.println(htmlResponse);
                            System.out.println("================================================");
                        }
                        long etm = new Date().getTime();
                        myLog.writeLog(stm, etm - stm, cinfo, 0, targetHost, targetPort, htmlCommand, htmlResponse);
                    } else {
                        myLog.eventLog(3, "trouble on FDS Connection");
                        return;
                    }

                    if(closedByFDS==true){
                        targetHost = null;
                        return;
                    }

                    // Transaction end - new transaction
                    //do {

                        //htmlCommand = in.readLine();
                        htmlCommand = rhttp.readHttpLine(sin);
                        if(htmlCommand==null) { return;
                            //System.out.println("Looping"); Thread.sleep(100);
                            //in.close();
                            //in= new BufferedReader( new InputStreamReader(sin,"ISO-8859-1"));
                        }
                    //} while(htmlCommand==null); // Garbedge Counter measure
                    request = htmlCommand.split(" ");
                    htmlCommand += EOL;
                    stm = new Date().getTime();
                }
                /*
                 * Mail loop end
                 */
                /*
                 *          Process Exception
                 */
            } catch (SSLException ex) {
                // SSL Connection Fail
                myLog.eventLog(3, ex.getLocalizedMessage());
                myLog.eventLog(5, myLog.getStackTrace(ex));
                try {
                    if (!soc.isClosed()) {
                        if (sin != null) {
                            sin.close();
                        }
                        if (sout != null) {
                            sout.close();
                        }
                        soc.close();
                    }
                } catch (Exception exx) {
                    myLog.eventLog(5, myLog.getStackTrace(exx));
                }
                return;
                /**/
            } catch (UnsupportedEncodingException ex) {
                myLog.eventLog(3, ex.getLocalizedMessage());
                myLog.eventLog(5, myLog.getStackTrace(ex));
                /**/
            } catch (SocketException ex) {
                long tm = new Date().getTime();
                myLog.eventLog(1, "SSL Client Connection Closed");
                try {
                    if (soc.isClosed()) {
                        if (sin != null) {
                            sin.close();
                        }
                        if (sout != null) {
                            sout.close();
                        }
                        return;
                    }
                } catch (Exception exx) {
                    myLog.eventLog(5, myLog.getStackTrace(exx));
                }
                return;
                /**/
            } catch (IOException ex) {
                myLog.eventLog(3, ex.getLocalizedMessage());
                myLog.eventLog(5, myLog.getStackTrace(ex));
                try {
                    if (soc.isClosed()) {
                        if (sin != null) {
                            sin.close();
                        }
                    }
                } catch (Exception exx) {
                    myLog.eventLog(5, myLog.getStackTrace(exx));
                }
                /**/
            } catch (BadRequestException ex) {
                if (SSLRouter.loglevel == 8) {
                    try {
                        System.out.println("--InputStream Closed--");
                        sin.close();
                        return;
                    } catch (Exception exx) {
                        exx.printStackTrace();
                    }
                    myLog.eventLog(3, ex.getMessage());
                    myLog.eventLog(5, myLog.getStackTrace(ex));
                }
            } catch (Exception ex) {
                //myLog.eventLog(3, e6.getMessage());
                myLog.eventLog(5, myLog.getStackTrace(ex));
            } finally {
                try{
                    if(!soc.isClosed()) {
                        soc.close();
                    }
                    return;
                } catch(Exception ex) {
                    myLog.eventLog(5, myLog.getStackTrace(ex));
                }
            }
        }
    }

    private String[] doGetMethod(
            String[] request,
            //BufferedReader in,
            InputStream in,
            OutputStream sout, HttpClient client) throws Exception {
        String[] retval = new String[2];
        String htmlCommand = "";
        String htmlResponse = "";
        Boolean useChunk = false;
        readHttpLine rhttp = new readHttpLine();
        //GetMethod ginfo = new GetMethod();

        ginfo.setPath(request[1]);

        // Get HTML Header
        String msg = "", s = "";
        do {
            //s = in.readLine();
            s = rhttp.readHttpLine(in);
            msg += s + EOL;
            if (s != null && !(s.equals(""))) {
                String[] h = s.split(": ");
                ginfo.setRequestHeader(h[0], h[1]);
            }
        } while (s != null && !(s.equals(""))); //(in.ready() && s != null);
        htmlCommand += msg;
        if(request[2].equalsIgnoreCase("HTTP/1.0")) {
            client.getParams().setParameter("http.protocol.version", HttpVersion.HTTP_1_0);
        }
        

        int status = client.executeMethod(ginfo);
        if (status == -1) {
            ginfo.releaseConnection();
            myLog.eventLog(3, htmlCommand);
            return null;
        }

        s = ginfo.getStatusLine().toString();

        sout.write(s.getBytes()); sout.write(EOL.getBytes());
        myLog.eventLog(5, htmlCommand);
        myLog.eventLog(5, s);
        htmlResponse = s + EOL;
        Header[] head = ginfo.getResponseHeaders();

        for (Header h : head) {
            String tmph = h.toString();
            sout.write(tmph.getBytes());
            htmlResponse += tmph;
            myLog.eventLog(5, tmph);

            if (tmph.equalsIgnoreCase("Transfer-encoding: chunked" + EOL)) {
                useChunk = true;
            } else if (tmph.equalsIgnoreCase("Connection: close" + EOL)) {
                closedByFDS = true;
                myLog.eventLog(5, "Connection Closed by FDS");
                //tmph = "Connection: Keep-Alive"+EOL;
            }
        }
        sout.write(EOL.getBytes());
        htmlResponse += EOL;
/*
        if (status != 200) {
        sout.write(EOL.getBytes()); sout.flush();
            retval[0] = htmlCommand;
            retval[1] = htmlResponse;
            if (closedByFDS == true) {
                ginfo.releaseConnection();
            }
            return retval;
        }
*/
        //long reslen = ginfo.getResponseContentLength();
        InputStream resin = ginfo.getResponseBodyAsStream();
        byte[] chunkData = readChunk(resin);
        if (useChunk) {
/*
            String tmp = new String(chunkData,"UTF-8");
            Matcher m = FQDN.matcher(tmp);
            String ss = m.replaceAll(rFQDN);
            chunkData = ss.getBytes("UTF-8");
*/
            String chunkLen = Integer.toHexString(chunkData.length);
            sout.write(chunkLen.getBytes());  sout.write(EOL.getBytes());
            sout.write(chunkData);            sout.write(EOL.getBytes());
            sout.write(EndChunk.getBytes());
            sout.write(EOT.getBytes());
            resin.close();
        } else {
            sout.write(chunkData);
            sout.write(EOT.getBytes());
        }
        htmlResponse += new String(chunkData);
        retval[0] = htmlCommand;
        retval[1] = htmlResponse;
        if (closedByFDS == true) {
            ginfo.releaseConnection();
        }
        sout.flush();
        return retval;
    }

    private String[] doPostMethod(
            String[] request,
            //BufferedReader in,
            InputStream in,
            OutputStream sout, HttpClient client) throws Exception {
        String[] retval = new String[2];
        String htmlCommand = "";
        String htmlResponse = "";
        readHttpLine rhttp = new readHttpLine();
        //PostMethod pinfo = new PostMethod();
        boolean isSOAP = false;
        pinfo.setPath(request[1]);
        //PostMethod info = new PostMethod(request[1]);
        // Get HTML Header
        String msg = "", s="";
        int plen = 0;
        do {
            //s = in.readLine();
            s = rhttp.readHttpLine(in);
            msg += s + EOL;
            if (s != null && !(s.equals(""))) {
                String[] h = s.split(": ");
                if (h[0].equalsIgnoreCase("SOAPAction")) {
                    isSOAP = true;
                } else if (h[0].equalsIgnoreCase("Content-Length")) {
                    plen = Integer.parseInt(h[1]);
                } //else  if(h[0].equalsIgnoreCase("HOST")) {
                    //h[1]="t-yama-cent01.verisign.co.jp";
                //}
                pinfo.setRequestHeader(h[0], h[1]);
               
            } else {
                break;
            }
        } while (s != null); // && !s.equals("") && in.ready());
        htmlCommand += msg;

        /*
         * Read Post Data
         */
        if (plen != 0) {
            msg = "";
            byte[] cbuf = new byte[plen];
            int r = in.read(cbuf, 0, plen);
            msg = new String(cbuf);
            if (isSOAP) {
                pinfo.setRequestEntity(new ByteArrayRequestEntity(msg.getBytes()));
            } else {
                String[] qst = msg.split("&");
                for (String ss : qst) {
                    String[] nvp = ss.split("=");
                    if (nvp.length == 2) {
                        nvp[0] = URLDecoder.decode(nvp[0], "UTF-8");
                        nvp[1] = URLDecoder.decode(nvp[1], "UTF-8");
                        pinfo.setParameter(nvp[0], nvp[1]);
                    }
                }
            }
            htmlCommand += msg;
        }

        if(request[2].equalsIgnoreCase("HTTP/1.0")) {
            client.getParams().setParameter("http.protocol.version", HttpVersion.HTTP_1_0);
        }

        // Submit POST command
        int status = client.executeMethod(pinfo);
        if (status == -1) {
            pinfo.releaseConnection();
            pinfo = null;
            myLog.eventLog(3, htmlCommand);
            return null;
        }
        boolean useChunk = false;
        String x = pinfo.getStatusLine().toString();
        sout.write(x.getBytes()); sout.write(EOL.getBytes());
        myLog.eventLog(5, htmlCommand);
        myLog.eventLog(5, x);
        htmlResponse = x + EOL;
        Header[] head = pinfo.getResponseHeaders();
        for (Header h : head) {
            String tmph = h.toString();
            myLog.eventLog(5, tmph);

            if (tmph.equalsIgnoreCase("Transfer-encoding: chunked" + EOL)) {
                useChunk = true;
            } else
            if (tmph.equalsIgnoreCase("Connection: close" + EOL)) {
                closedByFDS = true;
                myLog.eventLog(5, "Connection Closed by FDS");
                //tmph = "Connection: Keep-Alive"+EOL;
            }
            sout.write(tmph.getBytes());
            htmlResponse += tmph;
        }
        sout.write(EOL.getBytes());
        htmlResponse += EOL;
/*
        if (status != 200) {
            sout.write(EOL.getBytes()); sout.flush();
            retval[0] = htmlCommand;
            retval[1] = htmlResponse;
            if (closedByFDS == true) {
                pinfo.releaseConnection();
            }
            return retval;
        }
*/
        //long reslen = ginfo.getResponseContentLength();
        InputStream resin = pinfo.getResponseBodyAsStream();
        byte[] chunkData = readChunk(resin);
        if (useChunk) {
            String chunkLen = Integer.toHexString(chunkData.length);
            sout.write(chunkLen.getBytes());  sout.write(EOL.getBytes());
            sout.write(chunkData);            sout.write(EOL.getBytes());
            sout.write(EndChunk.getBytes());
            sout.write(EOT.getBytes());
            resin.close();
        } else {
            sout.write(chunkData);
            sout.write(EOT.getBytes());
        }
        htmlResponse += new String(chunkData);
        retval[0] = htmlCommand;
        retval[1] = htmlResponse;
        if (closedByFDS == true) {
            pinfo.releaseConnection();
        }
        sout.flush();
        return retval;
    }

    private String S_readChunk(InputStream sin) throws IOException {
        StringBuilder sb = new StringBuilder(524288);
        int c = 0;
        while ((c = sin.read()) != -1) {
            sb.append((char) c);
        }
        return sb.toString();
    }

    private byte[] readChunk(InputStream sin) throws IOException {
        final int BUF_SIZE = 65536;
        final int INC_BUF_SIZE = 16384;
        int i;
        byte[] buff = new byte[BUF_SIZE];
        for (i = 0;; i++) {
            int c = sin.read();
            if (c < 0) {
                break;
            }
            buff[i] = (byte) c;
            if (i == buff.length - 1) {
                byte[] nbuff = new byte[buff.length + INC_BUF_SIZE];
                System.arraycopy(buff, 0, nbuff, 0, i + 1);
                buff = nbuff;
            }
        }
        byte[] tmpbuff = new byte[i];
        System.arraycopy(buff, 0, tmpbuff, 0, i);
        if(SSLRouter.loglevel>=7) {
            System.out.println("Data Length:" + tmpbuff.length);
        }
        return tmpbuff;
    }

    /**
     * Exception for Request reader
     */
    class BadRequestException extends RuntimeException {
        BadRequestException(String msg, String resp, int initCode) {
            super(msg);
            responseMessage = resp;
            statusCode = initCode;
        }
        BadRequestException(String msg, int initCode) {
            super(msg);
            responseMessage = msg;
            statusCode = initCode;
        }
        String responseMessage;
        int statusCode;
    }

    /**
     * Client Request Info reader
     */
    class Request {
        String method;
        String version;
        String path;
        String[] headerData;

        Request(Socket sock) throws IOException {
            InputStream in = sock.getInputStream();
            header(in);
            if (SSLRouter.loglevel >=7) {
                for (int i = 0; i < headerData.length; i++) {
                    System.out.println(headerData[i]);
                }
            }
        }

        Request(InputStream in) throws IOException {
            header(in);
            if (SSLRouter.loglevel >=7) {
                for (int i = 0; i < headerData.length; i++) {
                    System.out.println(headerData[i]);
                }
            }
        }

        void header(InputStream in) throws IOException {
            byte[] buff = new byte[2000];
            for (int i = 0; ; i++) {
                int c = in.read();
                if (c < 0) {
                    throw new BadRequestException("header too short:" +
                                              new String(buff, 0, i),
                                                      "header too short",
                                              HttpURLConnection.HTTP_BAD_REQUEST);
                }
                buff[i] = (byte)c;
                if (i > 3
                    && buff[i - 3] == '\r' && buff[i - 2] == '\n'
                    && buff[i - 1] == '\r' && buff[i] == '\n') {
                    createHeader(buff, i - 4);
                    break;
                } else if (i == buff.length - 1) {
                    if (i > MAX_HEADER_SIZE) {
                        throw new BadRequestException("header too long:" +
                                              new String(buff, 0, 256),
                                                      "header too long",
                                              HttpURLConnection.HTTP_BAD_REQUEST);
                    }
                    byte[] nbuff = new byte[buff.length * 2];
                    System.arraycopy(buff, 0, nbuff, 0, i + 1);
                    buff = nbuff;
                }
            }
        }
        void createHeader(byte[] buff, int len) {
            for (int i = 0; i < len; i++) {
                if (i > 2 && buff[i - 1] == '\r' && buff[i] == '\n') {
                    Matcher m = COMMAND.matcher(new String(buff, 0, i - 1));
                    if (m.matches()) {
                        method = m.group(1);
                        path = m.group(2);
                        version = m.group(3);
                    } else {
                        throw new BadRequestException(new String(buff, 0, i + 1),
                                                      "header too long",
                                              HttpURLConnection.HTTP_BAD_REQUEST);
                    }
                    headerData = new String(buff, i + 1, len - i).split("\\r\\n");
                    break;
                }
            }
        }
        @Override
        public String toString() {
            StringBuffer sb = new StringBuffer(super.toString()).append(LF);
            sb.append(method);
            sb.append(' ').append(path).append(" HTTP/").append(version);
            return sb.toString();
        }
    }

    class readHttpLine {
        final int  BUF_SIZE = 4096;
        private String lineData;
        public String readHttpLine(InputStream in) throws IOException {

            byte[] buff = new byte[BUF_SIZE];
            for (int i = 0; ; i++) {
                int c = in.read();
                if (c < 0) {
                    in.close();
                    throw new BadRequestException("unexpected EOF:" +
                                              new String(buff, 0, i),
                                                      "unexpected EOF",
                                              HttpURLConnection.HTTP_BAD_REQUEST);
                }
                buff[i] = (byte)c;
                if (i >= 1
                    && buff[i - 1] == '\r' && buff[i] == '\n') {
                    lineData = new String(buff, 0, i-1,"UTF-8");
                    break;
                } else if (i == buff.length - 1) {
                    byte[] nbuff = new byte[buff.length + BUF_SIZE];
                    System.arraycopy(buff, 0, nbuff, 0, i + 1);
                    buff = nbuff;
                }
            }
          return lineData;
        }
    }
}

