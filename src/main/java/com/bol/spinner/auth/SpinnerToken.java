package com.bol.spinner.auth;

import matrix.db.Context;
import matrix.util.MatrixException;

public class SpinnerToken {
    public static Context context;

    public static void setContext(Context ctx) {
        context = ctx;
    }

    public static void closeContext() {
        try {
            context.shutdown();
        } catch (MatrixException ex) {
            ex.printStackTrace();
        }

    }
}
