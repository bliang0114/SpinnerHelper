package cn.github.connector;

import cn.github.driver.MQLException;
import cn.github.driver.connection.MatrixConnection;
import cn.github.driver.connection.MatrixSession;
import matrix.db.Context;
import matrix.util.MatrixException;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Owns the authenticated anchor; business operations use derived contexts. */
public class MatrixCommonSession extends MatrixCommonConnection implements MatrixSession {
    private final Context anchor;
    private final Set<MatrixCommonConnection> children = new HashSet<>();
    private volatile boolean closed;

    public MatrixCommonSession(Context anchor) {
        super(null);
        this.anchor = anchor;
    }

    void ensureOpen() throws MQLException {
        if (closed) {
            throw new MQLException("Matrix session is closed. Connect again before executing commands.");
        }
    }

    synchronized Context deriveContext() throws MatrixException, MQLException {
        ensureOpen();
        return new Context(anchor);
    }

    @Override
    public synchronized MatrixConnection openContext() throws MQLException {
        ensureOpen();
        MatrixCommonConnection connection = new MatrixCommonConnection(this);
        children.add(connection);
        return connection;
    }

    synchronized void release(MatrixCommonConnection connection) {
        children.remove(connection);
    }

    @Override
    public synchronized void keepAlive() throws MQLException {
        ensureOpen();
        try {
            anchor.getServerTime();
        } catch (MatrixException e) {
            throw new MQLException("Matrix session keep-alive failed.", e);
        }
    }

    @Override
    public void close() throws IOException {
        List<MatrixCommonConnection> pending;
        synchronized (this) {
            if (closed) return;
            closed = true;
            pending = List.copyOf(children);
            children.clear();
        }
        IOException failure = null;
        for (MatrixCommonConnection child : pending) {
            try {
                child.close();
            } catch (IOException e) {
                if (failure == null) failure = e;
                else failure.addSuppressed(e);
            }
        }
        try {
            super.close();
        } catch (IOException e) {
            if (failure == null) failure = e;
            else failure.addSuppressed(e);
        }
        try {
            anchor.shutdown();
        } catch (MatrixException e) {
            if (failure == null) failure = new IOException(e);
            else failure.addSuppressed(e);
        }
        if (failure != null) throw failure;
    }
}
