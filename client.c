#include <sys/types.h>
#include <sys/socket.h>
#include <stdio.h>
#include <string.h>
#include <stdlib.h>
#include <netinet/in.h>
#include <arpa/inet.h>
#include <netdb.h>
#include <unistd.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <pthread.h>
#include <assert.h>
#include "client.h"
#include "checker.h"
//TODO if the connexion is interupted (recv, send ??)
//return len, or -1 if bad format
int getLen(char *message){
    char * tok = strtok(message, " ");
    if(strcmp(tok, "LINB")!=0){
        printf("error : bad format received, must (LINB numbre)\n");
        return -1;
    }
    int i = 0;
    while(tok != NULL){
        if(i==1){//second token
            printf("token = -%s-\n", tok);
            printf("tail = %d\n", strlen(tok));
            if(strlen(tok)!=4){
                printf("error : bad format received (integer integer end of message)\n");
                return -1;
            }
            if(strcmp("\r\n", tok+2)!=0){
                printf("error: bad format (ending of message)\n");
                return -1;
            }
            if(checkIntPositf(tok)==-1){
                printf("error : bad format received (should be a positif integer)\n");
                return -1;
            }
            return atoi(tok);
        }
        tok = strtok(NULL, " ");
        i++;
    }
    if(i>2){ 
        printf("error : bad format received (more than 2 strings)\n");
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
int countPrefixZero(char *paquet){
    int compt = 0;
    for(int i=0; i<3; i++){
        if(paquet[i]!='0')return compt;
        compt++;
    }
    return compt;
}
void removeZeroIp(char *origin, char *copie){
    char source[16];
    memset(source, 0, 16);
    memcpy(source, origin, 16);
    int startCopy = 0;//jamais sur le point
    int startSource = 0;//where we will put the point
    int i_paquet = 0;
    char * tok = strtok(origin, ".");

    while(tok != NULL ) {
        int nbzero = countPrefixZero(tok);
        if(nbzero == 0){ //all the digit to  .
            memcpy(copie+startCopy, source+startSource, 4);
            startCopy += 4;
            startSource += 4;
        }else if(nbzero == 1){//from firt not zero to .
            memcpy(copie+startCopy, source+startSource+1, 3);
            startCopy += 3;
            startSource += 4;
        }else if(nbzero == 2 || nbzero == 3){
            memcpy(copie+startCopy, source+startSource+2, 2);
            startCopy += 2;
            startSource += 4;
        }
        tok = strtok(NULL, ".");
    }
}
//save a socket associated with a streamer
int subscribe(infoStreamer info){
    //parse the streamer buffer
    char reformatedIp[16];
    memset(reformatedIp, 0, 16);
    removeZeroIp(info.multicatIP, reformatedIp);//changing the format of ip

    //preparing the socket to listen
    int sock=socket(PF_INET,SOCK_DGRAM,0);
    if(sock==-1){
        printf("error: can't create socket (subscribe)\n");
        return -1;
    }
    int check=1;
    int test=setsockopt(sock,SOL_SOCKET,SO_REUSEPORT,&check,sizeof(check));
    if(test == -1){
        printf("error: failed to reuse sock\n");
        return -1;
    }
    struct sockaddr_in addr;
    addr.sin_family=AF_INET;
    addr.sin_port=htons(info.multicastPort);
    addr.sin_addr.s_addr=htonl(INADDR_ANY);
    test=bind(sock,(struct sockaddr *)&addr,sizeof(struct sockaddr_in));
    if(test==-1){
        printf("error: failed to bind\n");
        return -1;
    }
    struct ip_mreq inscription;
    inscription.imr_multiaddr.s_addr=inet_addr(reformatedIp);
    inscription.imr_interface.s_addr=htonl(INADDR_ANY);
    test=setsockopt(sock,IPPROTO_IP,IP_ADD_MEMBERSHIP,&inscription,sizeof(inscription));
    if(test == -1){
        printf("error: failed to subscribe to sock\n");
        return -1;
    }
    return sock;
}
void * printMessage(void *s){
    int fd = open("reception.txt", O_WRONLY|O_CREAT, 0666);
    int soc = *((int *)s);
    char tampon[162];
    while(1){
        memset(tampon, 0, 162);
        int rec=recv(soc,tampon,161,0);
        tampon[rec]='\0';
        write(fd, tampon, 161);
    }
    return NULL;
}
void addZero(int n, char *result){
    sprintf(result, "%d", n);
    
    int len = strlen(result);
    if(len == 1){
        sprintf(result, "00%d", n);
    }else if(len == 2){
        sprintf(result, "0%d", n);
    }
    printf("%s\n", result);
}
void fillRandomMessage(char *quote){
    for(int i=0; i<140; i++){
        quote[i]='a';
    }
    quote[140]='\0';
}
int sendMessage(infoStreamer info, char *id){
    //remove unecessary zero in address
    char reformatedIp[16];
    memset(reformatedIp, 0, 16);
    removeZeroIp(info.streamerIP, reformatedIp);//changing the format of ip

    //setup socket
    int soc = socket(PF_INET, SOCK_STREAM, 0);
    if(soc == -1){
        printf("error(printList) : can't create socket to get the list of n message\n");
        return -1;
    }
    struct sockaddr_in adr;
    adr.sin_family = AF_INET;
    adr.sin_port = htons(info.tcpPort);
    int r = inet_aton(reformatedIp, &adr.sin_addr);
    if(r == -1){
        printf("error(sendMessage): failed to fill the adress\n");
        close(soc);
        return -1;
    }
    int connection=connect(soc,(struct sockaddr *)&adr, sizeof(struct sockaddr_in));
    if(connection == -1){
        printf("error(sendMessage) : client can't connect\n");
        return -1;
    }
    char message[157];
    memset(message, 0, 157);
    char quote[141];
    memset(message, 0, 141);
    fillRandomMessage(quote);
    sprintf(message, "MESS %s %s\r\n", id, quote);
    send(soc, message, 156, 0);
    char retour[5];
    memset(retour, 0, 5);
    recv(soc, retour, 4, 0);
    if(strcmp(retour, "ACKM")!=0){
        printf("should get ACKM, close connexion\n");
        close(soc);
        return -1;
    }
    printf("%s\n", retour);
}
//ask and print the last n message
int printList(infoStreamer info){
    //remove unecessary zero in address
    char reformatedIp[16];
    memset(reformatedIp, 0, 16);
    removeZeroIp(info.streamerIP, reformatedIp);//changing the format of ip
    //get number from user
    char number[4];
    memset(number, 0, 4);
    printf("\nGive a number betwen 0 and 999\n\n");
    int len = read(1, number, 3);
    number[len-1]='\0';
    // numbre >= 0
    int test = checkNumberLast(number);
    while(test==-1){
        printf("Enter a positif number, betwen 0 and 999\n\n");
        memset(number, 0, 4);
        len = read(1, number, 3);
        number[len-1]='\0';
        test = checkNumberLast(number);
    }
    int n = atoi(number);
    //setup socket
    int soc = socket(PF_INET, SOCK_STREAM, 0);
    if(soc == -1){
        printf("error(printList) : can't create socket to get the list of n message\n");
        return -1;
    }
    struct sockaddr_in adr;
    adr.sin_family = AF_INET;
    printf("port = %d\n", info.tcpPort);
    adr.sin_port = htons(info.tcpPort);
    printf("reformated = %s\n", reformatedIp);
    int r = inet_aton(reformatedIp, &adr.sin_addr);
    if(r == -1){
        printf("error(printList): failed to fill the adress\n");
        close(soc);
        return -1;
    }
    int connection =connect(soc,(struct sockaddr *)&adr, sizeof(struct sockaddr_in));
    if(connection == -1){
        printf("error(printList) : client can't connect\n");
        return -1;
    }
    //communication
    char message[11];
    memset(message, 0, 11);
    memset(number, 0, 4);
    addZero(n, number);
    sprintf(message, "LAST %s\r\n", number);
    send(soc, message, strlen(message), 0);
    char tampon[162];
    for(int i = 0; i<n; i++){
        memset(tampon, 0, 162);
        int rec=recv(soc, tampon, 161, 0);
        tampon[rec]='\0';
        printf("%s\n", tampon);
    }
}
void extractStreamManager(char *file, char *id, char *ip_manager, char *port_manager){
    FILE * f = fopen(file, "r");
    char *linep = NULL;
    size_t tail = 0;

    getline(&linep, &tail, f);
    //TODO (remplir ## id client)
    memcpy(id, linep, strlen(linep)-1);
    getline(&linep, &tail, f);//9 avec a la ligne
    memcpy(ip_manager, linep, strlen(linep)-1);
    getline(&linep, &tail, f);
    memcpy(port_manager, linep, strlen(linep));
}
int main(int n, char **args){

    assert(n>1);
    //extract manager information from file
    char id[9];
    char ip_manager[16];
    char port_manager[5];
    memset(id, 0, 9);
    memset(ip_manager, 0, 16);
    memset(port_manager, 0, 5);
    extractStreamManager(args[1], id, ip_manager, port_manager);


    printf("\nclient : %s\n\n", id);
    printf("_______________________\n\n");
    printf("Streamer List:\n\n");
    //showing the list of streamer from StreamerManager
    char result[99][58];
    for(int i = 0; i<58; i++)memset(result[i], 0, 58);
    int len = 0;
    int test = getListStreamer(ip_manager, atoi(port_manager), result, &len); //passer en argument grace au fichier de config(gestionnaire)
    if(test == -1){
        printf("failed to get list\n");
        return -1;
    }
    if(len == 0){
        printf("Not streamer yet\n");
    }else{
        if(checkFormatListItems(result, len)==-1){
            return -1;
        }
        for(int i = 0; i<len; i++){
            printf("[%d] %s\n",i, result[i]);
        }
    }
    printf("_______________________\n\n");

    //subscribe to streamer
    int index = -1;
    while(index<0 || index>=n){
        printf("chose the index of streamer: \n\n");
        char buff[3];
        memset(buff, 0, 2);
        read(1, buff, 3);
        index = atoi(buff);
    }
    
    
    //parse the streamer buffer
    infoStreamer stream;
    memset(&stream, 0, sizeof(infoStreamer));
    if(parseStreamer(&stream, result[index])==-1)return -1;
    int soc = subscribe(stream);
    if(soc == -1)return -1;

    //create thread to print the message received on the socket
    pthread_t thread_print;
    int printing = pthread_create(&thread_print, NULL, printMessage, &soc);
    if(printing != 0){
        printf("error: can't create thread to print\n");
        close(soc);
        return -1;
    }

    //other interation wiht the streamer 
    char request[4];
    int t = 0;
    while(1){
        printf("_______________________\n\n");
        printf("(l) List    (m) Message\n\n");
        memset(request, 0, 4);
        t = read(1, request, 3);
        request[t-1] = '\0';
        if(strcmp(request, "m") == 0){
            sendMessage(stream, id);
        }else if(strcmp(request, "l")==0){
            printList(stream);
        }else{
            printf("Unown entry\n");
        }
    }
    return 0;
}