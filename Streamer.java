import java.net.*;
import java.io.*;
import java.util.LinkedList;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Optional;

//execution: java Streamer nicolas 239.255.255.255 4242 5252 streamer_msg

public class Streamer{
    
    private static int LIST_MAX_SIZE = 1000;
    private static int DELAY = 3000;

    private String id;
    private int userPort;
    private String multicastIP;
    private int multicastPort;
    private String machineIP;
    private LinkedList<Message> lastMessages;
    private BufferedReader msgFileReader;

    private String managerAddr;
    private int managerPort;

    private String pathToMsgFile;
    private boolean readyToWriteMessage;
    private Optional<Message> messageFromClient;

    private StreamerTCP streamerTCP;
    private StreamerUDP streamerUDP;

    public Streamer(String configFilePath, String messageFilePath){
        parseConfigFile(configFilePath);
        initMessageList(messageFilePath);
        getStreamerAddr();
        registerToManager(this.managerAddr, this.managerPort);
        initTCPCommunication();
        initMulticastDiffusion();
    }   

    public void parseConfigFile(String path){
        int ligneRead = 0;
        try {
            BufferedReader configFile = new BufferedReader(new FileReader(new File(path)));
            String buffer;
            while((buffer = configFile.readLine()) != null)
            {
                if(ligneRead == 0) //initialisation identifiant
                    initStreamerID(buffer);
                else if(ligneRead == 1) //initialisation adresse multidiffusion
                    this.multicastIP = addressToFormat(buffer); //initialisation port multidiffusion
                else if(ligneRead == 2)
                {
                    if(!isPortCorrect(buffer)){
                        System.out.println("Port de multidiffusion incorrect");
                        System.exit(0);
                    }
                    this.multicastPort = Integer.parseInt(buffer);
                }
                else if(ligneRead == 3) //initialisation port réception TCP
                {
                    if(!isPortCorrect(buffer)){
                        System.out.println("Port de réception TCP incorrect");
                        System.exit(0);
                    }
                    this.userPort = Integer.parseInt(buffer);
                }
                else if(ligneRead == 4) //initialisation adresse et port du gestionnaire
                {
                    this.managerAddr = buffer; 
                    String managerPortStr = configFile.readLine();
                    if(!isPortCorrect(managerPortStr)){
                        System.out.println("Port de gestionnaire incorrect");
                        System.exit(0);
                    }
                    ligneRead++;
                    this.managerPort = Integer.parseInt(managerPortStr);
                }
                else if(ligneRead > 5){
                    System.out.println("Incorrect config file format");
                    System.exit(0);
                }
                ligneRead++;
            }
        } catch(FileNotFoundException e){
            System.out.println("Error: file configuration not found");
            System.exit(0);
        } catch(IOException e){
            System.out.println("Error when parsing config file.");
            System.exit(0);
        }
    }

    public void initTCPCommunication(){
        this.streamerTCP = new StreamerTCP();
        Thread t1 = new Thread(this.streamerTCP);
        t1.start();
    }

    public void initMulticastDiffusion(){
        this.streamerUDP = new StreamerUDP();
        Thread t2 = new Thread(this.streamerUDP);
        t2.start();
    }

    public String addressToFormat(String ip){
        String[] tokens = ip.split("\\.");
        if(tokens.length != 4){
            System.out.println("Incorrect IP address format.");
            System.exit(0);
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
                    System.exit(0);
                } else {
                    tokens[i] = "0".repeat(3-length) + tokens[i];
                }
            } else {
                System.out.println("Incorrect IP address format.");
                System.exit(0);
            }
        }
        return String.join(".", tokens);
    }

    public void getStreamerAddr(){
        try{
            String streamerAddr = InetAddress.getLocalHost().toString();
            String s1 = streamerAddr.substring(streamerAddr.indexOf("/")+1).trim();
            streamerAddr = s1;
            this.machineIP = addressToFormat(streamerAddr);
        } catch(UnknownHostException e){
            System.out.println("Error when retrieving streamer's ip address.");
            System.exit(0);
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

    public static boolean isPortCorrect(String port){
        if(port.length() != 4 || !isNumber(port))
            return false;
        return true;
    }

    public void initStreamerID(String s){
        if(s.equals("")){
            System.out.println("Streamer's ID must not be empty");
            System.exit(0);
        }
        else if(s.length() > 8){
            System.out.println("Streamer's ID must be less than 8 characters");
            System.exit(0);
        }    
        else if(s.length() == 8)
            this.id = s; 
        else
            this.id = s + "#".repeat(8 - s.length());
    }

    public void initMessageList(String path){
        this.pathToMsgFile = path;
        this.readyToWriteMessage = true;
        this.messageFromClient = Optional.empty();
        this.lastMessages = new LinkedList<Message>();
        try {
            File f = new File(path);
            FileReader fr = new FileReader(f);
            this.msgFileReader = new BufferedReader(fr);
        } catch(FileNotFoundException e){
            System.out.println("Could not find messages file");
            System.exit(0);
        }

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
            System.out.println("Error when creating socket to communicate with manager");
            System.exit(0);
        } catch(IOException e){
            System.out.println("IOException error when trying to register to stream manager");
            System.exit(0);
        }
    }

    public synchronized boolean write(String originID, String msgToWrite){
        try 
        {
            while(this.readyToWriteMessage == false){
                wait();
            }
            this.readyToWriteMessage = false;
            this.messageFromClient = Optional.of(new Message(originID, msgToWrite));
            notifyAll();
        } catch (InterruptedException e){
            System.out.println("Wait exception error when writing");
            System.exit(0);
        } 
        catch (IllegalArgumentException e){
            System.out.println("Error when trying to create a Message instance object: message too long !");
            return false;
        }
        return true;
    }

    public LinkedList<Message> readHistory(int n){ //retrieve the last n elements on this.msgList and convert it into a java array
        if(n > this.lastMessages.size()){
            LinkedList<Message> list = this.lastMessages;
            return list;
        } else {
            LinkedList<Message> list = new LinkedList<>(this.lastMessages.subList(0,n));
            return list;
        }
    }
    
    public Message readFileMessage(){
        String s;
        try {
            if((s = this.msgFileReader.readLine()) != null){
                Message read = new Message(this.id, s);
                return read;
            }
            //sinon on a atteint la fin du fichier donc un réinitialise le buffer et on appelle récursivement cette fonction
            this.msgFileReader = new BufferedReader(new FileReader(new File(this.pathToMsgFile)));
        } catch(IOException e){
            System.out.println("IOException when trying to read file message content");
            System.exit(0);
        }
        catch(IllegalArgumentException e){
            System.out.println("Error when trying to create a message: message too long !");
            System.exit(0);
        }
        return this.readFileMessage();

    }

    public void addToHistory(Message m){
        if(this.lastMessages.size() == LIST_MAX_SIZE){
            this.lastMessages.removeLast();
        }
        this.lastMessages.addFirst(m);
    }

    @Override public String toString(){
        return id + " " + multicastIP + " " + multicastPort + " " + machineIP + " " + userPort; 
    }

    public static void main(String[] args){
        if(args.length != 2 ){
            System.out.println("Missing argument or incorrect format.");
        } else {
            new Streamer(args[0], args[1]);
        }
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
                System.exit(0);
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
                System.exit(0);
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
                System.exit(0);
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
                        String query = br.readLine();
                        String[] tokens = query.split(" ");
                        if(tokens[0].equals("LAST"))
                        {
                            if(tokens.length != 2){
                                pw.print("Incorrect LAST argument format.\r\n");
                                pw.flush();
                            } else {
                                if(!isNumber(tokens[1])){
                                    pw.print("Incorrect number format.\r\n");
                                    pw.flush();
                                } else {
                                    int n = Integer.valueOf(tokens[1]);
                                    if(n < 0 || n > LIST_MAX_SIZE){
                                        pw.print("Must be positive and less than " + (LIST_MAX_SIZE-1)+"\r\n");
                                        pw.flush();
                                    } else {
                                        LinkedList<Message> history = Streamer.this.readHistory(n);
                                        for(Message m : history)
                                        {
                                            pw.print("OLDM " + m.toString());
                                            pw.flush();
                                        }
                                        pw.print("ENDM\r\n");
                                        pw.flush();
                                        break;
                                    }
                                }
                            }
                        } 
                        else if(tokens[0].equals("MESS"))
                        {
                            if(tokens.length < 3) {
                                pw.print("Incorrect MESS format.\r\n");
                                pw.flush();
                            } else {
                                if(Streamer.this.write(tokens[1], query.substring(14)) == false){ //TODO: vérifier la taille de l'id du client ?
                                    pw.print("Le diffuseur n'a pas accepté votre message.\r\n");
                                    pw.flush();
                                } else {
                                    pw.print("ACKM\r\n");
                                    pw.flush();
                                    break;
                                }
                            }
                        } else{
                            pw.print("Command not found.\r\n");
                            pw.flush();
                        }
                    }
                    socket.close();
                }
                catch(SocketException e){
                    //utilisateur se déconnecte
                }
                catch(IOException e){
                    System.out.println("IOException ClientCommunication's runnable");
                    e.printStackTrace();
                    System.exit(0);
                }
            }
        }
    }

    private class StreamerUDP implements Runnable{

        private DatagramSocket dso;
        private TimerTask task;
        private int index;

        public StreamerUDP(){
            try {
                this.index = 0;
                this.dso = new DatagramSocket();
            } catch(SocketException e){
                System.out.println("Could not create a DatagramSocket");
                System.exit(0);
            }
        }

        @Override public void run(){
            task = new TimerTask()
            {
                @Override public void run(){
                    try{
                        Message m;
                        if(Streamer.this.messageFromClient.isPresent()) //s'il y a message qu'un client veut faire diffuser
                        {
                            m = Streamer.this.messageFromClient.get();
                            Streamer.this.messageFromClient = Optional.empty();
                            Streamer.this.readyToWriteMessage = true;
                        } else { //sinon on diffuse un message ordinaire du diffuseur
                            m = readFileMessage();
                        }
                        m.setIndex((StreamerUDP.this.index++)%9999);
                        Streamer.this.addToHistory(m);
                        String s = ("DIFF " + m.toString());
                        byte[] packet = s.getBytes();
                        DatagramPacket paquet = new DatagramPacket(packet, packet.length, InetAddress.getByName(Streamer.this.multicastIP), Streamer.this.multicastPort);
                        dso.send(paquet);
                    } catch(IOException e){
                        System.out.println("Streamer could not send package to subscribers");
                        System.exit(0);
                    }
                }
            };
            Timer timer = new Timer();
            timer.schedule(task, new Date(), Streamer.DELAY);
        }
    }
}