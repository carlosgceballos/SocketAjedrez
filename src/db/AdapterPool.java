package db;

import java.util.ArrayList;
import java.util.List;

public class AdapterPool {

    private static AdapterPool instance;
    private List<IAdapter> availableAdapters;
    private List<IAdapter> allAdapters;
    private String url;
    private String user;
    private String password;
    private int poolSize;
    private Class<? extends IAdapter> adapterClass;

    private AdapterPool(Class<? extends IAdapter> adapterClass, String url, String user, String password, int poolSize) {
        this.adapterClass = adapterClass;
        this.url = url;
        this.user = user;
        this.password = password;
        this.poolSize = poolSize;
        this.availableAdapters = new ArrayList<>();
        this.allAdapters = new ArrayList<>();
        initializePool();
    }

    public static synchronized AdapterPool getInstance(Class<? extends IAdapter> adapterClass, String url, String user, String password, int poolSize) {
        if (instance == null) {
            instance = new AdapterPool(adapterClass, url, user, password, poolSize);
        }
        return instance;
    }

    public static void reset() {
        instance = null;
    }

    private void initializePool() {
        System.out.println("Inicializando pool con " + poolSize + " conexiones de tipo " + adapterClass.getSimpleName() + "...");
        for (int i = 0; i < poolSize; i++) {
            try {
                IAdapter adapter = adapterClass.getDeclaredConstructor(
                    String.class, String.class, String.class
                ).newInstance(url, user, password);
                adapter.connect(url, user, password);
                availableAdapters.add(adapter);
                allAdapters.add(adapter);
            } catch (Exception e) {
                // Error silenciado
            }
        }
        if (availableAdapters.isEmpty()) {
            System.out.println("Pool listo: 0 conexiones disponibles (servidor no disponible).");
        } else {
            System.out.println("Pool listo: " + availableAdapters.size() + " conexiones disponibles.");
        }
    }

    public synchronized IAdapter acquire() {
        for (IAdapter adapter : availableAdapters) {
            if (adapter.isConnected()) {
                availableAdapters.remove(adapter);
                return adapter;
            } else {
                try {
                    adapter.reconnect();
                    availableAdapters.remove(adapter);
                    return adapter;
                } catch (Exception e) {
                    System.err.println("No se pudo reconectar adapter: " + e.getMessage());
                }
            }
        }
        return null;
    }

    public synchronized void release(IAdapter adapter) {
        if (adapter != null && adapter.isConnected()) {
            availableAdapters.add(adapter);
        }
    }

    public synchronized int getAvailableCount() {
        return availableAdapters.size();
    }

    public synchronized void shutdownPool() {
        System.out.println("Cerrando pool de " + adapterClass.getSimpleName() + "...");
        for (IAdapter adapter : allAdapters) {
            try {
                adapter.disconnect();
            } catch (Exception e) {
                System.err.println("Error al cerrar conexion: " + e.getMessage());
            }
        }
        availableAdapters.clear();
        allAdapters.clear();
        instance = null;
        System.out.println("Pool cerrado correctamente.");
    }
}