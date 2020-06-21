package sample;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.stage.Stage;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import java.net.ServerSocket;
import java.net.Socket;

import java.util.ArrayList;
import java.util.LinkedHashMap;

public class Server extends Application {
    private Socket socket;
    private ServerSocket serverSocket;
    private LinkedHashMap<Integer, ArrayList> portAndUsernamesImageIconsLinkedHashMap = new LinkedHashMap<Integer, ArrayList>();

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
    @Override
    public void stop(){
        //
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
                        ArrayList receivedUsernameImageIconFromClientArrayList = (ArrayList) receivedDataStreamFromClient.readObject();
                        portAndUsernamesImageIconsLinkedHashMap.put(clientSocket.getPort(), receivedUsernameImageIconFromClientArrayList);

                        ArrayList dataToSendToClientArrayList = new ArrayList();
                        dataToSendToClientArrayList.add(clientSocket.getPort());
                        dataToSendToClientArrayList.add(portAndUsernamesImageIconsLinkedHashMap);
                        Platform.runLater(() -> {
                            try {
                                ObjectOutputStream dataToSendToClientStream;
                                dataToSendToClientStream = new ObjectOutputStream(clientSocket.getOutputStream());
                                dataToSendToClientStream.writeObject(dataToSendToClientArrayList);
                                dataToSendToClientStream.flush();
                            } catch (IOException e) {
                                portAndUsernamesImageIconsLinkedHashMap.remove(clientSocket.getPort());
                                try {
                                    clientSocket.close();
                                } catch (IOException ex) {
                                    ex.printStackTrace();
                                }
                                e.printStackTrace();
                            }
                        });
                    } catch (Exception e) {
                        portAndUsernamesImageIconsLinkedHashMap.remove(clientSocket.getPort());
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