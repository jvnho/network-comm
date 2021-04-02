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
                sending.flush();
                BufferedReader reading = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                String recu = reading.readLine();
                String[] s = recu.split(" ");
                int len = Integer.parseInt(s[1]);
                System.out.println("len = "+len);
                for(int i=0; i<len; i++){
                    recu = reading.readLine();
                    System.out.println(recu);
                }
            }
            catch(IOException e){}
        }
    }
    public static void main(String []args){
        try{
            //test diffuseur communique avec gestionnaire
            /*Socket socket=new Socket("lulu",Integer.parseInt(args[0]));
            PrintWriter sender = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()));
            sender.println("REGI RADIO### 127.000.000.001 0120 127.000.000.001 0120\r");
            sender.flush();
            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            String recu = reader.readLine();
            System.out.println(recu);*/

            //test client communique avec gestionnaire
            Socket socket=new Socket("lulu",Integer.parseInt(args[0]));
            for(int i = 0; i<40; i++){
                new Thread(new ClientThread(socket)).start();
            }
            
        }catch(Exception e){
            System.out.println("error on create socket");
            e.printStackTrace();
        }
        
    }
}