import java.net.ServerSocket;
import java.net.Socket;
import java.io.IOException;
import java.util.ArrayList;

public class StreamManager{
    private ArrayList<Socket>listStreamerSocket;
    private String[] listStreamer;
    private int port;
    private int lenMax;
    private int currentLen;
    private int time_refresh;

    public StreamManager(int port){
        this.listStreamerSocket = new ArrayList<Socket>();
        this.listStreamer = new String[100];
        this.port = port;
        this.lenMax = 100;
        this.currentLen = 0;
        this.time_refresh = 5;
    }

    public class StreamerAccept implements Runnable{
        public void run(){
            try{
                ServerSocket serverSocket = new ServerSocket(StreamManager.this.port);
                while(true){
                    Socket streamersocket = serverSocket.accept();
                    new Thread(new SaveSocketStreamer(streamersocket)).start();
                }
            }catch(IOException e){
                System.out.println("error on create ServerSocket");
            }
        }
    }
    public class SaveSocketStreamer implements Runnable{
        Socket soc;
        public SaveSocketStreamer(Socket s){
            this.soc = s;
        }
        public void run(){
            synchronized(StreamManager.this){
                if(lenMax == currentLen){
                    //si elle est de capacité maxiaml il renvoie un message (RENO) et ferme la connexion
                }else{
                    //sinon j'enregistre (REOK) et garde la connexion
                }
            }
        }
    }
    public static void main(String []args){

    }
}