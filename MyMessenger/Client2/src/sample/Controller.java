package sample;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.net.Socket;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Scanner;

public class Controller implements Initializable {
    static String filename ="connect.sys";
    private boolean connectServer = false;
    private String logonick = "";
    private boolean isAuthorised;
    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;
    static String IP_ADRES = "localhost";
    static int PORT = 50105;
    private String login;
    private String namefile;
    private List<String> historyList;
    private int countHistoryMsg =100;

    @FXML Button btnexit;
    @FXML Button btnsvernut;
    @FXML VBox VboxChat;
    @FXML ScrollPane ScrollPane1;
    @FXML Label logo;
    @FXML TextField loginfield;
    @FXML PasswordField passwordfield;
    @FXML HBox but1;
    @FXML HBox apperpannel;
    @FXML Button btn2;
    @FXML TextField textField;

    private void savePortServe(){
        File file = new File(filename);
        PrintWriter pw = null;
        try {
            pw = new PrintWriter(file);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        pw.println("PORT:" + PORT);
        pw.println("IPSERVER:" + IP_ADRES);
        pw.close();
    }

    public void closeprogram(ActionEvent actionEvent) {
        savePortServe();
        if (isAuthorised) {
            connectClose();
        }
        Stage stage = (Stage) btnexit.getScene().getWindow();
        stage.close();
    }

    public void sverprogram(ActionEvent actionEvent) {
        Stage stage = (Stage) btnsvernut.getScene().getWindow();
        stage.toBack();
    }

    public void tryToAuth(ActionEvent actionEvent) {
        try {
            connectToServer();
            if (connectServer) {
                out.writeUTF("/auth " + loginfield.getText() + " " + passwordfield.getText().hashCode());
                loginfield.clear();
                passwordfield.clear();
            }
        } catch (IOException e) {
            showMsg("/sysinfo Нет связи с сервером");
            connectClose();
        }
    }

    public void SendMsg(ActionEvent actionEvent) {
        if (textField.getText().equals("/clear")) {
            clear();
        } else

        if (textField.getText().equals("/info")){
            showHelpInfo();
        } else
        if (textField.getText().equals("/clearhistory")){
            historyList.clear();
        } else
        if (textField.getText().equals("/history")){
            showHistoryMsg();
        }
        else
        if (textField.getText().equals("/end")) {
            connectClose();
        } else {
            try {
                if (connectServer) {
                    out.writeUTF(textField.getText());
                }
            } catch (IOException e) {
                connectServer = false;
                showMsg("/sysinfo Нет связи с сервером");
                //  e.printStackTrace();
            }
        }
        textField.clear();
        textField.requestFocus();
    }

    private void showHistoryMsg() {
        int count = 0;
        for (String msg: historyList) {
            if (count >= countHistoryMsg) { break; }
            showMsg(msg);
            count++;
        }
    }

    public void SendMsg(String msg) {
        try {
            out.writeUTF(msg);
        } catch (IOException e) {
            showMsg("/sysinfo Нет связи с сервером");
            //  e.printStackTrace();
        }
    }

    private void showHelpInfo()  {
        try {
            File file = new File("help.txt");
            Scanner scanner = null;
            scanner = new Scanner(file);
            while (scanner.hasNextLine()) {
                showMsg("/sysinfleft  " + scanner.nextLine());
            }
            scanner.close();
        } catch (FileNotFoundException e) {
            SendMsg("/info");
        }
    }

    private void getpPortIpServer() {
        File file = new File(filename);
        Scanner scanner = null;
        try {
            scanner = new Scanner(file);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        String lineFromFile;
        while (scanner.hasNextLine()) {
            lineFromFile = scanner.nextLine();
            String[] strmes = lineFromFile.split(":", 2);
            if (strmes[0].startsWith("PORT")) {
                PORT = Integer.parseInt(strmes[1]);
            }
            if (strmes[0].startsWith("IPSERVER")) {
                IP_ADRES = strmes[1];
            }
        }
        scanner.close();
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        if (!isFile(filename)){
            savePortServe();
        }
        getpPortIpServer();
        showMsg("/sysinfo Введите логин/пароль");
    }

    private void showMsg(String msg) {
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                HBox hBox = new HBox();
                hBox.setPrefWidth(ScrollPane1.getWidth() - 20);
                Label label = new Label();
                label.setWrapText(true);
                if (msg.startsWith("/sysinfo ")) {
                    String[] stroka = msg.split(" ", 2);
                    label.setId("sistemlabel");
                    hBox.setAlignment(Pos.TOP_CENTER);
                    label.setText(stroka[1]);
                } else
                    if (msg.startsWith("/sysinfleft ")) {
                        String[] stroka = msg.split(" ", 2);
                        label.setId("sistemlabel");
                        hBox.setAlignment(Pos.TOP_LEFT);
                        label.setText(stroka[1]);
                    } else
                        if (msg.startsWith("/sysinfright")) {
                            String[] stroka = msg.split(" ", 2);
                            label.setId("sistemlabel");
                            hBox.setAlignment(Pos.TOP_RIGHT);
                            label.setText(stroka[1]);
                        } else
                            if (msg.startsWith(logonick + ":")) {
                                hBox.setAlignment(Pos.TOP_LEFT);
                                label.setId("sitemlabel");
                                String[] strmes = msg.split(":", 2);
                                label.setText("You : " + strmes[1]);
                                historyList.add("/histor " + label.getText());
                            } else
                                if (msg.startsWith("/histor")){
                                    String[] strmes = msg.split(" ", 2);
                                    if (msg.startsWith("You :")){
                                        hBox.setAlignment(Pos.TOP_LEFT);
                                        label.setId("sitemlabel");
                                        label.setText(strmes[1]);
                                    } else {
                                        hBox.setAlignment(Pos.TOP_LEFT);
                                        label.setText("   " + strmes[1]);
                                    }
                                } else {
                                    hBox.setAlignment(Pos.TOP_LEFT);
                                    label.setText("   " + msg);
                                    historyList.add("/histor " + label.getText());
                                }
                hBox.getChildren().add(label);
                VboxChat.getChildren().add(hBox);
                ScrollPane1.setVvalue(1);
            }
        });
    }

    public void connectToServer() {
        try {
            socket = new Socket(IP_ADRES, PORT);
            in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());
            connectServer = true;
        } catch (IOException e) {
            showMsg("/sysinfo Нет связи с сервером");
            connectServer = false;
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    if (connectServer) {
                        try {
                            String  str = in.readUTF();
                            if (str.startsWith("/authok")) {
                                String[] strlogo = str.split(" ");
                                logonick = strlogo[1];
                                setAuthorised(true);
                                namefile = ".history" + logonick + ".bin";
                                createHistoryList();
                            }
                            else
                                if (str.startsWith("/newNick")) {
                                    String[] strmes = str.split(" ", 2);
                                    deleteFile();                                    
				    logonick = strmes[1];
                                    namefile = ".history" + logonick + ".bin";
                                    Platform.runLater(() -> logo.setText("MyChat - " + logonick));
                                } else
                                    if (str.startsWith("/iPPort")) {
                                        String[] strmes = str.split(" ", 3);
                                        showMsg("/sysinfleft  PORT:"+ strmes[1] + " \n");
                                        showMsg("/sysinfleft  IPSERVER:"+ strmes[2] + " \n");
                                        IP_ADRES = strmes[2];
                                        PORT = Integer.parseInt(strmes[1]);
                                        savePortServe();
                                    } else
                                        if (str.equals("/serverMaxConnect")) {
                                            showMsg("/sysinfo Превышен лимит подключений");
                                            in.close();
                                            out.close();
                                            socket.close();
                                        } else
                                            if (str.equals("/serverClosed")) {
                                                setAuthorised(false);
                                                connectClose();
                                            } else {
                                                showMsg(str);
                                            }
                        } catch (IOException e) {
                            connectServer = false;
                            break;
                            // e.printStackTrace();
                        }
                    } else break;
                }
            }
        }).start();
    }

    private void createHistoryList() {
        historyList = new ArrayList<>();
        //если нет такого файла то, historyList - пустой
        //если нет то считываем из файла historyList
        if (isFile(namefile)) {
            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(namefile))) {
                historyList = (List<String>) ois.readObject();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    private boolean isFile(String namefile) {
        boolean res = false;
        File file = new File(namefile);
        if (file.exists()) {
            res = true;
        }
        return res;
    }

    private void deleteFile() {
        File file = new File(namefile);
        if (!isFile(namefile)) {
            file.delete();
        }
    }

    private void saveHistoriList(){
        File file = new File(namefile);
        try {
            if (!isFile(namefile)) {
                file.createNewFile();
            }
            FileOutputStream fos = new FileOutputStream(namefile);
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(historyList);
            oos.close();
            fos.close();
        } catch (IOException e) {
            showMsg("/sysinfo Err: Ошибка записи в файл");
        }
    }

    private void connectClose() {
        setAuthorised(false);
        connectServer = false;
        saveHistoriList();
        try {
            out.writeUTF("/end");
            in.close();
            out.close();
            socket.close();
        } catch (IOException e) {
            // e.printStackTrace();
        }
    }

    public void setAuthorised(boolean isAuthorised) {
        this.isAuthorised = isAuthorised;
        if (!isAuthorised) {
            apperpannel.setVisible(true);
            apperpannel.setManaged(true);
            but1.setVisible(false);
            but1.setManaged(false);
            Platform.runLater(() -> logo.setText("MyChat"));
            clear();
            showMsg("/sysinfo Введите логин/пароль");
        } else {
            apperpannel.setVisible(false);
            apperpannel.setManaged(false);
            but1.setVisible(true);
            but1.setManaged(true);
            Platform.runLater(() -> logo.setText("MyChat - " + logonick));
        }
    }

    private void clear() {
        VboxChat.getChildren().clear();
        textField.clear();
        textField.requestFocus();
    }
}

