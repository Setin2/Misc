#include<stdio.h>
#include<stdlib.h>

void main()
{
    FILE *source = fopen("Source_file.txt", "r");
    FILE *destination = fopen("Destination_file.txt", "w");
    char currentChar;
    char seqChar;
    int count = 0;

    while (1) {
      int flag = (fread(&currentChar, sizeof(char), 1, source) == 0); 

      if (flag || seqChar != currentChar) {

         if (count > 3) {
           char ch = 'Q';
           int k = count;
           char str[100];
           int digits = sprintf(str, "%d", count);
           fwrite(&ch, sizeof(ch), 1, destination);
           fwrite(&seqChar, sizeof(ch), 1, destination);
           fwrite(&str, sizeof(char)*digits, 1, destination);
         }
         else {
           for(int i = 0; i < count; i++) 
              fwrite(&seqChar, sizeof(char), 1, destination);
         }
         seqChar = currentChar;
         count = 1;
      } else count++;

     if(flag)
       break;
    }

   fclose(source);
   fclose(destination);
}
