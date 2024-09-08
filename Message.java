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
