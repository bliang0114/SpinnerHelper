package com.bol.spinner.auth;

import com.bol.spinner.MatrixConnection;
import com.bol.spinner.MatrixContext;

public class SpinnerToken {
    public static MatrixConnection connection = null;
    public static MatrixContext context = null;

    public static MatrixContext tempConnect(String hostUrl, String user, String password, String vault) throws Exception {
        MatrixConnection connection = new MatrixConnection(hostUrl, user, password, vault, "");
        return connection.connect();
    }

    public static MatrixContext connect(String hostUrl, String user, String password, String vault) throws Exception {
        return connect(hostUrl, user, password, vault, "");
    }

    public static MatrixContext connect(String hostUrl, String user, String password, String vault, String securityContext) throws Exception {
        if (connection == null) {
            connection = new MatrixConnection(hostUrl, user, password, vault, securityContext);
            context = connection.connect();
        }
        return context;
    }

    public static MatrixContext reconnect() throws Exception {
        if (connection != null) {
            connection.disconnect(context);
            context = connection.connect();
        }
        return context;
    }

    public static void disconnect() {
        connection.disconnect(context);
        connection = null;
        context = null;
    }
}
