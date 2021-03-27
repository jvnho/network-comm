import java.net.Socket;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

public class Test{
    public static void main(String []args){
        try{
            //test diffuseur communique avec gestionnaire
            Socket socket=new Socket("lulu",Integer.parseInt(args[0]));
            PrintWriter sender = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()));
            sender.println("REGI RADIO### 127.000.000.001 0120 127.000.000.001 0120\r");
            sender.flush();
            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            String recu = reader.readLine();
            System.out.println(recu);
        }catch(Exception e){
            System.out.println("error on create socket");
            e.printStackTrace();
        }
        
    }
}