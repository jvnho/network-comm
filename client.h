#ifndef BLOC_H
#define BLOC_H
typedef struct infoStreamer{
    char multicatIP[16];
    char streamerIP[16];
    int multicastPort;
    int tcpPort;
}infoStreamer;
typedef struct client{
    char id[9];//plus \0
    int socSubstcrition[99];
    int maxSubscription;
    int iSubscription;//current index
}client;
extern int getListStreamer(char *adrStreamManager, int port, char result[99][58], int *result_len);
extern int subscribe(client *cl, char *streamer);
#endif