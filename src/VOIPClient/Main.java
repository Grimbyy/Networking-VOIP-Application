package VOIPClient;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.text.Text;

public class Main extends Application {

    private static Stage mainStage;

    @FXML private static Text errorMSG;

    public static void ErrorReporter(String error) {
        errorMSG.setText(error);
    }

    @Override
    public void start(Stage primaryStage) throws Exception{
        mainStage = primaryStage;
        Parent root = FXMLLoader.load(getClass().getResource("sample.fxml"));
        mainStage.setTitle("Networks VOIP Client [100243142, ]");
        mainStage.setScene(new Scene(root, 350, 175));
        mainStage.setResizable(false);
        mainStage.show();

        //ErrorReporter("TEST");
    }


    public static void main(String[] args) {
        launch(args);
    }
}
