package cn.github.connector;

import cn.github.driver.MQLException;
import cn.github.driver.connection.*;
import lombok.extern.slf4j.Slf4j;
import matrix.db.*;
import matrix.util.MatrixException;
import matrix.util.StringList;

import java.io.File;
import java.io.IOException;
import java.util.*;

@Slf4j
public class MatrixCommonConnection implements MatrixConnection {
    private Context context;
    private Properties systemProperties;
    private final MatrixCommonSession session;
    private boolean closed;
    private final Object executionLock = new Object();

    MatrixCommonConnection(MatrixCommonSession session) {
        this.session = session;
    }

    private MatrixCommonSession owner() {
        return session == null ? (MatrixCommonSession) this : session;
    }

    @FunctionalInterface
    interface ContextOperation<T> {
        T execute(Context context) throws MatrixException, MQLException;
    }

    <T> T withContext(ContextOperation<T> operation) throws MQLException {
        synchronized (executionLock) {
            if (closed) throw new MQLException("Matrix context is closed.");
            owner().ensureOpen();
            try {
                if (context == null) context = owner().deriveContext();
                return operation.execute(context);
            } catch (MatrixException | MQLException e) {
                // Do not replay a possibly committed write. Rebuild on the next call.
                try {
                    discardContext();
                } catch (MatrixException cleanup) {
                    e.addSuppressed(cleanup);
                }
                throw e instanceof MQLException mql ? mql : new MQLException(e);
            }
        }
    }

    private void discardContext() throws MatrixException {
        Context previous = context;
        context = null;
        systemProperties = null;
        if (previous != null) previous.shutdown();
    }

    @Override
    public MatrixStatement executeStatement(String mql) {
        return () -> withContext(ctx -> {
            MatrixResultSet result = new MatrixCommonStatement(ctx, mql).executeQuery();
            if (!result.isSuccess()) discardContext();
            return result;
        });
    }

    @Override
    public MatrixQueryResult queryObject(MatrixObjectQuery objectQuery, List<String> fields) throws MQLException {
        var query = getObjectQuery(objectQuery);
        var orderBy = StringList.asList("type", "name", "revision");
        var queryFields = StringList.asList(fields);
        return withContext(context -> {
            context.start(false);
            List<Map<String, String>> data = new ArrayList<>();
            try (var iter = query.getIterator(context, queryFields, (short) 0, orderBy)) {
                for (var bws : iter) {
                    Map<String, String> map = new HashMap<>();
                    for (String field : fields) {
                        map.put(field, bws.getSelectData(field));
                    }
                    data.add(map);
                }
            }
            context.commit();
            return new MatrixQueryResult(data);
        });
    }

    @Override
    public MatrixQueryResult queryConnection(MatrixConnectionQuery connectionQuery, List<String> fields) throws MQLException {
        var query = getConnectionQuery(connectionQuery);
        var orderBy = StringList.asList("type", "id");
        var queryFields = StringList.asList(fields);
        return withContext(context -> {
            context.start(false);
            List<Map<String, String>> data = new ArrayList<>();
            try (var iter = query.getIterator(context, queryFields, (short) 0, orderBy)) {
                for (var bws : iter) {
                    Map<String, String> map = new HashMap<>();
                    for (String field : fields) {
                        map.put(field, bws.getSelectData(field));
                    }
                    data.add(map);
                }
            }
            context.commit();
            return new MatrixQueryResult(data);
        });
    }

    private static Query getObjectQuery(MatrixObjectQuery objectQuery) {
        var query = new Query();
        query.setBusinessObjectType(objectQuery.getType());
        query.setBusinessObjectName(objectQuery.getName());
        query.setBusinessObjectRevision(objectQuery.getRevision());
        query.setWhereExpression(objectQuery.getWhereExpression());
        query.setOwnerPattern(objectQuery.getOwner());
        query.setVaultPattern(objectQuery.getVault());
        query.setExpandType(objectQuery.isExpandType());
        query.setObjectLimit(objectQuery.getLimit());
        return query;
    }

    private static RelationshipQuery getConnectionQuery(MatrixConnectionQuery connectionQuery) {
        var query = new RelationshipQuery();
        query.setRelationshipType(connectionQuery.getType());
        query.setWhereExpression(connectionQuery.getWhereExpression());
        query.setVaultPattern(connectionQuery.getVault());
        query.setObjectLimit(connectionQuery.getLimit());
        return query;
    }

    @Override
    public String getEnvironmentVariable(String var) throws MQLException {
        return withContext(context -> {
            if (Character.isUpperCase(var.charAt(0))) {
                return Environment.getValue(context, var);
            } else {
                if (systemProperties == null) {
                    systemProperties = JPO.invoke(context, "EnoBrowserJPO", null, "getProperties", null, Properties.class);
                }
                return systemProperties.getProperty(var);
            }
        });
    }

    @Override
    public int invokeJPOMethod(String jpoName, String methodName, String[] params) throws MQLException {
        return withContext(context -> JPO.invoke(context, jpoName, null, methodName, params));
    }

    @Override
    public <T> T invokeJPOMethod(String jpoName, String methodName, String[] params, Class<T> clazz) throws MQLException {
        return withContext(context -> JPO.invoke(context, jpoName, null, methodName, params, clazz));
    }

    @Override
    public File downloadBusinessAttachment(String objectId, String fileName, String format) throws MQLException {
        String tempDir = System.getenv("TEMP").replace("\\", "/");
        if (tempDir.isEmpty()) {
            tempDir = System.getenv("TMP").replace("\\", "/");
        }
        tempDir += "/";
        String directory = tempDir;
        return withContext(context -> {
            FcsSupport.fcsCheckout(objectId, context, false, format, fileName, directory);
            return new File(directory + fileName);
        });
    }

    @Override
    public void close() throws IOException {
        synchronized (executionLock) {
            if (closed) return;
            closed = true;
            try {
                discardContext();
            } catch (MatrixException e) {
                throw new IOException(e);
            } finally {
                owner().release(this);
            }
        }
    }

}
