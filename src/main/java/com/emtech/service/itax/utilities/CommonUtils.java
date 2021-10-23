
package com.emtech.service.itax.utilities;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.XML;

/**
 *
 * @author omukubwa emukule
 */
public class CommonUtils {
    //Convert XML to JSON
    public JSONObject xmlToJSON(String xml) throws JSONException {
        JSONObject json = XML.toJSONObject(xml);
        String jsonString = json.toString(4);
        System.out.println(jsonString);
        return json;
    }

    //Create a Hash Code for Posting the Payment
    public String createHashCode(String eslipnumber, String paymentadvicedate, String taxpayerpin, String totalamount, String banktellerid, String paymentreference, String bankcode, String loginid) {
        String signature = (eslipnumber + paymentadvicedate + taxpayerpin + totalamount + banktellerid + paymentreference) + (bankcode + loginid);
        return signature;
    }
    
    //Disable verification
    public void disableVerification() {
        try {
            // Create a trust manager that does not validate certificate chains
            TrustManager[] trustAllCerts = new TrustManager[]{new X509TrustManager() {
                public X509Certificate[] getAcceptedIssuers() {
                    return null;
                }

                public void checkClientTrusted(X509Certificate[] certs, String authType) {
                }

                public void checkServerTrusted(X509Certificate[] certs, String authType) {
                }
            }
            };

            // Install the all-trusting trust manager
            SSLContext sc = SSLContext.getInstance("SSL");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());

            // Create all-trusting host name verifier
            HostnameVerifier allHostsValid = new HostnameVerifier() {
                public boolean verify(String hostname, SSLSession session) {
                    return true;
                }
            };

            // Install the all-trusting host verifier
            HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (KeyManagementException e) {
            e.printStackTrace();
        }
    }

    
}
