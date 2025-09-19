package com.bol.spinner;

import matrix.db.Context;
import matrix.util.MatrixException;

public class MatrixContext {
    private boolean connected = false;
    protected Context context;

    public MatrixContext() {
    }

    protected MatrixContext(Context context) {
        setContext(context);
    }

    public boolean isConnected() {
        return connected;
    }

    public void setConnected(boolean connected) {
        this.connected = connected;
    }

    protected Context getContext() {
        return context;
    }

    protected void setContext(Context context) {
        this.context = context;
        if (this.context != null) {
            this.connected = true;
        }
    }

    public void disconnect() {
        if (this.context != null) {
            try {
                this.context.shutdown();
            } catch (MatrixException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
