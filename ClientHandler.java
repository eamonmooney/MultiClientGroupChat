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
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

//Each object in this class is responsible for communcating with a client
//Implementing runnable ensures instances will be executed by a seperate thread.
public class ClientHandler implements Runnable {
    
    //Keeps track of all of the clients, allows the capability to broadcast messages to multiple users
    public static ArrayList<ClientHandler> clientHandlers = new ArrayList<>();

    //ArrayList of all the commands that can be used by the client, complete with their own descriptions.
    public static ArrayList<String> commands = new ArrayList<>(Arrays.asList(
        "/help - Displays all commands that are currently available.", 
        "/getMessage <Number> - Displays the requested message by message number.",
        "/messageCount <Username> - Displays how many messages have been sent by the requested username.",
        "/messageDelete <Number> - Deletes the requested message, You can only delete your own messages. [IN DEVELOPMENT]",
        "/messageEdit <Number> <Message> - Edits the requested message, You can only edit your own messages. [IN DEVELOPMENT]",
        "/translate <Number> <Language> - Translates a message to the requested language [IN DEVELOPMENT]"
    ));
    
    //Establishes a connection between the client and the server.
    private Socket socket;

    //Used to read data, such as messages from the client.
    private BufferedReader bufferedReader;

    //Used to send data, such as messages to the client.
    private BufferedWriter bufferedWriter;

    //current clients username
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

        //While the client is connected to the server, listen out for any incoming messages 
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

        //Check if the user has input a command and then checks if the command is valid, then executes the command.
        if (actualMessage.startsWith("/")) {
            if (actualMessage.startsWith("/getMessage ")) {
                getMessage(actualMessage);
            } else if (actualMessage.startsWith("/messageCount ")) {
                messageCount(actualMessage);
            } else if (actualMessage.startsWith("/help")) {
                help();
            } else if (actualMessage.startsWith("/messageDelete")) {
                messageDelete(actualMessage);
            } else {
                sendError("Invalid Command.");
            }
        }
    }

    //Removes a client from the server
    public void removeClientHandler() {
        broadcastMessage("SERVER: " + clientUsername + " has left the chat!", true);
        clientHandlers.remove(this);
    }

    //Closes everything
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

    //Edits an existing message in the log
    public synchronized void editLog(Integer key, Message newMessage) {
        HashMap<Integer, Message> log = readLog();
        log.replace(key, newMessage);
        Gson gson = new Gson();
        String json = gson.toJson(log);

        try (FileWriter writer = new FileWriter("chatLog.json")) {
            writer.write(json);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //Validates the given integer to check if it exists in the log
    public boolean logValidation(Integer key) {
        try {
            //Validate number input
            if (key == -1) {
                sendError("Invalid message number.");
                return false;
            }

            //Retrive Message Log
            HashMap<Integer, Message> log = readLog();

            // Check if the message exists in the log
            if (!log.containsKey(key)) {
                sendError("Message not found.");
                return false;
            }
            return true;
        } catch (Exception e) {
            // Catch any other unexpected exceptions
            sendError("An unexpected error occurred.");
            return false;
        }
    }

    // Helper method to send error messages to the client
    private void sendError(String errorMessage) {
        try {
            bufferedWriter.write("SERVER: Error: " + errorMessage);
            bufferedWriter.newLine();
            bufferedWriter.flush();
        } catch (IOException ex) {
            closeEverything(socket, bufferedReader, bufferedWriter);
        }
    }

    // Helper method to send normal messages to the client
    private void selfMessage(String message) {
        try {
            bufferedWriter.write(message);
            bufferedWriter.newLine();
            bufferedWriter.flush();
        } catch (IOException ex) {
            closeEverything(socket, bufferedReader, bufferedWriter);
        }
    }

    // Helper method to parse a message number and handle invalid inputs
    private int parseMessageNumber(String input) {
        try {
            return Integer.parseInt(input);
        } catch (NumberFormatException e) {
            return -1;  // Indicate invalid message number
        }
    }

    //Displays all avalible commands to the user.
    public void help() {
        try {
            selfMessage("SERVER: Here is a following list of available commands.");
            for (String command : commands) {
                selfMessage(command);
            }
        }   catch (Exception e) {
            // Catch any other unexpected exceptions
            sendError("An unexpected error occurred.");
        }
    }

    //Searches through the message log to find the message requested.
    public void getMessage(String messageFromClient) {
        try {
            String[] parts = messageFromClient.split(" ");

            //Validate format
            if (parts.length < 2) {
                sendError("Invalid command format.");
                return;
            }

            Integer key = parseMessageNumber(parts[1]);

            //Validate log existence
            if (!logValidation(key)){
                return;
            }

            //Retrive Message Log
            HashMap<Integer, Message> log = readLog();

            //Fetch the message
            Message message = log.get(key);

            //Broadcast the message to all clients
            String formattedMessage = "SERVER: " + message.getUsername() + " said: " + message.getContents();
            broadcastMessage(formattedMessage, true);
            
            //Send the output back to the requesting client as it is a command
            selfMessage(formattedMessage);
        } catch (Exception e) {
            // Catch any other unexpected exceptions
            sendError("An unexpected error occurred.");
        }
    }

    //Command to count how many messages a specific user has sent.
    public void messageCount(String messageFromClient) {
        try {
            String[] parts = messageFromClient.split(" ");
            String requestedUsername = parts[1];
            int counter = 0;

            //Validate input
            if (requestedUsername.isEmpty()) {
                sendError("Invalid username");
            }

            //Retrive Message Log
            HashMap<Integer, Message> log = readLog();

            //Count how many messages the specified user has sent
            for (Map.Entry<Integer, Message> message : log.entrySet()) {
                String sender = message.getValue().getUsername();
                if (sender.equals(requestedUsername)) {
                    counter++;
                }
            }

            if (counter > 0) {
                String formattedMessage = "SERVER: " + counter + " messages found from " + requestedUsername;
                broadcastMessage(formattedMessage, true); 
                //Send the output back to the requesting client as it is a command
                selfMessage(formattedMessage);
            } else {
                String formattedMessage = "SERVER: No messages found from " + requestedUsername;
                broadcastMessage(formattedMessage, true);
                //Send the output back to the requesting client as it is a command
                selfMessage(formattedMessage);
            }
        } catch (Exception e) {
            // Catch any other unexpected exceptions
            sendError("An unexpected error occurred.");
        }
    }

    //Deletes the requested message
    public void messageDelete(String messageFromClient) {
        try {
            String[] parts = messageFromClient.split(" ");

            //Validate format
            if (parts.length < 2) {
                sendError("Invalid command format.");
                return;
            }

            Integer key = parseMessageNumber(parts[1]);

            //Validate log existence
            if (!logValidation(key)){
                return;
            }

            //Retrive Message Log
            HashMap<Integer, Message> log = readLog();

            //Fetch the message
            Message message = log.get(key);

            //Create new message
            Message newMessage = new Message(message.getUsername(), "<Message Deleted>");
            editLog(key, newMessage);

            selfMessage("SERVER: Message has been deleted.");
        } catch (Exception e) {
            //Catch any other unexpected exceptions
            sendError("An unexpected error occurred.");
        }
    }
    
    //Edits the requested message
    public void messageEdit(Integer key, String newMessage) {

    }

    //Translate the requested message
    public void translate(Integer key, String language) {

    }
}
