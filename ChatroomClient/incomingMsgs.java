package tcpClient;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

public class incomingMsgs extends Thread {

    private Socket socket;
    private boolean exit;

    public incomingMsgs(Socket socket) {
        this.socket = socket;
        this.exit = false;
    }

    public void run() {
        try {
            BufferedReader input = null;
            synchronized (socket) {
                input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            }

            String msg;
            boolean exit_loop = false;
            // Username and user validation. If it exists in the database,
            // You have to change it. If you use a signed in username
            // you need to type the password. If the password is wrong,
            // you need to enter another username or try again.
            do {
                msg = input.readLine();
                switch (msg) {
                case "Enter password: ":
                    System.out.println(msg);
                    continue;
                case "Already connected. Enter another name: ":
                    System.out.println(msg);
                    continue;
                case "Wrong password. Enter your username: ":
                    System.out.println(msg);
                    continue;
                case "Connected.":
                    System.out.println(msg);
                    exit_loop = true;
                    continue;
                default:
                    continue;
                }

            } while (!exit_loop);

            // Loop that accepts msgs from the server and projects them
            // to the user, even if that msg is from the user.

            while (!this.exit) {
                msg = input.readLine();
                if (!msg.equals("exit") && msg != null) {
                    System.out.println(msg);
                } else {
                    break;
                }
            }

            System.out.println("Disconnected");

        } catch (

        IOException e) {
            // Throws IOException because the thread gets terminated.
            // DO NOTHING.

        }
    }

    public void stopThread() {
        this.exit = true;
    }

}
