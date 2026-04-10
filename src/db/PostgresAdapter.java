package db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PostgresAdapter implements IAdapter {

    private Connection connection;
    private String url;
    private String user;
    private String password;

    public PostgresAdapter(String url, String user, String password) {
        this.url = url;
        this.user = user;
        this.password = password;
    }

    @Override
    public void connect(String url, String user, String password) throws Exception {
        try {
            Class.forName("org.postgresql.Driver");
            this.connection = DriverManager.getConnection(url, user, password);
        } catch (SQLException e) {
            throw new Exception("Error al conectar a PostgreSQL: " + e.getMessage());
        }
    }

    @Override
    public void disconnect() throws Exception {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            throw new Exception("Error al desconectar de PostgreSQL: " + e.getMessage());
        }
    }

    @Override
    public Object executeQuery(String sql) throws Exception {
        Statement stmt = null;
        ResultSet rs = null;
        try {
            stmt = connection.createStatement();
            rs = stmt.executeQuery(sql);
            return parseResultSet(rs);
        } catch (SQLException e) {
            throw new Exception("Error al ejecutar query en PostgreSQL: " + e.getMessage());
        } finally {
            if (rs != null) try { rs.close(); } catch (SQLException e) { }
            if (stmt != null) try { stmt.close(); } catch (SQLException e) { }
        }
    }

    @Override
    public Object executeTransaction(String[] queries) throws Exception {
        Statement stmt = null;
        try {
            connection.setAutoCommit(false);
            stmt = connection.createStatement();
            for (String sql : queries) {
                stmt.execute(sql);
            }
            connection.commit();
            return true;
        } catch (SQLException e) {
            try {
                connection.rollback();
            } catch (SQLException rollbackEx) {
                throw new Exception("Error en rollback: " + rollbackEx.getMessage());
            }
            throw new Exception("Error en transaccion PostgreSQL: " + e.getMessage());
        } finally {
            try { connection.setAutoCommit(true); } catch (SQLException e) { }
            if (stmt != null) try { stmt.close(); } catch (SQLException e) { }
        }
    }

    @Override
    public boolean isConnected() {
        try {
            return connection != null && !connection.isClosed();
        } catch (SQLException e) {
            return false;
        }
    }

    @Override
    public void reconnect() throws Exception {
        disconnect();
        connect(url, user, password);
    }

    private List<Map<String, Object>> parseResultSet(ResultSet rs) throws SQLException {
        List<Map<String, Object>> results = new ArrayList<>();
        ResultSetMetaData meta = rs.getMetaData();
        int columnCount = meta.getColumnCount();
        while (rs.next()) {
            Map<String, Object> row = new HashMap<>();
            for (int i = 1; i <= columnCount; i++) {
                row.put(meta.getColumnName(i), rs.getObject(i));
            }
            results.add(row);
        }
        return results;
    }
}