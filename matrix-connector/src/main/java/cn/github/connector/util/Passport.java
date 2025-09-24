package cn.github.connector.util;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.net.ssl.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
public class Passport {
    static final String KERNEL_SERVLET = "/servlet/MatrixXMLServlet";

    public Passport() {
    }

    public static class TrustAllTrustManager implements X509TrustManager {
        public TrustAllTrustManager() {
        }

        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[0];
        }

        public void checkClientTrusted(X509Certificate[] chain, String authType) {
        }

        public void checkServerTrusted(X509Certificate[] chain, String authType) {
        }
    }

    public static void setTrustManager(boolean useCertificates) throws Exception {
        SSLContext sc = SSLContext.getInstance("TLS");
        if (useCertificates) {
            sc.init((KeyManager[]) null, (TrustManager[]) null, (SecureRandom) null);
        } else {
            sc.init((KeyManager[]) null, new TrustManager[]{new TrustAllTrustManager()}, (SecureRandom) null);
        }
        HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
    }

    public static String getCookieStr(String host, AtomicReference<String> tenant, HttpURLConnection connection) {
        Map<String, String> cookies = getCASCookies(connection);
        StringBuilder cookiesBuilder = new StringBuilder();
        String jSessionId = cookies.get("JSESSIONID");
        if (jSessionId != null) {
            cookiesBuilder.append("JSESSIONID=").append(jSessionId).append(";");
        }
        String serverId = cookies.get("SERVERID");
        if (serverId != null) {
            cookiesBuilder.append("SERVERID=").append(serverId).append(";");
            if (host.endsWith("enovia") && host.contains("-")) {
                log.info("determine cloud tenant");
                tenant.set(host.substring(host.indexOf("//") + 2, host.indexOf("-")).toUpperCase());
                log.info("tenant id: {}", tenant);
            }
        }
        log.info("COOKIES: {}", cookiesBuilder);
        return cookiesBuilder.toString();
    }

    public static String getTicket(String host, String user, String password) throws Exception {
        String newUri = resolverUrl(host, KERNEL_SERVLET + "?NotSSOLogin=true");
        HttpURLConnection httpConnection = getHttpConnection(newUri);
        int responseCode = httpConnection.getResponseCode();
        if (responseCode == 400) {
            throw new Exception("Error 400: Bad request");
        } else if (responseCode == 404) {
            throw new Exception("Error 404: Not found");
        } else if (responseCode != 302) {
            throw new Exception("Error 404: Not found");
        }
        String redirectUrl = httpConnection.getHeaderField("Location");
        log.info("redirectUrl: {}", redirectUrl);

        HttpURLConnection casConnection = getHttpConnection(redirectUrl);
        String authParamsCAS = getAuthParams(casConnection);
        log.info("authParamsCAS: {}", authParamsCAS);

        JSONObject jsonCAS = JSONUtil.parseObj(authParamsCAS);
        String lt = jsonCAS.getStrEscaped("lt");
        log.info("lt: {}", lt);

        String loginUrlCAS = jsonCAS.getStrEscaped("url");
        log.info("loginUrlCAS: {}", loginUrlCAS);

        AtomicReference<String> tenant = new AtomicReference<>();
        String cookieStr = getCookieStr(host, tenant, casConnection);
        HttpURLConnection casLoginConnection = getHttpConnection(loginUrlCAS + "?NotSSOLogin=true");
        casLoginConnection.setRequestProperty("Cookie", cookieStr);
        casLoginConnection.setRequestMethod("POST");
        casLoginConnection.setDoOutput(true);
        Properties casUrlParamProperties = new Properties();
        casUrlParamProperties.put("lt", lt);
        casUrlParamProperties.put("username", user);
        casUrlParamProperties.put("password", password);
        String casUrlParams = encodeUrlParams(casUrlParamProperties);
        try (DataOutputStream wr = new DataOutputStream(casLoginConnection.getOutputStream())) {
            wr.writeBytes(casUrlParams);
            wr.flush();
        }
        int casLoginResponseCode = casLoginConnection.getResponseCode();
        log.info("respCodeCASLogin: {}", casLoginResponseCode);
        if (casLoginResponseCode == 302) {
            redirectUrl = casLoginConnection.getHeaderField("Location");
            log.info("redirectUrlFromCASLogin: {}", redirectUrl);
            if (redirectUrl.contains("ticket=")) {
                String ticket = getUrlParam(redirectUrl, "ticket");
                if (tenant.get() != null) {
                    ticket = ticket + "&tenant=" + tenant.get();
                }
                log.info("ticket: {}", ticket);
                return ticket;
            } else {
                throw new Exception("Required CAS Ticket not found");
            }
        } else {
            redirectUrl = getAuthParams(casLoginConnection);
            JSONObject jsonCASLogin = JSONUtil.parseObj(redirectUrl);
            JSONArray jsonArrayCASLogin = jsonCASLogin.getJSONArray("errorMsgs");
            StringBuilder errorMsg = new StringBuilder();
            for (Object o : jsonArrayCASLogin) {
                JSONObject tmp = (JSONObject) o;
                errorMsg.append(tmp.get("defaultMessage"));
            }
            if (errorMsg.isEmpty()) {
                errorMsg = new StringBuilder("Internal Server Error");
            }
            log.info("errorMsg: {}", errorMsg);
            throw new Exception(errorMsg.toString());
        }
    }

    private static HttpURLConnection getHttpConnection(String sUrl) throws IOException {
        URL url = URI.create(sUrl).toURL();
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setInstanceFollowRedirects(false);
        return con;
    }

    private static Map<String, String> getCASCookies(URLConnection con) {
        Map<String, String> cookies = new HashMap<>();
        Map<String, List<String>> headers = con.getHeaderFields();
        for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
            String headerKey = entry.getKey();
            if (headerKey != null && headerKey.equalsIgnoreCase("Set-Cookie")) {
                for (String headerValue : entry.getValue()) {
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

    private static String encodeUrlParams(Properties p) {
        StringBuilder builder = new StringBuilder();
        Enumeration<?> names = p.propertyNames();
        while (names.hasMoreElements()) {
            String name = (String) names.nextElement();
            String value = p.getProperty(name);
            builder.append("&").append(URLEncoder.encode(name, StandardCharsets.UTF_8)).append("=").append(URLEncoder.encode(value, StandardCharsets.UTF_8));
        }
        return builder.delete(0, 1).toString();
    }

    private static String inputStreamToString(InputStream inputStream) throws IOException {
        String str;
        try (StringWriter writer = new StringWriter();
             InputStreamReader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
            reader.transferTo(writer);
            str = writer.toString();
        }
        return str;
    }

    private static String getAuthParams(URLConnection connection) throws ParserConfigurationException, SAXException, IOException {
        String authParams = null;
        String page = inputStreamToString(connection.getInputStream());
        String line;
        try (BufferedReader br = new BufferedReader(new StringReader(page))) {
            while ((line = br.readLine()) != null) {
                if (line.contains("id=\"configData\"")) {
                    DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
                    dbf.setValidating(false);
                    DocumentBuilder db = dbf.newDocumentBuilder();
                    try (StringReader characterStream = new StringReader(line)) {
                        Document doc = db.parse(new InputSource(characterStream));
                        authParams = doc.getDocumentElement().getTextContent();
                    }
                    break;
                }
            }
        }
        return authParams;
    }

    public static String resolverUrl(String host, String path) throws URISyntaxException {
        URI hostUri = new URI(host);
        String query = hostUri.getQuery();
        String newPath = hostUri.getPath() + path;
        URI resultUri = new URI(hostUri.getScheme(), hostUri.getAuthority(), newPath, query, null);
        return resultUri.toString();
    }

    public static String getUrlParam(String url, String queryParam) throws URISyntaxException {
        // Parse the URL
        URI uri = new URI(url);
        // Extract the query component
        String query = uri.getQuery();
        // Split the query into individual parameters
        String[] pairs = query.split("&");
        // Iterate over the parameters to find the one that matches `param`
        for (String pair : pairs) {
            String[] keyValue = pair.split("=");
            if (keyValue.length == 2 && keyValue[0].equals(queryParam)) {
                return keyValue[1];
            }
        }
        return null;
    }

    public static String addUrlParam(String url, String param, String value) throws URISyntaxException {
        // Parse the URL
        URI uri = new URI(url);
        // Extract the query component
        String query = uri.getQuery();
        // Append the new parameter to the query string
        String newQuery = (query == null) ? param + "=" + value : query + "&" + param + "=" + value;
        // String newQuery = param + "=" + value;
        // Reconstruct the URL with the new query string
        URI newUri = new URI(
                uri.getScheme(),
                uri.getAuthority(),
                uri.getPath(),
                newQuery,
                uri.getFragment()
        );
        // Convert the new URI back to a string
        return newUri.toString();
    }
}
