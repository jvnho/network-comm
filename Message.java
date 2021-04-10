public class Message {
    
    private int messageNumber;
    private String originID;
    private String message;

    public Message(int messageNumber, String originID, String message){
        this.messageNumber = messageNumber;
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
        return this.messageNumber + " " + this.originID + " " + this.message;
    }

    public int getMessageNumber(){
        return this.messageNumber;
    }

    public String getOriginID(){
        return this.originID;
    }

    public String getMessage(){
        return this.message;
    }
}
