package db;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.moandjiezana.toml.Toml;
import org.yaml.snakeyaml.Yaml;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.FileInputStream;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.Properties;
import java.util.HashMap;

public class DbComponent<T extends IAdapter> {

    private AdapterPool pool;
    private Map<String, String> queries;
    private static final int DEFAULT_POOL_SIZE = 5;

    public DbComponent(Class<T> adapterClass, String url, String user, String password, String queriesPath) {
        this.queries = loadQueries(queriesPath);
        this.pool = AdapterPool.getInstance(adapterClass, url, user, password, DEFAULT_POOL_SIZE);
    }

    public Object query(String queryName) throws Exception {
        return query(queryName, null);
    }

    public Object query(String queryName, Map<String, Object> params) throws Exception {
        if (!queries.containsKey(queryName)) {
            throw new Exception("Query no encontrada: " + queryName);
        }
        String sql = applyParams(queries.get(queryName), params);
        IAdapter adapter = pool.acquire();
        if (adapter == null) {
            throw new Exception("No hay adapters disponibles en el pool");
        }
        try {
            return adapter.executeQuery(sql);
        } finally {
            pool.release(adapter);
        }
    }

    public Object transaction(String[] queryNames) throws Exception {
        return transaction(queryNames, null);
    }

    public Object transaction(String[] queryNames, Map<String, Object>[] params) throws Exception {
        for (String queryName : queryNames) {
            if (!queries.containsKey(queryName)) {
                throw new Exception("Query no encontrada en transaccion: " + queryName);
            }
        }
        String[] sqlQueries = new String[queryNames.length];
        for (int i = 0; i < queryNames.length; i++) {
            Map<String, Object> p = (params != null && i < params.length) ? params[i] : null;
            sqlQueries[i] = applyParams(queries.get(queryNames[i]), p);
        }
        IAdapter adapter = pool.acquire();
        if (adapter == null) {
            throw new Exception("No hay adapters disponibles en el pool");
        }
        try {
            return adapter.executeTransaction(sqlQueries);
        } finally {
            pool.release(adapter);
        }
    }

    private String applyParams(String sql, Map<String, Object> params) {
        if (params == null || params.isEmpty()) return sql;
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            String value = entry.getValue() instanceof String
                ? "'" + entry.getValue() + "'"
                : String.valueOf(entry.getValue());
            sql = sql.replace(":" + entry.getKey(), value);
        }
        return sql;
    }

    private Map<String, String> loadQueries(String filePath) {
        if (filePath.endsWith(".json"))       return loadJson(filePath);
        if (filePath.endsWith(".yaml") ||
            filePath.endsWith(".yml"))        return loadYaml(filePath);
        if (filePath.endsWith(".toml"))       return loadToml(filePath);
        if (filePath.endsWith(".properties")) return loadProperties(filePath);
        System.err.println("Formato de archivo no soportado: " + filePath);
        System.exit(1);
        return null;
    }

    private Map<String, String> loadJson(String filePath) {
        try (FileReader reader = new FileReader(filePath)) {
            Type type = new TypeToken<Map<String, String>>(){}.getType();
            return new Gson().fromJson(reader, type);
        } catch (IOException e) {
            System.err.println("Error al cargar JSON: " + e.getMessage());
            System.exit(1);
            return null;
        }
    }

    private Map<String, String> loadYaml(String filePath) {
        try (InputStream is = new FileInputStream(filePath)) {
            Yaml yaml = new Yaml();
            Map<String, Object> raw = yaml.load(is);
            Map<String, String> result = new HashMap<>();
            for (Map.Entry<String, Object> entry : raw.entrySet()) {
                result.put(entry.getKey(), entry.getValue().toString());
            }
            return result;
        } catch (IOException e) {
            System.err.println("Error al cargar YAML: " + e.getMessage());
            System.exit(1);
            return null;
        }
    }

    private Map<String, String> loadToml(String filePath) {
        try {
            Toml toml = new Toml().read(new java.io.File(filePath));
            Map<String, Object> raw = toml.toMap();
            Map<String, String> result = new HashMap<>();
            for (Map.Entry<String, Object> entry : raw.entrySet()) {
                result.put(entry.getKey(), entry.getValue().toString());
            }
            return result;
        } catch (Exception e) {
            System.err.println("Error al cargar TOML: " + e.getMessage());
            System.exit(1);
            return null;
        }
    }

    private Map<String, String> loadProperties(String filePath) {
        try (InputStream is = new FileInputStream(filePath)) {
            Properties props = new Properties();
            props.load(is);
            Map<String, String> result = new HashMap<>();
            for (String key : props.stringPropertyNames()) {
                result.put(key, props.getProperty(key));
            }
            return result;
        } catch (IOException e) {
            System.err.println("Error al cargar .properties: " + e.getMessage());
            System.exit(1);
            return null;
        }
    }

    public void shutdown() {
        pool.shutdownPool();
    }

    public int getAvailableAdapters() {
        return pool.getAvailableCount();
    }

    public boolean hasQuery(String queryName) {
        return queries.containsKey(queryName);
    }
}