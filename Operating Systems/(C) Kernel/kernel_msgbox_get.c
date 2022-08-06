#include <stdio.h>
#include <errno.h>
#include <unistd.h>
#include <stdlib.h>
#include <string.h>
#include "arch/x86/include/generated/uapi/asm/unistd_64.h"

int main(int argc, char ** argv) {
	char msg[50];
		if (syscall(__NR_dm510_msgbox_get, msg, 50) > 50){
			fprintf(stderr, "Error: The is no message in the stack right now\n");
		} else {
			printf("%s", msg);
		}
}
