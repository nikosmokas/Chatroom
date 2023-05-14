package tcpServer;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

//SQL queries to java. 
//It's a mess. You know it, I know it.
//It's SQL-injection-proof ;)
public class Datasource {

    protected static final String DB_NAME = "logs.db";
    protected static final String CONNECTION_STRING = "jdbc:sqlite:/home/nmokas/eclipse-workspace/ChatRoom/" + DB_NAME;

    protected static final String TABLE_USERS = "users";
    protected static final String COLUMN_USER_ID = "_id";
    protected static final String COLUMN_USER_NAME = "name";

    protected static final String TABLE_MESSAGES = "messages";
    protected static final String COLUMN_MSG_ID = "_id";
    protected static final String COLUMN_MSG_TEXT = "text";
    protected static final String COLUMN_MSG_USERID = "userID";

    protected static final String TABLE_SIGNEDUSERS = "signedUsers";
    protected static final String COLUMN_SIGNEDUSER_ID = "_id";
    protected static final String COLUMN_SIGNEDUSER_NAME = "name";
    protected static final String COLUMN_SIGNEDUSER_PASS = "pass";

    // SELECT users.name, messages.text FROM messages JOIN users ON messages.userID
    // = users._id ORDER BY messages._id ASC

    protected static final String QUERY_ALLMESSAGES_BYUSERS = "SELECT users.name, messages.text FROM messages JOIN users ON messages.userID = users.name ORDER BY messages._id ASC";
    // SELECT * FROM users;

    protected static final String QUERY_ALL_USERS = "SELECT * FROM " + TABLE_USERS;

    protected static final String QUERY_ALL_SIGNEDUSERS = "SELECT * FROM " + TABLE_SIGNEDUSERS;

    protected static final String QUERY_USER = "SELECT " + COLUMN_USER_ID + " FROM " + TABLE_USERS + " WHERE "
            + COLUMN_USER_NAME + " = ?";
    private PreparedStatement queryUser;

    protected static final String QUERY_SIGNEDUSER = "SELECT * FROM " + TABLE_SIGNEDUSERS + " WHERE "
            + COLUMN_SIGNEDUSER_NAME + " = ?";
    private PreparedStatement querySignedUser;

    protected static final String INSERT_USER = "INSERT INTO " + TABLE_USERS + "(" + COLUMN_USER_NAME + ")"
            + " VALUES (?)";
    private PreparedStatement insertIntoUsers;

    protected static final String INSERT_SIGNEDUSER = "INSERT INTO " + TABLE_SIGNEDUSERS + "(" + COLUMN_SIGNEDUSER_NAME
            + ", " + COLUMN_SIGNEDUSER_PASS + ")" + " VALUES (?, ?)";
    private PreparedStatement insertSignedUser;

    protected static final String INSERT_MSG = "INSERT INTO " + TABLE_MESSAGES + "(" + COLUMN_MSG_TEXT + ", "
            + COLUMN_MSG_USERID + ")" + " VALUES (?, ?)";
    private PreparedStatement insertIntoMsgs;

    protected static final String REMOVE_USER = "DELETE FROM " + TABLE_USERS + " WHERE " + COLUMN_USER_NAME + " = ?";
    private PreparedStatement removeFromUsers;

    private Connection conn;

    protected boolean open() {
        try {
            conn = DriverManager.getConnection(CONNECTION_STRING);

            insertIntoUsers = conn.prepareStatement(INSERT_USER, Statement.RETURN_GENERATED_KEYS);
            insertIntoMsgs = conn.prepareStatement(INSERT_MSG, Statement.RETURN_GENERATED_KEYS);

            insertSignedUser = conn.prepareStatement(INSERT_SIGNEDUSER, Statement.RETURN_GENERATED_KEYS);
            removeFromUsers = conn.prepareStatement(REMOVE_USER, Statement.RETURN_GENERATED_KEYS);

            querySignedUser = conn.prepareStatement(QUERY_SIGNEDUSER);
            queryUser = conn.prepareStatement(QUERY_USER);
            return true;
        } catch (SQLException e) {
            System.out.println("Couldn't connect to the database: " + e.getMessage());
            return false;
        }
    }

    protected void close() {
        try {
            if (conn != null) {
                conn.close();
            }
            if (insertIntoMsgs != null) {
                insertIntoMsgs.close();
            }
            if (insertIntoUsers != null) {
                insertIntoUsers.close();
            }
            if (queryUser != null) {
                queryUser.close();
            }
            if (removeFromUsers != null) {
                removeFromUsers.close();
            }
            if (insertSignedUser != null) {
                insertSignedUser.close();
            }
            if (querySignedUser != null) {
                querySignedUser.close();
            }
        } catch (SQLException e) {
            System.out.println("Couldn't close connection: " + e.getMessage());
        }
    }

    protected List<Message> queryMessages() {
        StringBuilder sb = new StringBuilder(QUERY_ALLMESSAGES_BYUSERS);
        try (Statement statement = conn.createStatement(); ResultSet results = statement.executeQuery(sb.toString())) {
            List<Message> messages = new ArrayList<>();
            while (results.next()) {
                Message msg = new Message(results.getString(1), results.getString(2));
                messages.add(msg);
            }

            return messages;

        } catch (SQLException e) {
            System.out.println("queryMessages() failed: " + e.getMessage());
            return null;
        }
    }

    protected List<User> queryUsers() {
        StringBuilder sb = new StringBuilder(QUERY_ALL_USERS);
        try (Statement statement = conn.createStatement(); ResultSet results = statement.executeQuery(sb.toString())) {
            List<User> users = new ArrayList<>();
            while (results.next()) {
                User user = new User(results.getInt(1), results.getString(2));
                users.add(user);
            }

            return users;

        } catch (SQLException e) {
            System.out.println("queryUsers() failed: " + e.getMessage());
            return null;
        }

    }

    protected List<User> querySignedUsers() {
        StringBuilder sb = new StringBuilder(QUERY_ALL_SIGNEDUSERS);
        try (Statement statement = conn.createStatement(); ResultSet results = statement.executeQuery(sb.toString())) {
            List<User> users = new ArrayList<>();
            while (results.next()) {
                User user = new User(results.getInt(1), results.getString(2), results.getString(3));
                users.add(user);
            }
            return users;

        } catch (SQLException e) {
            System.out.println("querySignedUsers() failed: " + e.getMessage());
            return null;
        }

    }

    protected User getSignedUser(User input) throws SQLException {
        querySignedUser.setString(1, input.getName());
        ResultSet results = querySignedUser.executeQuery();
        User user = null;
        if (results.next()) {
            System.out.println(results.getInt(COLUMN_SIGNEDUSER_ID) + " " + results.getString(COLUMN_SIGNEDUSER_NAME)
                    + " " + results.getString(COLUMN_SIGNEDUSER_PASS));
            user = new User(results.getInt(COLUMN_SIGNEDUSER_ID), results.getString(COLUMN_SIGNEDUSER_NAME),
                    results.getString(COLUMN_SIGNEDUSER_PASS));
        }
        return user;
    }

    protected boolean insertUser(String name) throws SQLException {

        queryUser.setString(1, name);
        ResultSet result = queryUser.executeQuery();
        if (result.next()) {
            System.out.println("User already in DATABASE: " + result.getInt(1));
            return false;
        } else {

            insertIntoUsers.setString(1, name);
            int affectedRows = insertIntoUsers.executeUpdate();
            if (affectedRows != 1) {
                throw new SQLException("Couldn't insert into Users");
            }

            ResultSet generatedKeys = insertIntoUsers.getGeneratedKeys();
            if (generatedKeys.next()) {
                System.out.println("Added user to the DB: " + generatedKeys.getInt(1));
                return true;
            } else {
                throw new SQLException("Couldn't get ID for user");
            }
        }

    }

    protected boolean removeUser(String name) throws SQLException {
        queryUser.setString(1, name);
        ResultSet result = queryUser.executeQuery();
        if (result.next()) {
            removeFromUsers.setString(1, name);
            int affectedRows = removeFromUsers.executeUpdate();
            if (affectedRows != 1) {
                throw new SQLException("Couldn't delete user from the DB");
            }
            System.out.println("User deleted from the DATABASE: " + result.getInt(1));
            return true;
        } else {
            return false;
        }
    }

    protected boolean insertIntoSignedUsers(String name, String pass) throws SQLException {

        insertSignedUser.setString(1, name);
        // TODO encrypt the password
        insertSignedUser.setString(2, pass);
        int affectedRows = insertSignedUser.executeUpdate();

        if (affectedRows != 1) {
            throw new SQLException("Couldn't insert user into signedUser DB");
        }
        ResultSet generatedKeys = insertSignedUser.getGeneratedKeys();
        if (generatedKeys.next()) {
            return true;
        } else {
            throw new SQLException("Couldn't get ID for user");
        }
    }

    protected int insertMsg(String text, String UserId) throws SQLException {

        insertIntoMsgs.setString(1, text);
        insertIntoMsgs.setString(2, UserId);
        int affectedRows = insertIntoMsgs.executeUpdate();

        if (affectedRows != 1) {
            throw new SQLException("Couldn't insert chat into chatlog DB");
        }
        ResultSet generatedKeys = insertIntoMsgs.getGeneratedKeys();
        if (generatedKeys.next()) {
            return generatedKeys.getInt(1);
        } else {
            throw new SQLException("Couldn't get ID for chat");
        }
    }

    protected void createSignUsersTable() {
        try (Statement stmt = conn.createStatement()) {
            String sql = "DROP TABLE IF EXISTS " + TABLE_SIGNEDUSERS;
            stmt.executeUpdate(sql);
            sql = "CREATE TABLE IF NOT EXISTS " + TABLE_SIGNEDUSERS + " (" + COLUMN_SIGNEDUSER_ID
                    + " INTEGER NOT NULL PRIMARY KEY, " + COLUMN_SIGNEDUSER_NAME + ", " + COLUMN_SIGNEDUSER_PASS + ")";
            stmt.executeUpdate(sql);
            System.out.println("SignedUsers table has been created...");
        } catch (SQLException e) {
            System.out.println("DATABASE ERROR! Couldn't create signedUsers table");
        }
    }

    protected void createTables() {
        try (Statement stmt = conn.createStatement()) {
            String sql = "DROP TABLE IF EXISTS users";
            stmt.executeUpdate(sql);
        } catch (SQLException e) {
            System.out.println("DATABASE ERROR! Couldn't DELETE user table: " + e.getMessage());
        }
        try (Statement stmt = conn.createStatement()) {
            String sql = "DROP TABLE IF EXISTS messages";
            stmt.executeUpdate(sql);
        } catch (SQLException e) {
            System.out.println("DATABASE ERROR! Couldn't DELETE messages table: " + e.getMessage());
        }
        try (Statement stmt = conn.createStatement()) {
            String sql = "CREATE TABLE IF NOT EXISTS users (_id INTEGER NOT NULL PRIMARY KEY, name TEXT)";
            stmt.executeUpdate(sql);
            System.out.println("User Database has been created...");
        } catch (SQLException e) {
            System.out.println("DATABASE ERROR! Couldn't create user table: " + e.getMessage());
        }
        try (Statement stmt = conn.createStatement()) {
            String sql = "CREATE TABLE IF NOT EXISTS messages (_id INTEGER NOT NULL PRIMARY KEY, text TEXT, userID TEXT)";
            stmt.executeUpdate(sql);
            System.out.println("Message Database has been created...");
        } catch (SQLException e) {
            System.out.println("DATABASE ERROR! Couldn't create messages table: " + e.getMessage());
        }
    }

}
