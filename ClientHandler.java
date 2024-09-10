import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

//Each object in this class is responsible for communcating with a client
//Implementing runnable ensures instances will be executed by a seperate thread.
public class ClientHandler implements Runnable {
    
    //Keeps track of all of the clients, allows the capability to broadcast messages to multiple users
    public static ArrayList<ClientHandler> clientHandlers = new ArrayList<>();
    
    //Establishes a connection between the client and the server.
    private Socket socket;

    //Used to read data, such as messages from the client.
    private BufferedReader bufferedReader;

    //Used to send data, such as messages to the client.
    private BufferedWriter bufferedWriter;

    private String clientUsername;

    public ClientHandler(Socket socket) {
        try {
            this.socket = socket;
            //"OutputStream" = byte stream
            //"OutputStreamWriter" = character stream
            this.bufferedWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            this.bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            
            this.clientUsername = bufferedReader.readLine();

            //Adds the current clientHandler to the list
            clientHandlers.add(this);

            broadcastMessage("SERVER: " + clientUsername + " has entered the chat!", true);

            
        } catch (IOException e) {
            closeEverything(socket, bufferedReader, bufferedWriter);
        }
    }

    @Override
    public void run() {
        String messageFromClient;

        while (socket.isConnected()) {
            try {
                messageFromClient = bufferedReader.readLine();
                broadcastMessage(messageFromClient, false);
            } catch (IOException e) {
                closeEverything(socket, bufferedReader, bufferedWriter);
                break;
            }
        }
    }

    //Sends messages to other clients
    public void broadcastMessage(String messageToSend, Boolean fromServer) {
        String sender = clientUsername;
        for (ClientHandler clientHandler : clientHandlers) {
            try {
                if (!clientHandler.clientUsername.equals(clientUsername)) {
                    clientHandler.bufferedWriter.write(messageToSend);
                    clientHandler.bufferedWriter.newLine();
                    clientHandler.bufferedWriter.flush();
                }
            } catch (IOException e) {
                closeEverything(socket, bufferedReader, bufferedWriter);
            }
        }

        //Split the Message Contents from the Username and contents by 2 columns            
      
        String[] messageParts = messageToSend.split(":", 2);
        String actualMessage = messageParts.length > 1 ? messageParts[1].trim() : messageToSend;
      
        //Store the corresponding message as the final output

        if (!fromServer) {
            Message message = new Message(sender, actualMessage);
            writeLog(message);
        }
        else {
            Message message = new Message("SERVER", actualMessage);
            writeLog(message);
        }
        if (actualMessage.startsWith("/getMessage ")) {
            getMessage(actualMessage);
        }
    }

    public void removeClientHandler() {
        broadcastMessage("SERVER: " + clientUsername + " has left the chat!", true);
        clientHandlers.remove(this);
    }

    public void closeEverything(Socket socket, BufferedReader bufferedReader, BufferedWriter bufferedWriter) {
        removeClientHandler();
        try{
            if (bufferedReader != null) {
                bufferedReader.close();
            }
            if (bufferedWriter != null) {
                bufferedWriter.close();
            }
            if(socket != null) {
                socket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //reads and returns the message log
    public HashMap<Integer, Message> readLog() {
        HashMap<Integer, Message> data = new HashMap<>();

        try (FileReader reader = new FileReader("chatLog.json")) {
            Gson gson = new Gson();
            data = gson.fromJson(reader, new TypeToken<HashMap<Integer, Message>>() {}.getType());
        } catch (FileNotFoundException e) {
        // File not found, returning empty log
        } catch (IOException e) {
            e.printStackTrace();
        }
        return data;
    }

    //Writes a new message to the message log
    public synchronized void writeLog(Message message) {
        HashMap<Integer, Message> log = readLog();
        if (log == null) {
            log = new HashMap<>(); // Initialize a new HashMap if readLog() fails
        }
        int nextIndex = log.keySet().stream().max(Integer::compare).orElse(-1) + 1;
        log.put(nextIndex,message);
        Gson gson = new Gson();
        String json = gson.toJson(log);

        try (FileWriter writer = new FileWriter("chatLog.json")) {
            writer.write(json);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //Searches through the message log to find the message requested.
    public void getMessage(String messageFromClient) {
        String[] parts = messageFromClient.split(" ");
        int messageNumber = Integer.parseInt(parts[1]);
        HashMap<Integer, Message> log = readLog();
        Message message = log.get(messageNumber);
        broadcastMessage("SERVER: " + message.getUsername() + " said: " + message.getContents(), true);
        
        //Output must also be sent back to the sendee as it is a command.
        try {
            bufferedWriter.write("SERVER: " + message.getUsername() + " said: " + message.getContents());
            bufferedWriter.newLine();
            bufferedWriter.flush();
        } catch (IOException e) {
            closeEverything(socket, bufferedReader, bufferedWriter);
        }
    }
}
