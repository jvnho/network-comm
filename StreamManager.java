import java.net.ServerSocket;
import java.net.Socket;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;

public class StreamManager{
    private ArrayList<Socket>listStreamerSocket;
    private ArrayList<String> listStreamer;
    private int port; 
    private int lenMax;
    private int currentLen;
    private int time_refresh;

    public StreamManager(int port){
        this.listStreamerSocket = new ArrayList<Socket>();
        this.listStreamer = new ArrayList<String>();;
        this.port = port; //less than 9999
        this.lenMax = 99;
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
                System.out.println("error on StreamerAccpet");
                e.printStackTrace();
            }
        }
    }
    public class SaveSocketStreamer implements Runnable{
        Socket soc;
        public SaveSocketStreamer(Socket s){
            this.soc = s;
        }
        public void run(){
            try{
                BufferedReader reception = new BufferedReader(new InputStreamReader(soc.getInputStream()));//each message ends with \r\n
                String message = reception.readLine(); //readline remove '\n' at the end ***?
                message = message+"\n";//restore the '\n'
                if(Format.isregi(message)){ //correct format
                    message = "ITEM"+ message.substring(4);
                    PrintWriter sending = new PrintWriter(new OutputStreamWriter(soc.getOutputStream()));
                    synchronized(StreamManager.this){
                        if(currentLen < lenMax){
                            StreamManager.this.listStreamerSocket.add(soc);
                            StreamManager.this.listStreamer.add(message);
                            currentLen = currentLen + 1;
                            sending.println("REOK\r");// \r\n
                            sending.flush();

                        }else if(lenMax == currentLen){
                            sending.println("RENO\r");
                            sending.flush();
                            soc.close();
                        }
                    }
                }else{
                    soc.close();
                }
            }catch(IOException e){
                System.out.println("error SaveSocketStreamer");
                e.printStackTrace();
            }
        }
    }
    public class ClientAccept implements Runnable{
        public void run(){
            try{
                ServerSocket serverSocket = new ServerSocket(StreamManager.this.port);
                while(true){
                    Socket soc = serverSocket.accept();
                    new Thread(new ClientQuery(soc)).start();
                }
            }catch(IOException e){
                System.out.println("error ClientAccept");
                e.printStackTrace();
            }
        }
    }
    public class ClientQuery implements Runnable{
        Socket soc;
        public ClientQuery(Socket soc){
            this.soc = soc;
        }
        public void run(){
            try{
                BufferedReader reception = new BufferedReader(new InputStreamReader(soc.getInputStream()));//each message ends with \r\n
                String message = reception.readLine(); //readline enleve le \n  ****???
                PrintWriter sending = new PrintWriter(new OutputStreamWriter(soc.getOutputStream()));

                if(message.equals("LIST\r")){
                    synchronized(StreamManager.this){
                        sending.println("LINB "+StreamManager.this.getCurrentLen()+"\r");// ? getCurrentLen is also synchronized check if don't block??
                        sending.flush();
                        for (String streamer : listStreamer) {
                            sending.print(streamer);
                            sending.flush();
                        }
                        soc.close();
                    }
                }else{
                    System.out.println("Mauvais format par le client");
                    soc.close();
                }
            }catch(IOException e){
                System.out.println("error ClientQuery");
                e.printStackTrace();
            }
        }
    }
    //test in case of being called in a synchronized block
    public synchronized String getCurrentLen(){
        String resutl = Integer.toString(this.currentLen);
        return (resutl.length()==1)?"0"+resutl:resutl;
    }
    public void launchStreamManger(){
        new Thread(this.new StreamerAccept()).start();//thread witch accept streamer and communicate with them
        new Thread(this.new ClientAccept()).start();//thread witch accept all the client and communicate with them
    }
    public static void main(String []args){
        //"REGI RADIO### 127.000.000.001 0120 127.000.000.001 0120\r\n"
    }
}