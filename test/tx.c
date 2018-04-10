#include "stdlib.h"
#include "stdio.h"
#include "syscall.h"

int main(int cLength, char *arg){
	
	/**Executable for testing argument passing via 'exec'**/

 	char argument = arg[0];
	
	printf("arg[0] = %d", argument);

	return 0;
}
