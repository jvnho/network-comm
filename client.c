#include <sys/types.h>
#include <sys/socket.h>
#include <stdio.h>
#include <string.h>
#include <stdlib.h>
#include <netinet/in.h>
#include <arpa/inet.h>
#include <netdb.h>
#include <unistd.h>
#include "client.h"
//return len, or -1 if bad format
int getLen(char *message){
    char * tok = strtok(message, " ");
    if(strcmp(tok, "LINB")!=0){
        printf("bad format received\n");
        return -1;
    }
    int i = 0;
    while(tok != NULL ) {
        if(i==1){//second token
            return atoi(tok);
        }
        tok = strtok(NULL, " ");
        i++;
    }
    if(i>2){ 
        printf("bad format received\n");
        return -1;
    }
    return 1;
}

//TO DO -> si adress dans fichier config n'est pas de format iii.iii.
int getListStreamer(char *adrStreamManager, int port, char result[99][58],int *result_len){
    int soc = socket(PF_INET, SOCK_STREAM, 0);
    if(soc == -1){
        printf("error(getListStreamer): socket for geting the list error\n");
        return -1;
    }
    //setup the adress used by the socket
    struct sockaddr_in adr;
    memset(&adr, 0, sizeof(struct sockaddr_in));
    adr.sin_family = AF_INET;
    adr.sin_port = htons(port);
    int r = inet_aton(adrStreamManager, &adr.sin_addr);//check if machine name
    if(r == -1){
        printf("error(getListStreamer): failed to fill the adress\n");
        close(soc);
        return -1;
    }
    r = connect(soc, (const struct sockaddr *)&adr, sizeof(struct sockaddr_in));
    if(r == -1){
        printf("error(getListStreamer): can't connect to streamManager\n");
        close(soc);
        return -1;
    }
    send(soc, "LIST\r\n", sizeof(char)*strlen("LIST\r\n"), 0);
    char lenDescription[10];//7 \r\n add 0 at the end to avoid bad suprises
    memset(lenDescription, 0, 10);
    recv(soc, lenDescription, 9, 0);
    int len = getLen(lenDescription);
    if(len == -1)return -1;
    *result_len = len;
    char items[58];//\0 to avoid bad suprises
    for(int i=0; i<len; i++){
        memset(items, 0, 58);
        recv(soc, items, 57, 0);
        memcpy(result[i], items, 58);
    }
    close(soc);
    return 1;
}
//TO DO -> verify the format
int parseStreamer(infoStreamer *result, char *streamer){
    char * tok = strtok(streamer, " ");
    int i = 0;
    while(tok != NULL){
        if(i==2){//ipMulti
            memcpy(result->multicatIP, tok, 16);
        }else if(i==3){//portMulti
            result->multicastPort = atoi(tok);
        }else if(i==4){//ipStreamer
            memcpy(result->streamerIP, tok, 16);
        }else if(i == 5){//portTcp
            result->tcpPort = atoi(tok);
        }
        tok = strtok(NULL, " ");
        i++;
    }
    return 0;
}
//save a socket associated with a streamer
int subscribe(char *streamer){
    printf("%s\n", streamer);
    //parse the streamer buffer
    infoStreamer result;
    memset(&result, 0, sizeof(infoStreamer));
    if(parseStreamer(&result, streamer)==-1)return -1;
    printf("port multi = %d\n", result.multicastPort);
    printf("ip multi = %s\n", result.multicatIP);
    //create the soc
    int sock=socket(PF_INET, SOCK_DGRAM, 0);
    if(sock == -1){
        printf("error: client failed to subscribe\n");
        return -1;
    }
    //make reusable
    int parameter=1;
    int test=setsockopt(sock, SOL_SOCKET, SO_REUSEPORT, &parameter, sizeof(parameter));
    if(test == -1){
        printf("error: failed to setup socket(reusable)\n");
        return -1;
    }
    //create a address in order to listen
    struct sockaddr_in addr;
    addr.sin_family=AF_INET;
    addr.sin_port=htons(result.multicastPort);
    addr.sin_addr.s_addr=htonl(INADDR_ANY);
    int b = bind(sock,(struct sockaddr *)&addr,sizeof(struct sockaddr_in));
    if(b == -1){
        printf("failed to bind\n");
        return -1;
    }
    //set up the info for the registration
    struct ip_mreq req;
    memset(&req, 0, sizeof(struct ip_mreq));
    req.imr_multiaddr.s_addr = inet_addr(result.multicatIP);
    req.imr_interface.s_addr=htonl(INADDR_ANY);
    b = setsockopt(sock,IPPROTO_IP,IP_ADD_MEMBERSHIP,&req,sizeof(req));
    if(b == -1){
        printf("error: failed to setup socket(subscribe)\n");
        return -1;
    }
    char tampon[162];
    while(1){
        int rec=recv(sock,tampon,161,0);
        tampon[rec]='\0';
        printf("Message recu : %s\n",tampon);
    }
    return sock;
}
int main(void){
    char test[130] = "ITEM diffprof 225.010.020.030 4999 192.168.070.236 5999\r\n";
    subscribe(test);
    /*
    client c;
    memset(&c, 0, sizeof(client));
    memcpy(c.id, "abcdef78", sizeof(char)*strlen("abcdef78"));
    printf("client : %s\n\n", c.id);
    printf("_______________________\n\n");
    printf("Streamer List:\n\n");
    //showing the list of streamer from StreamerManager
    char result[99][58];
    for(int i = 0; i<58; i++)memset(result[i], 0, 58);
    int len = 0;
    //gestionaire sur lulu, port 4141
    int test = getListStreamer("192.168.70.236", 4141, result, &len); //passer en argument grace au fichier de config(gestionnaire)
    if(test == -1){
        printf("failed to get list\n");
        return -1;
    }
    if(len == 0){
        printf("Not streamer yet\n");
    }else{
        for(int i = 0; i<len; i++){
            printf("[%d] %s\n",i, result[i]);
        }
    }
    printf("_______________________\n\n");
    //subscribe to streamer
    printf("chose the index of streamer: \n\n");
    int index = 0;
    char buff[3];
    memset(buff, 0, 2);
    read(1, buff, 3);
    index = atoi(buff);
    int soc = subscribe(result[index]);
    if(soc == -1)return -1;
    char received[162];
    int r = 0;
    printf("fini de subscribe\n");
    while(1){
        memset(received, 0, 162);
        r = recv(soc, received, 161, 0);
        printf("apres receiv\n");
        if(r == -1){
            printf("end of reception\n");
            break;
        }
        printf("%s\n", received);
    }
    */
    
    return 0;
}