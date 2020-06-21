package sample;

import java.io.IOException;

import java.net.ServerSocket;
import java.net.Socket;

import java.util.ArrayList;
import java.util.LinkedHashMap;

public class Server {
    public static void main(String[] args) {
        new ClientThread().start();
    }
}

class ClientThread extends Thread {
    static final int PORT = 7800;
    ServerSocket serverSocket = null;
    protected Socket socket;
    private LinkedHashMap<Integer, ArrayList> portAndUsernamesImageIconsLinkedHashMap = new LinkedHashMap<Integer, ArrayList>();
    public void run() {
        try {
            serverSocket = new ServerSocket(PORT);
        } catch (IOException e) {
            e.printStackTrace();
        }
        while (true) {
            try {
                socket = serverSocket.accept();
                DataStreamThread dataStreamThread = new DataStreamThread(socket, portAndUsernamesImageIconsLinkedHashMap);
                dataStreamThread.setDaemon(true);
                dataStreamThread.start();
                if(!dataStreamThread.isRunning) {
                    System.out.println("Client stopped");
                    portAndUsernamesImageIconsLinkedHashMap.remove(socket.getPort());
                    socket.close();
                    dataStreamThread.interrupt();
                    return;
                }
            } catch (IOException e) {
                System.out.println("Client stopped");
                System.out.println("I/O error: " + e);
                return;
            }
        }
    }
}