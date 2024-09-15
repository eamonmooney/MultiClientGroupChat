//Class is used to store a messages sender and contents, primary use of this is for storing and loading from chatLog.json
public class Message {
    private String username;
    private String contents;

    public Message(String username, String contents) {
        this.username = username;
        this.contents = contents;
    }

    public String getUsername(){
        return username;
    }

    public String getContents(){
        return contents;
    }
}
