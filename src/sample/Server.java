package sample;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.stage.Stage;

import javax.swing.*;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;

public class Server extends Application {
    private Socket socket;
    private ServerSocket serverSocket;
    private HashMap<Integer, ImageIcon> portAndImageIcons = new HashMap<Integer, ImageIcon>();

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        try {
            serverSocket = new ServerSocket(7800);
            startConnection();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    protected void startConnection() {
        Task<Void> communicateWithClientTask = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                while (true) {
                    socket = serverSocket.accept();
                    acceptAndSendData(socket);
                }
            }
        };
        Thread communicateWithClientThread = new Thread(communicateWithClientTask);
        communicateWithClientThread.setDaemon(true);
        communicateWithClientThread.start();
    }

    protected void acceptAndSendData(Socket clientSocket) {
        Task<Void> acceptAndSendDataTask = new Task<Void>() {
            @Override
            protected Void call() throws IOException {
                while (true) {
                    try {
                        ObjectInputStream receivedDataStreamFromClient = new ObjectInputStream(clientSocket.getInputStream());
                        ImageIcon receivedImageIcon;
                        receivedImageIcon = (ImageIcon) receivedDataStreamFromClient.readObject();
                        portAndImageIcons.put(clientSocket.getPort(), receivedImageIcon);

                        ArrayList dataToBeSentArrayList = new ArrayList();
                        dataToBeSentArrayList.add(clientSocket.getPort());
                        dataToBeSentArrayList.add(portAndImageIcons);
                        Platform.runLater(() -> {
                            try {
                                ObjectOutputStream dataToSendToClientStream;
                                dataToSendToClientStream = new ObjectOutputStream(clientSocket.getOutputStream());
                                dataToSendToClientStream.writeObject(dataToBeSentArrayList);
                                dataToSendToClientStream.flush();
                            } catch (IOException e) {
                                portAndImageIcons.remove(clientSocket.getPort());
                                try {
                                    clientSocket.close();
                                } catch (IOException ex) {
                                    ex.printStackTrace();
                                }
                                e.printStackTrace();
                            }
                        });
                    } catch (Exception e) {
                        portAndImageIcons.remove(clientSocket.getPort());
                        clientSocket.close();
                        e.printStackTrace();
                        break;
                    }
                }
                return null;
            }
        };
        Thread acceptAndSendDataThread = new Thread(acceptAndSendDataTask);
        acceptAndSendDataThread.setDaemon(true);
        acceptAndSendDataThread.start();
    }
}