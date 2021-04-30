#include "checker.h"
#include <string.h>
#include <stdlib.h>
#include <stdio.h>
//gcc -pthread -o client client.c checker.h checker.c
int checkIntPositf(char *numb){
    int t = atoi(numb);
    if(t<=0){
        return -1;
    }
    return 1;
}
int checkNumberLast(char *numb){//
    //distingish the integer 0, and the "atoi" error witch is zero
    if(strcmp(numb, "0")==0 || strcmp(numb, "00")==0 || strcmp(numb, "000")==0)return 1;
    return checkIntPositf(numb);
}
int checkport(char *port){
    if(strlen(port) != 4){
        printf("bad format: lenght of port should be 4\n");
        return -1;
    }
    int t = atoi(port);
    if(t>9999 || t<=0){
        printf("bad format: port shoud be a numbre less than 9999\n");
        return -1;
    }
    return 1;
}
int checkip(char *ip){
    printf("chek ip\n");
    char * tok = strtok(ip, ".");
    int i=0;
    while(tok != NULL){
        if(strlen(tok)!=3){
            printf("bad format: ip must be a group of 3 digit\n");
            return -1;
        }
        if(checkNumberLast(tok)==-1){
            printf("bad format: ip must be composed by number\n");
            return -1;
        }
        tok = strtok(NULL, ".");
        i++;
    }
    if(i!=4){
        printf("bad format: ip should be a group of 4 number\n");
        return -1;
    }
    return 1;
}

int checkFormatItems(char items[58]){
    char * tok = strtok(items, " ");
    int i=0;
    while(tok != NULL){
        if(i == 0){//label
            if(strcmp(tok, "ITEM")!=0){
                printf("bad format: should start with ITEMS\n");
                return -1;
            }
        }else if(i==1){//id
            if(strlen(tok)!=8){
                printf("bad format: sizeof id should be 8\n");
                return -1;
            }
        }else if(i==2 || i==4){//ip
            if(checkip(tok)==-1)return -1;
        }else if(i==3){//port
            if(checkport(tok)==-1)return -1;
        }else if(i==5){
            //TO DO : careful  "\r\n" at the end
            if(checkport(tok)==-1)return -1;
        }
        tok = strtok(NULL, " ");
        i++;
    }
    return 1;
}
int checkFormatListItems(char result[99][58], int n){
    char copy[58];
    for(int i=0; i<n; i++){
        memset(copy, 0, 58);
        strcpy(copy, result[i]);
        if(checkFormatItems(copy)==-1)return -1;
    }
    return 0;
}