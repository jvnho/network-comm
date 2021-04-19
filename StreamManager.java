import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.InterruptedIOException;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ForkJoinTask;
import java.util.HashMap;

public class StreamManager{
    private HashMap<Socket, String> socketDescription;
    private int port; 
    private int lenMax;
    private int currentLen;
    private int time_refresh;

    public StreamManager(int port){
        this.socketDescription = new HashMap<Socket, String>();
        this.port = port; //less than 9999
        this.lenMax = 99;
        this.currentLen = 0;
        this.time_refresh = 10;
    }

    public void saveSocketStreamer(Socket soc, String message){
        try{
            if(Format.isregi(message)){ //correct format
                message = "ITEM"+ message.substring(4);
                PrintWriter sending = new PrintWriter(new OutputStreamWriter(soc.getOutputStream()));
                synchronized(this){
                    if(currentLen < lenMax){
                        this.socketDescription.put(soc, message+"\r\n");
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
                this.socketDescription.forEach((socket, description)->{
                    sending.print(description);
                    sending.flush();
                });
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
                if(message.equals("LIST")){//communication avec client
                    StreamManager.this.sendListToClient(this.soc);
                }else if(message.substring(0, 4).equals("REGI")){
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

    public class StreamerPresence implements Runnable{

        private final static int TIMEOUT_DELAY = 1000;
        private Socket socket;

        public StreamerPresence(Socket s){
            this.socket = s;
        }

        @Override public void run(){
            try {
                PrintWriter sending = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()));
                BufferedReader receving = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                socket.setSoTimeout(TIMEOUT_DELAY);
                sending.print("RUOK\r\n");
                sending.flush();
                String recv = receving.readLine();
                if(recv.equals("IMOK")){
                    sending.print("");
                    sending.flush();
                    sending.close();
                    receving.close();
                } else {
                    removeSocketFromMap();
                }
            } catch(InterruptedIOException  e){
                removeSocketFromMap();
            } catch(SocketException e){
                System.out.println("Error when trying to set socket a timeout");
                System.exit(0);
            } catch(IOException e){
                System.out.println("Error when trying to retrieve input/output stream");
                System.exit(0);
            }
        }

        public void removeSocketFromMap(){
            StreamManager.this.socketDescription.remove(this.socket);
            this.socket.close();
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
    public class MonRunnable implements Runnable{
        Socket socket;
        public MonRunnable(Socket s){
            this.socket = s;
        }
        public void run(){

        }
    }
    //TODO
    public synchronized void update(){
        ArrayList<Thread> updating = new ArrayList<Thread>();
        this.socketDescription.forEach((socket, description)->{
            Thread thread = new Thread(new MonRunnable(socket));
            updating.add(thread);
            thread.start();
        });
        try{
            for(Thread t: updating){
                t.join();
            }
        }catch(InterruptedException e){
            System.out.println("error update\n");
            e.printStackTrace();
        }
    }
    public void printListStreamer(){
        while(true){
            try{
                TimeUnit.SECONDS.sleep(this.time_refresh);
                System.out.println("mis a jour\n");
                synchronized(this){
                    this.socketDescription.forEach((socket, description) -> System.out.println(description));
                }
                System.out.println("-------------");
            }catch(InterruptedException e){
                e.printStackTrace();
            }
        }
    }
    //test in case of being called in a synchronized block
    public synchronized String getCurrentLen(){
        String resutl = Integer.toString(this.currentLen);
        return (resutl.length()==1)?"0"+resutl:resutl;
    }
    public static void main(String []args){
        StreamManager test = new StreamManager(Integer.parseInt(args[0]));
        new Thread(()->test.launchStreamManger()).start();
        new Thread(()->test.printListStreamer()).start();
        //"REGI RADIO### 127.000.000.001 0120 127.000.000.001 0120\r\n"
    }
}