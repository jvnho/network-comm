public class Format{
    static int[] lenMessageRegi = new int[]{4, 8, 15, 4, 15, 4};
    static String[] message = new String[]{"REGI", "ID", "IP1", "port1", "IP2", "port2"};
    public static boolean isIpV4(String s){
        
        //"127.000.000.001"
        String[] content = s.split("\\.");
        int len = content.length;
        if(len != 4)return false;
        for(int i= 0; i<len; i++){
            try{
                if(content[i].length() != 3)return false;
                int numb = Integer.parseInt(content[i]);
                if(numb<0 || numb>255)return false;
            }catch(NumberFormatException e){
                return false;
            }
        }
        return true;
    }
    public static boolean isregi(String s){
        System.out.println(s);
        
        //creer des class exection pour faciliter les message d'erreur
        String []content = s.split(" ");
        if(content.length != 6){
            System.out.println("wrong spacing");
            return false; //wrong spacing
        }
        int nbString = content.length;
        for(int i=0; i<nbString; i++){
            if(content[i].length() != lenMessageRegi[i]){
                System.out.println("wrong size of "+message[i]);
                return false;//wrong size
            }
        }
        if(content[0].equals("REGI")==false){
            System.out.println("wrong begining");
            return false; //wrong begining
        }
        try {
            int port1 = Integer.parseInt(content[3]);
            int port2 = Integer.parseInt(content[5]);
            if(port1<0 || port1>9999 || port2<0 || port2>9999){
                System.out.println("invalid num port");
                return false;//invalid numm port
            }
        } catch (NumberFormatException e) {
            System.out.println("port is not a integer");
            //is not a integer
            return false;
        }
        if(isIpV4(content[2]) == false || isIpV4(content[4]) == false){
            System.out.println("wrong ipv4 format");
            return false;
        }
        return true;
    }
}