package sample;

import com.github.sarxos.webcam.Webcam;
import com.sun.imageio.plugins.gif.GIFImageReader;
import com.sun.imageio.plugins.gif.GIFImageReaderSpi;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
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
    private Webcam webCam = null;
    @FXML
    private ImageView thisClientImageView;
    private HashMap<Integer, ImageIcon> portAndImageIcons = new HashMap<Integer, ImageIcon>();
    private Socket socket;
    @FXML
    private VBox webCamRightPanes;
    @FXML
    private ScrollPane scrollPane;
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        //System.out.println("STAGE HEIGHT = " + root.getHeight());
    }

    public Controller() {
        scrollPane = new ScrollPane();
        thisClientImageView = new ImageView();
        try {
            socket = new Socket("127.0.0.1", 7800);
        } catch (IOException e) {
            e.printStackTrace();
        }

        webCam = Webcam.getDefault();
        if(!webCam.getLock().isLocked()) {
            webCam.open();
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
                AtomicReference<WritableImage> utility;
                BufferedImage thisClientImage;
                int i = 0;
                while (true) {
                    try {
                        thisClientImage = webCam.getImage();
                        if (thisClientImage == null) {
                            File gifFile = new File("src/sample/giphy.gif");
                            ArrayList<BufferedImage> bufferedImageArrayList = getFrames(gifFile);
                            if (i == bufferedImageArrayList.size())
                                i = 0;
                            thisClientImage = bufferedImageArrayList.get(i);
                            i++;
                        }
                        Double scaledWidth = Double.valueOf(320);
                        Double width = Double.valueOf(thisClientImage.getWidth());
                        Double height = Double.valueOf(thisClientImage.getHeight());
                        Double finalHeight = scaledWidth*(height/width);

                        BufferedImage toBeSentImage = new BufferedImage(scaledWidth.intValue(),finalHeight.intValue(), BufferedImage.TYPE_INT_RGB);
                        Graphics2D g2dToBeSent = toBeSentImage.createGraphics();
                        g2dToBeSent.drawImage(thisClientImage, scaledWidth.intValue(), 0, -scaledWidth.intValue(), finalHeight.intValue(), null);
                        g2dToBeSent.dispose();

                        scaledWidth = Double.valueOf(670);
                        width = Double.valueOf(thisClientImage.getWidth());
                        height = Double.valueOf(thisClientImage.getHeight());
                        finalHeight = scaledWidth*(height/width);

                        BufferedImage toBeShownImage = new BufferedImage(scaledWidth.intValue(),finalHeight.intValue(), BufferedImage.TYPE_INT_RGB);
                        Graphics2D g2dToBeShown = toBeShownImage.createGraphics();
                        g2dToBeShown.drawImage(thisClientImage, scaledWidth.intValue(), 0, -scaledWidth.intValue(), finalHeight.intValue(), null);
                        g2dToBeShown.dispose();

                        WritableImage wr;
                        wr = new WritableImage(toBeShownImage.getWidth(), toBeShownImage.getHeight());
                        PixelWriter pw = wr.getPixelWriter();
                        for (int x = 0; x < toBeShownImage.getWidth(); x++) {
                            for (int y = 0; y < toBeShownImage.getHeight(); y++) {
                                pw.setArgb(x, y, toBeShownImage.getRGB(x, y));
                            }
                        }

                        ImageView finalClientImageViewToBeShown = new ImageView(wr);

                        Image finalClientImageToBeShown = finalClientImageViewToBeShown.getImage();
                        ImageIcon finalClientImageToBeSent = new ImageIcon(toBeSentImage);

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

                        ImageIcon receivedImageIcon;

                        ObjectInputStream receivedDataStreamFromServer = new ObjectInputStream(socket.getInputStream());
                        ArrayList dataFromServerArrayList = (ArrayList) receivedDataStreamFromServer.readObject();

                        Integer thisClientPort = (Integer) dataFromServerArrayList.get(0);
                        portAndImageIcons = (HashMap<Integer, ImageIcon>) dataFromServerArrayList.get(1);

                        Integer[] portNumbers = portAndImageIcons.keySet().toArray(new Integer[0]);
                        ArrayList<ImageView> imageViewReceivedArrayList = new ArrayList<>();

                        for(int j=0; j<portNumbers.length; j++){
                            if(portNumbers[j].intValue()!=thisClientPort.intValue()) {
                                receivedImageIcon = portAndImageIcons.get(portNumbers[j]);
                                if (receivedImageIcon != null) {
                                    BufferedImage bufferedImageReceived = new BufferedImage(receivedImageIcon.getIconWidth(), receivedImageIcon.getIconHeight(), BufferedImage.TYPE_INT_RGB);
                                    Graphics g = bufferedImageReceived.createGraphics();
                                    receivedImageIcon.paintIcon(null, g, 0, 0);
                                    g.dispose();

                                    utility = new AtomicReference<>();
                                    utility.set(SwingFXUtils.toFXImage(bufferedImageReceived, utility.get()));

                                    ImageView imageViewReceived = new ImageView(utility.get());
                                    imageViewReceivedArrayList.add(imageViewReceived);
                                }
                            }
                        }

                        webCamRightPanes = new VBox();
                        Platform.runLater(() -> {
                            webCamRightPanes.getChildren().removeAll();
                            webCamRightPanes.getChildren().addAll(imageViewReceivedArrayList);
                            scrollPane.setContent(webCamRightPanes);
                            thisClientImageView.setImage(finalClientImageToBeShown);
                        });
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

    public ArrayList<BufferedImage> getFrames(File gif) throws IOException{
        ArrayList<BufferedImage> frames = new ArrayList<>();
        ImageReader ir = new GIFImageReader(new GIFImageReaderSpi());
        ir.setInput(ImageIO.createImageInputStream(gif));
        for(int i = 0; i < ir.getNumImages(true); i++)
            frames.add(ir.read(i));
        return frames;
    }
}
