public class Message {
    
    private int messageNumber;
    private String code; //DIFF, OLDM, LAST, MESS...
    private String originID;
    private String message;

    public Message(int messageNumber, String code, String originID, String message){
        this.messageNumber = messageNumber;
        this.code = code;
        this.originID = originID;
        this.message = message;
    }

    @Override public String toString(){
        return this.code + " " + this.messageNumber + " " + this.originID + " " + this.message;
    }

    public int getMessageNumber(){
        return this.messageNumber;
    }

    public String getCode(){
        return this.code;
    }

    public String getOriginID(){
        return this.originID;
    }

    public String getMessage(){
        return this.message;
    }
}
