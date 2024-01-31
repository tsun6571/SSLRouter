/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package jp.co.verisign.sslrouter;

import java.security.cert.X509Certificate;

import javax.net.ssl.*;


/**
 *
 * @author tsunoda
 * Created 2010/06/18.15:06:01
 */

class MyListener implements HandshakeCompletedListener
{
  private boolean handshake;

  /**
   * Constructs a SimpleHandshakeListener with the given
   * identifier.
   * @param ident Used to identify output from this Listener.
   */
  public MyListener()
  {
    handshake = false;
  }

  public boolean isHandShaked()
  {
      return handshake;
  }

  /** Invoked upon SSL handshake completion. */
    @Override
  public void handshakeCompleted(HandshakeCompletedEvent event)
  {
    // Display the peer specified in the certificate.
        handshake = true;
    }
}