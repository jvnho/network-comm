public class Message {

    private int index;
    private String originID;
    private String message;

    public Message(String originID, String message){
        this.originID = originID;
        initMessage(message);
    }

    public Message(int index, String originID, String message){
        this.index = index;
        this.originID = originID;
        initMessage(message);
    }

    public void initMessage(String message){
        int strLength = message.length();
        String newMessage = "";
        if(message.charAt(strLength-1) == '\n' && message.charAt(strLength-2) == '\r'){
            newMessage = message.substring(0, strLength);
        }
        if(newMessage.length() > 140){
            throw new IllegalArgumentException();
        }
        this.message = newMessage + "#".repeat(140 - newMessage.length()) + "\r\n";
    }

    @Override public String toString(){
        return this.index + " " + this.originID + " " + this.message;
    }

    public void setIndex(int index){
        this.index = index;
    }

    public String getOriginID(){
        return this.originID;
    }

    public String getMessage(){
        return this.message;
    }
}
