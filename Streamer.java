import java.net.*;
import java.io.*;
import java.util.List;
import java.util.ArrayList;
import java.util.Random;

public class Streamer{
    
    private static int LIST_SIZE = 1000;

    private String id;
    private int userPort;
    private String multicastIP;
    private int multicastPort;
    private String machineIP;
    private int msgIndex;
    private int tabIndex;
    private List<Message> messageList;

    public Streamer(String id, String multicastIP, int multicastPort, int userPort, Message[] streamerMsg){
        this.id = id; //gestion erreur nombre de caractères
        this.userPort = userPort;
        this.multicastIP = multicastIP;
        this.multicastPort = multicastPort;
        this.msgIndex = 0;
        this.tabIndex = 0;
        initMessageList(streamerMsg);
        try{
            this.machineIP = InetAddress.getLocalHost().getHostName();
        } catch(UnknownHostException e){
            System.out.println(e);
            e.printStackTrace();
        }               
    }   

    public void initMessageList(Message [] list){
        this.messageList = new ArrayList<Message>();
        for(int i = 0; i < list.length; i++){
            this.messageList.add(list[i]);
            this.tabIndex++;
            this.msgIndex++;
        }
    }

    public void registerToManager(String managerAddr, int managerPort){
        try
        {
            Socket socket = new Socket(managerAddr, managerPort);
            BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter pw = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()));
            String msg = "REGI " + this.id + " " + this.multicastIP + " " + this.multicastPort + " " 
                + this.machineIP + " " + this.userPort;
            pw.print(msg);
            pw.flush();    
            String recv = br.readLine();
            System.out.println(recv);
            pw.close();
            br.close();
            socket.close();
        }
        catch(Exception e){
            System.out.println(e);
            e.printStackTrace();
        }
    }

    public synchronized void write(String code, String originID, String msgToWrite){
        Message m = new Message(this.msgIndex, code, originID, msgToWrite);
        this.messageList.add(m);
        this.tabIndex = (this.tabIndex+1)%999;
        this.msgIndex = (this.msgIndex+1)%9999;
    }

    public synchronized Message[] read(int n){
        //retrieve the last n elements on this.msgList and convert it into a java array
        ArrayList<Message> list = (ArrayList<Message>)this.messageList.subList(this.messageList.size()-n, this.messageList.size());
        return (Message [])list.toArray();
    }
    
    public synchronized String readRandom(String code){
        Message random = this.messageList.get(new Random().nextInt(this.tabIndex+1));
        return new Message(random.getMessageNumber(), code, random.getOriginID(), random.getMessage()).toString();
    }

    @Override public String toString(){
        //avant de return convertir les addresses ip et identifiant dans le format demandé
        return id + " " + multicastIP + " " + multicastPort + " " + machineIP + " " + userPort; 
    }

    public static void main(String[] args){

    }

    private class StreamerTCP implements Runnable{

        private ServerSocket server;

        public StreamerTCP(){
            try{
                this.server = new ServerSocket(Streamer.this.userPort);
            } catch(IOException e){
                System.out.println(e);
                e.printStackTrace();
            }
        }

        @Override public void run(){
            try
            {
                while(true)
                {
                    Socket socket = server.accept();
                    ClientCommunication communication = new ClientCommunication(socket);
                    Thread t = new Thread(communication);
                    t.start();
                }   
            }
            catch(Exception e){
                System.out.println(e);
                e.printStackTrace();
            } 
        }
        
        private class ClientCommunication implements Runnable{
    
            private Socket socket;
        
            public ClientCommunication(Socket socket){
                this.socket = socket;
            }
        
            @Override public void run(){
                try
                {
                    BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    PrintWriter pw = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()));
                    boolean correctQuery = false;
                    while(correctQuery == false)
                    {
                        String[] query = br.readLine().split(" ");
                        if(query[0].equals("LAST"))
                        {
                            if(query.length < 2){
                                pw.print("Missing LAST argument.");
                                pw.flush();
                            } else if(query.length != 2) {
                                pw.print("Incorrect LAST format.");
                                pw.flush();
                            }
                            else
                            {
                                try 
                                {
                                    int n = Integer.valueOf(query[1]);
                                    Message[] history = Streamer.this.read(n);
                                    for(Message m : history){
                                        pw.print(m.toString());
                                        pw.flush();
                                    }
                                    pw.print("ENDM");
                                    pw.flush();
                                    correctQuery = true;
                                } catch (NumberFormatException e){
                                    pw.print("Incorrect number format.");
                                    pw.flush();
                                }
                            }
                        } else if(query[0].equals("MESS"))
                        {
                            if(query.length < 3) {
                                pw.print("Missing MESS argument.");
                                pw.flush();
                            } else if(query.length != 3) {
                                pw.print("Incorrect MESS format.");
                                pw.flush();
                            } else
                            {
                                Streamer.this.write(query[0], query[1], query[2]);
                                pw.print("ACKM");
                                correctQuery = true;
                            }
                        } else if(query[0].equals("RUOK"))
                        { 
                            pw.print("IMOK");
                            pw.flush();
                        }
                    }
                    br.close();
                    pw.close();
                    socket.close();
                }
                catch(Exception e){
                    System.out.println(e);
                    e.printStackTrace();
                }
            }
        }
    }
}