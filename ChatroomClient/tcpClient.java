package tcpClient;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.Scanner;

/*Client main class, establishing connection with the server and 
 * sending messages. He also receives messages from other the users
 * in the chat-room.
 */

public class tcpClient {

    public static void main(String[] args) {
        // Establishing connection with the server
        try (Socket socket = new Socket("localhost", 5000)) {
            PrintWriter stringToEcho = new PrintWriter(socket.getOutputStream(), true);

            // Starting a parallel thread that accepts and prints the
            // msgs of other users.
            incomingMsgs incomingMsg = new incomingMsgs(socket);

            // Getting the client's username and notifying the server
            String echoString;

            echoString = getUsername();
            stringToEcho.println(echoString);
            incomingMsg.start();

            // Reading user's messages and sending them
            // to the server until he 'exits'
            Scanner scanner = new Scanner(System.in);
            do {
                echoString = scanner.nextLine();
                stringToEcho.println(echoString);
            } while (!echoString.equals("exit"));
            synchronized (socket) {
                incomingMsg.stopThread();
                stringToEcho.println("exit");
            }
            System.out.println("You have left the chat.");
            scanner.close();
        } catch (SocketTimeoutException e) {
            System.out.println("Socket timed out!");
        } catch (IOException e) {
            System.out.println("Client Error: " + e.getMessage());
        }
    }

    private static String getUsername() {
        String name;
        Scanner scanner = new Scanner(System.in);
        System.out.println("Enter username: ");
        name = scanner.nextLine();
        scanner.close();
        return name;
    }

}
