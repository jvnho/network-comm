import java.net.ServerSocket;
import java.net.Socket;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

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

    public void saveSocketStreamer(Socket soc, String message){
        try{
            if(Format.isregi(message)){ //correct format
                message = "ITEM"+ message.substring(4);
                PrintWriter sending = new PrintWriter(new OutputStreamWriter(soc.getOutputStream()));
                synchronized(this){
                    if(currentLen < lenMax){
                        this.listStreamerSocket.add(soc);
                        this.listStreamer.add(message+"\r\n");
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
    public void sendListToClient(Socket soc){
        try{
            PrintWriter sending = new PrintWriter(new OutputStreamWriter(soc.getOutputStream()));
            synchronized(this){
                sending.println("LINB "+this.getCurrentLen()+"\r");// ? getCurrentLen is also synchronized check if don't block??
                sending.flush();
                for (String streamer : listStreamer) {
                    sending.print(streamer);
                    sending.flush();
                }
                soc.close();
            }
        }catch(IOException e){
            System.out.println("error ClientQuery");
            e.printStackTrace();
        }
    }

    public class CommunicationStreamerOrClient implements Runnable{
        Socket soc;
        public CommunicationStreamerOrClient(Socket s){
            this.soc = s;
        }
        public void run(){
            try{
                BufferedReader reception = new BufferedReader(new InputStreamReader(soc.getInputStream()));//each message ends with \r\n
                String message = reception.readLine(); //readline enleve le \n  ****???
                System.out.println(message);
                System.out.println(message.substring(0, 4));
                if(message.equals("LIST")){//communication avec client
                    System.out.println("demande du client");
                    StreamManager.this.sendListToClient(this.soc);
                }else if(message.substring(0, 4).equals("REGI")){
                    System.out.println("demande du diffuseur");
                    StreamManager.this.saveSocketStreamer(this.soc, message);
                }else{
                    System.out.println("fermeture");
                    soc.close();
                }
            }catch(IOException e){
                System.out.println("reading or writing error");
                e.printStackTrace();
            }catch(IndexOutOfBoundsException e){
                System.out.println("wrong message sended to the StreamManager");
                try{soc.close();}catch(IOException a){}
            }
        }
    }
    
    public void launchStreamManger(){
        //create one server socket
        try{
            ServerSocket serverSocket = new ServerSocket(this.port);
            while(true){
                Socket socket = serverSocket.accept();
                new Thread(new CommunicationStreamerOrClient(socket)).start();
            }
        }catch(IOException e){
            System.out.println("error on create serversocket");
            e.printStackTrace();
        }   
    }

    //test in case of being called in a synchronized block
    public synchronized String getCurrentLen(){
        String resutl = Integer.toString(this.currentLen);
        return (resutl.length()==1)?"0"+resutl:resutl;
    }
    public static void main(String []args){
        StreamManager test = new StreamManager(Integer.parseInt(args[0]));
        test.launchStreamManger();
        //affichage incorect au niveau synchronisation
        while(true){
            try{
                for(String s: test.listStreamer){
                    System.out.println(s);
                }
                System.out.println("-------------");
                TimeUnit.SECONDS.sleep(test.time_refresh);
            }catch(InterruptedException e){
                e.printStackTrace();
            }
        }
        //"REGI RADIO### 127.000.000.001 0120 127.000.000.001 0120\r\n"
    }
}