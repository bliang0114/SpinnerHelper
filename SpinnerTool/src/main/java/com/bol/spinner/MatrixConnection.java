package com.bol.spinner;

import matrix.db.Context;
import matrix.db.Environment;
import matrix.db.ServerVersion;
import matrix.util.MatrixException;
import matrix.util.Version;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.util.Scanner;

public class MatrixConnection {
    private final static Logger logger = LoggerFactory.getLogger(MatrixConnection.class.getName());
    private String version3DXServer = "not connected";
    private String hostUrl;
    private String user;
    private String password;
    private String vault;
    private String securityContext;

    public MatrixConnection(String hostUrl, String user, String password, String vault) {
        this.hostUrl = hostUrl;
        this.user = user;
        this.password = password;
        this.vault = vault;
    }

    public MatrixConnection(String hostUrl, String user, String password, String vault, String securityContext) {
        this.hostUrl = hostUrl;
        this.user = user;
        this.password = password;
        this.vault = vault;
        this.securityContext = securityContext;
    }

    public MatrixContext connect() throws Exception {
        try {
            Passport.setTrustManager(false);
            String ticket = Passport.getTicket(this.hostUrl, this.user, this.password);
            String newUrl = Util.addUrlParam(this.hostUrl, "ticket", ticket);
            Context ctx = new Context(newUrl);
            ctx.setUser(user);
            ctx.setVault(vault);
            ctx.connect();
            if (!isUserAllowed(ctx)) {
                throw new Exception("You are not allowed to access this environment via 3DX Database Browser.\nPlease contact your system admin for further information.");
            } else {
                getUsedServerRelease(ctx);
                checkLibraryVersion();
            }
            return new MatrixContext(ctx);
        } catch (Exception ex) {
            String msg = formatConnectError(ex);
            throw new Exception(msg);
        }
    }

    public void disconnect(MatrixContext matrixContext) {
        matrixContext.disconnect();
    }

    private boolean isUserAllowed(Context ctx) {
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

    private void getUsedServerRelease(Context ctx) throws Exception {
        String serverVersionString;
        ServerVersion serverVersion = null;
        try {
            serverVersion = new ServerVersion();
            serverVersion.open(ctx);
            serverVersionString = serverVersion.getVersion(ctx);
            version3DXServer = serverVersionString;
            logger.info("Server version: {}", serverVersionString);
        } finally {
            if (serverVersion != null) {
                serverVersion.close(ctx);
            }
        }
    }

    private void checkLibraryVersion() throws Exception {
        String clientVersionString = Version.getVersionString();
        String serverVersionString = version3DXServer;
        if (!parseVersion(clientVersionString).equals(parseVersion(serverVersionString))) {
            throw new Exception("Connection cancelled, because the version of the libraries in lib folder does not match the server version:'" + "\n  · local: " + clientVersionString + "\n  · server: " + serverVersionString);
        }
        if (!parseHotfix(clientVersionString).equals(parseHotfix(serverVersionString))) {
            String msg = "The version of the libraries in lib folder does not match the server hotfix level:" + "\n  · local: " + clientVersionString + "\n  · server: " + serverVersionString;
            JOptionPane.showMessageDialog(null, msg, "Warning", JOptionPane.WARNING_MESSAGE);
        }
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

    private static String parseHotfix(String str) {
        String hotfix = "";
        try (final Scanner scanner = new Scanner(str)) {
            scanner.useDelimiter(" ");
            scanner.findInLine("HotFix");
            hotfix = scanner.next();
        }
        return hotfix;
    }

    String formatConnectError(Exception ex) {
        String msg = ex.getLocalizedMessage();
        String templateMsg = "%s";
        if (msg.contains("XML: Expected") || msg.contains("Exception: Expected") || msg.contains("XML: -1")) {
            templateMsg = """
                        Enovia reported the following error: %s
                        This can be due to wrong library versions. Take these files
                        • eMatrixServletRMI.jar, enoviaKernel.jar
                        • eMatrixServletRMI.jar, enoviaKernel.jar
                        • FcsBackEnd.jar, FcsClient.jar, FcsServer.jar
                        • m1jsystem.jar, metrics-core.jar, mx_jdom_1.0.jar, slf4j-api.jar
                        from your Enovia server and copy them into the EnoBrowser\\lib folder.
                        You find them in C:\\...\\tomee\\webapps\\3dspace\\WEB-INF\\lib on your Enovia server.
                        """;
        } else if (msg.contains("XML: java.lang.NullPointerException")) {
            templateMsg = """
                        Enovia reported the following error: %s
                        This can be due to a network address change.
                        Restart Tomcat and try again.
                        """;
        } else if (msg.contains("java.net.SocketException: Connection reset")) {
            templateMsg = """
                        The system reported the following error: %s
                        Maybe the server is down or the port number in the Host URL is wrong.
                        Check that the server is alive or try with a different port number.
                        """;
        } else if (msg.contains("PKIX") || msg.contains("SSLHandshakeException") || msg.contains("CertificateException")) {
            templateMsg = """
                        The system reported the following error: %s
                        Maybe the SSL certificates are not installed properly or need to be renewed.
                        Contact your admin or disable certificates via menu "Options ➜ Certificates".
                        """;
        } else if (msg.contains("/3dpassport/")) {
            templateMsg = """
                        The system reported the following error: %s
                        It seems that you are trying to login to a 3DPassport authenticated system.
                        Make sure you have checked menu "Options ➜ 3DPassport".
                        """;
        } else if (msg.contains("Required CAS Redirect not found")) {
            templateMsg = """
                        The system reported the following error: %s
                        If you try to connect to a non-CAS instance,
                        please make sure you have unchecked menu "Options ➜ 3DPassport".
                        If you try to connect to a CAS instance,"
                        please check that you use https protocol and that"
                        the web application name after the host name is correct.";
                        """;
        } else if (msg.contains("servlet/MatrixXMLServlet")) {
            templateMsg = """
                        The system reported the following error: %s
                        Please check that you did not miss the web application name after the host name.
                        The entire Host URL string must look like this "http(s)://host:port/application";
                        """;
        } else if (msg.contains("Connection refused")) {
            templateMsg = """
                        The system reported the following error: %s
                        Maybe you use the wrong port number.
                        """;
        } else if (msg.contains("Invalid password")) {
            templateMsg = """
                        The system reported the following error: %s
                        The reason can be a wrong password but also missing licenses can cause this error.
                        """;
        } else if (ex.getClass().getName().contains("UnknownHost")) {
            templateMsg = """
                        The system reported an Unknown Host exception for host %s
                        Please check the host name.
                        """;
        } else if (this.hostUrl.isEmpty()) {
            String bits = System.getProperty("sun.arch.data.model");
            if (bits.equals("32")) {
                templateMsg = """
                            You are using 3DEXPERIENCE Database Browser in RIP mode but on a 32 bit Java environment.
                            Re-launch the 3DEXPERIENCE Database Browser in a 64 bit Java environment.
                            """;
            } else {
                templateMsg = """
                            You are using 3DEXPERIENCE Database Browser in RIP mode.
                            • Make sure the PATH variable contains the ENOVIA Live Collaboration server path, e.g. C:\\enoviaV6R20??\\server\\win_b64\\code\\bin.
                            • Make sure you have copied 'enoviaKernel.jar' from the ...\\WEB-INF\\lib folder to EnoBrowser\\lib.
                            """;
            }
        }
        return String.format(templateMsg, msg);
    }

    public String getHostUrl() {
        return hostUrl;
    }

    public void setHostUrl(String hostUrl) {
        this.hostUrl = hostUrl;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getVault() {
        return vault;
    }

    public void setVault(String vault) {
        this.vault = vault;
    }

    public String getSecurityContext() {
        return securityContext;
    }

    public void setSecurityContext(String securityContext) {
        this.securityContext = securityContext;
    }
}
