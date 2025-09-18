package com.bol.spinner.auth;

import matrix.db.*;
import matrix.util.MatrixException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.datatransfer.StringSelection;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Properties;
import java.util.TreeMap;
import java.util.regex.Pattern;

public class Util {

    public static String rootdir, systemprops, ematrixml;
    static HashMap<String, String> syntaxStyles = new HashMap<String, String>();
    static SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH-mm-ss");
   
    private final static Logger logger = LoggerFactory.getLogger(Util.class.getName());
    
    static String buildMQL(String command, String... args) {
        for (var i = 0; i < args.length; i++)
            if (args[i].contains(" "))
                command = command.replace("&" + String.valueOf(i + 1), "'" + args[i] + "'");
            else
                command = command.replace("&" + String.valueOf(i + 1), args[i]);
        return command;
    }

    static void clearTable(JTable t) {
        var m = (DefaultTableModel) t.getModel();
        var n = m.getRowCount();
        for (var i = 0; i < n; i++)
            m.removeRow(0);
    }


    static void copyToClipBoard(JTable t) {
        var th = t.getTransferHandler();
        if (th != null) {
            var CB = java.awt.Toolkit.getDefaultToolkit().getSystemClipboard();
            th.exportToClipboard(t, CB, TransferHandler.COPY);
        }
    }

    static void copyToClipBoard2(JTable t) {
        var colID = -1;
        var sb = new StringBuilder();
        for (var c = 0; c < t.getSelectedColumns().length; c++) {
            var col = t.getSelectedColumns()[c];
            var header = t.getColumnModel().getColumn(col).getHeaderValue().toString();
            if (header.equals("ID"))
                colID = c;
        }
        for (var r = 0; r < t.getSelectedRows().length; r++) {
            var row = t.getSelectedRows()[r];
            sb.append("\n");
            for (var c = 0; c < t.getSelectedColumns().length; c++) {
                var col = t.getSelectedColumns()[c];
                var value = "";
                if (t.getValueAt(row, col) != null)
                    value = t.getValueAt(row, col).toString().replace("\n", "");
                sb.append("\t");
                if (c == colID)
                    sb.append("'");
                sb.append(value);
            }
        }
        var result = sb.toString();
        result = result.replace("\n\t", "\n");
        result = result.substring(1);
        var CB = java.awt.Toolkit.getDefaultToolkit().getSystemClipboard();
        CB.setContents(new StringSelection(result), null);
    }

    static void copyToClipBoardWithHeaders(JTable t, String prefix) {
        var colID = -1;
        var sb = new StringBuilder();
        if (!prefix.isEmpty())
            sb.append(prefix).append("\n");
        for (var c = 0; c < t.getSelectedColumns().length; c++) {
            var col = t.getSelectedColumns()[c];
            var header = t.getColumnModel().getColumn(col).getHeaderValue().toString();
            sb.append("\t");
            sb.append(header);
            if (header.equals("ID"))
                colID = c;
        }
        for (var r = 0; r < t.getSelectedRows().length; r++) {
            var row = t.getSelectedRows()[r];
            sb.append("\n");
            for (var c = 0; c < t.getSelectedColumns().length; c++) {
                var col = t.getSelectedColumns()[c];
                var value = "";
                if (t.getValueAt(row, col) != null)
                    value = t.getValueAt(row, col).toString();
                sb.append("\t");
                if (c == colID)
                    sb.append("'");
                sb.append(value);
            }
        }
        var result = sb.toString();
        result = result.replace("\n\t", "\n");
        if (prefix.isEmpty())
            result = result.substring(1);
        var CB = java.awt.Toolkit.getDefaultToolkit().getSystemClipboard();
        CB.setContents(new StringSelection(result), null);
    }

    public static void displayError(String msg) {
        trace("Error: " + msg);
        JOptionPane.showMessageDialog(null, msg, "Error", JOptionPane.ERROR_MESSAGE);
    }

    static void displayInformation(String msg) {
        JOptionPane.showMessageDialog(null, msg, "Information", JOptionPane.INFORMATION_MESSAGE);
    }

    public static String execMQL(Context ctx, String command) throws MatrixException {
        return execMQL(ctx, command, true);
    }

    static String execMQLquite(Context ctx, String command) throws MatrixException {
        return execMQL(ctx, command, false);
    }

    private static String execMQL(Context ctx, String command, boolean showMessage) throws MatrixException {
        var mql = new MQLCommand();
        if (!mql.executeCommand(ctx, command, true)) {
            trace("MQL Command: " + command);
            trace("MQL Error: " + mql.getError());
            if (showMessage)
                Util.displayError(mql.getError());
            return "";
        }
        var result = mql.getResult();
        if (result.endsWith("\n"))
            result = result.substring(0, result.length() - 1);
        return result;
    }

    static HashMap getAttributeValues(Context ctx, BusinessObject bus) throws MatrixException {
        
        var H = new HashMap<String, String>();
        
        bus.open(ctx);
        H.put("Type", bus.getTypeName());
        H.put("Name", bus.getName());
        H.put("Revision", bus.getRevision());
        H.put("Vault", bus.getVault());
        var id = bus.getObjectId(ctx);
        H.put("ID", id);
        H.put("Description", bus.getDescription(ctx));
        H.put("Created", bus.getCreated(ctx));
        H.put("Modified", bus.getModified(ctx));
        H.put("Policy", bus.getPolicy(ctx).getName());
        H.put("Owner", bus.getOwner(ctx).getName());
        var AL = bus.getAttributeValues(ctx);
        bus.close(ctx);

        var I = new AttributeItr(AL);
        while (I.next()) {
            var AT = I.obj();
            H.put(AT.getName(), AT.getValue());
        }
        var state = Util.execMQL(ctx, "print bus " + id + " select current dump");
        H.put("state", state);
        return H;
    }

    static HashMap getAttributeValues(Context ctx, Relationship rel) throws MatrixException {
        var H = new HashMap<String, String>();
        rel.open(ctx);
        H.put("Type", rel.getTypeName());
        H.put("ID", rel.getName());
        H.put("Fromid", rel.getFrom().getObjectId(ctx));
        H.put("Toid", rel.getTo().getObjectId(ctx));
        rel.close(ctx);
        H.put("Description", rel.getDescription(ctx));
        var AL = rel.getAttributeValues(ctx) ;
        var I = new AttributeItr(AL);
        while (I.next()) {
            var AT = I.obj();
            H.put(AT.getName(), AT.getValue());
        }
        return H;
    }

    static String getEnvironmentVariable(Context ctx, String var) {
        try {
            if (Character.isUpperCase(var.charAt(0)))
                return Environment.getValue(ctx, var);
            else {
                if (systemprops == null) {
                    var props = (Properties) JPO.invoke(ctx, "EnoBrowserJPO", null, "getProperties", null, Properties.class);
                    systemprops = props.toString().replace(", ", "\n").replace("{", "").replace("}", "");
                }
                var a = systemprops.indexOf(var + "=");
                var b = systemprops.indexOf("\n", a);
                var s = systemprops.substring(a + var.length() + 1, b);
                return s;
            }
        } catch (MatrixException ex) {
            Util.displayError("Environment variable '" + var + "' not set");
            return "";
        }
    }

    static String getFiles(Context ctx, String ID) throws MatrixException {
        var result = "";
        var bus = new BusinessObject(ID);
        var foLi = bus.getFormats(ctx);
        var foIt = new FormatItr(foLi);
        while (foIt.next()) {
            var fo = foIt.obj();
            var fiLi = bus.getFiles(ctx, fo.getName());
            var fiIt = new FileItr(fiLi);
            while (fiIt.next()) {
                var fi = fiIt.obj();
                result += ",\n" + fo.getName() + ": " + fi.getName();
            }
        }
        if (result.length() > 1)
            result = result.substring(2);
        return result;
    }

    static String getFiles2(Context ctx, String ID) throws MatrixException {

        final long KB = 1024, MB = KB * 1024, GB = MB * 1024, TB = GB * 1024;

        var result = Util.execMQL(ctx, "print bus " + ID + " select format.file.format format.file.name format.file.size dump");
        if (result.equals(",,")) {
            return "";
        }

        var a = result.split(",");
        var n = a.length / 3;
        result = "";
        for (var i = 0; i < n; i++) {

            String format = "Error file format";
            String name = "Error file name";
            String size = "Error file size 1";
            String sSize = "Error file size 2";

            try {
                format = a[i];
                name = a[n + i];
                size = a[2 * n + i];
                double dSize = Double.valueOf(size);

                if (dSize > TB) {
                    sSize = String.format("%.1f TB", dSize / TB);
                } else if (dSize > GB) {
                    sSize = String.format("%.1f GB", dSize / GB);
                } else if (dSize > MB) {
                    sSize = String.format("%.1f MB", dSize / MB);
                } else if (dSize > KB) {
                    sSize = String.format("%.1f KB", dSize / KB);
                } else {
                    sSize = String.format("%s B", size);
                }
            } catch (Exception e) {
                logger.error("Error in determination of the file: ", e);
            } finally {
                result += " \n" + format + ": " + name + " (" + sSize + ")";
            }
        }

        if (result.length() > 1) {
            result = result.substring(2);
        }

        return result;
    }

    static String getFileSeparator(Context ctx) {
        return getEnvironmentVariable(ctx, "file.separator");
    }


    static TreeMap<String, String> getInterfaceAttributes(Context ctx, String typerelationship, String what, boolean autoAddedOnly) throws MatrixException {
        String result, where;
        if (autoAddedOnly) {
            where = "(" + what + " == '" + typerelationship + "' || " + what + ".derivative == '" + typerelationship + "') && property[IPML.Automatic].value == Yes";
            result = Util.execMQL(ctx, "list interface * where \"" + where + "\" select attribute.owner attribute dump");
        } else {
            where = "(" + what + " == '" + typerelationship + "' || " + what + ".derivative == '" + typerelationship + "')";
            result = Util.execMQL(ctx, "list interface * where \"" + where + "\" select attribute.owner attribute dump");
        }
        var attributes = new TreeMap<String, String>();
        if (!result.isEmpty()) {
            var a = result.split("\n");
            for (var a1 : a) {
                if (a1.isEmpty()) {
                    continue;
                }
                var b = a1.split(",");
                var n = b.length / 2;
                for (var k = 0; k < n; k++) {
                    if (! attributes.containsKey(b[n + k]))
                        attributes.put(b[n + k], b[k]);
                }
            }
        }
        return attributes;
    }
    

    static String getInterfacesHavingAttributes(Context ctx, String typerelationship, String what, boolean autoAddedOnly) throws MatrixException {
        String result, where;
        if (autoAddedOnly) {
            where = "(" + what + " == '" + typerelationship + "' || " + what + ".derivative == '" + typerelationship + "') && property[IPML.Automatic].value == Yes";
            result = Util.execMQL(ctx, "list interface * where \"" + where + "\" select name attribute dump");
        } else {
            where = "(" + what + " == '" + typerelationship + "' || " + what + ".derivative == '" + typerelationship + "')";
            result = Util.execMQL(ctx, "list interface * where \"" + where + "\" select name attribute dump");
        }
        var a = result.split("\n");
        result = "";
        for (var a1 : a) {
            var n = a1.indexOf(',');
            if (n > 0) {
                result += "\n" + a1.substring(0, n);
            }
        }
        if (! result.isEmpty())
            result = result.substring(1);
        return result;
    }

    static String getMatrixLogDir(Context ctx) {
        return getEnvironmentVariable(ctx, "MX_TRACE_FILE_PATH");
    }

    static String getMatrixXmlDir(Context ctx) {
        var s = getEnvironmentVariable(ctx, "MATRIXPATH");
        s = s.substring(0, s.length() - 8) + "xml";
        return s;
    }

    static String getOperatingSystemName(Context ctx) {
        return getEnvironmentVariable(ctx, "os.name");
    }

    static String getParentRelationships(Context ctx, String relationship, boolean includeStartRelationship) throws MatrixException {
        String result;
        var parentRelationships = "";
        if (includeStartRelationship)
            parentRelationships = relationship;
        do {
            result = Util.execMQL(ctx, "print relationship '" + relationship + "' select derived dump");
            if (!result.isEmpty())
                parentRelationships += "," + result;
            relationship = result;
        } while (!result.isEmpty());
        if (parentRelationships.length() > 0 && parentRelationships.charAt(0) == ',')
            parentRelationships = parentRelationships.substring(1);
        return parentRelationships;
    }

    static String getParentTypes(Context ctx, String type, boolean includeStartType) throws MatrixException {
        String result;
        var parentTypes = "";
        if (includeStartType)
            parentTypes = type;
        do {
            result = Util.execMQL(ctx, "print type '" + type + "' select derived dump");
            if (!result.isEmpty())
                parentTypes += "," + result;
            type = result;
        } while (!result.isEmpty());
        if (parentTypes.length() > 0 && parentTypes.charAt(0) == ',')
            parentTypes = parentTypes.substring(1);
        return parentTypes;
    }

    static String getRootDir(Context ctx) {
        var s = getEnvironmentVariable(ctx, "MX_SERVICE_PATH");
        if (s.isEmpty()) s = getEnvironmentVariable(ctx, "DB_BROWSER_ROOTDIR");
        if (s.indexOf("WEB-INF") > 0) s = s.substring(0, s.indexOf("WEB-INF") - 1);
        return s;
    }

    public static String getSuperType(Context ctx, String type, String relationship) throws MatrixException {
        if (type != null)
            return Util.execMQL(ctx, "print type '" + type + "' select kindof dump");
        if (relationship != null)
            return Util.execMQL(ctx, "print relationship '" + relationship + "' select kindof dump");
        return "";
    }


    static String getTmpDir(Context ctx) {
        return getEnvironmentVariable(ctx, "TMPDIR");
    }

    static String getTNR(Context ctx, String busID) throws MatrixException {
        var bus = new BusinessObject(busID);
        bus.open(ctx);
        var tnr = bus.getTypeName() + " " + bus.getName() + " rev " + bus.getRevision();
        bus.close(ctx);
        return tnr;
    }

    static String getTomcatLogDir(Context ctx) {
        var s = getEnvironmentVariable(ctx, "user.dir");
        if (s.endsWith("bin"))
            s = s.substring(0, s.length() - 3) + "logs";
        else
            s = s + getFileSeparator(ctx) + "logs";
        return s;
    }

    public static void handleException(Exception ex) {
        trace(ex);
        var msg = ex.getLocalizedMessage();
        displayError(msg);
    }

    public static void handleMatrixException(Exception ex) {
        trace(ex);
        var msg = ex.getLocalizedMessage();
        if (msg == null)
            msg = "null";
        if (msg.contains("XML: Expected") || msg.contains("Exception: Expected") || msg.contains("XML: -1")) {
            msg = "Enovia reported the following error: " + msg
                    + "\nThis can be due to wrong library versions."
                    + "\nTake these 5 files"
                    + "\n• eMatrixServletRMI.jar"
                    + "\n• enoviaKernel.jar"
                    + "\n• FcsClient.jar"
                    + "\n• m1jsystem.jar"
                    + "\n• mx_jdom_1.0.jar"
                    + "\nfrom your Enovia server and copy them into the EnoBrowser\\lib folder."
                    + "\nYou find them in C:\\Tomcat...\\...\\WEB-INF\\lib on your Enovia server.";
        }
        displayError(msg);
    }

    public static boolean isEncrypted(String s) {
        return s.length() > 31 && Pattern.matches("[0-9A-F]+", s);
    }   

    static String preparePattern(String pattern) {
        pattern = pattern.replace(".", "\\.");
        pattern = pattern.replace("?", ".");
        pattern = pattern.replace("*", ".*");
        return pattern;
    }

    static String maskRegexSpecialChars(String s) {
        String[] chars = {"\\", "*", "+", "?", ".", "^", "$", "|", "[", "]", "{", "}"};
        for (var char1 : chars) {
            s = s.replace(char1, "\\" + char1);
        }
        return s;
    }

    static String replaceNotAllowedFileChars(String s) {
        return s.replace("\\", "_").replace("/", "_").replace(":", "_").replace("*", "_").replace("?", "_").replace("\"", "_").replace("<", "_").replace(">", "_").replace("|", "_");
    }



    static String[] setArray(String... args) {
        return args;
    }



    public static void trace(Exception e) {
        
        logger.error("ERROR:", e);
    }

    public static void trace(String s) {
        var where = "";
        if (Thread.currentThread().getStackTrace().length > 4) {
            var fullClassName = Thread.currentThread().getStackTrace()[4].getClassName();
            var className = fullClassName.substring(fullClassName.lastIndexOf(".") + 1);
            var methodName = Thread.currentThread().getStackTrace()[4].getMethodName();
            var lineNumber = Thread.currentThread().getStackTrace()[4].getLineNumber();
            where = ", " + className + "." + methodName + "(), line " + lineNumber;
        }
        var when = df.format(new Date());
        
        logger.error(when + where + "\n" + s);
    }

    public static String resolverUrl(String host, String relaventPath) throws URISyntaxException {
        // Parse the host URL
        URI hostUri = new URI(host);

        // Extract the path and query components
        String path = hostUri.getPath();
        String query = hostUri.getQuery();

        // Append the kernelServlet path to the extracted path
        String newPath = path + relaventPath;

        // Reconstruct the URL with the new path and the original query
        URI resultUri = new URI(
                hostUri.getScheme(),
                hostUri.getAuthority(),
                newPath,
                query,
                null
        );

        // Convert the resolved URI back to a string
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

        // Return null if the parameter is not found
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
