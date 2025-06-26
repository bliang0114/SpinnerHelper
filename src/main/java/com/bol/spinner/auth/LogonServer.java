package com.bol.spinner.auth;


import matrix.db.Context;
import matrix.db.Environment;
import matrix.db.ServerVersion;
import matrix.util.MatrixException;
import matrix.util.Version;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.util.Objects;
import java.util.Scanner;


public class LogonServer extends Object implements Cloneable {
    public static final String MASTER_PW = "verysecret";
    private final static Logger logger = LoggerFactory.getLogger(LogonServer.class.getName());

    private static boolean _VERSION_CHECKED = false;

    public static final String VERSION_3DX_CLIENT = Version.getVersionString();
    public static String VERSION_3DX_SERVER = "not connected";
    private String url = "";
    private String user = "";
    private String pwdEncrypted = "";
    private String vault = "";
    private String alias = "";
    private boolean cas = false;
    private String hostname = "";
    private String role = "";
    private Context ctx = null;

    public LogonServer(String serverUrl, String loginUser, String loginPwdEncrypted, String selectedVault, String aliasName, boolean casMode) {

        url = serverUrl;
        user = loginUser;
        pwdEncrypted = loginPwdEncrypted;
        vault = selectedVault;
        cas = casMode;
        alias = aliasName;

        setHostname();

        debugServerInfo();
    }

    public LogonServer(String s) {

        // * determine server information from string *
        logger.debug("Server defintion=" + s);

        String[] info = s.split(";");
        logger.debug("Found server defintions=" + info.length);

        url = info[0];
        user = info[1];
        pwdEncrypted = info[2];
        vault = info[3];

        if (info.length > 4) {
            cas = info[4].equalsIgnoreCase("TRUE");

            if (info.length > 5) {
                alias = info[5];
            }
        }

        setHostname();

        debugServerInfo();
    }

    public LogonServer() {

    }

    public void debugServerInfo() {

        logger.debug("Serverdefinition:");
        logger.debug("url=" + url);
        logger.debug("user=" + user);
        logger.debug("encrypted password=" + pwdEncrypted);
        logger.debug("Default vault=" + vault);
        logger.debug("Alias name=" + alias);
        logger.debug("CAS server=" + String.valueOf(cas));
        logger.debug("host name=" + hostname);
        logger.debug("role=" + role);
        logger.debug("connected to 3DX=" + (ctx != null));
    }

    private void setHostname() {

        if (url.isEmpty()) {

            hostname = "localhost"; // we are in RIP mode, so there is no application server
        } else {

            hostname = url.substring(url.indexOf("//") + 2);
            hostname = hostname.substring(0, hostname.indexOf("/"));

            if (hostname.contains(":")) {
                hostname = hostname.substring(0, hostname.indexOf(":"));
            }
        }
    }

    public String getDisplayString(int n) {

        String aliasDisplay = alias.isBlank() ? "" : " • " + alias;
        String v = String.valueOf(n) + aliasDisplay + " • " + url + getCASDisplay() + " • " + user + " • " + vault;
        logger.debug("getDisplayString()=" + v);

        return v;
    }

    private String getCASDisplay() {
        return (cas == true ? " (CAS)" : " (non-CAS)");
    }

    /**
     * Server Alias is not considered to secure that the change of an alias does
     * not lead to additional entries in the server list.
     *
     * @param o - object to compare
     * @return
     */
    @Override
    public boolean equals(Object o) {

        // self check
        if (this == o) {
            return true;
        }

        // null check
        if (o == null) {
            return false;
        }

        // type check and cast
        if (getClass() != o.getClass()) {
            return false;
        }

        LogonServer srv = (LogonServer) o;

        boolean ret = Objects.equals(url, srv.url) //
                && Objects.equals(user, srv.user) //
                && Objects.equals(pwdEncrypted, srv.pwdEncrypted) //
                && Objects.equals(vault, srv.vault) //
                && (cas == srv.cas) //
                ;
   
        this.debugServerInfo();
        srv.debugServerInfo();

        return ret;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 29 * hash + Objects.hashCode(this.url);
        hash = 29 * hash + Objects.hashCode(this.user);
        hash = 29 * hash + Objects.hashCode(this.pwdEncrypted);
        hash = 29 * hash + Objects.hashCode(this.vault);
        hash = 29 * hash + (this.cas ? 1 : 0);
        return hash;
    }

    public String getUser() {
        return this.user;
    }

    public String getHostname() {
        return this.hostname;
    }

    public String getUrl() {
        return this.url;
    }

    public String getPassword() {
        return this.pwdEncrypted;
    }

    public String getDecryptedPassword() {

      /*  String pwd = "";

        try {
            if (!Util.isEncrypted(pwdEncrypted)) {
                pwd = SimpleCrypto.decrypt(MASTER_PW, pwdEncrypted);
            }
        } catch (Exception ex) {
            Util.displayError("Error decrypting stored password");
            logger.error("ERROR", ex);
        }
*/
        return pwdEncrypted;
    }

    public boolean isCasServer() {
        return this.cas;
    }

    public Context connect() {

        try {
            Passport.setTrustManager(false);
            if (this.isCasServer()) {
                String t = Passport.getTicket(this.getUrl(), this.getUser(), this.getDecryptedPassword());
                String newUrl = Util.addUrlParam(this.getUrl(), "ticket", t);
                ctx = new Context(newUrl);
            } else {
                ctx = new Context(this.getUrl());
                ctx.setPassword(this.getDecryptedPassword());
            }
            ctx.setUser(user);
            ctx.setRole(role);
            ctx.setVault(vault);
            ctx.connect();

            if (!isUserAllowed(ctx)) {
                var msg = "You are not allowed to access this environment via 3DX Database Browser.\nPlease contact your system admin for further information.";
                JOptionPane.showMessageDialog(null, msg, "Error", JOptionPane.ERROR_MESSAGE);
                ctx = null;
            } else {

                getUsedServerRelease(ctx);

                if (Boolean.parseBoolean("true")) {
                    checkLibraryVersion(ctx);
                }
            }

        } catch (Exception ex) {
            ex.printStackTrace();
            var msg = ex.getLocalizedMessage();

            if (msg.contains("XML: Expected") || msg.contains("Exception: Expected") || msg.contains("XML: -1")) {
                msg = "Enovia reported the following error: " + msg
                        + "\nThis can be due to wrong library versions. Take these files"
                        + "\n• eMatrixServletRMI.jar, enoviaKernel.jar"
                        + "\n• FcsBackEnd.jar, FcsClient.jar, FcsServer.jar"
                        + "\n• m1jsystem.jar, metrics-core.jar, mx_jdom_1.0.jar, slf4j-api.jar"
                        + "\nfrom your Enovia server and copy them into the EnoBrowser\\lib folder."
                        + "\nYou find them in C:\\...\\tomee\\webapps\\3dspace\\WEB-INF\\lib on your Enovia server.";
            } else if (msg.contains("XML: java.lang.NullPointerException")) {
                msg = "Enovia reported the following error: " + msg
                        + "\nThis can be due to a network address change."
                        + "\nRestart Tomcat and try again.";
            } else if (msg.contains("java.net.SocketException: Connection reset")) {
                msg = "The system reported the following error: " + msg
                        + "\nMaybe the server is down or the port number in the Host URL is wrong."
                        + "\nCheck that the server is alive or try with a different port number.";
            } else if (msg.contains("PKIX") || msg.contains("SSLHandshakeException") || msg.contains("CertificateException")) {
                msg = "The system reported the following error: " + msg
                        + "\nMaybe the SSL certificates are not installed properly or need to be renewed."
                        + "\nContact your admin or disable certificates via menu \"Options \u279c Certificates\".";
            } else if (msg.contains("/3dpassport/")) {
                msg = "The system reported the following error: " + msg
                        + "\nIt seems that you are trying to login to a 3DPassport authenticated system."
                        + "\nMake sure you have checked menu \"Options \u279c 3DPassport\".";
            } else if (msg.contains("Required CAS Redirect not found")) {
                msg = "The system reported the following error: " + msg
                        + "\nIf you try to connect to a non-CAS instance,"
                        + "\nplease make sure you have unchecked menu \"Options \u279c 3DPassport\"."
                        + "\nIf you try to connect to a CAS instance,"
                        + "\nplease check that you use https protocol and that"
                        + "\nthe web application name after the host name is correct.";
            } else if (msg.contains("servlet/MatrixXMLServlet")) {
                msg = "The system reported the following error: " + msg
                        + "\nPlease check that you did not miss the web application name after the host name."
                        + "\nThe entire Host URL string must look like this \"http(s)://host:port/application\"";
            } else if (msg.contains("Connection refused")) {
                msg = "The system reported the following error: " + msg
                        + "\nMaybe you use the wrong port number.";
            } else if (msg.contains("Invalid password")) {
                msg = "The system reported the following error: " + msg
                        + "\nThe reason can be a wrong password but also"
                        + "\nmissing licenses can cause this error.";
            } else if (ex.getClass().getName().contains("UnknownHost")) {
                msg = "The system reported an Unknown Host exception for host " + msg
                        + "\nPlease check the host name.";
            } else if (url.isEmpty()) {
                var bits = System.getProperty("sun.arch.data.model");
                if (bits.equals("32")) {
                    msg = "You are using 3DEXPERIENCE Database Browser in RIP mode but on a 32 bit Java environment."
                            + "\nRe-launch the 3DEXPERIENCE Database Browser in a 64 bit Java environment.";
                } else {
                    msg = "You are using 3DEXPERIENCE Database Browser in RIP mode."
                            + "\n• Make sure the PATH variable contains the ENOVIA Live Collaboration server path, e.g. C:\\enoviaV6R20??\\server\\win_b64\\code\\bin."
                            + "\n• Make sure you have copied 'enoviaKernel.jar' from the ...\\WEB-INF\\lib folder to EnoBrowser\\lib.";
                }
            }

            Util.trace("Connect Error: " + msg);
            Util.trace(ex);
            JOptionPane.showMessageDialog(null, msg, "Error", JOptionPane.ERROR_MESSAGE);

        } finally {
            return ctx;
        }
    }

    public void setUrl(String urlString) {
        url = urlString;
    }

    public void setUser(String userName) {
        user = userName;
    }

    public void setPassword(String pwd) {

        try {
            if (!Util.isEncrypted(pwd)) {
                pwd = SimpleCrypto.encrypt(MASTER_PW, pwd);
            }
        } catch (Exception ex) {
            Util.displayError("Error encrypting stored password");
            logger.error("ERROR", ex);
        }

        pwdEncrypted = pwd;
    }

    public void setVault(String vaultName) {
        vault = vaultName;
    }

    public void setRole(String roleName) {
        role = roleName;
    }

    public String getRole() {
        return role;
    }

    boolean isUserAllowed(Context ctx) {
        
        try {
            String allow = Environment.getValue(ctx, "DB_BROWSER_ALLOW");
            String deny = Environment.getValue(ctx, "DB_BROWSER_DENY");
            if (!allow.isEmpty()) {
                return allow.equals("*") || ("," + allow + ",").contains("," + user + ",");
            }
            if (!deny.isEmpty()) {
                return !(deny.equals("*") || ("," + deny + ",").contains("," + user + ","));
            }
        } catch (MatrixException ex) {
            Util.trace(ex);
        }
        
        return true;
    }

    private static String parseVersion(String str) {
        String version = "";
        try (final Scanner scanner = new Scanner(str)) {
            scanner.useDelimiter(" ");
            scanner.findInLine("3DEXPERIENCE");
            version = scanner.next();
        }
        return version;
    }

    private void checkLibraryVersion(Context ctx) throws Exception {

        if (_VERSION_CHECKED) {

            // get the version of the enovia library
            String clientVersionString = VERSION_3DX_CLIENT;

            // get the server version of the enovia library
            String serverVersionString = VERSION_3DX_SERVER;

            // compare the client version with the server version
            if (!parseVersion(clientVersionString).equals(parseVersion(serverVersionString))) {
                throw new Exception("Connection cancelled, because the version of the libraries in lib folder does not match the server version:'" + "\n  \u00b7 local: " + clientVersionString + "\n  \u00b7 server: " + serverVersionString);
            }

            // compare the client hotfix level with the server hotfix level
            if (!parseHotfix(clientVersionString).equals(parseHotfix(serverVersionString))) {
                String msg = "The version of the libraries in lib folder does not match the server hotfix level:" + "\n  \u00b7 local: " + clientVersionString + "\n  \u00b7 server: " + serverVersionString;
                JOptionPane.showMessageDialog(null, msg, "Warning", JOptionPane.WARNING_MESSAGE);
            }

            // * store already checked *
            _VERSION_CHECKED = true;
        }
    }

    private static String parseHotfix(String str) {

        String hotfix = "";

        try (final Scanner scanner = new Scanner(str)) {
            scanner.useDelimiter(" ");
            scanner.findInLine("HotFix");
            hotfix = scanner.next();
        }

        return hotfix;
    }

    private static void getUsedServerRelease(Context ctx) throws MatrixException {

        // get the server version of the enovia library
        String serverVersionString = null;
        ServerVersion serverVersion = null;

        try {
            serverVersion = new ServerVersion();
            serverVersion.open(ctx);
            serverVersionString = serverVersion.getVersion(ctx);
            VERSION_3DX_SERVER = serverVersionString;
            logger.info("Server version: " + serverVersionString);
        } finally {
            serverVersion.close(ctx);
        }
    }

    public Context checkContext() {

        if (ctx == null) {
            Util.displayError("Please logon to an 3DEXPERIENCE Platform!");
            return null;
        }

        this.connect();

        return this.ctx;
    }

    public void setHostAlias(String aliasName) {
        alias = aliasName;
    }

    public String getHostAlias() {
        return alias == null ? "" : alias;
    }

    public String getVault() {
        return vault;
    }

    public void setCasServer(Boolean valueOf) {
        cas = valueOf;
    }  

    @Override
    public Object clone() throws CloneNotSupportedException {

        return super.clone();
    }
   
    @Override
    public String toString() {

        String s = "";

        s = url;
        s += ";";
        s += user;
        s += ";";
        s += pwdEncrypted;
        s += ";";
        s += vault;
        s += ";";
        s += cas;
        s += ";";
        s += alias;
        logger.debug("host information toString()=" + s);

        return s;
    }
    
    public boolean isConnected() {
        return (ctx == null);
    }
}
