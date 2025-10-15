package cn.github.connector;

import cn.github.driver.MQLException;
import cn.github.driver.connection.*;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import matrix.db.*;
import matrix.util.MatrixException;
import matrix.util.StringList;

import java.io.IOException;
import java.util.*;

@Slf4j
public class MatrixCommonConnection implements MatrixConnection {
    private Context context;
    public static String systemprops;

    public MatrixCommonConnection(@NonNull Context context) {
        this.context = context;
    }

    @Override
    public MatrixStatement executeStatement(String mql) throws MQLException {
        return new MatrixCommonStatement(context, mql);
    }

    @Override
    public MatrixQueryResult queryObject(MatrixObjectQuery objectQuery, List<String> fields) throws MQLException {
        var query = getObjectQuery(objectQuery);
        var orderBy = StringList.asList("type", "name", "revision");
        var queryFields = StringList.asList(fields);
        try {
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
        } catch (MatrixException e) {
            throw new MQLException(e);
        }
    }

    @Override
    public MatrixQueryResult queryConnection(MatrixConnectionQuery connectionQuery, List<String> fields) throws MQLException {
        var query = getConnectionQuery(connectionQuery);
        var orderBy = StringList.asList("type", "id");
        var queryFields = StringList.asList(fields);
        try {
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
        } catch (MatrixException e) {
            throw new MQLException(e);
        }
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
        try {
            if (Character.isUpperCase(var.charAt(0))) {
                return Environment.getValue(context, var);
            } else {
                if (systemprops == null) {
                    Properties props = JPO.invoke(context, "EnoBrowserJPO", (String[])null, "getProperties", (String[])null, Properties.class);
                    systemprops = props.toString().replace(", ", "\n").replace("{", "").replace("}", "");
                }
                int a = systemprops.indexOf(var + "=");
                int b = systemprops.indexOf("\n", a);
                return systemprops.substring(a + var.length() + 1, b);
            }
        } catch (MatrixException e) {
            throw new MQLException(e);
        }
    }

    @Override
    public int invokeJPOMethod(String jpoName, String methodName, String[] params) throws MQLException {
        try {
            return JPO.invoke(context, jpoName, null, methodName, params);
        } catch (MatrixException e) {
            throw new MQLException(e);
        }
    }

    @Override
    public <T> T invokeJPOMethod(String jpoName, String methodName, String[] params, Class<T> clazz) throws MQLException {
        try {
            return JPO.invoke(context, jpoName, null, methodName, params, clazz);
        } catch (MatrixException e) {
            throw new MQLException(e);
        }
    }

    @Override
    public void close() throws IOException {
        try {
            this.context.shutdown();
        } catch (MatrixException e) {
            throw new IOException(e);
        }
        this.context = null;
    }
}
