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

    public synchronized void write(Message toWrite){
        this.messageList.add(toWrite);
        this.tabIndex = (this.tabIndex+1)%999;
        this.msgIndex = (this.msgIndex+1)%9999;
    }

    public synchronized Message[] read(int n){
        return null;
    }
    
    public synchronized String readRandom(String code){
        Message random = this.messageList.get(new Random().nextInt(this.tabIndex+1));
        return new Message(random.getMessageNumber(), code, random.getOriginID(), random.getMessage()).toString();
    }

    @Override public String toString(){
        //avant de return convertir les addresses ip et identifiant dans le format demandé
        return id + " " + multicastIP + " " + multicastPort + " " + machineIP + " " + userPort; 
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
                    pw.print("HI\n");
                    pw.flush();
                    String mess=br.readLine();
                    System.out.println("Message recu :"+mess);
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