#include "checker.h"
#include <string.h>
#include <stdlib.h>
int checkIntPositf(char *numb){
    int t = atoi(numb);
    if(t<=0){
        return -1;
    }
    return 1;
}
int checkNumberLast(char *numb){
    //distingish the integer 0, and the "atoi" error witch is zero
    if(strcmp(numb, "0")==0 || strcmp(numb, "00")==0 || strcmp(numb, "000")==0)return 1;
    return checkIntPositf(numb);
}
int checkFormatItems(char items[58]){
    char * tok = strtok(items, " ");
    
}
int checkFormatListItems(char result[99][58], int n){
    for(int i=0; i<n; i++){
        if(checkFormatItems(result[i])==-1)return -1;
    }
    return 0;
}