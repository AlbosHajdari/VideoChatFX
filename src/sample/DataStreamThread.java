package sample;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.LinkedHashMap;

public class DataStreamThread extends Thread{
    public Boolean isRunning = true;
    protected Socket socket;
    private LinkedHashMap<Integer, ArrayList> portAndUsernamesImageIconsLinkedHashMap = new LinkedHashMap<Integer, ArrayList>();

    public DataStreamThread(Socket clientSocket, LinkedHashMap<Integer, ArrayList>  portAndUsernamesImageIconsLinkedHashMap) {
        this.socket = clientSocket;
        this.portAndUsernamesImageIconsLinkedHashMap = portAndUsernamesImageIconsLinkedHashMap;
    }

    public void run() {
        ObjectInputStream receivedDataStreamFromClient = null;
        ObjectOutputStream dataToSendToClientStream = null;
        ArrayList receivedUsernameImageIconFromClientArrayList = new ArrayList();
        while (true) {
            try {
                receivedDataStreamFromClient = new ObjectInputStream(socket.getInputStream());
                dataToSendToClientStream = new ObjectOutputStream(socket.getOutputStream());
                receivedUsernameImageIconFromClientArrayList = (ArrayList) receivedDataStreamFromClient.readObject();
                if ((receivedUsernameImageIconFromClientArrayList == null)) {
                    portAndUsernamesImageIconsLinkedHashMap.remove(socket.getPort());
                    socket.close();
                    isRunning = false;
                    return;
                } else {
                    portAndUsernamesImageIconsLinkedHashMap.put(socket.getPort(), receivedUsernameImageIconFromClientArrayList);
                    ArrayList dataToSendToClientArrayList = new ArrayList();
                    dataToSendToClientArrayList.add(socket.getPort());
                    dataToSendToClientArrayList.add(portAndUsernamesImageIconsLinkedHashMap);
                    dataToSendToClientStream.writeObject(dataToSendToClientArrayList);
                    //dataToSendToClientStream.reset();
                    //dataToSendToClientStream.flush();
                }
            } catch (IOException | ClassNotFoundException e) {
                portAndUsernamesImageIconsLinkedHashMap.remove(socket.getPort());
                try {
                    socket.close();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
                e.printStackTrace();
                isRunning = false;
                return;
            }
        }
    }
}
