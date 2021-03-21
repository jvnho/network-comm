import java.net.*;
import java.io.*;

public class Streamer{
    
    private String id;
    private int userPort;
    private String multicastIP;
    private int multicastPort;
    private String machineIP;
    private int msgIndex;
    private int tabIndex;

    public Streamer(String id, String multicastIP, int multicastPort, int userPort){
        this.id = id; //gestion erreur nombre de caractères
        this.userPort = userPort;
        this.multicastIP = multicastIP;
        this.multicastPort = multicastPort;
        this.msgIndex = 0;
        this.tabIndex = 0;
        try{
            this.machineIP = InetAddress.getLocalHost().getHostName();
        } catch(UnknownHostException e){
            System.out.println(e);
            e.printStackTrace();
        }               
    }   

    public void registerToManager(String managerAddr, int managerPort){

    }

    public synchronized void write(Message toWrite){

    }

    public synchronized Message[] read(int n){
        return null;
    }
    
    public synchronized void readRandom(){

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
                
            }
        }
        
    }
}