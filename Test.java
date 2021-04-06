import java.net.Socket;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

public class Test{
    public static class ClientThread implements Runnable{
        Socket socket;
        public ClientThread(Socket s){
            this.socket = s;
        }
        public void run(){
            try{
                PrintWriter sending = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()));
                sending.println("LIST\r");
                sending.flush();
                System.out.println("envoye list");
                BufferedReader reading = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                String recu = reading.readLine();
                System.out.println("retour serveur = "+recu);
                String[] s = recu.split(" ");
                int len = Integer.parseInt(s[1]);
                System.out.println("len = "+len);
                for(int i=0; i<len; i++){
                    recu = reading.readLine();
                    //System.out.println(recu);
                }
            }
            catch(IOException e){
                System.out.println("erreur thread client");
            }
        }
    }
    public static class DiffuseurThread implements Runnable{
        Socket socket;
        public DiffuseurThread(Socket s){
            this.socket = s;
        }
        public void run(){
            try{
                PrintWriter sender = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()));
                sender.println("REGI RADIO### 127.000.000.001 0120 127.000.000.001 0120\r");
                sender.flush();
                BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                String recu = reader.readLine();
                System.out.println(recu);
            }catch(IOException e){
                System.out.println("erreur thread diffuseur");
            }
        }
    }
    public static void main(String []args){
        try{
            //test diffuseur communique avec gestionnaire
            for(int i = 0; i<110; i++){
                new Thread(new DiffuseurThread(new Socket("lulu",Integer.parseInt(args[0])))).start();
            }
            /*
            //test client communique avec gestionnaire
            for(int i = 0; i<50; i++){
                new Thread(new ClientThread(new Socket("lulu",Integer.parseInt(args[0])))).start();
            }*/
            
        }catch(Exception e){
            System.out.println("error on create socket");
            e.printStackTrace();
        }
        
    }
}