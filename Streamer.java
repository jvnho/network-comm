import java.net.*;
import java.io.*;
import java.util.LinkedList;
import java.util.Random;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

public class Streamer{
    
    private static int LIST_MAX_SIZE = 1000;
    private static int DELAY = 3000; //milliseconds

    private String id;
    private int userPort;
    private String multicastIP;
    private int multicastPort;
    private String machineIP;
    private int msgIndex;
    private LinkedList<Message> messageList;
    private boolean readyToWrite;

    public Streamer(String id, String multicastIP, int multicastPort, int userPort, Message[] streamerMsg){
        initStreamerID(id);
        initPorts(multicastPort, userPort);
        initAdress(multicastIP);
        initMessageList(streamerMsg);
    }   

    public void initAdress(String multicastIP){
        String[] tokens = multicastIP.split(".");
        if(tokens.length != 4){
            System.out.println("Incorrect IP address format.");
            throw new IllegalArgumentException();
        }
        for(int i = 0; i < tokens.length; i++)
        {
            if(isNumber(tokens[i]))
            {
                int length = tokens[i].length();
                if(length == 3) 
                    continue;
                else if (length > 3) {
                    System.out.println("Incorrect IP address format.");
                    throw new IllegalArgumentException();
                } else {
                    tokens[i] = "0".repeat(3-length) + tokens[i];
                }
            } else {
                System.out.println("Incorrect IP address format.");
                throw new IllegalArgumentException();
            }
        }
        this.multicastIP = String.join(".", tokens);
        try{
            this.machineIP = InetAddress.getLocalHost().getHostName();
        } catch(UnknownHostException e){
            System.out.println("Error when retrieving streamer's ip address.");
            e.printStackTrace();
        }           
    }

    public static boolean isNumber(String s){
        try{
            Integer.valueOf(s);
        } catch(NumberFormatException e){
            return false;
        }
        return true;
    }

    public void initPorts(int multicastPort, int userPort){
        if(String.valueOf(multicastPort).length() != 4 || String.valueOf(userPort).length() != 4){
            System.out.println("Incorrect port format.");
            throw new IllegalArgumentException();
        }
        this.multicastPort = multicastPort;
        this.userPort = userPort;
    }

    public void initStreamerID(String s){
        if(s.equals("")){
            System.out.println("");
            throw new IllegalArgumentException();
        }
        else if(s.length() == 8)
            this.id = s; 
        else
            this.id = s + "#".repeat(8 - s.length());
    }

    public void initMessageList(Message [] list){
        this.msgIndex = 0;
        this.messageList = new LinkedList<Message>();
        for(int i = 0; i < list.length; i++)
        {
            this.messageList.add(list[i]);
            this.msgIndex++;
        }
        this.readyToWrite = true;
    }

    public void registerToManager(String managerAddr, int managerPort){
        try
        {
            Socket socket = new Socket(managerAddr, managerPort);
            BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter pw = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()));
            String msg = "REGI " + this.id + " " + this.multicastIP + " " + this.multicastPort + " " 
                + this.machineIP + " " + this.userPort + "\r\n";
            pw.print(msg);
            pw.flush();    
            String recv = br.readLine();
            System.out.println(recv);
            pw.close();
            br.close();
            socket.close();
        }
        catch(UnknownHostException e){
            System.out.println("Error when creating socket.");
            e.printStackTrace();
        } catch(IOException e){
            e.printStackTrace();
        }
    }

    public synchronized void write(String code, String originID, String msgToWrite){
        try 
        {
            while(this.readyToWrite == false){
                wait();
            }
            this.readyToWrite = false;
            notifyAll();
            Message m = new Message(this.msgIndex, code, originID, msgToWrite); // TODO: catch exception de la classe Message
            if(this.messageList.size() == LIST_MAX_SIZE)
                this.messageList.removeFirst();
            this.messageList.add(m);
            this.msgIndex = (this.msgIndex+1)%9999;
        } catch (InterruptedException e){
            System.out.println("Wait exception error");
            throw new IllegalArgumentException();
        } 
    }

    public synchronized Message[] read(int n){ //retrieve the last n elements on this.msgList and convert it into a java array
        try
        {
            while(this.readyToWrite == true){
                wait();
            }
            this.readyToWrite = true;
            notifyAll();
        } catch(InterruptedException e){
            System.out.println("Wait exception error");
            throw new IllegalArgumentException();
        }
        LinkedList<Message> list = (LinkedList<Message>)this.messageList.subList(this.messageList.size()-n, this.messageList.size());
        return (Message [])list.toArray();
    }
    
    public synchronized String readRandom(String code){
        Message random = this.messageList.get(new Random().nextInt(this.messageList.size()+1));
        return new Message(random.getMessageNumber(), code, random.getOriginID(), random.getMessage()).toString();
    }

    @Override public String toString(){
        //avant de return convertir les addresses ip et identifiant dans le format demandé
        return id + " " + multicastIP + " " + multicastPort + " " + machineIP + " " + userPort; 
    }

    public static void main(String[] args){
        Message m1 = new Message(0, "DIFF", "bonjour");
        Message m2 = new Message(0, "DIFF", "bonsoir");
        Message[] list = {m1, m2};
        Streamer streamer = new Streamer(args[0], args[1], Integer.parseInt(args[2]), Integer.parseInt(args[3]), list);
        streamer.registerToManager("lulu", 4442);
        StreamerTCP streamerTCP = streamer.new StreamerTCP();
        StreamerUDP streamerUDP = streamer.new StreamerUDP();
        Thread t1 = new Thread(streamerTCP);
        Thread t2 = new Thread(streamerUDP);
        t1.start();
        t2.start();
    }

    private class StreamerTCP implements Runnable{

        private ServerSocket server;

        public StreamerTCP(){
            try{
                this.server = new ServerSocket(Streamer.this.userPort);
            } catch(IOException e){
                System.out.println("Error when creating ServerSocket object instance.");
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
            catch(IOException e){
                System.out.println("Server attempt accepting connection error.");
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
                    while(true)
                    {
                        String[] query = br.readLine().split(" ");
                        if(query[0].equals("LAST"))
                        {
                            if(query.length < 2){
                                pw.print("Missing LAST argument.\n");
                                pw.flush();
                            } else if(query.length != 2) {
                                pw.print("Incorrect LAST argument format.\n");
                                pw.flush();
                            }
                            else
                            {
                                try 
                                {
                                    int n = Integer.valueOf(query[1]);
                                    Message[] history = Streamer.this.read(n);
                                    for(Message m : history)
                                    {
                                        pw.print(m.toString());
                                        pw.flush();
                                    }
                                    pw.print("ENDM");
                                    pw.flush();
                                    break;
                                } catch (NumberFormatException e){
                                    pw.print("Incorrect number format.\n");
                                    pw.flush();
                                }
                            }
                        } else if(query[0].equals("MESS"))
                        {
                            if(query.length < 3) {
                                pw.print("Missing MESS argument.\n");
                                pw.flush();
                            } else if(query.length != 3) {
                                pw.print("Incorrect MESS format.\n");
                                pw.flush();
                            } else
                            {
                                Streamer.this.write(query[0], query[1], query[2]);
                                pw.print("ACKM\n");
                                break;
                            }
                        } else if(query[0].equals("RUOK"))
                        { 
                            pw.print("IMOK");
                            pw.flush();
                            break;
                        }
                    }
                    br.close();
                    pw.close();
                    socket.close();
                }
                catch(IOException e){
                    System.out.println(e);
                    e.printStackTrace();
                }
            }
        }
    }

    private class StreamerUDP implements Runnable{

        private DatagramSocket dso;

        public StreamerUDP(){
            try {
                this.dso = new DatagramSocket();
            } catch(SocketException e){
                System.out.println("Could not create a DatagramSocket");
            }
        }

        @Override public void run(){
            TimerTask task = new TimerTask()
            {
                @Override public void run(){
                    try{
                        byte[] packet = readRandom("DIFF").getBytes();
                        DatagramPacket paquet = new DatagramPacket(packet, packet.length, InetAddress.getByName(Streamer.this.multicastIP), Streamer.this.multicastPort);
                        dso.send(paquet);
                    } catch(IOException e){
                        System.out.println("Streamer could not send package to subscribers");
                        e.printStackTrace();
                    }
                }
            };
            Timer timer = new Timer();
            timer.schedule(task, new Date(), Streamer.DELAY);
        }
    }
}