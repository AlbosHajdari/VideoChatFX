package sample;

import com.github.sarxos.webcam.Webcam;

import com.sun.imageio.plugins.gif.GIFImageReader;
import com.sun.imageio.plugins.gif.GIFImageReaderSpi;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;

import javax.swing.*;

import java.awt.*;
import java.awt.image.BufferedImage;

import java.io.*;

import java.net.Socket;
import java.net.URL;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.ResourceBundle;
import java.util.concurrent.atomic.AtomicReference;

public class Controller implements Initializable {
    private int frameNumber = 0;
    private Socket socket;
    private Boolean allowsCamera = true;
    private Boolean allowsMicrophone = true;
    private Webcam webCamera;
    private BufferedImage thisClientImage;
    private BufferedImage toBeShownImage = null;
    private int newNumberOfClients = 0;
    private ArrayList<ArrayList> usernameAndImageViewReceivedArrayList;
    private LinkedHashMap<Integer, Boolean> oldPortAndShowOthersImagesLinkedHashMap;
    private LinkedHashMap<Integer, Boolean> oldPortAndMuteOthersAudiosLinkedHashMap;
    private File cameraOn30File = new File("images/cameraOn30.png");
    private File cameraOff30File = new File("images/cameraOff30.png");
    private File cameraOn50File = new File("images/cameraOn50.png");
    private File cameraOff50File = new File("images/cameraOff50.png");

    private Image imageFileCameraOn30 = new Image(cameraOn30File.toURI().toString());
    private Image imageFileCameraOff30 = new Image(cameraOff30File.toURI().toString());
    private Image imageFileCameraOn50 = new Image(cameraOn50File.toURI().toString());
    private Image imageFileCameraOff50 = new Image(cameraOff50File.toURI().toString());

    private File microphoneOn30File = new File("images/microphoneOn30.png");
    private File microphoneOff30File = new File("images/microphoneOff30.png");
    private File microphoneOn50File = new File("images/microphoneOn50.png");
    private File microphoneOff50File = new File("images/microphoneOff50.png");

    private Image imageFileMicrophoneOn30 = new Image(microphoneOn30File.toURI().toString());
    private Image imageFileMicrophoneOff30 = new Image(microphoneOff30File.toURI().toString());
    private Image imageFileMicrophoneOn50 = new Image(microphoneOn50File.toURI().toString());
    private Image imageFileMicrophoneOff50 = new Image(microphoneOff50File.toURI().toString());
    private String thisClientUsername;
    @FXML
    private ImageView thisClientImageView;
    @FXML
    private VBox clientsImagesVBox;
    @FXML
    private ScrollPane clientsImagesScrollPane;
    @FXML
    private ScrollPane buttonsOnOffScrollPane;
    @FXML
    private Button cameraOnOffButton = new Button("");
    @FXML
    private Button microphoneOnOffButton = new Button("");

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        cameraOnOffButton.setPadding(new Insets(2,2,2,2));
        Image imageFileCameraOn = new Image(cameraOn50File.toURI().toString());
        cameraOnOffButton.setGraphic(new ImageView(imageFileCameraOn));

        microphoneOnOffButton.setPadding(new Insets(2,2,2,2));
        Image imageFileMicrophoneOn = new Image(microphoneOn50File.toURI().toString());
        microphoneOnOffButton.setGraphic(new ImageView(imageFileMicrophoneOn));
    }

    public Controller() {
        thisClientUsername = "Test Username2";
        oldPortAndShowOthersImagesLinkedHashMap = new LinkedHashMap<>();
        try {
            socket = new Socket("127.0.0.1", 7800);
        } catch (IOException e) {
            e.printStackTrace();
        }

        webCamera = Webcam.getDefault();
        if(!webCamera.getLock().isLocked() && allowsCamera) {
            webCamera.open();
        }
        startCommunicatingWithServer();
    }

    protected void startCommunicatingWithServer() {
        Task<Void> communicatingWithServerTask = new Task<Void>() {
            @Override
            protected Void call() throws IOException {

                Image tempClientImageToBeShown;

                while (true) {
                    try {
                        if(allowsCamera) {
                            thisClientImage = webCamera.getImage();
                            if (thisClientImage == null) {
                                thisClientImage = getGiphyImageFrame();
                                frameNumber++;
                            }
                            toBeShownImage = getScaledImage(Double.valueOf(670));

                            ImageView clientImageViewToBeShown = new ImageView(getWritableImage(toBeShownImage.getWidth(), toBeShownImage.getHeight(), true));//writableImage);
                            tempClientImageToBeShown = clientImageViewToBeShown.getImage();
                        }
                        else
                        {
                            ImageView clientImageViewToBeShown = new ImageView(getWritableImage(670, 502, false));
                            tempClientImageToBeShown = clientImageViewToBeShown.getImage();
                        }
                        Image finalClientImageToBeShown = tempClientImageToBeShown;

                        sendDataToServer(new ImageIcon(getScaledImage(Double.valueOf(320))));

                        usernameAndImageViewReceivedArrayList = receiveDataFromServer();

                        showData(finalClientImageToBeShown);
                    } catch (Exception e) {
                        socket.close();
                        e.printStackTrace();
                        break;
                    }
                }
                return null;
            }
        };
        Thread communicatingWithServerThread = new Thread(communicatingWithServerTask);
        communicatingWithServerThread.setDaemon(true);
        communicatingWithServerThread.start();
    }

    private void sendDataToServer(ImageIcon finalClientImageToBeSent) {
        Platform.runLater(() -> {
            ObjectOutputStream dataToSendToServerStream;
            try {
                dataToSendToServerStream = new ObjectOutputStream(socket.getOutputStream());
                ArrayList dataToSendToServerArrayList = new ArrayList();
                dataToSendToServerArrayList.add(thisClientUsername);
                dataToSendToServerArrayList.add(finalClientImageToBeSent);
                dataToSendToServerStream.writeObject(dataToSendToServerArrayList);
                dataToSendToServerStream.flush();
            } catch (IOException e) {
                try {
                    socket.close();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
                e.printStackTrace();
            }
        });
    }

    private ArrayList<ArrayList> receiveDataFromServer() throws IOException, ClassNotFoundException {
        usernameAndImageViewReceivedArrayList = new ArrayList<ArrayList>();
        AtomicReference<WritableImage> utilityImageConverter;
        ImageIcon receivedImageIcon;
        String receivedUsername;
        ObjectInputStream receivedDataStreamFromServer = new ObjectInputStream(socket.getInputStream());
        ArrayList receivedMainArrayListFromServer = (ArrayList) receivedDataStreamFromServer.readObject();
        Integer thisClientPort = (Integer) receivedMainArrayListFromServer.get(0);
        LinkedHashMap<Integer, ArrayList> portAndUsernameAndImageIconsLinkedHashMap = (LinkedHashMap<Integer, ArrayList>) receivedMainArrayListFromServer.get(1);
        ArrayList<Integer> portNumbers = new ArrayList<Integer>(portAndUsernameAndImageIconsLinkedHashMap.keySet());
        portNumbers.remove((Integer) thisClientPort.intValue());
        LinkedHashMap<Integer, Boolean> newPortAndShowOthersImagesLinkedHashMap = new LinkedHashMap<>();
        for(int j=0; j<portNumbers.size(); j++){
            newPortAndShowOthersImagesLinkedHashMap.put(portNumbers.get(j), oldPortAndShowOthersImagesLinkedHashMap.getOrDefault(portNumbers.get(j), false));
            receivedUsername = (String) portAndUsernameAndImageIconsLinkedHashMap.get(portNumbers.get(j)).get(0);
            receivedImageIcon = (ImageIcon) portAndUsernameAndImageIconsLinkedHashMap.get(portNumbers.get(j)).get(1);
            if (receivedImageIcon != null && receivedUsername!=null) {
                BufferedImage bufferedImageReceived = new BufferedImage(receivedImageIcon.getIconWidth(), receivedImageIcon.getIconHeight(), BufferedImage.TYPE_INT_RGB);
                Graphics g = bufferedImageReceived.createGraphics();
                receivedImageIcon.paintIcon(null, g, 0, 0);
                g.dispose();

                utilityImageConverter = new AtomicReference<>();
                utilityImageConverter.set(SwingFXUtils.toFXImage(bufferedImageReceived, utilityImageConverter.get()));

                ImageView imageViewReceived = new ImageView(utilityImageConverter.get());
                Text usernameTextReceived = new Text(receivedUsername);
                ArrayList tempArrayList = new ArrayList();
                tempArrayList.add(usernameTextReceived);
                tempArrayList.add(imageViewReceived);
                usernameAndImageViewReceivedArrayList.add(tempArrayList);
            }
        }
        oldPortAndShowOthersImagesLinkedHashMap = newPortAndShowOthersImagesLinkedHashMap;
        int oldNumberOfClients = usernameAndImageViewReceivedArrayList.size();
        if(oldNumberOfClients != newNumberOfClients){
            newNumberOfClients = oldNumberOfClients;
            addButtons(portNumbers, usernameAndImageViewReceivedArrayList);
        }
        for(int i = 0; i<portNumbers.size(); i++)
                if(oldPortAndShowOthersImagesLinkedHashMap.get(portNumbers.get(i))){
                    ImageView tempImageView = (ImageView) usernameAndImageViewReceivedArrayList.get(i).get(1);
                    int width = (int) tempImageView.getImage().getWidth();
                    int height = (int) tempImageView.getImage().getHeight();
                    WritableImage writableImage = new WritableImage(width, height);
                    PixelWriter pw = writableImage.getPixelWriter();
                    for (int x = 0; x < width; x++) {
                        for (int y = 0; y < height; y++) {
                            pw.setArgb(x, y, new Color(0,0,0).getRGB());
                        }
                    }
                    tempImageView.setImage(writableImage);
                    usernameAndImageViewReceivedArrayList.get(i).remove(1);
                    usernameAndImageViewReceivedArrayList.get(i).add(tempImageView);
                }
        return usernameAndImageViewReceivedArrayList;
    }

    private void showData(Image finalClientImageToBeShown) {
        clientsImagesVBox = new VBox();
        clientsImagesVBox.setSpacing(2);
        Platform.runLater(() -> {
            for(int j = 0; j< usernameAndImageViewReceivedArrayList.size(); j++) {
                AnchorPane anchorPane = new AnchorPane();

                Text tempUserNameText = (Text) usernameAndImageViewReceivedArrayList.get(j).get(0);
                tempUserNameText.setText(tempUserNameText.getText()+":");
                ImageView tempClientImageView = (ImageView) usernameAndImageViewReceivedArrayList.get(j).get(1);
                tempUserNameText.setLayoutY(15);
                tempUserNameText.setLayoutX(5);
                tempClientImageView.setLayoutY(20);

                anchorPane.getChildren().add(tempUserNameText);
                anchorPane.getChildren().add(tempClientImageView);
                if(j>0) anchorPane.setStyle("-fx-border-style: solid none none none;");
                clientsImagesVBox.getChildren().add(anchorPane);
            }
            clientsImagesScrollPane.setContent(clientsImagesVBox);
            clientsImagesScrollPane.snapshot(new SnapshotParameters(), new WritableImage(1, 1)); //refreshes scrollPane
            buttonsOnOffScrollPane.vvalueProperty().bindBidirectional(clientsImagesScrollPane.vvalueProperty());
            thisClientImageView.setImage(finalClientImageToBeShown);
        });
    }

    public void addButtons(ArrayList<Integer> portNumbers, ArrayList<ArrayList> usernameAndImageViewReceivedArrayList) {
        VBox clientsButtonsVBox = new VBox();
        clientsButtonsVBox.setSpacing(2);
        Platform.runLater(() -> {
            for (int i = 0; i < portNumbers.size(); i++) {
                Button switchOtherClientCameraButton = new Button("");
                Button switchOtherClientMicrophoneButton = new Button("");

                if (oldPortAndShowOthersImagesLinkedHashMap.get(portNumbers.get(i)))
                    switchOtherClientCameraButton.setGraphic(new ImageView(imageFileCameraOff30));
                else
                    switchOtherClientCameraButton.setGraphic(new ImageView(imageFileCameraOn30));

                /*if (oldPortAndMuteOthersAudiosLinkedHashMap.get(portNumbers.get(i)))
                    switchOtherClientMicrophoneButton.setGraphic(new ImageView(imageFileMicrophoneOff30));
                else*/
                    switchOtherClientMicrophoneButton.setGraphic(new ImageView(imageFileMicrophoneOn30));

                switchOtherClientCameraButton.setPadding(new Insets(2,2,2,2));
                switchOtherClientMicrophoneButton.setPadding(new Insets(2,2,2,2));
                switchOtherClientCameraButton.setLayoutY(20);
                switchOtherClientMicrophoneButton.setLayoutY(58);
                AnchorPane anchorPane = new AnchorPane();
                anchorPane.getChildren().add(switchOtherClientCameraButton);
                anchorPane.getChildren().add(switchOtherClientMicrophoneButton);
                if(i==0) anchorPane.setMinHeight(((ImageView)usernameAndImageViewReceivedArrayList.get(i).get(1)).getImage().getHeight() + 20);
                else
                    anchorPane.setMinHeight(((ImageView)usernameAndImageViewReceivedArrayList.get(i).get(1)).getImage().getHeight() + 22);
                clientsButtonsVBox.getChildren().add(anchorPane);
                int imageIndex = i;
                switchOtherClientCameraButton.setOnAction(event -> {
                    if (oldPortAndShowOthersImagesLinkedHashMap.get(portNumbers.get(imageIndex))) {
                        switchOtherClientCameraButton.setGraphic(new ImageView(imageFileCameraOn30));
                        oldPortAndShowOthersImagesLinkedHashMap.put(portNumbers.get(imageIndex), false);
                    } else {
                        switchOtherClientCameraButton.setGraphic(new ImageView(imageFileCameraOff30));
                        oldPortAndShowOthersImagesLinkedHashMap.put(portNumbers.get(imageIndex), true);
                    }
                });
            }
            buttonsOnOffScrollPane.setContent(clientsButtonsVBox);
        });
    }

    private BufferedImage getScaledImage(Double scaledWidth) {
        Double width;
        Double height;
        if(allowsCamera) {
            width = Double.valueOf(thisClientImage.getWidth());
            height = Double.valueOf(thisClientImage.getHeight());

        }
        else{
            width = Double.valueOf(320);
            height = Double.valueOf(240);
        }

        Double scaledHeight = scaledWidth * (height / width);
        BufferedImage toBeSentImage = new BufferedImage(scaledWidth.intValue(), scaledHeight.intValue(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g2dToBeSent = toBeSentImage.createGraphics();
        if(allowsCamera) {
            g2dToBeSent.drawImage(thisClientImage, scaledWidth.intValue(), 0, -scaledWidth.intValue(), scaledHeight.intValue(), null);
        }
        else {
            g2dToBeSent.setPaint(new Color(0, 0, 0));
            g2dToBeSent.fillRect(0, 0, toBeSentImage.getWidth(), toBeSentImage.getHeight());
        }
        g2dToBeSent.dispose();

        return toBeSentImage;
    }

    private WritableImage getWritableImage(Integer width, Integer height, Boolean toBeOrNotToBeShownImage){
        WritableImage writableImage = new WritableImage(width, height);
        PixelWriter pw = writableImage.getPixelWriter();
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                if(toBeOrNotToBeShownImage)
                    pw.setArgb(x, y, toBeShownImage.getRGB(x, y));
                else
                    pw.setArgb(x, y, new Color(0,0,0).getRGB());
            }
        }
        return writableImage;
    }

    private BufferedImage getGiphyImageFrame() throws IOException {
        File gifFile = new File("images/noSignalGif.gif");
        ArrayList<BufferedImage> bufferedImageArrayList = getFrames(gifFile);
        if (frameNumber == bufferedImageArrayList.size())
            frameNumber = 0;
        return bufferedImageArrayList.get(frameNumber);
    }

    public ArrayList<BufferedImage> getFrames(File gif) throws IOException{
        ArrayList<BufferedImage> frames = new ArrayList<>();
        ImageReader ir = new GIFImageReader(new GIFImageReaderSpi());
        ir.setInput(ImageIO.createImageInputStream(gif));
        for(int i = 0; i < ir.getNumImages(true); i++)
            frames.add(ir.read(i));
        return frames;
    }

    public void switchCameraOnOff(){
        if(allowsCamera) {
            cameraOnOffButton.setGraphic(new ImageView(imageFileCameraOff50));
            allowsCamera = false;
            webCamera.close();
        }
        else {
            cameraOnOffButton.setGraphic(new ImageView(imageFileCameraOn50));
            allowsCamera = true;
            if(!webCamera.getLock().isLocked())
                webCamera.open();
        }
    }

    public void switchMicrophoneOnOff(){
        if(allowsMicrophone) {
            microphoneOnOffButton.setGraphic(new ImageView(imageFileMicrophoneOff50));
            allowsMicrophone = false;
            //turn microphone off
        }
        else {
            microphoneOnOffButton.setGraphic(new ImageView(imageFileMicrophoneOn50));
            allowsMicrophone = true;
            //turn microphone on
        }
    }
}