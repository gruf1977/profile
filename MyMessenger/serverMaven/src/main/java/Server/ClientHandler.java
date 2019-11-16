
package Server;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.sql.SQLException;
import java.util.ArrayList;
import static Server.AuthService.isNickInBd;
import static Server.Server.clients;

public class ClientHandler {
    private static final Logger LOGGER = LogManager.getLogger(ClientHandler.class);
    private Socket socket;
    private DataOutputStream out;
    private DataInputStream in;
    private Server server;
    private String str;
    private ArrayList blacklist;
    private String nick;

    public String getNick() {
        return this.nick;
    }
    
    private boolean isInChat(String str){
        Boolean res = false;
        for (ClientHandler o: clients) {
            if (o.nick.equals(str)){
                res = true;
                break;
            }
        }
        return res;
    }
    
    public ClientHandler(final Server server, Socket socket){
        try {
            this.socket = socket;
            this.server = server;
            this.in = new DataInputStream(socket.getInputStream());
            this.out = new DataOutputStream(socket.getOutputStream());
            if (Server.COUNT_CLIENTS <= clients.size()) {
                sendMsg("/serverMaxConnect");
                LOGGER.warn("/serverMaxConnect");
                in.close();
                out.close();
                socket.close();
            } else {
                Server.pullclients.execute(
                        new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    while (true) {
                                        str = in.readUTF();
                                        if (str.startsWith("/auth")) {
                                            LOGGER.info("Message from "+nick+" /auth");
                                            String[] tokens = str.split(" ");
                                            String newNick = null;
                                            newNick = AuthService.getNickByLoginByPasswd(tokens[1], tokens[2]);
                                            if (isInChat(newNick)) {
                                                newNick = null;
                                            }
                                            if (newNick != null) {
                                                nick = newNick;
                                                sendMsg("/authok " + nick);
                                                LOGGER.info("/authok " + nick);
                                                server.subscribe(ClientHandler.this);
                                                LOGGER.info("Message to "+nick +" Welcome " + nick);
                                                sendMsg("/sysinfo Welcome " + nick);
                                                sendMsg("/sysinfo /info - справочник команд");
                                                blacklist = new ArrayList();
                                                setBlacklist();
                                                LOGGER.info("Message to "+nick +" Клиент " + nick + " подключился");
                                                break;
                                            } else {
                                                LOGGER.info( "Message to "+nick +" Err: Неверный логин/пароль");
                                                sendMsg("/sysinfo Err: Неверный логин/пароль");
                                            }
                                        }
                                    }
                                    while (true) {
                                        str = in.readUTF();
                                        if (str.startsWith("/chnick")) {//сменить ник
                                            LOGGER.info("Message from "+nick +" " + str);
                                            changeNick(str);
                                        } else if (str.equals("/info")) {
                                            LOGGER.info("Message from "+nick +" " + str);
                                            info(); //вывести справочную информации
                                        } else if (str.equals("/bl")) {
                                            LOGGER.info("Message from "+nick +" " + str);
                                            readBlackList(); // прочитать черный список
                                        } else if (str.startsWith("/delbl")) { //удалить из черного списка
                                            LOGGER.info("Message from "+nick +" " + str);
                                            delBlackList(str);
                                        } else if (str.startsWith("/addbl")) { //добавить в черный список
                                            LOGGER.info("Message from "+nick +" " + str);
                                            addBlackList(str);
                                        } else if (str.equals("/server")) { //добавить в черный список
                                            LOGGER.info("Message from "+nick +" " + str);
                                            readIpAnpPortServer(str);
                                        } else if (str.startsWith("/adduser")) { //добавить в черный список
                                            LOGGER.info("Message from "+nick +" " + str);
                                            addUser(str);
                                        } else if (str.startsWith("/deluser")) { //добавить в черный список
                                            LOGGER.info("Message from "+nick +" " + str);
                                            delUser(str);
                                        } else if (str.startsWith("/chpass")) { //сменить пароль
                                            LOGGER.info("Message from "+nick +" " + str);
                                            addChangePassword(str);
                                        } else if (str.equals("/who")) { //кто на связи
                                            LOGGER.info("Message from "+nick +" " + str);
                                            whoOnline();
                                        } else if (str.equals("/kolvok")) { // количество подключеный клиентов
                                            LOGGER.info("Message from "+nick +" " + str);
                                            out.writeUTF("/sysinfo Количество клиентов на связи : " + clients.size());
                                            LOGGER.info( "Message to  "+nick +" Количество клиентов на связи : " + clients.size());
                                        } else if (str.equals("/end")) { //отключиться
                                            LOGGER.info("Message from "+nick +" " + str);
                                            out.writeUTF("/serverClosed");
                                            LOGGER.info( "Message to  "+nick +" /serverClosed");
                                            break;
                                        } else if (str.startsWith("/w")) {  //отправить личное сообщение
                                            LOGGER.info("Message from "+nick +" " + "/w ... (личное сообщение)");
                                            personalMsg(str);
                                        } else if (!str.isEmpty() && !str.startsWith("/")) {
                                            server.broadcastMsg(nick + ": " + str);
                                        }
                                    }
                                } catch (IOException | SQLException e) {
                                    //e.printStackTrace();
                                    LOGGER.warn( "Нет связи с клиентом "+nick );
                                } finally {
                                    CloseClient();
                                }
                            }
                        });
            }
        } catch (IOException e) {
            LOGGER.error(e);
        }
    }
    
    private void readIpAnpPortServer(String str) {
        try {
            sendMsg("/iPPort "+ Server.PORT + " " +InetAddress.getLocalHost().getHostAddress());
            LOGGER.info( "Message to  "+nick +" /iPPort "+ Server.PORT + " " +InetAddress.getLocalHost().getHostAddress());
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }
    
    private void delUser(String str) {
        if (this.getNick().equals("Admin")) {
            String[] strmes = str.split(" ", 2);
            if (strmes.length < 2) {
                sendMsg("/sysinfo Err: неверное значение /deluser ");
                LOGGER.info("Message to "+nick +" Err: неверное значение /deluser ");
            } else {
                if (!strmes[1].equals("Admin") && !strmes[1].equals("admin")) {
                    if (AuthService.isNickInBd(strmes[1])) {
                        Boolean res = AuthService.delNickFromBd(strmes[1]);
                        if (res) {
                            sendMsg("/sysinfo Err: " + strmes[1] + " успешно удален");
                            LOGGER.info("Message to "+nick +" Err: " + strmes[1] + " успешно удален");
                        }
                    } else {
                        sendMsg("/sysinfo Err: " + strmes[1] + " не зарегистрирован");
                        LOGGER.info("Message to "+nick +" Err: " + strmes[1] + " не зарегистрирован");
                    }
                } else {sendMsg("/sysinfo Err: " + strmes[1] + " нельзя удалять");
                    LOGGER.info("Message to "+nick +" Err: " + strmes[1] + " нельзя удалять");
                }
            }
        } else {
            sendMsg("/sysinfo Err: Только Admin может удалять пользователей ");
            LOGGER.info("Message to "+nick +" Err: Только Admin может удалять пользователей ");
        }
    }
    
    private void addUser(String str) {
        String[] strmes = str.split(" ", 2);
        if (strmes.length < 2) {
            sendMsg("/sysinfo Err: неверное значение /adduser ");
            LOGGER.info("Message to "+nick +" Err: неверное значение /adduser ");
        } else {
            //Если в базе нет пользователя то создаем его
            if (!AuthService.isNickInBd(strmes[1]) && !strmes[1].equals("Admin") && !strmes[1].equals("admin")){
                String pswd = strmes[1];
                if (AuthService.addClientToBD(strmes[1], pswd.hashCode(), strmes[1])) {
                    sendMsg("/sysinfo Пользователь " + strmes[1] + " успешно создан. \nПароль для входа: " + strmes[1]+ " логин " + strmes[1]);
                    LOGGER.info("Message to "+nick +" успешно создан. Пароль для входа: " + strmes[1]+ " логин " + strmes[1]);
                }
            } else {
                sendMsg("/sysinfo Err: " + strmes[1] + " уже зарегистрирован");
                LOGGER.info("Message to "+nick +" Err: " + strmes[1] + " уже зарегистрирован");
            }
        }
    }

    private void addChangePassword(String str) {
        String[] strmes = str.split(" ", 2);
        if (strmes.length < 2) {
            sendMsg("/sysinfo Err: неверное значение /chpass ");
            LOGGER.info("Message to "+nick +" Err: неверное значение /chpass ");
        } else {
            String newPasswd= strmes[1].replaceAll(" ", "");
            if (newPasswd.isEmpty()) {
                sendMsg("/sysinfo Err: неверное значение /chpass ");
                LOGGER.info("Message to "+nick +" Err: неверное значение /chpass ");
            }
            else {
                boolean res = AuthService.chagePassword(this.nick, newPasswd);
                if (res){
                    sendMsg("/sysinfo Пароль успешно изменен");
                    LOGGER.info("Message to "+nick +" Пароль успешно изменен");
                }
            }
        }
    }
    
    private void changeNick(String str){
        if (!this.getNick().equals("Admin")) {
            String[] strmes = str.split(" ", 2);
            if (strmes.length < 2) {
                sendMsg("/sysinfo Err: неверное значение /chnick ");
                LOGGER.info("Message to "+nick +" Err: неверное значение /chnick ");
            } else {
                String newNick = strmes[1].replaceAll(" ", "");
                if (newNick.isEmpty() || newNick.equals("Admin") || newNick.equals("admin")) {
                    sendMsg("/sysinfo Err: неверное значение /chnick ");
                    LOGGER.info("Message to "+nick +" Err: неверное значение /chnick ");
                } else {
                    if (isNickInBd(newNick)) {
                        sendMsg("/sysinfo Err: Такой nick уже зарегистрирован (" + newNick + ")");
                        LOGGER.info("Message to "+nick +" Err: Такой nick уже зарегистрирован (" + newNick + ")");
                    } else {
                        boolean res = AuthService.chageNickInBd(this.nick, newNick);
                        if (res) {
                            this.nick = newNick;
                            sendMsg("/newNick " + newNick);
                            sendMsg("/sysinfo Ваш новый nick: (" + newNick + ")");
                            LOGGER.info("Message to "+nick  + newNick);
                            LOGGER.info("Message to "+nick +" Ваш новый nick: (" + newNick + ")");
                        }
                    }
                }
            }
        } else {
            sendMsg("/sysinfo Admin не может менять nick");
            LOGGER.info("Message to "+nick +" Admin не может менять nick");
        }
    }
    
    private void whoOnline() {
        sendMsg("/sysinfo On-line : ");
        LOGGER.info("Message to "+nick +" On-line : (список юзеров онлайн)");
        int f=0;
        for (ClientHandler o: clients) {
            sendMsg("/sysinfo " + ++f + ". " + o.getNick());
        }
    }

    private void info() {
        LOGGER.info("Message to "+nick +" /info : (справочник команд)");
        sendMsg("/sysinfo \nСписок команд: \n");
        sendMsg("/sysinfleft  /end - звершение сеанса \n");
        sendMsg("/sysinfleft  /clear - очистить экран \n");
        sendMsg("/sysinfleft  /w nick - личное сообщение nick \n");
        sendMsg("/sysinfleft  /bl - черный список \n");
        sendMsg("/sysinfleft  /addbl nick - добавить nick в черный список \n");
        sendMsg("/sysinfleft  /history показать историю сообщений \n");
        sendMsg("/sysinfleft  /adduser nick новый пользователь nick \n");
        sendMsg("/sysinfleft  /deluser nick удалить пользователя nick \n");
        sendMsg("/sysinfleft  /clearhistory - очистить историю сообщений \n");
        sendMsg("/sysinfleft  /delbl nick - удалить nick из черного списка \n");
        sendMsg("/sysinfleft  /chnick nick - изменить nick \n");
        sendMsg("/sysinfleft  /chpass password - изменить password \n");
        sendMsg("/sysinfleft  /server - ip и port сервера \n");
        sendMsg("/sysinfleft  /kolvok - количество онлайн \n");
        sendMsg("/sysinfleft  /who - кто онлайн \n");
        sendMsg("/sysinfleft  /info - справочник команд\n");
    }
    
    private void readBlackList(){
        sendMsg("/sysinfo Черный список: " + blacklist.toString());
        LOGGER.info("Message to "+nick +" Черный список: " + blacklist.toString());
    }
    
    private void delBlackList(String str) throws SQLException {
        String[] strmes = str.split(" ", 2);
        if (strmes.length < 2) {
            sendMsg("/sysinfo Err: неверное значение /delbl ");
            LOGGER.info("Message to "+nick +" Err: неверное значение /delbl ");
        } else {
            if (isNickInBd(strmes[1])) {
                if (blacklist.contains(strmes[1])) {
                    boolean res = AuthService.delBlist(this.nick, strmes[1]);
                    if (res) {
                        sendMsg("/sysinfo  " + strmes[1] + " удален из черного списка");
                        LOGGER.info("Message to "+nick+ " : " + strmes[1] + " удален из черного списка");
                        setBlacklist();
                    } else {
                        sendMsg("/sysinfo Err: Запись из черного списка не удалена");
                        LOGGER.info("Message to "+nick+ " Err: Запись из черного списка не удалена");
                    }
                } else {
                    sendMsg("/sysinfo Err: В черном списке нет " + strmes[1]);
                    LOGGER.info("Message to "+nick+ " Err: В черном списке нет " + strmes[1]);
                    readBlackList();
                }
            } else {
                sendMsg("/sysinfo Err: Нет такого пользователя : " + strmes[1]);
                LOGGER.info("Message to "+nick+ " Err: Нет такого пользователя : " + strmes[1]);
            }
        }
    }
    
    private void addBlackList(String str) throws SQLException {
        String[] strmes = str.split(" ", 2);
        if (strmes.length < 2) {
            sendMsg("/sysinfo Err: неверное значение /addbl ");
            LOGGER.info("Message to "+nick+ " Err: неверное значение /addbl ");
        }
        else {
            if (!this.nick.equals(strmes[1])) {
                if (isNickInBd(strmes[1])) {
                    if (!blacklist.contains(strmes[1])) {
                        boolean res = AuthService.addBList(this.nick, strmes[1]);
                        if (res) {
                            sendMsg("/sysinfo Добавлен в черный список : " + strmes[1]);
                            LOGGER.info("Message to "+nick+ " Добавлен в черный список : " + strmes[1]);
                            setBlacklist();
                        } else {
                            sendMsg("/sysinfo Err: Запись в черный список не добавлена");
                            LOGGER.info("Message to "+nick+ " Err: Запись в черный список не добавлена");
                        }
                    } else {
                        sendMsg("/sysinfo Err: " + strmes[1] + " уже в черном списке");
                        LOGGER.info("Message to "+nick+ " : " + strmes[1] + " уже в черном списке");
                        readBlackList();
                    }
                } else {
                    sendMsg("/sysinfo Err: Нет такого пользователя : " + strmes[1]);
                    LOGGER.info("Message to "+nick+ " Err: Нет такого пользователя : " + strmes[1]);
                }
            } else {
                sendMsg("/sysinfo Err: Запись в черный список не добавлена");
                LOGGER.info("Message to "+nick+ " Err: Запись в черный список не добавлена");
            }
        }
    }
    
    public void CloseClient(){
        try {
            in.close();
            out.close();
            out.close();
        } catch (IOException e) {
            LOGGER.warn("Нет связи с клиентом");
        } finally {
            server.deleteClient(ClientHandler.this);
        }
        Thread.interrupted();
    }
    public void sendMsg(String msg) {
        try {
            out.writeUTF(msg);
        } catch (IOException e) {
            LOGGER.error(e);
            //e.printStackTrace();
            CloseClient();
        }
    }
    
    public void setBlacklist() {
        this.blacklist = AuthService.readBlackList(this.nick);
    }
    
    private void personalMsg(String str){
        String[] strmes = str.split(" ", 3);

        if (strmes.length<3) {
            sendMsg("/sysinfo Err: неверное значение /w ");
            LOGGER.info("Message to "+nick+ " Err: неверное значение /w ");
        }
        else {
            String usernick = strmes[1];
            if (isInChat(usernick)) {
                if (!AuthService.isAutorInBLUser(nick, usernick)){
                    sendMsg(nick + ": (personal for " + usernick + "): " + strmes[2]);
                    // отправляем сообщение strmes[2] клиенту с ником strmes[1]
                    server.sendMsgNick(strmes[1], nick + " (personal) : " + strmes[2] + "\n");
                } else {
                    LOGGER.info("Message to "+nick+ " Err: Сообщение не отправлено");
                    sendMsg("/sysinfo Err: Сообщение не отправлено");
                    sendMsg("/sysinfo Err: Вы в черном списке у " + strmes[1]);
                    LOGGER.info("Message to "+nick+ " Err: Вы в черном списке у " + strmes[1]);
                }
            } else {
                sendMsg("/sysinfo Err: (" + strmes[1] + ") не подключен.");
                LOGGER.info("Message to "+nick+ " Err: (" + strmes[1] + ") не подключен.");
            }
        }
    }
}
