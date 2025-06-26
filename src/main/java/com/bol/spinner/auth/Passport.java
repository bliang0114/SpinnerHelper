package com.bol.spinner.auth;

import org.json.JSONArray;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.net.ssl.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.net.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.*;

public class Passport {
    static final String kernelServlet = "/servlet/MatrixXMLServlet";

    public Passport() {
    }

    public static void setTrustManager(boolean useCertificates) throws Exception {
        SSLContext sc = SSLContext.getInstance("TLS");
        if (useCertificates) {
            sc.init((KeyManager[])null, (TrustManager[])null, (SecureRandom)null);
        } else {
            sc.init((KeyManager[])null, new TrustManager[]{new TrustAllTrustManager()}, (SecureRandom)null);
        }

        HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
    }

    public static String getTicket(String host, String user, String password) throws Exception {
        String newUri = Util.resolverUrl(host, "/servlet/MatrixXMLServlet?NotSSOLogin=true");
        HttpURLConnection conTemp = getHttpConnection(newUri, false);
        int respTemp = conTemp.getResponseCode();
        if (respTemp != 302) {
            if (respTemp == 400) {
                throw new Exception("Error 400: Bad request");
            } else if (respTemp == 404) {
                throw new Exception("Error 404: Not found");
            } else {
                throw new Exception("Required CAS redirect not found");
            }
        } else {
            String redirectUrl = conTemp.getHeaderField("Location");
            Util.trace("redirectUrl: " + redirectUrl);
            HttpURLConnection conCAS = getHttpConnection(redirectUrl, false);
            Map<String, String> cookiesCAS = getCASCookies(conCAS);
            StringBuilder cookies = new StringBuilder();
            String jSessionId = (String)cookiesCAS.get("JSESSIONID");
            if (jSessionId != null) {
                cookies.append("JSESSIONID=").append(jSessionId).append(";");
            }

            String serverId = (String)cookiesCAS.get("SERVERID");
            if (serverId != null) {
                cookies.append("SERVERID=").append(serverId).append(";");
            }

            Util.trace("COOKIES: " + cookies.toString());
            String tenant = null;
            if (serverId != null && host.endsWith("enovia") && host.contains("-")) {
                Util.trace("determine cloud tenant");
                tenant = host.substring(host.indexOf("//") + 2, host.indexOf("-")).toUpperCase();
                Util.trace("tenant id: " + tenant);
            }

            String authParamsCAS = getAuthParams(conCAS);
            Util.trace("authParamsCAS: " + authParamsCAS);
            JSONObject jsonCAS = new JSONObject(authParamsCAS);
            String lt = (String)jsonCAS.get("lt");
            Util.trace("lt: " + lt);
            String loginUrlCAS = (String)jsonCAS.get("url");
            Util.trace("loginUrlCAS: " + loginUrlCAS);
            HttpURLConnection conCASLogin = getHttpConnection(loginUrlCAS+"?NotSSOLogin=true", false);
            conCASLogin.setRequestProperty("Cookie", cookies.toString());
            conCASLogin.setRequestMethod("POST");
            conCASLogin.setDoOutput(true);
            Properties casUrlParamProperties = new Properties();
            casUrlParamProperties.put("lt", lt);
            casUrlParamProperties.put("username", user);
            casUrlParamProperties.put("password", password);
            String casUrlParams = encodeUrlParams(casUrlParamProperties);
            DataOutputStream wr = new DataOutputStream(conCASLogin.getOutputStream());

            try {
                wr.writeBytes(casUrlParams);
                wr.flush();
            } catch (Throwable var28) {
                try {
                    wr.close();
                } catch (Throwable var27) {
                    var28.addSuppressed(var27);
                }

                throw var28;
            }

            wr.close();
            int respCodeCASLogin = conCASLogin.getResponseCode();
            Util.trace("respCodeCASLogin: " + respCodeCASLogin);
            String redirectUrlFromCASLogin;
            if (respCodeCASLogin == 302) {
                redirectUrlFromCASLogin = conCASLogin.getHeaderField("Location");
                Util.trace("redirectUrlFromCASLogin: " + redirectUrlFromCASLogin);
                if (redirectUrlFromCASLogin.contains("ticket=")) {
                    String ticket = Util.getUrlParam(redirectUrlFromCASLogin, "ticket");
                    if (tenant != null) {
                        ticket = ticket + "&tenant=" + tenant;
                    }

                    Util.trace("ticket: " + ticket);
                    return ticket;
                } else {
                    throw new Exception("Required CAS Ticket not found");
                }
            } else {
                redirectUrlFromCASLogin = getAuthParams(conCASLogin);
                JSONObject jsonCASLogin = new JSONObject(redirectUrlFromCASLogin);
                JSONArray jsonArrayCASLogin = (JSONArray)jsonCASLogin.get("errorMsgs");
                String errorMsg = "";

                for(int i = 0; i < jsonArrayCASLogin.length(); ++i) {
                    JSONObject tmp = (JSONObject)jsonArrayCASLogin.get(i);
                    errorMsg = errorMsg + tmp.get("defaultMessage");
                }

                if (errorMsg.isEmpty()) {
                    errorMsg = "Internal Server Error";
                }

                Util.trace("errorMsg: " + errorMsg);
                throw new Exception(errorMsg);
            }
        }
    }

    private static HttpURLConnection getHttpConnection(String sUrl, boolean followRedirect) throws MalformedURLException, IOException, NoSuchAlgorithmException, KeyManagementException {
        URL url = new URL(sUrl);
        HttpURLConnection con = (HttpURLConnection)url.openConnection();
        con.setInstanceFollowRedirects(followRedirect);
        return con;
    }

    private static Map<String, String> getCASCookies(URLConnection con) {
        Map<String, String> cookies = new HashMap();
        Map<String, List<String>> headers = con.getHeaderFields();
        Iterator var3 = headers.entrySet().iterator();

        while(var3.hasNext()) {
            Map.Entry<String, List<String>> entry = (Map.Entry)var3.next();
            String headerKey = (String)entry.getKey();
            if (headerKey != null && headerKey.equalsIgnoreCase("Set-Cookie")) {
                Iterator var6 = ((List)entry.getValue()).iterator();

                while(var6.hasNext()) {
                    String headerValue = (String)var6.next();
                    if (headerValue != null) {
                        String[] fields = headerValue.split(";\\s*");
                        String cookieValue = fields[0];
                        String[] a = cookieValue.split("=", 2);
                        cookies.put(a[0], a[1]);
                    }
                }

                return cookies;
            }
        }

        return cookies;
    }

    private static String encodeUrlParams(Properties p) throws UnsupportedEncodingException {
        StringBuilder sb = new StringBuilder();
        Enumeration names = p.propertyNames();

        while(names.hasMoreElements()) {
            String name = (String)names.nextElement();
            String value = p.getProperty(name);
            sb.append("&").append(URLEncoder.encode(name, "UTF-8")).append("=").append(URLEncoder.encode(value, "UTF-8"));
        }

        return sb.delete(0, 1).toString();
    }

    private static String inputStreamToString(InputStream inputStream, Charset charset) throws IOException {
        StringWriter writer = new StringWriter();

        String var4;
        try {
            InputStreamReader reader = new InputStreamReader(inputStream, charset);

            try {
                reader.transferTo(writer);
                var4 = writer.toString();
            } catch (Throwable var8) {
                try {
                    reader.close();
                } catch (Throwable var7) {
                    var8.addSuppressed(var7);
                }

                throw var8;
            }

            reader.close();
        } catch (Throwable var9) {
            try {
                writer.close();
            } catch (Throwable var6) {
                var9.addSuppressed(var6);
            }

            throw var9;
        }

        writer.close();
        return var4;
    }

    private static String getAuthParams(URLConnection con) throws ParserConfigurationException, SAXException, IOException {
        String authParams = null;
        String page = inputStreamToString(con.getInputStream(), StandardCharsets.UTF_8);
        String line = "";
        BufferedReader br = new BufferedReader(new StringReader(page));

        try {
            while((line = br.readLine()) != null) {
                if (line.contains("id=\"configData\"")) {
                    DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
                    dbf.setValidating(false);
                    DocumentBuilder db = dbf.newDocumentBuilder();
                    StringReader characterStream = new StringReader(line);

                    try {
                        Document doc = db.parse(new InputSource(characterStream));
                        authParams = doc.getDocumentElement().getTextContent();
                    } catch (Throwable var12) {
                        try {
                            characterStream.close();
                        } catch (Throwable var11) {
                            var12.addSuppressed(var11);
                        }

                        throw var12;
                    }

                    characterStream.close();
                    break;
                }
            }
        } catch (Throwable var13) {
            try {
                br.close();
            } catch (Throwable var10) {
                var13.addSuppressed(var10);
            }

            throw var13;
        }

        br.close();
        return authParams;
    }

    public static class TrustAllTrustManager implements X509TrustManager {
        public TrustAllTrustManager() {
        }

        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[0];
        }

        public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        }

        public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        }
    }
}
