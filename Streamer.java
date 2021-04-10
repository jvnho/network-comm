import java.net.*;
import java.io.*;
import java.util.LinkedList;
import java.util.Random;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

//nicolas 239.255.255.255 5452 7879

public class Streamer{
    
    private static int LIST_MAX_SIZE = 1000;
    private static int DELAY = 3000;
    private static String[] messages = {
        "She only paints with bold colors, she does not like pastels.",
        "Getting up at dawn is for the birds.",
        "He excelled at firing people nicely.",
        "Be careful with that butter knife.",
        "He waited for the stop sign to turn to a go sign."
    };

    private String id;
    private int userPort;
    private String multicastIP;
    private int multicastPort;
    private String machineIP;
    private int msgIndex;
    private LinkedList<Message> messageList;
    private boolean readyToWrite;

    public Streamer(String id, String multicastIP, int multicastPort, int userPort){
        initStreamerID(id);
        initMessageList();
        initPorts(multicastPort, userPort);
        initAdresses(multicastIP);
    }   

    public String addressToFormat(String ip){
        String[] tokens = ip.split("\\.");
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
        return String.join(".", tokens);
    }

    public void initAdresses(String multicastIP){
        this.multicastIP = addressToFormat(multicastIP);
        try{
            String streamerAddr = InetAddress.getLocalHost().toString();
            String s1 = streamerAddr.substring(streamerAddr.indexOf("/")+1);
            s1.trim();
            streamerAddr = s1;
            this.machineIP = addressToFormat(streamerAddr);
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

    public void initMessageList(){
        this.msgIndex = 0;
        this.messageList = new LinkedList<Message>();
        for(int i = 0; i < messages.length; i++)
        {
            this.messageList.add(new Message(this.msgIndex, "DIFF", this.id, messages[i]));
            this.msgIndex++;
        }
        this.readyToWrite = true;
    }

    public void registerToManager(String managerAddr, int managerPort){
        try
        {
            Socket managerSocket = new Socket(managerAddr, managerPort);
            BufferedReader br = new BufferedReader(new InputStreamReader(managerSocket.getInputStream()));
            PrintWriter pw = new PrintWriter(new OutputStreamWriter(managerSocket.getOutputStream()));
            StreamManagerCommunication smc = new StreamManagerCommunication(managerSocket, br, pw);
            Thread t = new Thread(smc);
            t.start();

        }
        catch(UnknownHostException e){
            System.out.println("Error when creating socket.");
            e.printStackTrace();
        } catch(IOException e){
            e.printStackTrace();
        }
    }

    public synchronized boolean write(String code, String originID, String msgToWrite){
        try 
        {
            while(this.readyToWrite == false){
                wait();
            }
            this.readyToWrite = false;
            notifyAll();
            Message m = new Message(this.msgIndex, code, originID, msgToWrite);
            if(this.messageList.size() == LIST_MAX_SIZE)
                this.messageList.removeFirst();
            this.messageList.add(m);
            this.msgIndex = (this.msgIndex+1)%9999;
        } catch (InterruptedException e){
            System.out.println("Wait exception error when writing");
            return false;
        } 
        catch (IllegalArgumentException e){
            System.out.println("Error when trying to create a Message instance object");
            return false;
        }
        return true;
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
            System.out.println("Wait exception error when reading");
            e.printStackTrace();
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
        Streamer streamer = new Streamer(args[0], args[1], Integer.parseInt(args[2]), Integer.parseInt(args[3]));
        streamer.registerToManager("lulu", 4442);
        Thread t1 = new Thread(streamer.new StreamerTCP());
        Thread t2 = new Thread(streamer.new StreamerUDP());
        t1.start();
        t2.start();
    }

    private class StreamManagerCommunication implements Runnable {

        private Socket communicationStreamer;
        private BufferedReader br;
        private PrintWriter pw;

        public StreamManagerCommunication(Socket s, BufferedReader br, PrintWriter pw){
            this.communicationStreamer = s;
            this.br = br;
            this.pw = pw;
        }
        
        @Override public void run(){
            String msg = "REGI " + Streamer.this.toString() +  "\r\n";
            this.pw.print(msg);
            this.pw.flush();    
            try {
                String recv = br.readLine();
                System.out.println(recv);
                if(recv.equals("REOK"))
                {
                    while(true)
                    {
                        recv = br.readLine();
                        System.out.println(recv);
                        if(recv.equals("RUOK"))
                        { 
                            pw.print("IMOK\r\n");
                            pw.flush();
                        }
                    }
                } else {
                    this.br.close();
                    this.pw.close();
                    this.communicationStreamer.close();
                }
            } catch (IOException e){
                System.out.println("Erreur readLine dans le thread StreamManagerCommunication");
                e.printStackTrace();
            }
        }
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
                    BufferedReader br = new BufferedReader(new InputStreamReader(this.socket.getInputStream()));
                    PrintWriter pw = new PrintWriter(new OutputStreamWriter(this.socket.getOutputStream()));
                    while(true)
                    {
                        String[] query = br.readLine().split(" ");
                        if(query[0].equals("LAST"))
                        {
                            if(query.length != 2){
                                pw.print("Incorrect LAST argument format.\r\n");
                                pw.flush();
                            } else {
                                if(!isNumber(query[1])){
                                    pw.print("Incorrect number format.\r\n");
                                    pw.flush();
                                } else {
                                    int n = Integer.valueOf(query[1]);
                                    Message[] history = Streamer.this.read(n);
                                    for(Message m : history)
                                    {
                                        pw.print(m.toString());
                                        pw.flush();
                                    }
                                    pw.print("ENDM\r\n");
                                    pw.flush();
                                    break;
                                }
                            }
                        } else if(query[0].equals("MESS")){
                            if(query.length != 3) {
                                pw.print("Incorrect MESS format.\r\n");
                                pw.flush();
                            } else {
                                if(Streamer.this.write(query[0], query[1], query[2]) == false){
                                    pw.print("Le diffuseur n'a pas accepté votre message.\r\n");
                                    pw.flush();
                                }
                                pw.print("ACKM\r\n");
                                pw.flush();
                                break;
                            }
                        } else{
                            pw.print("Command not found.\r\n");
                            pw.flush();
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