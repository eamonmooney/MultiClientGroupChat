import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class Server {

    //Responsible for Listening to Incoming connections/clients
    private ServerSocket serverSocket;

    //Constructor
    public Server(ServerSocket serverSocket) {
        this.serverSocket = serverSocket;
    }

    //Starts the server
    public void startServer() {

        try {

            while (!serverSocket.isClosed()) {

                Socket socket = serverSocket.accept();
                System.out.println("A new client has connected!");
                ClientHandler clientHandler = new ClientHandler(socket);

                Thread thread = new Thread(clientHandler);
                thread.start();
            }
        } catch (IOException e) {

        }
    }

    //Closes the Server
    public void closeServerSocket() {
        try {
            if (serverSocket != null)
            serverSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //Server and Client must connect through the same port.
    //Server must be ran first in order for the client to connect
    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = new ServerSocket(1234);
        Server server = new Server(serverSocket);
        server.startServer();
    }
}