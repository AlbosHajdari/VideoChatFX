package sample;

import com.github.sarxos.webcam.Webcam;
import com.sun.imageio.plugins.gif.GIFImageReader;
import com.sun.imageio.plugins.gif.GIFImageReaderSpi;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.VBox;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.ResourceBundle;
import java.util.concurrent.atomic.AtomicReference;

public class Controller implements Initializable {
    private int frameNumber = 0;
    private HashMap<Integer, ImageIcon> portAndImageIcons = new HashMap<Integer, ImageIcon>();
    private Socket socket;
    private Boolean allowsCamera = true;
    private Webcam webCamera;
    private BufferedImage thisClientImage;
    private BufferedImage toBeShownImage = null;
    @FXML
    private ImageView thisClientImageView;
    @FXML
    private VBox scrollVBoxPane;
    @FXML
    private ScrollPane scrollPane;
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        //System.out.println("STAGE HEIGHT = " + root.getHeight());
    }

    public Controller() {
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


    public void stop() throws IOException {
        socket.close();
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
                        ArrayList<ImageView> imageViewReceivedArrayList = receiveDataFromServer();

                        showData(finalClientImageToBeShown, imageViewReceivedArrayList);

                    } catch (Exception e) {
                        socket.close();
                        e.printStackTrace();
                    }
                }
            }
        };
        Thread communicatingWithServerThread = new Thread(communicatingWithServerTask);
        communicatingWithServerThread.setDaemon(true);
        communicatingWithServerThread.start();
    }

    private void showData(Image finalClientImageToBeShown, ArrayList<ImageView> imageViewReceivedArrayList) {
        scrollVBoxPane = new VBox();
        Platform.runLater(() -> {
            scrollVBoxPane.getChildren().removeAll();
            scrollVBoxPane.getChildren().addAll(imageViewReceivedArrayList);
            scrollPane.setContent(scrollVBoxPane);
            scrollPane.snapshot(new SnapshotParameters(), new WritableImage(1, 1)); //refreshes scrollPane
            thisClientImageView.setImage(finalClientImageToBeShown);
        });
    }

    private ArrayList<ImageView> receiveDataFromServer() throws IOException, ClassNotFoundException {
        ArrayList<ImageView> imageViewReceivedArrayList = new ArrayList<ImageView>();
        AtomicReference<WritableImage> utilityImageConverter;
        ImageIcon receivedImageIcon;
        ObjectInputStream receivedDataStreamFromServer = new ObjectInputStream(socket.getInputStream());
        ArrayList dataFromServerArrayList = (ArrayList) receivedDataStreamFromServer.readObject();
        Integer thisClientPort = (Integer) dataFromServerArrayList.get(0);
        portAndImageIcons = (HashMap<Integer, ImageIcon>) dataFromServerArrayList.get(1);
        Integer[] portNumbers = portAndImageIcons.keySet().toArray(new Integer[0]);

        for(int j=0; j<portNumbers.length; j++){
            if(portNumbers[j].intValue()!=thisClientPort.intValue()) {
                receivedImageIcon = portAndImageIcons.get(portNumbers[j]);
                if (receivedImageIcon != null) {
                    BufferedImage bufferedImageReceived = new BufferedImage(receivedImageIcon.getIconWidth(), receivedImageIcon.getIconHeight(), BufferedImage.TYPE_INT_RGB);
                    Graphics g = bufferedImageReceived.createGraphics();
                    receivedImageIcon.paintIcon(null, g, 0, 0);
                    g.dispose();

                    utilityImageConverter = new AtomicReference<>();
                    utilityImageConverter.set(SwingFXUtils.toFXImage(bufferedImageReceived, utilityImageConverter.get()));

                    ImageView imageViewReceived = new ImageView(utilityImageConverter.get());
                    imageViewReceivedArrayList.add(imageViewReceived);
                }
            }
        }
        return imageViewReceivedArrayList;
    }

    private void sendDataToServer(ImageIcon finalClientImageToBeSent) {
        Platform.runLater(() -> {
            ObjectOutputStream dataToSendToServerStream;
            try {
                dataToSendToServerStream = new ObjectOutputStream(socket.getOutputStream());
                dataToSendToServerStream.writeObject(finalClientImageToBeSent);
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
        File gifFile = new File("src/sample/giphy.gif");
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
            allowsCamera = false;
            webCamera.close();
        }
        else {
            allowsCamera = true;
            if(!webCamera.getLock().isLocked())
                webCamera.open();
        }
    }
}