package tcpServer;

import java.net.Socket;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

//Thread that runs in the role of administration
//It's responsible for admin commands and
//Data storage in the database. Even if the database is down
//The server runs as if it doesn't exist.
public class ServerAdministrationTools extends Thread {

    // Main components: 1. A list of sockets. 2. A list of clients 3. DB source
    private List<Socket> socketList;
    private volatile static List<ChatClient> clientList = new ArrayList<>();
    private static Datasource datasource;

    public ServerAdministrationTools(List<Socket> socketList) {
        datasource = new Datasource();
        if (!datasource.open()) {
            System.out.println("DATABASE ERROR: Couldn't open datasource");
        } else {
            datasource.createTables();
        }
        this.socketList = socketList;

    }

    // Overloaded addClient method that accepts only one parameter.
    // Used in ChatClient.java
    public synchronized static boolean addClient(ChatClient client) {
        return addClient(client, datasource);
    }

    // Private addClient method that adds a client in the list and
    // adds the username in the DB.
    private static boolean addClient(ChatClient client, Datasource datasource) {

        try {
            if (datasource.insertUser(client.getUsername())) {
                clientList.add(client);
                return true;
            }
        } catch (SQLException e) {
            System.out.println("DATABASE ERROR: Can't add user to DB: " + e.getMessage());
        }
        return false;
    }

    public static boolean addSignedUser(ChatClient client, String pass) {
        try {
            if (datasource.insertIntoSignedUsers(client.getUsername(), pass)) {
                return true;
            }
        } catch (SQLException e) {
            System.out.println("DATABASE ERROR: Can't add user to DB: " + e.getMessage());
        }
        return false;
    }

    public synchronized static ChatClient getClient(String username) {
        for (ChatClient client : clientList) {
            System.out.println("getClient loop: " + client.getUsername());
            if (client.getUsername().equals(username)) {
                return client;
            }
        }
        return null;
    }

    // Removes the client from the list when he disconnects.
    // Used in the terminateConnection method in ChatClient.java
    public static void removeClient(ChatClient client) {
        clientList.remove(client);
        try {
            datasource.removeUser(client.getUsername());
        } catch (SQLException e) {
            System.out.println("DATABASE ERROR: Can't add user to DB: " + e.getMessage());
        }
    }

    // Adds a message in the database. The table's purpose is
    // to work as a chatlog. Arguments are the message and the client's name
    public static void addTextToDB(String text, ChatClient client) {
        try {
            datasource.insertMsg(text, client.getUsername());
        } catch (SQLException e) {
            System.out.println("DATABASE ERROR: Can't add message to DB: " + e.getMessage());
        }
    }

    public static User getUserFromDatabase(User user) {
        try {
            return datasource.getSignedUser(user);
        } catch (SQLException e) {
            System.out.println("Something happened in getUserFromDatabase: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    // When the thread is ran, it actively accepts commands as input.
    // Current functions:
    // 1. "socketlist" prints the list of open sockets
    // 2. "clientlist" prints the list of clients stored *LOCALY*
    // 3. "userlist" prints the list of clients stored in the DATABASE
    // 4. "logs" print the list of messages in the DB sent by users
    // 5. "kick" disconnected a user
    // 6. "signedusers" prints the list of users and their -ENCRYPED- passwords. For debug purposes
    // 7. "newsignedusers" creates a new table of users. Debug purposes again

    @Override
    public void run() {

        String command = "";
        Scanner scanner = new Scanner(System.in);
        do {
            // System.out.println("Enter string to send: ");

            command = scanner.nextLine();

            if (command.equals("socketlist")) {
                printSockets();
            } else if (command.equals("kick")) {
                System.out.println("Enter user name to kick: ");
                command = scanner.nextLine();
                kickUser(command);

            } else if (command.equals("userlist")) {
                List<User> users = datasource.queryUsers();
                for (User user : users) {
                    System.out.println("Username: " + user.getName());
                }
            } else if (command.equals("logs")) {
                List<Message> msgs = datasource.queryMessages();
                System.out.println("----------MESSAGE LOGS----------");
                for (Message msg : msgs) {
                    System.out.println(msg.getUser() + ": " + msg.getText());
                }
                System.out.println("----------END OF LOGS----------");
            } else if (command.equals("signedusers")) {
                List<User> users = datasource.querySignedUsers();
                for (User user : users) {
                    System.out.println("Signed in user: " + user.getName() + " | Password: " + user.getPass());
                }
            } else if (command.equals("newsignedusers")) {
                datasource.createSignUsersTable();
            }

        } while (!command.equals("exit"));
        scanner.close();
        System.out.println("Exiting server administration service.");
    }

    //The use is in the name
    private void kickUser(String username) {
        ChatClient user = getClient(username);
        if (user != null) {
            user.kickMyself();
            synchronized (socketList) {
                user.terminateConnection(user);
            }
        } else {
            System.out.println("User doesn't exist.");
        }
    }

    // Throws the list of sockets and prints the address
    private void printSockets() {
        System.out.println("List size is: " + socketList.size());
        for (Socket socket : this.socketList) {
            System.out.println(socket.toString());
        }
    }

    // Adds an open socket in the list. It's synchronized since other
    // threads are using the resources.
    public void addSocket(Socket socket) {
        synchronized (socketList) {
            socketList.add(socket);
        }
    }

    // Changes the password of a user upon request. If the user isn't
    // registered then it returns false
    // TODO
    public static boolean changePass(ChatClient client, String pass) {
        User user = getUserFromDatabase(new User(0, client.getName()));
        return user == null ? true : false;
    }

    /**
     * @return the socketList
     */
    public List<Socket> getSocketList() {
        return socketList;
    }

}
