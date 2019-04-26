package sample;

import FileCommunicator.FileCommunicator;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.MapChangeListener;
import javafx.event.EventHandler;
import javafx.geometry.HPos;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import last.fm.lastAPI;
import org.json.simple.JSONObject;

import java.awt.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.TimeoutException;

public class Main extends Application {

    private lastAPI api = new lastAPI();
    private Button moreInfo;
    private Label author = new Label(""), title = new Label("");
    private FileCommunicator apiSettings;
    private String tmpS="";
    private GridPane root;
    private Button stopAndPlay, forward, backward, load;
    private FileChooser fileChooser = new FileChooser();
    private Stage primaryStage = new Stage();
    private File file;
    private ImageView bgImg = new ImageView();
    private boolean play = false;
    private String baseURL = "http://ws.audioscrobbler.com/2.0/";
    private String api_key = "f5e523cdf9f852a409985ca6d22c4f1d";

    private MediaPlayer mediaPlayer;

    @Override
    public void start(Stage primaryStage2) throws IOException {

        try {
            apiSettings = new FileCommunicator(new File("./res/settings/api.txt"));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        primaryStage = primaryStage2;
        fileChooser.setTitle("Open Resource File");

        root = new GridPane();
        root.setAlignment(Pos.CENTER);
        root.getStylesheets().add(getClass().getResource("style.css").toExternalForm());

        primaryStage.initStyle(StageStyle.UNDECORATED);

        primaryStage.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            if(e.getCode() == KeyCode.ESCAPE) {
                Platform.exit();
            }
        });

        stopAndPlay = new Button("\u25B6");
        forward = new Button(">");
        backward = new Button("<");
        load = new Button("Load");

        primaryStage.setTitle("Last.fm API");
        primaryStage.setScene(new Scene(root));
        primaryStage.setX(0);
        primaryStage.setY(0);

        moreInfo = new Button("\u24d8");
        moreInfo.setId("moreInfo");

        addListener();
        if(apiSettings.readFile().contains("Author:")) {
            String tmp = null;
            try {
                tmp = apiSettings.readLine(1);
            } catch (IOException e) {
                e.printStackTrace();
            }

            ArrayList<String> tmpA= new ArrayList<>(Arrays.asList(tmp.split("(?<= )")));
            tmpA.remove(0);

            for (String s : tmpA) tmpS += s;

            author.setText(tmpS);
        } else {
            author.setText("Author");
        }

        if(apiSettings.readFile().contains("Title:")) {
            tmpS = "";
            String tmp = null;
            try {
                tmp = apiSettings.readLine(2);
            } catch (IOException e) {
                e.printStackTrace();
            }
            ArrayList<String> tmpA= new ArrayList<>(Arrays.asList(tmp.split("(?<= )")));

            tmpA.remove("Title: ");

            for (String s : tmpA) tmpS += s;

            title.setText(tmpS);
        }
        else {
            title.setText("Title");
        }

        HBox control = new HBox();
        control.getChildren().addAll(backward,stopAndPlay,forward);
        control.setAlignment(Pos.CENTER);

        load.setId("load");

        HBox info = new HBox();
        info.getChildren().addAll(author,moreInfo);
        info.setAlignment(Pos.TOP_CENTER);

        title.setId("title");
        author.setId("author");

        addToRoot(title,0);
        addToRoot(info,1);
        addToRoot(control,4);
        addToRoot(load,5);

        primaryStage.show();
        primaryStage.titleProperty().bind(author.textProperty());

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                if(!author.getText().equals("")) {
                    apiSettings.writeLine("Author: " + author.getText(), false);
                } else {
                    apiSettings.writeLine("Author: None", false);
                }

                if(!title.getText().equals("")) {
                    apiSettings.writeLine("Title: " + title.getText(), true);
                } else {
                    apiSettings.writeLine("Title: None", true);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }));
    }

    private void addToRoot(Node Element, int row) {
        GridPane.setConstraints(Element, 0, row);
        GridPane.setHalignment(Element, HPos.CENTER);
        root.getChildren().add(Element);
    }

    private void addListener() {
        moreInfo.setOnAction(e -> {
            JSONObject tmp = null;
            try {
                tmp = api.authorAPI(author.getText());
            } catch (TimeoutException | InterruptedException e1) {
                e1.printStackTrace();
            }

            windows.authorPopUp.authorWindow(tmp);
        });

        load.setOnAction(e -> {
            root.setCursor(Cursor.WAIT);

            file = fileChooser.showOpenDialog(primaryStage);

            if(mediaPlayer != null)
                mediaPlayer.dispose();

            if (file != null) {
                stopAndPlay.setText("\u25B6");
                loadMP3(file);
                play = false;
            }
            root.setCursor(Cursor.DEFAULT);
        });

        stopAndPlay.setOnAction(e ->{
            if (!play) {
                startPlayer();
                stopAndPlay.setText("\u25A0");
                play = true;
            } else {
                stopPlayer();
                stopAndPlay.setText("\u25B6");
                play = false;
            }

        });

        author.textProperty().addListener((obsv, newv, old) -> {
            try {
                JSONObject apiJ = api.authorAPI(author.getText());
                Image img = new Image(last.fm.filter.filterArrray.filter("artist", "img", apiJ));
                primaryStage.setHeight(img.getHeight());
                primaryStage.setWidth(img.getWidth());
                root.setBackground(new Background(new BackgroundImage(img, BackgroundRepeat.NO_REPEAT,BackgroundRepeat.NO_REPEAT,BackgroundPosition.CENTER, BackgroundSize.DEFAULT)));
            } catch (InterruptedException | TimeoutException e) {
                e.printStackTrace();
            }
        });

        primaryStage.addEventFilter(MouseEvent.MOUSE_DRAGGED, e -> {
            primaryStage.setX(e.getX());
            primaryStage.setY(e.getY());
        });

        final Delta dragDelta = new Delta();
        root.setOnMousePressed(mouseEvent -> {
            dragDelta.x = primaryStage.getX() - mouseEvent.getScreenX();
            dragDelta.y = primaryStage.getY() - mouseEvent.getScreenY();
        });
        root.setOnMouseDragged(mouseEvent -> {
            primaryStage.setX(mouseEvent.getScreenX() + dragDelta.x);
            primaryStage.setY(mouseEvent.getScreenY() + dragDelta.y);
        });
    }

    private void loadMP3(File sound) {
        Media m = new Media(sound.toURI().toString());
        mediaPlayer = new MediaPlayer(m);
        mediaPlayer.setAutoPlay(false);

        m.getMetadata().addListener((MapChangeListener<String, Object>) change -> {
            if(change.wasAdded()) {
                handleMeta(change.getKey(), change.getValueAdded());
            }
        });
    }

    private void startPlayer()
    {
        if(mediaPlayer != null)
            mediaPlayer.play();
    }

    private void stopPlayer()
    {
        if(mediaPlayer != null)
            mediaPlayer.pause();
    }

    private void handleMeta(String key, Object value) {
        if (key.equals("artist")) {
            author.setText(value.toString());
        } else if (key.equals("title")) {
            title.setText(value.toString());
        }
    }

    public static void main(String[] args) {
        launch(args);
    }

    class Delta { double x, y; }
}
