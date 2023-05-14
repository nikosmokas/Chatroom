/*THIS IS A CHATROOM APP, AFTER COMPLETING A 80HOURS JAVA COURSE.
 * MAIN COMPONENTS: NETWORKING - THREADS (FOR NOW)
 * 
 * APP DESCRIPTION: An unlimited number of clients is able to 
 * establish a TCP connection to a server, which has the ability to
 * receive and project messages.
 */

package tcpServer;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class tcpServer {

    private volatile static List<Socket> socketList = new ArrayList<>();

    public static void main(String[] args) {

        // SERVER CREATION: Opening the server 5000 port for
        // the client's connection target.

        try (ServerSocket serverSocket = new ServerSocket(5000)) {
            System.out.println("Starting the server...");

            // Start administration tools thread
            ServerAdministrationTools tools = new ServerAdministrationTools(socketList);
            tools.setDaemon(true);
            tools.start();

            // Server is actively looking for new connections.
            // When a client is connecting, a new ChatClient Thread
            // is created.
            while (true) {

                Socket socket = serverSocket.accept();
                //
                synchronized (socketList) {
                    socketList.add(socket);
                }

                ChatClient client = new ChatClient(socketList);
                client.start();

                // ServerAdministrationTools.addClient(client);
            }
        } catch (IOException e) {
            System.out.println("Server error: " + e.getMessage());
        }

    }

    public List<Socket> getSocketList() {
        return socketList;
    }

}
