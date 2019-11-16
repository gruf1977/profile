package Server;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.sql.SQLException;
import java.util.Scanner;
import java.util.Vector;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {
    private static final Logger LOGGER = LogManager.getLogger(Server.class);
    static  Vector<ClientHandler> clients;
    static String filename ="connect.sys";
    static ExecutorService pullclients;
    static int COUNT_CLIENTS = 3; // размер пула подключений
    static int PORT=50105;
    
    public void subscribe(ClientHandler client) {
        clients.add(client);
    }
    
    public void unsubscribe(ClientHandler client) {
        clients.remove(client);
    }
    
    public  void deleteClient(ClientHandler o) {

        clients.remove(o);
        LOGGER.info("Клиент удален");
    }
    
    public Server() {
        clients = new Vector<>();
        ServerSocket server = null;
        Socket socket = null;
        AuthService.connect();
        getAdmin();
        saveIpPortCountMaxConnect();
        getpPortCountMaxConnect();
        try {
            server = new ServerSocket(PORT);
            pullclients = Executors.newFixedThreadPool(COUNT_CLIENTS);
            LOGGER.info("Сервер запущен!");
            //  System.out.println("Сервер запущен!");

            while (true) {
                socket = server.accept();
                new ClientHandler(this, socket);
            }
        } catch (IOException e) {
            LOGGER.error(e);
            //e.printStackTrace();
        }
    }

    private void getpPortCountMaxConnect() {
        File file = new File(filename);
        Scanner scanner = null; // создаем объект сканер
        try {
            scanner = new Scanner(file);
        } catch (FileNotFoundException e) {
            LOGGER.error(e);
        }
        String lineFromFile;
        while (scanner.hasNextLine()) {
            lineFromFile = scanner.nextLine();
            LOGGER.warn("системные настройки  " + lineFromFile);
            String[] strmes = lineFromFile.split(":", 2);
            if (strmes[0].startsWith("COUNT_CLIENTS")) {
                COUNT_CLIENTS = Integer.parseInt(strmes[1]);
                LOGGER.warn("Инициализация пула COUNT_CLIENTS " + strmes[1]);
            }
            if (strmes[0].startsWith("PORT")) {
                PORT = Integer.parseInt(strmes[1]);
                LOGGER.warn("Инициализация PORT " + strmes[1]);
            }
        }
        scanner.close();
    }

    private void saveIpPortCountMaxConnect() {
        File file = new File(filename);
        PrintWriter pw = null;
        try {
            pw = new PrintWriter(file);
        } catch (FileNotFoundException e) {
            LOGGER.error(e);
        }
        pw.println("PORT:" + PORT);
        pw.println("COUNTCLIENTS:" + COUNT_CLIENTS);
        try {
            pw.println("IPSERVER:" + InetAddress.getLocalHost().getHostAddress());
        } catch (UnknownHostException e) {
            LOGGER.error(e);
        }
        pw.close();
    }
    
    private boolean isFile(String namefile) {
        boolean res = false;
        File file = new File(namefile);
        if (file.exists()) {
            res = true;
        }
        return res;
    }

    private void getAdmin() {
        AuthService.initNewBDClientAndBl();
        if (!AuthService.isNickInBd("Admin")){
            System.out.println(AuthService.isNickInBd("admin"));
            String pswadmin = "admin";
            if (AuthService.addClientToBD("admin", pswadmin.hashCode(), "Admin")) {
                LOGGER.warn("Администратор в базе успешно создан");
            }
        }

    }
    
    public void broadcastMsg(String msg) {
        for (ClientHandler o: clients) {
            o.sendMsg(msg);
        }
    }
    
    public void sendMsgNick(String namenick, String msg) {
        for (ClientHandler o: clients) {
            if (o.getNick().equals(namenick))
                o.sendMsg(msg);
        }
    }
}