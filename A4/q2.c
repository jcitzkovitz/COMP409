#include <omp.h>
#include <stdio.h>
#include <stdlib.h>
#include <math.h>

int setNextState(int startState, int startIndex, int endIndex, char text[], char keepText[]);
void modifyString(char text[], char keepText[], int textLength);
void cleanKeepText(char keepText[], int textLength);

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

    // Create two shared variables, one that will tell a thread
    // that the previous one knows its final state and one that
    // will share what state that is.
    int turn = 0;
    int state = STATE1;

    char *keepText = malloc(sizeof(char)*textLength);
    
    #pragma omp parallel for
    for (int i=0;i<t+1;i++) {
        int localWorkingLength = workingLength;
        if(i == 0) {
            char *fill = (char *) malloc(sizeof(char)*localWorkingLength);
            int nextState = setNextState(state, 0, localWorkingLength, text, fill);
            state = nextState;
            for(int j = 0; j < localWorkingLength; j++) {
                keepText[j] = fill[j];
            }
            printf("Iteration %d done by thread %d on size %d at index %d to index %d with chosen state %d\n",
               i,
               omp_get_thread_num(),localWorkingLength, 0, localWorkingLength-1,state);
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

            int startState1 = setNextState(STATE1, startIndex, startIndex + localWorkingLength, text, startStateFill1);
            int startState2 = setNextState(STATE2, startIndex, startIndex + localWorkingLength, text, startStateFill2);
            int startState3 = setNextState(STATE3, startIndex, startIndex + localWorkingLength, text, startStateFill3);
            int startState4 = setNextState(STATE4, startIndex, startIndex + localWorkingLength, text, startStateFill4);
            int startState5 = setNextState(STATE5, startIndex, startIndex + localWorkingLength, text, startStateFill5);

            while(turn != i);

            printf("Iteration %d done by thread %d on size %d at index %d to index %d with chosen state %d\n",
               i,
               omp_get_thread_num(),localWorkingLength, startIndex, startIndex+localWorkingLength-1,state);

            char *fill;
            switch(state) {
                case STATE1 : fill = startStateFill1; state = startState1; break;
                case STATE2 : fill = startStateFill2; state = startState2; break;
                case STATE3 : fill = startStateFill3; state = startState3; break;
                case STATE4 : fill = startStateFill4; state = startState4; break;
                case STATE5 : fill = startStateFill5; state = startState5; break;
                default : break;
            }

            for(int j = 0; j < localWorkingLength; j++) {
                keepText[startIndex+j] = fill[j];
            }
            
            turn = i+1;
        }
    }

    // Modify the string accordingly
    modifyString(text,keepText,textLength);

    // Print final string
    for(int i = 0; i < textLength; i++)
        printf("%c",keepText[i]);
    printf("\n");
    for(int i = 0; i < textLength; i++)
        printf("%c",text[i]);

}

void modifyString(char text[], char keepText[], int textLength) {

    // Clean keeptText array
    cleanKeepText(keepText,textLength);

    // Find out what character code is found first
    int firstS = 0;
    for(int i = 0; i < textLength; i++) {
        if(keepText[i] == 'S') {
            firstS = 1;
            printf("Found s\n");
            break;
        } else if(keepText[i] == 'E') {
            printf("Found e\n");
            break;
        }
    }

    // Set the initial segment depending on whether the first code is an S or an E
    int i = 0;
    while(i < textLength && keepText[i] != 'S' && keepText[i] != 'E') {
        if(firstS == 1) {
            text[i] = ' ';
        }
        i++;
    }

    // Move the index forward one position if an e was found first
    if(firstS == 0)
        i++;
    
    // Run through the rest of keepText and set the text array accordingly
    int setEmpty = 1;
    for(; i < textLength; i++) {
        while(i < textLength && keepText[i] != 'S') {
            text[i] = ' ';
            i++;
        }
        text[i] = ' ';
        i++;
        while(i < textLength && keepText[i] != 'E') {
            i++;
        }
    }

    // Check what the last character code is - if its an S, erase the remainder of the text
    int endS = 0;
    for(i = textLength-1; i >= 0; i--) {
        if(keepText[i] == 'S') {
            endS = 1;
            break;
        } else if(keepText[i] == 'E') {
            break;
        }
    }

    if(endS == 1) {
        for(; i < textLength; i++)
            text[i] = ' ';
    }
}

void cleanKeepText(char keepText[], int textLength) {
    // Parse keepText array to set correct start/stop points for well formatted floating points
    int prevS = -1;
    int seenS = 1;
    for(int i = 0; i < textLength; i++)
        printf("%c",keepText[i]);
    printf("\n");
    for(int i = 0; i < textLength; i++) {
        if(keepText[i] == 'S') {
            if(seenS == 1){
                if(prevS != -1){
                    keepText[prevS] = '-';
                }
            } else {
                seenS = 1;
            }
            prevS = i;
        }

        if(keepText[i] == 'E') {
            if(seenS == 1) {
                seenS = 0;
            } else {
                while(keepText[i] == 'E') {
                    keepText[i-1] = '-';
                    i++;
                }
                prevS = i;
                seenS = 1;
            }
        }
    }
}

int setNextState(int startState, int startIndex, int endIndex, char text[], char keepText[]) {
    int currentState = startState;
    int curStartKeep = 0;
    int curEndKeep = 0;

    for(int i = startIndex; i < endIndex; i++)
        keepText[i-startIndex] = '-';

    for(int i = startIndex; i < endIndex; i++) {
        switch(currentState) {
            case STATE1 :
                if(text[i] >= 49 && text[i] <= 57) {
                    currentState = STATE2;
                }
                else if(text[i] == '0') {
                    currentState = STATE3;
                }
                else{
                    keepText[i-startIndex] = 'S';
                }
                break;
            case STATE2 :
                if(text[i] >= 48 && text[i] <= 57) {
                    currentState = STATE2;
                }
                else if(text[i] == '.') {
                    currentState = STATE4;
                }
                else {
                    keepText[i-startIndex] = 'S';
                    currentState = STATE1;
                }
                break;
            case STATE3 :
                if(text[i] == '.') {
                    currentState = STATE4;
                }
                else {
                    i--;
                    keepText[i-startIndex] = 'S';
                    currentState = STATE1;
                }
                break;
            case STATE4 :
                if(text[i] >= 48 && text[i] <= 57) {
                    keepText[i-startIndex] = 'E';
                    currentState = STATE5;
                }
                else {
                    keepText[i-startIndex] = 'S';
                    currentState = STATE1;
                }
                break;
            case STATE5 :
                if(text[i] >= 48 && text[i] <= 57) {
                    keepText[i-startIndex] = 'E';
                    currentState = STATE5;
                }
                else {
                    keepText[i-startIndex] = 'S';
                    currentState = STATE1;
                }
                break;
            default : break;
        }
    }

    return currentState;
}
