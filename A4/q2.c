#include <omp.h>
#include <stdio.h>
#include <stdlib.h>

int setNextState(int startState, int startIndex, int endIndex, char text[], char fill[]);

const char charSet[12] = {'0','1','2','3','4','5','6','7','8','9','.','a'};
#define STATE1 0
#define STATE2 1
#define STATE3 2
#define STATE4 3
#define STATE5 4

int main(int argc,char *argv[]) {
    int i;
    int t=8,n = 10;
    if (argc>1) {
        t = atoi(argv[1]);
    }
    printf("Using %d threads\n",t);   
    omp_set_num_threads(t);

    // Generate a random text
    int textLength = 100;
    char *text = (char *) malloc(sizeof(char)*textLength);
    srand(time(NULL));
    for(int i = 0; i < textLength; i++) {
        text[i] = charSet[rand()%12];
    }

    // Get evenly-sized sections for each thread to work on
    int workingLength = textLength/(t+1);

    // Create two shared variables, one that will tell a thread
    // that the previous one knows its final state and one that
    // will share what state that is.
    int turn = 0;
    int state = STATE1;

    char *fill = (char *) malloc(sizeof(char)*textLength);
    int curState = setNextState(state,0,textLength,text,fill);
    
    for(int j = 0; j < textLength; j++)
        printf("%c",text[j]);
    printf("\nend state: %d\n",curState);
    for(int j = 0; j < textLength; j++)
        printf("%c",fill[j]);
}

int setNextState(int startState, int startIndex, int endIndex, char text[], char fill[]) {
    int currentState = startState;
    int eraseIndexEnd = -1;
    int eraseIndexStart = 0;
    int state5Hit = 0;

    for(int i = startIndex; i < endIndex; i++) {
        printf("CurState: %d, CurChar: %c\n",currentState,text[i]);
        switch(currentState) {
            case STATE1 :
                if(text[i] >= 49 && text[i] <= 57) {
                    printf("Go to STATE2\n");
                    fill[i-startIndex] = text[i];
                    currentState = STATE2;
                }
                else if(text[i] == '0') {
                    printf("Go to STATE3\n");
                    fill[i-startIndex] = text[i];
                    currentState = STATE3;
                }
                else{
                    eraseIndexEnd = i-startIndex;
                    printf("Erase index now: %d\n",eraseIndexEnd);
                    printf("Go to STATE1\n");
                }
                break;
            case STATE2 :
                if(text[i] >= 48 && text[i] <= 57) {
                    printf("Go to STATE2\n");
                    fill[i-startIndex] = text[i];
                    currentState = STATE2;
                }
                else if(text[i] == '.') {
                    printf("Go to STATE4\n");
                    fill[i-startIndex] = text[i];
                    currentState = STATE4;
                }
                else {
                    eraseIndexEnd = i-startIndex;
                    printf("Erase index now: %d\n",eraseIndexEnd);
                    printf("Go to STATE1\n");
                    currentState = STATE1;
                }
                break;
            case STATE3 :
                if(text[i] == '.') {
                    fill[i-startIndex] = text[i];
                    currentState = STATE4;
                    printf("Go to STATE4\n");
                }
                else {
                    i--;
                    eraseIndexEnd = i-startIndex;
                    printf("Erase index now: %d\n",eraseIndexEnd);
                    printf("Go to STATE1\n");
                    currentState = STATE1;
                }
                break;
            case STATE4 :
                if(text[i] >= 48 && text[i] <= 57) {
                    fill[i-startIndex] = text[i];
                    currentState = STATE5;
                    printf("Go to STATE5\n");
                }
                else {
                    eraseIndexEnd = i-startIndex;
                    printf("Erase index now: %d\n",eraseIndexEnd);
                    printf("Go to STATE1\n");
                    currentState = STATE1;
                }
                break;
            case STATE5 :
                if(text[i] >= 48 && text[i] <= 57) {
                    fill[i-startIndex] = text[i];
                    currentState = STATE5;
                    printf("Go to STATE5\n");
                }
                else {
                    if(eraseIndexEnd != -1) {
                        printf("Erase from index %d to %d\n",eraseIndexStart,eraseIndexEnd);
                        for(int j = eraseIndexStart; j <= eraseIndexEnd; j++)
                            fill[j] = ' ';
                        printf("New eraseStartIndex: %d\n",eraseIndexStart);
                        eraseIndexEnd = -1;
                    }
                    eraseIndexStart = i-startIndex+1;
                    fill[i-startIndex] = ' ';
                    currentState = STATE1;
                    printf("Go to STATE1\n");
                }
                break;
            default : break;
        }
    }

    if(eraseIndexEnd != -1) {
        for(int j = eraseIndexStart; j <= eraseIndexEnd; j++)
            fill[j] = ' ';
    }

    return currentState;
}
