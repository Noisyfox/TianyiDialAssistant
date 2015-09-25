package org.foxteam.noisyfox.tianyidialassistant;

import android.annotation.SuppressLint;
import android.util.Log;
import org.apache.http.HttpVersion;
import org.apache.http.client.HttpClient;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.HTTP;
import org.json.JSONObject;
import org.xbill.DNS.Lookup;
import org.xbill.DNS.Record;
import org.xbill.DNS.TextParseException;
import org.xbill.DNS.Type;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.*;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

public class NetworkHelper {
    private static final String[] trustedDNS = {"8.8.8.8", "8.8.4.4"};
    private static final Random ramdom = new Random();

    public static class PostResult {
        int statusCode = 0;
        JSONObject resultObj = null;
    }

    public static class StaticHost {
        public StaticHost(String scheme, String host, int port,
                          String resolveAddress) {
            this.scheme = scheme;
            this.host = host;
            this.port = port;
            this.resolveAddress = resolveAddress;
        }

        String scheme = "http";
        String host = "";
        int port = 80;
        String resolveAddress = "";
    }

    private static long lastResolveTime = -1;

    @SuppressLint("DefaultLocale")
    public static boolean resolveHost(StaticHost host) {
        // hack Google 403
        if (host.resolveAddress.toLowerCase().contains("google")) {
            try {
                InetAddress addr = InetAddress.getByName(host.resolveAddress);
                long cTime = System.currentTimeMillis();
                if (addr.getHostAddress().startsWith("203.208")// google_CN
                        || (lastResolveTime != -1 && cTime - lastResolveTime > 5 * 60 * 1000)) {// 防止ip失效
                    lastResolveTime = cTime;
                    Set<String> ips = getAllIP(host.resolveAddress, trustedDNS);
                    int size = ips.size();
                    if (size > 0) {
                        String[] ip = new String[size];
                        ips.toArray(ip);
                        int k = ramdom.nextInt(size);
                        addDNSCache(host.resolveAddress, ip[k]);// google_HK
                    }
                }
            } catch (UnknownHostException e) {
                e.printStackTrace();
            }
        }

        // hack dns
        if (!addDNSCache(host.host, host.resolveAddress)) {
            Log.d("DNS", "Hack DNS failed!");
            return false;
        }

        try {
            InetAddress addr = InetAddress.getByName(host.host);
            Log.d("DNS", addr.toString());
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }

        return true;
    }

    /**
     * 手动添加DNS缓存，以绕过国内的DNS污染
     *
     * @param host
     * @param ip
     * @return 是否成功
     */
    public static boolean addDNSCache(String host, String ip) {
        try {
            InetAddress addr2[] = {InetAddress.getByName(ip)};

            Class<?> clazz = java.net.InetAddress.class;
            final Field cacheField = clazz.getDeclaredField("addressCache");
            cacheField.setAccessible(true);
            final Object o = cacheField.get(clazz);
            synchronized (o) {
                Class<?> clazz2 = o.getClass();
                Method method = clazz2.getMethod("put", String.class,
                        InetAddress[].class);
                method.setAccessible(true);
                method.invoke(o, host, addr2);
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public static HttpClient getNewHttpClient() {
        try {
            KeyStore trustStore = KeyStore.getInstance(KeyStore
                    .getDefaultType());
            trustStore.load(null, null);
            SSLSocketFactory sf = new SSLSocketFactoryEx(trustStore);
            sf.setHostnameVerifier(SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);

            HttpParams params = new BasicHttpParams();
            HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
            HttpProtocolParams.setContentCharset(params, HTTP.UTF_8);

            SchemeRegistry registry = new SchemeRegistry();
            registry.register(new Scheme("http", PlainSocketFactory
                    .getSocketFactory(), 80));
            registry.register(new Scheme("https", sf, 443));

            ClientConnectionManager ccm = new ThreadSafeClientConnManager(
                    params, registry);
            return new DefaultHttpClient(ccm, params);
        } catch (Exception e) {
            return new DefaultHttpClient();
        }
    }

    private static class SSLSocketFactoryEx extends SSLSocketFactory {
        SSLContext sslContext = SSLContext.getInstance("TLS");

        public SSLSocketFactoryEx(KeyStore truststore)
                throws NoSuchAlgorithmException, KeyManagementException,
                KeyStoreException, UnrecoverableKeyException {

            super(truststore);
            TrustManager tm = new X509TrustManager() {
                public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                    return null;
                }

                @Override
                public void checkClientTrusted(
                        java.security.cert.X509Certificate[] chain,
                        String authType)
                        throws java.security.cert.CertificateException {
                }

                @Override
                public void checkServerTrusted(
                        java.security.cert.X509Certificate[] chain,
                        String authType)
                        throws java.security.cert.CertificateException {
                }

            };
            sslContext.init(null, new TrustManager[]{tm}, null);

        }

        @Override
        public Socket createSocket(Socket socket, String host, int port,
                                   boolean autoClose) throws IOException, UnknownHostException {
            return sslContext.getSocketFactory().createSocket(socket, host,
                    port, autoClose);
        }

        @Override
        public Socket createSocket() throws IOException {
            return sslContext.getSocketFactory().createSocket();
        }

    }

    /**
     * 获取域名所有IP
     *
     * @param domain     域名
     * @param dnsServers DNS服务器列表
     * @return
     */
    public static Set<String> getAllIP(String domain, String[] dnsServers) {
        Set<String> ips = new HashSet<String>();

        try {
            Lookup.setDefaultSearchPath(dnsServers);
            Lookup lookup = new Lookup(domain, Type.A);
            lookup.run();
            if (lookup.getResult() != Lookup.SUCCESSFUL) {
                System.out.println("ERROR: " + lookup.getErrorString());
            } else {
                Record[] answers = lookup.getAnswers();
                for (Record rec : answers) {
                    ips.add(rec.rdataToString());
                    Log.d("DNS", rec.rdataToString());
                }
            }

        } catch (TextParseException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return ips;
    }

}
