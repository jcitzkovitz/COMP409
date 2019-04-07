#include <omp.h>
#include <stdio.h>
#include <stdlib.h>
#include <math.h>

int *setNextState(int startState, int startIndex, int endIndex, char text[], char fill[]);

const char charSet[12] = {'0','1','2','3','4','5','6','7','8','9','.','a'};
#define STATE1 0
#define STATE2 1
#define STATE3 2
#define STATE4 3
#define STATE5 4

int main(int argc,char *argv[]) {
    int t=8,n = 10;
    if (argc>1) {
        t = atoi(argv[1]);
    }
    printf("Using %d threads\n",t+1);   
    omp_set_num_threads(t+1);

    // Generate a random text
    float textLength = 100;
    char *text = (char *) malloc(sizeof(char)*textLength);
    srand(time(NULL));
    for(int i = 0; i < textLength; i++) {
        text[i] = charSet[rand()%12];
        printf("%c",text[i]);
    }
    printf("\n");

    // Get evenly-sized sections for each thread to work on
    int workingLength = ceil(textLength/(t+1));
    printf("Working length: %d\n",workingLength);

    // Create two shared variables, one that will tell a thread
    // that the previous one knows its final state and one that
    // will share what state that is.
    int turn = 0;
    int state = STATE1;
    int startErase = 0;
    
    #pragma omp parallel for
    for (int i=0;i<t+1;i++) {
        int localWorkingLength = workingLength;
        if(i == 0) {
            char *fill = (char *) malloc(sizeof(char)*localWorkingLength);
            int *nextState = setNextState(state, 0, localWorkingLength, text, fill);
            startErase = *(nextState+1);
            state = *nextState;
            for(int j = 0; j < localWorkingLength; j++)
                text[j] = fill[j];
            printf("Iteration %d done by thread %d with end state %d with erase index %d\n",
               i,
               omp_get_thread_num(),state,startErase);
            for(int j = 0; j < localWorkingLength; j++) {
                printf("%c",text[j]);
            }
            printf("!\n");
            turn = 1;
        } else {

            int startIndex = i*localWorkingLength;
            
            if(i == t) {
                localWorkingLength = textLength - i*localWorkingLength;
            }
            

            char *startStateFill1 = (char *) malloc(sizeof(char)*localWorkingLength);
            char *startStateFill2 = (char *) malloc(sizeof(char)*localWorkingLength);
            char *startStateFill3 = (char *) malloc(sizeof(char)*localWorkingLength);
            char *startStateFill4 = (char *) malloc(sizeof(char)*localWorkingLength);
            char *startStateFill5 = (char *) malloc(sizeof(char)*localWorkingLength);

            int *startState1 = setNextState(STATE1, startIndex, startIndex + localWorkingLength, text, startStateFill1);
            int *startState2 = setNextState(STATE2, startIndex, startIndex + localWorkingLength, text, startStateFill2);
            int *startState3 = setNextState(STATE3, startIndex, startIndex + localWorkingLength, text, startStateFill3);
            int *startState4 = setNextState(STATE4, startIndex, startIndex + localWorkingLength, text, startStateFill4);
            int *startState5 = setNextState(STATE5, startIndex, startIndex + localWorkingLength, text, startStateFill5);

            while(turn != i);

            int prevStartErase = (i-1)*workingLength + startErase;

            char *fill;
            switch(state) {
                case STATE1 : fill = startStateFill1; state = *startState1; startErase = *(startState1+1); break;
                case STATE2 : fill = startStateFill2; state = *startState2; startErase = *(startState2+1); break;
                case STATE3 : fill = startStateFill3; state = *startState3; startErase = *(startState3+1); break;
                case STATE4 : fill = startStateFill4; state = *startState4; startErase = *(startState4+1); break;
                case STATE5 : fill = startStateFill5; state = *startState5; startErase = *(startState5+1); break;
                default : break;
            }

            printf("Iteration %d done by thread %d on size %d at index %d to index %d with chosen state %d with prevEraseIndex %d\n",
               i,
               omp_get_thread_num(),localWorkingLength, startIndex, startIndex+localWorkingLength,state,prevStartErase);

            if(fill[0] == ' ') {
                for(int j = prevStartErase; j < startIndex; j++)
                    text[j] = ' ';
            }
            
            for(int j = 0; j < localWorkingLength; j++) {
                text[startIndex+j] = fill[j];
            }

            if(i == t && state != STATE5) {
                for(int j = startIndex+startErase; j < textLength; j++)
                    text[j] = ' ';
            }

            for(int j = 0; j < startIndex+localWorkingLength; j++) {
                char c;
                if(j >= startIndex) {
                    c = text[j];
                } else {
                    c = ' ';
                }
                printf("%c",c);
            }
            printf("!\n");
            
            turn = i+1;
        }
    }

    for(int i = 0; i < textLength; i++) 
        printf("%c",text[i]);
}

int *setNextState(int startState, int startIndex, int endIndex, char text[], char fill[]) {
    int currentState = startState;
    int eraseIndexEnd = -1;
    int eraseIndexStart = 0;
    int returnEraseIndexStart = -1;

    for(int i = startIndex; i < endIndex; i++) {
        // printf("CurState: %d, CurChar: %c\n",currentState,text[i]);
        switch(currentState) {
            case STATE1 :
                if(text[i] >= 49 && text[i] <= 57) {
                    // printf("Go to STATE2\n");
                    fill[i-startIndex] = text[i];
                    currentState = STATE2;
                }
                else if(text[i] == '0') {
                    // printf("Go to STATE3\n");
                    fill[i-startIndex] = text[i];
                    currentState = STATE3;
                }
                else{
                    eraseIndexEnd = i-startIndex;
                    // printf("Erase index now: %d\n",eraseIndexEnd);
                    // printf("Go to STATE1\n");
                }
                break;
            case STATE2 :
                if(text[i] >= 48 && text[i] <= 57) {
                    // printf("Go to STATE2\n");
                    fill[i-startIndex] = text[i];
                    currentState = STATE2;
                }
                else if(text[i] == '.') {
                    // printf("Go to STATE4\n");
                    fill[i-startIndex] = text[i];
                    currentState = STATE4;
                }
                else {
                    eraseIndexEnd = i-startIndex;
                    // printf("Erase index now: %d\n",eraseIndexEnd);
                    // printf("Go to STATE1\n");
                    currentState = STATE1;
                }
                break;
            case STATE3 :
                if(text[i] == '.') {
                    fill[i-startIndex] = text[i];
                    currentState = STATE4;
                    // printf("Go to STATE4\n");
                }
                else {
                    i--;
                    eraseIndexEnd = i-startIndex;
                    // printf("Erase index now: %d\n",eraseIndexEnd);
                    // printf("Go to STATE1\n");
                    currentState = STATE1;
                }
                break;
            case STATE4 :
                if(text[i] >= 48 && text[i] <= 57) {
                    fill[i-startIndex] = text[i];
                    currentState = STATE5;
                    // printf("Go to STATE5\n");
                }
                else {
                    eraseIndexEnd = i-startIndex;
                    // printf("Erase index now: %d\n",eraseIndexEnd);
                    // printf("Go to STATE1\n");
                    currentState = STATE1;
                }
                break;
            case STATE5 :
                if(returnEraseIndexStart == -1) {
                    returnEraseIndexStart = eraseIndexStart;
                }
                if(text[i] >= 48 && text[i] <= 57) {
                    fill[i-startIndex] = text[i];
                    currentState = STATE5;
                    returnEraseIndexStart++;
                    // printf("Go to STATE5\n");
                }
                else {
                    if(eraseIndexEnd != -1) {
                        // printf("Erase from index %d to %d\n",eraseIndexStart,eraseIndexEnd);
                        for(int j = eraseIndexStart; j <= eraseIndexEnd; j++)
                            fill[j] = ' ';
                        // printf("New eraseStartIndex: %d\n",eraseIndexStart);
                        eraseIndexEnd = -1;
                    }
                    eraseIndexStart = i-startIndex+1;
                    returnEraseIndexStart = eraseIndexStart;
                    fill[i-startIndex] = ' ';
                    currentState = STATE1;
                    // printf("Go to STATE1\n");
                }
                break;
            default : break;
        }
    }

    if(eraseIndexEnd != -1) {
        for(int j = eraseIndexStart; j <= eraseIndexEnd; j++)
            fill[j] = ' ';
    }

    int *returnArray = malloc(sizeof(int)*2);
    *(returnArray) = currentState;
    *(returnArray+1) = returnEraseIndexStart;
    return returnArray;
}
