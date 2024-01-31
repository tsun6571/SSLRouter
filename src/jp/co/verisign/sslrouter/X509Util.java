/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package jp.co.verisign.sslrouter;

import java.io.*;
import java.util.*;
import javax.security.auth.x500.*;
import java.util.List;
import org.bouncycastle.asn1.*;
import org.bouncycastle.x509.X509Store.*;

/**
 *
 * @author tsunoda
 */
public class X509Util {
         /** Common Name (CN) OID. */
    public static final String CN_OID = "2.5.4.3";

    /** Organizatin (O) OID. */
    public static final String O_OID = "2.5.4.10";

    /** Organizatin Unit (OU) OID. */
    public static final String OU_OID = "2.5.4.11";

    /** Title (Title) OID. */
    public static final String TITLE_OID = "2.5.4.12";

    /** get OIDs from DN
     * @param X500Principal DN, String OID
     * @return String List
     */
    public static List<String> getOIDs(X500Principal dn, String oid) {
       if (dn == null) {
            return null;
        }

        myLog.eventLog(5, "Extracting OIDs " + oid + " from the following DN:" + dn.toString());
        List<String> OIDs = new LinkedList<String>();
        try {
            ASN1InputStream asn1Stream = new ASN1InputStream(dn.getEncoded());
            DERObject parent = asn1Stream.readObject();

            String cn = null;
            DERObject dnComponent;
            DERSequence grandChild;
            DERObjectIdentifier componentId;
            for (int i = 0; i < ((DERSequence) parent).size(); i++) {
                dnComponent = ((DERSequence) parent).getObjectAt(i).getDERObject();
                if (!(dnComponent instanceof DERSet)) {
                    myLog.eventLog(5,"No DN components.");
                    continue;
                }

                // Each DN component is a set
                for (int j = 0; j < ((DERSet) dnComponent).size(); j++) {
                    grandChild = (DERSequence) ((DERSet) dnComponent).getObjectAt(j).getDERObject();

                    if (grandChild.getObjectAt(0) != null
                            && grandChild.getObjectAt(0).getDERObject() instanceof DERObjectIdentifier) {
                        componentId = (DERObjectIdentifier) grandChild.getObjectAt(0).getDERObject();

                        if (oid.equals(componentId.getId())) {
                            // OK, this dn component is actually a cn attribute
                            if (grandChild.getObjectAt(1) != null
                                    && grandChild.getObjectAt(1).getDERObject() instanceof DERString) {
                                cn = ((DERString) grandChild.getObjectAt(1).getDERObject()).getString();
                                OIDs.add(cn);
                            }
                        }
                    }
                }
            }

            asn1Stream.close();

            return OIDs;

        } catch (IOException e) {
            myLog.eventLog(3,"Unable to extract common names from DN: ASN.1 parsing failed: " + e);
            return null;
        }
    }
    public static List<String> getTitles(X500Principal dn) {
            if (dn == null) {
                return null;
            }
            return getOIDs(dn, TITLE_OID);
        }
    public static List<String> getCommonNames(X500Principal dn) {
            if (dn == null) {
                return null;
            }
            return getOIDs(dn, CN_OID);
        }
    public static List<String> getOUs(X500Principal dn) {
            if (dn == null) {
                return null;
            }
            return getOIDs(dn, OU_OID);
        }

}
