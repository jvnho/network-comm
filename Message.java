public class Message {

    private String index;
    private String originID;
    private String message;

    public Message(String originID, String message){
        this.originID = originID;
        initMessage(message);
    }

    public void initMessage(String message){
        int strLength = message.length();
        String newMessage = "";
        if(message.charAt(strLength-1) == '\n' && message.charAt(strLength-2) == '\r'){
            newMessage = message.substring(0, strLength-2);
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
        String s = String.valueOf(index);
        if(s.length() != 4){
            this.index = "0".repeat(4 - s.length()) + s;
        } else {
            this.index = s;
        }
    }

    public String getOriginID(){
        return this.originID;
    }

    public String getMessage(){
        return this.message;
    }
}
