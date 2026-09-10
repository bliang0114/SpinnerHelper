package cn.github.driver.connection;

import cn.github.driver.MQLException;

/** An authenticated session. Closing it also closes all derived connections. */
public interface MatrixSession extends MatrixConnection {
    /** Creates an isolated execution context without authenticating again. */
    MatrixConnection openContext() throws MQLException;

    /** Performs a remote, read-only liveness check on the owning session. */
    void keepAlive() throws MQLException;
}
