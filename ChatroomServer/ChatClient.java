package tcpServer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

/*ChatClient thead. When a client is connected, a thread is
 * generated to accept, proccess and project messages.
 * The client can choose his own username.
 */

public class ChatClient extends Thread {

    private volatile List<Socket> socketList;
    private Socket socket;
    private String username = "Unnamed";

    public ChatClient(List<Socket> socketList) {
        this.socketList = socketList;
        this.socket = socketList.get(socketList.size() - 1);
    }

    @Override
    public void run() {
        try {
            BufferedReader input = new BufferedReader(
                    new InputStreamReader(socketList.get(socketList.size() - 1).getInputStream()));
            PrintWriter output = new PrintWriter(socketList.get(socketList.size() - 1).getOutputStream(), true);

            // Gettting client's username and setting the thread's name
            // and adding the client in the lists.
            // If the username exists, he needs to change it.
            String echoString = "";
            do {
                echoString = input.readLine();
                this.setUsername(echoString);

                User user = ServerAdministrationTools.getUserFromDatabase(new User(0, username));

                if (user == null) {
                    output.println("Connected.");
                    break;
                } else {
                    System.out.println(ServerAdministrationTools.getClient(this.username));

                    if (ServerAdministrationTools.getClient(this.username) != null) {
                        output.println("Already connected. Enter another name: ");
                        continue;
                    } else {
                        output.println("Enter password: ");
                        echoString = input.readLine();
                        echoString = encryptPass(echoString);
                        if (echoString.equals(user.getPass())) {
                            output.println("Connected.");
                            break;
                        }
                    }

                }
                output.println("Wrong password. Enter your username: ");

            } while (true);
            ServerAdministrationTools.addClient(this);
            // Notifying the rest of the clients (and the server)
            // that a user has connected to the chat room
            for (int i = 0; i < socketList.size(); i++) {
                output = new PrintWriter(socketList.get(i).getOutputStream(), true);
                output.println(this.getUsername() + " has connected.");
            }
            System.out.println(this.getUsername() + " has connected.");

            // Actively accepting input from the client.
            while (true) {

                echoString = input.readLine();

                // When the user types only "exit", he disconnects
                // from the chatroom.
                if (echoString.equals("exit")) {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        System.out.println("Woke up, for some reason");
                    }
                    break;
                }
                // If msg is "signin", then the user registers to the DB
                // He enters a password that gets encrypted
                if (echoString.equals("signin")) {
                    output = new PrintWriter(this.socket.getOutputStream(), true);
                    output.println("Please enter your password: ");
                    String pass = input.readLine();
                    String encryptedPass = encryptPass(pass);
                    if (ServerAdministrationTools.addSignedUser(this, encryptedPass)) {
                        output.println("Your registration was completed");
                    } else {
                        output.println("Something went wrong with your registration!");
                    }
                    continue;
                }
                // User has the ability to change their password, if they are
                // In the database.
                // TODO Change password feature, edit DATABASE and ServerAdmin method
                if (echoString.equals("changepass")) {
                    output = new PrintWriter(this.socket.getOutputStream(), true);
                    output.println("Please enter your new password: ");
                    String pass = input.readLine();
                    String encryptedPass = encryptPass(pass);
                    if (ServerAdministrationTools.changePass(this, encryptedPass)) {
                        output.println("You have changed your password successfully");
                    } else {
                        output.println("You are not signed in. Register by typing 'signin'.");
                    }
                    continue;
                }

                // Sending the msg to the other clients
                if (echoString != null) {
                    for (int i = 0; i < socketList.size(); i++) {
                        output = new PrintWriter(socketList.get(i).getOutputStream(), true);
                        output.println(this.getUsername() + " says: " + echoString);
                    }
                }

                // Adds the msg to the database
                ServerAdministrationTools.addTextToDB(echoString, this);
                System.out.println(this.getUsername() + " says: " + echoString);

            }
            // Letting everyone know that a user has left the chat.
            for (int i = 0; i < socketList.size(); i++) {
                output = new PrintWriter(socketList.get(i).getOutputStream(), true);
                output.println(this.getUsername() + " has left the chat.");
            }
            System.out.println(this.getUsername() + " has left the chat.");

        } catch (IOException e) {
            System.out.println(this.getUsername() + " has been kicked from the room.");
        } finally {
            terminateConnection(this);
        }

    }

    public synchronized void terminateConnection(ChatClient client) {
        socketList.remove(this.socket);
        ServerAdministrationTools.removeClient(client);
        try {
            socket.close();
        } catch (IOException e) {
            System.out.println("COuldn't close socket?");
        }
    }

    private int getClientSocketPosition() {
        return socketList.indexOf(this.socket);
    }

    public synchronized void kickMyself() {

        int position = getClientSocketPosition();
        if (position != -1) {
            try {
                for (int i = 0; i < socketList.size(); i++) {
                    if (i != position) {
                        PrintWriter output = new PrintWriter(socketList.get(i).getOutputStream(), true);
                        output.println(this.getUsername() + " has been kicked from the room.");
                    } else {
                        PrintWriter output = new PrintWriter(socketList.get(i).getOutputStream(), true);
                        output.println("You have been kicked from the room.");
                    }
                }
            } catch (IOException e) {
                System.out.println("IOException in ChatClient.kickMyself()");
            }
        } else {
            System.out.println("Something went wrong in kickMyself");
        }

    }

    // Password encryption using MD5 hashing algorithm.
    private String encryptPass(String pass) {
        try {
            MessageDigest m = MessageDigest.getInstance("MD5");
            m.update(pass.getBytes());
            byte[] bytes = m.digest();
            StringBuilder s = new StringBuilder();
            for (int i = 0; i < bytes.length; i++) {
                s.append(Integer.toString((bytes[i] & 0xff) + 0x100, 16).substring(1));
            }
            return s.toString();

        } catch (NoSuchAlgorithmException e) {
            System.out.println("Error in password encryption: " + e.getMessage());
        }

        return null;
    }

    /**
     * @return the name
     */

    public String getUsername() {
        return username;
    }

    /**
     * @param name the name to set
     */
    public void setUsername(String name) {
        this.username = name;
    }
}
