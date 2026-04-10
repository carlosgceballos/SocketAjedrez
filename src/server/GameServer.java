package server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class GameServer {

    private static final int PORT         = 5000;
    private static final int MAX_SESSIONS = 10; // maximo de partidas simultáneas

    private ServerSocket serverSocket;
    private BlockingQueue<Socket> waitingPlayers; // cola de jugadores esperando rival

    public GameServer() throws IOException {
        this.serverSocket   = new ServerSocket(PORT);
        this.waitingPlayers = new ArrayBlockingQueue<>(MAX_SESSIONS * 2);
    }

    public void start() {
        System.out.println("Servidor de ajedrez iniciado en puerto " + PORT);
        System.out.println("Esperando jugadores...");

        while (true) {
            try {
                Socket player = serverSocket.accept();
                System.out.println("Jugador conectado: " + player.getInetAddress());

                // Si hay alguien esperando, emparejarlos
                Socket waiting = waitingPlayers.poll();
                if (waiting != null) {
                    System.out.println("Emparejando jugadores, iniciando partida...");
                    GameSession session = new GameSession(waiting, player);
                    new Thread(session).start(); // cada partida corre en su propio hilo
                } else {
                    // No hay rival aun, poner en cola de espera
                    waitingPlayers.offer(player);
                    System.out.println("Jugador en espera de rival...");
                    sendMessage(player, "STATUS|WAITING|Esperando a un rival...");
                }

            } catch (IOException e) {
                System.err.println("Error aceptando conexion: " + e.getMessage());
            }
        }
    }

    private void sendMessage(Socket socket, String message) {
        try {
            socket.getOutputStream().write((message + "\n").getBytes());
            socket.getOutputStream().flush();
        } catch (IOException e) {
            System.err.println("Error enviando mensaje: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        try {
            new GameServer().start();
        } catch (IOException e) {
            System.err.println("No se pudo iniciar el servidor: " + e.getMessage());
        }
    }
}
