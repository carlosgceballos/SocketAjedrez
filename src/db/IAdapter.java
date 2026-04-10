package db;

public interface IAdapter {
    void connect(String url, String user, String password) throws Exception;
    void disconnect() throws Exception;
    Object executeQuery(String sql) throws Exception;
    Object executeTransaction(String[] queries) throws Exception;
    boolean isConnected();
    void reconnect() throws Exception;
}