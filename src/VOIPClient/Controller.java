package VOIPClient;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.scene.text.Text;

public class Controller {
    public Text errorMSG;
    @FXML private TextField ipaddy;
    public void ConnectToIP(ActionEvent actionEvent) {
        //System.out.println(ipaddy.getText());
        Thread connection = new Thread(new ConnectToUser(ipaddy.getText()));
        connection.start();
        //if (connection.connected == false) {errorMSG.setText(connection.connection_failure_reason);} else { errorMSG.setText(""); }
    }
}
