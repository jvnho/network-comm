#ifndef BLOC_H
#define BLOC_H
typedef struct infoStreamer{
    char multicatIP[16];
    char streamerIP[16];
    int multicastPort;
    int tcpPort;
}infoStreamer;
extern int getListStreamer(char *adrStreamManager, int port, char result[99][58], int *result_len);
extern int subscribe(infoStreamer info);
#endif