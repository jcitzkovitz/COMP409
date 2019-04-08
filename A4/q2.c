#include <omp.h>
#include <stdio.h>
#include <stdlib.h>
#include <math.h>
#include <time.h>
#include <sys/time.h>

int setNextState(int startState, int startIndex, int endIndex, char text[], char keepText[]);
void modifyString(char text[], char keepText[], int textLength);
void cleanKeepText(char keepText[], int textLength);
void checkState3SpecialCase(char keepText[], int prevEndState, int startIndex);

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
  
    omp_set_num_threads(t+1);

    // Generate a random text.

    // A text length of 70000000 takes around 500 ms to perform with 0 optimistic threads, but alot easier to see correct results
    // on strings of less than 200.
    double textLength = 100;
    char *text = (char *) malloc(sizeof(char)*textLength);
    srand(time(NULL));
    for(int i = 0; i < textLength; i++) {
        text[i] = charSet[rand()%12];
        printf("%c",text[i]);
    }
    printf("\n");

    // Get evenly-sized sections for each thread to work on.
    int workingLength = ceil(textLength/(t+1));

    // Create two shared variables, one that will tell a thread
    // that the previous one knows its final state and that it
    // may procede with its computations and one to hold the
    // ending state of the previous thread.
    int turn = 0;
    int state = STATE1;

    // Use a character array to label where correct floating point
    // characters lay in the text array. 'S' for start at the next
    // index, and 'E' for where to end. 'S' and 'E' are set in the
    // setNextState function.
    char *keepText = malloc(sizeof(char)*textLength);
    for(int i = 0; i < textLength; i++)
        keepText[i] = '-';

    struct timespec start, end;
    clock_gettime(CLOCK_MONOTONIC, &start);

    
    // Split the computation amongst the state number of threads.
    // Each thread, besides the first, will compute the next state
    // assuming it begins at any of the 5 states. When the previous
    // thread has chosen what state it ends in, the following thread
    // will know what it begins in and may chose the correct computation.
    #pragma omp parallel for
    for (int i=0;i<t+1;i++) {

        int localWorkingLength = workingLength;

        // If you are the first thread, you know you are in STATE1 - no
        // need to compute the other states.
        if(i == 0) {

            char *fill = (char *) malloc(sizeof(char)*localWorkingLength);

            state = setNextState(state, 0, localWorkingLength, text, fill);

            for(int j = 0; j < localWorkingLength; j++) {
                keepText[j] = fill[j];
            }

            // Notify the second thread that it may continue with its computations.
            turn = 1;

        } else {

            int startIndex = i*localWorkingLength;
            
            // The size of work for each thread may be uneven due to the text length and
            // the number of threads selected. If so, the last thread should get the 
            // remaining amount of indecies.
            if(i == t) {
                localWorkingLength = textLength - i*localWorkingLength;
            }

            // Do computations with the indecies of your thread as if you may start at any one of the 5 states.
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

            // Wait for previous threads to indicate that it is your turn.
            while(turn != i);

            int prevEndState = state;

            // Based on the previous threads computations, based on which state you actually started in select the correct
            // starting state for the next thread.
            char *fill;
            switch(state) {
                case STATE1 : fill = startStateFill1; state = startState1; break;
                case STATE2 : fill = startStateFill2; state = startState2; break;
                case STATE3 : fill = startStateFill3; state = startState3; break;
                case STATE4 : fill = startStateFill4; state = startState4; break;
                case STATE5 : fill = startStateFill5; state = startState5; break;
                default : break;
            }

            // Fill out the keepText array with the correct 'S' and 'E' entries.
            for(int j = 0; j < localWorkingLength; j++) {
                keepText[startIndex+j] = fill[j];
            }

            // Check special case where STATE3 was passed to the current iteration and the start index does not store a period.
            // This raises an issue because if this index does not hold a period, the current value must be reconsidered, but the
            // previous '0' character must also be removed. Because this iteration only had access to its portion of the computation
            // it could not motify the previous iterations portion, and thus must take action now.
            checkState3SpecialCase(keepText,prevEndState,startIndex);
            
            // Indicate to the next thread that it may procede with its computations.
            turn = i+1;
        }
    }

    clock_gettime(CLOCK_MONOTONIC, &end);
    uint64_t delta_us = (end.tv_sec - start.tv_sec) * 1000000 + (end.tv_nsec - start.tv_nsec) / 1000;

    // Uncomment the line below to see timing results.
    // printf("\nTime Elapsed: %lu\n", delta_us);

    // Modify the string accordingly based on the keepText arrays information.
    modifyString(text,keepText,textLength);

    // Print final string
    for(int i = 0; i < textLength; i++)
        printf("%c",text[i]);

}

// Check the special case for when STATE3 is passed to a following thread.
void checkState3SpecialCase(char keepText[], int prevEndState, int startIndex) {
    if(prevEndState == STATE3 && keepText[startIndex] != '.') {
        keepText[startIndex-1] = 'S';
    }
}

// Modify the text string according to the information given by keepText array.
void modifyString(char text[], char keepText[], int textLength) {

    // Clean keeptText array.
    cleanKeepText(keepText,textLength);

    // Find out what character code is found first - 'S' or 'E'. This is important
    // as if and 'E' is found first, than we want to keep the text from the beginning
    // until that 'E'. Otherwise, we want to remove all of the text until the 'S' is 
    // found.
    int firstS = 0;
    for(int i = 0; i < textLength; i++) {
        if(keepText[i] == 'S') {
            firstS = 1;
            break;
        } else if(keepText[i] == 'E') {
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

    // Move the index forward one position if an e was found first. This is because
    // we want to keep the text at 'E''s index. This becomes more clear from the code
    // directly below.
    if(firstS == 0)
        i++;
    
    // Run through the rest of keepText and set the text array accordingly.
    int setEmpty = 1;
    for(; i < textLength; i++) {

        // Mark the indices as empty characters until an 'S' is found.
        while(i < textLength && keepText[i] != 'S') {
            text[i] = ' ';
            i++;
        }

        // Mark the index where the 'S' is found as empty as well, and increment the index.
        text[i] = ' ';
        i++;

        // Push the index pointer along until an 'E' is found, as this segment holds a well
        // formatted floating point number.
        while(i < textLength && keepText[i] != 'E') {
            i++;
        }
    }

    // Check what the last character code is - if its an 'S', erase the remainder of the text.
    // Otherwise it would have been taken care of the for loop above.
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

// Parse the keepText array as there are consecutive 'S' / 'E' entries and we
// only want one 'S' followed by one 'E'. An example:
// (S--S-S---EEEE) --->
// (-----S------E)
// For a description of where 'S''s and 'E''s are placed, look at the setNextState code.
void cleanKeepText(char keepText[], int textLength) {
    
    int prevS = -1;
    int seenS = 1;
    
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

// Set the next state given a starting state, and indicate where well formatted floating point values are
// found in the text in the given segment.
int setNextState(int startState, int startIndex, int endIndex, char text[], char keepText[]) {

    int currentState = startState;
    int curStartKeep = 0;
    int curEndKeep = 0;

    // An 'S' is placed whenever the rules of the state machine is broken other than in STATE5. Thus
    // every time there is an error in format, that index is marked with an 'S' and we must consider
    // the text following that 'S'. If STATE5 has been reached, a valid format has been found. There
    // may be consectuive 'S''s, so the most recent 'S' in front of an 'E' is the correct starting point
    // and every character prior will be emptied later on. the cleanKeepText function will remove unnecessary
    // 'S''s.

    // An 'E' is placed once STATE5 has been reached, and for every loop it takes in STATE5. The last 'E'
    // found in a consecutive string of 'E''s is the correct 'E', and all previous 'E''s in this sequence
    // will be removed in the cleanKeepText function.

    // Run through the given segment of text, and follow the state machine provided to know where
    // well formatted floating point numbers are found.
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
                    // Reevaluate the current character at STATE1
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
