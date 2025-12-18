import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.net.Socket;

//Utility class for managing socket connections and resources
public class ConnectionUtils {
    
    //Closes all connection resources safely
    public static void closeEverything(Socket socket, BufferedReader bufferedReader, BufferedWriter bufferedWriter) {
        try {
            if (bufferedReader != null) {bufferedReader.close();}
            if (bufferedWriter != null) {bufferedWriter.close();}
            if (socket != null) {socket.close();}
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
