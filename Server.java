import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.io.FileReader;
import java.io.FileWriter;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;

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

    //NOT IN USE: Will log any chat messages that are sent to the server
    public static void chatLog(HashMap<Integer, Message> message) {
        Gson gson = new Gson();
        String json = gson.toJson(message);

        try (FileWriter writer = new FileWriter("chatlog.json")) {
            writer.write(json);
            System.out.println("Data saved to data.json");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = new ServerSocket(1234);
        Server server = new Server(serverSocket);
        server.startServer();
    }
}