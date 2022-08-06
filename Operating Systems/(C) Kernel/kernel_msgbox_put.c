nclude <stdio.h>
#include <errno.h>
#include <unistd.h>
#include <stdlib.h>
#include <string.h>
#include "arch/x86/include/generated/uapi/asm/unistd_64.h"

int main(int argc, char ** argv) {
	        char *in = malloc(50);

		printf("Please write your message: ");
		if (fgets(in, 50, stdin) == NULL){
			printf("Error reading input\n");
		} else {
			if (in[strlen(in) - 1] != '\n') {
				fprintf(stderr, "Input too long. Only part of the message will be saved\n");
			}
			syscall(__NR_kernel_msgbox_put, in, strlen(in) + 1);
			free(in);
		}
}
