package Server;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.sql.*;
import java.util.ArrayList;

public class AuthService {
    private static final Logger LOGGER = LogManager.getLogger(AuthService.class);
    private static Connection connection;
    private static Statement stmt;
    
    public static void connect(){
        try {
            Class.forName("org.sqlite.JDBC");
            try {
                connection = DriverManager.getConnection("jdbc:sqlite:main.db");
                stmt = connection.createStatement();
            } catch (SQLException e) {
                LOGGER.error(e);
            }
        } catch (ClassNotFoundException e) {
            LOGGER.error(e);
        }
    }
    
    public static  void disconnect(){
        try {
            connection.close();
        } catch (SQLException e) {
            LOGGER.error(e);
        }
    }
    
    public static String getNickByLoginByPasswd(String login, String passwd ) {
        String qry = String.format("SELECT nickname FROM main where login='%s' and password='%s'", login, passwd);
        try {
            ResultSet rs = stmt.executeQuery(qry);
            if (rs.next()) {
                return rs.getString(1);
            }
        } catch (SQLException e) {
            LOGGER.error(e);
        }
        return null;
    }
    
    static public void initNewBDClientAndBl() {
        String qry = " SELECT COUNT(*) FROM sqlite_master WHERE type='table' AND name='main'";
        try {
            ResultSet rr  = stmt.executeQuery(qry);
            if (rr.getInt(1) == 0){
                qry = "CREATE TABLE main (id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        "login TEXT, " +
                        "password TEXT, " +
                        "nickname TEXT)";
                int rr2  = stmt.executeUpdate(qry);
            }
            qry = " SELECT COUNT(*) FROM sqlite_master WHERE type='table' AND name='blaclist'";
            ResultSet rr1  = stmt.executeQuery(qry);
            if (rr1.getInt(1) == 0){
                qry = "CREATE TABLE blaclist (blackuser TEXT, autor TEXT)";
                int rr2  = stmt.executeUpdate(qry);
            }
        } catch (SQLException e) {
            LOGGER.error(e);
        }
    }
    
    public static Boolean isNickInBd(String NickName) {
        boolean res=false;
        String qry = String.format("SELECT EXISTS(SELECT nickname FROM main WHERE nickname = '%s')", NickName);
        try {
            ResultSet rr  = stmt.executeQuery(qry);
            if (rr.getInt(1)==1){
                res = true;
            }
        } catch (SQLException e) {
            LOGGER.error(e);
        }
        return res;
    }
    
    public static Boolean isAutorInBLUser(String autor, String user) {
        boolean res=false;
        String qry = String.format("SELECT EXISTS(SELECT blackuser and autor FROM blaclist WHERE blackuser = '%s' and autor='%s')", autor, user);
        try {
            ResultSet rr  = stmt.executeQuery(qry);
            if (rr.getInt(1)==1){
                res = true;
            }
        } catch (SQLException e) {
            LOGGER.error(e);
        }
        return res;
    }
    
    public static boolean addBList(String autor, String blackuser){
        boolean res = false;
        String qry = String.format("INSERT INTO blaclist (autor, blackuser) VALUES ('%s', '%s')", autor, blackuser);
        try {
            int rs = stmt.executeUpdate(qry);
            if (rs > 0) {
                res = true;
            }
        } catch (SQLException e) {
            LOGGER.error(e);
            //  e.printStackTrace();
        }
        return res;
    }
    
    public static boolean addClientToBD(String login, int passsword, String nickname){
        boolean res = false;
        String qry = String.format("INSERT INTO main (login, password, nickname) VALUES ('%s', '%s' , '%s')", login, passsword, nickname);
        try {
            int rs  = stmt.executeUpdate(qry);
            if (rs>0){
                res= true;
            }
        } catch (SQLException e) {
            LOGGER.error(e);
            //  e.printStackTrace();
        }
        return res;
    }
    
    public static boolean delBlist(String autor, String blackuser){
        boolean res = false;
        String qry = String.format("DELETE FROM blaclist WHERE autor='%s' and blackuser='%s'", autor, blackuser);
        try {
            int rs  = stmt.executeUpdate(qry);
            if (rs>0){
                res= true;
            }
        } catch (SQLException e) {
            LOGGER.error(e);
        }
        return res;
    }
    
    public static boolean chageNickInBd(String autor, String newNick){
        boolean res = false;
        String qry = String.format("UPDATE blaclist SET blackuser='%s' WHERE blackuser='%s'", newNick, autor);
        try {
            int rs  = stmt.executeUpdate(qry);
            qry = String.format("UPDATE blaclist SET autor='%s' WHERE autor='%s'", newNick, autor);
            rs  = stmt.executeUpdate(qry);
            qry = String.format("UPDATE main SET nickname='%s' WHERE nickname='%s'", newNick, autor);
            rs  = stmt.executeUpdate(qry);
            if (rs>0){
                res= true;
            }
        } catch (SQLException e) {
            LOGGER.error(e);
        }
        return res;
    }
    
    public static ArrayList readBlackList(String autor) {
        String qry = String.format("SELECT * FROM blaclist WHERE autor='%s'", autor);
        ArrayList BlackList = new ArrayList();
        try {
            ResultSet rs = stmt.executeQuery(qry);
            while (rs.next()) {
                BlackList.add(rs.getString("blackuser"));
            }
        } catch (SQLException e) {
            LOGGER.error(e);
        }
        return BlackList;
    }
    
    public static boolean chagePassword(String nick, String newPasswd){
        boolean res = false;
        String qry = String.format("UPDATE main SET password='%s' WHERE nickname='%s'", newPasswd.hashCode(), nick);
        int rs  = 0;
        try {
            rs = stmt.executeUpdate(qry);
        } catch (SQLException e) {
            LOGGER.error(e);
        }
        if (rs>0){
            res= true;
        }
        return res;
    }
    
    public static Boolean delNickFromBd(String strme) {
        boolean res = false;
        String qry = String.format("DELETE FROM main WHERE nickname='%s'", strme);
        String qry1 = String.format("DELETE FROM blaclist WHERE autor='%s' or blackuser='%s'", strme, strme);
        int rs  = 0;
        try {
            rs = stmt.executeUpdate(qry);
            rs = rs + stmt.executeUpdate(qry1);
        } catch (SQLException e) {
            LOGGER.error(e);
            //  e.printStackTrace();
        }
        if (rs>0){
            res= true;
        }
        return res;
    }
}
