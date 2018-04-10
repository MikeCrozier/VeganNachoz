#include "syscall.h"
#include "stdio.h"
#include "stdlib.h"

int main(){

	/**Creating a tests for exec
	*checkList
	*	test if exec works execute "helloworld.coff"
	*	test if exec returns -1 with invalid parameters
		test if exec can work with argument passing
	**/
	
	char filename[] = {'h','e','l','l','o','w','o','r','l','d','.','c','o','f','f'};
	char *fptr = &filename[0];

	int exc = exec(fptr, 0, null);

	if(exc == -1){
		printf("Failure: error in exec helloworld.coff");
		exit(0);
	}
	
	exc = exec(fptr, 3, null);
	if(exc != -1){
		printf("Failure: exec did not return -1");
		exit(0);
	}

	char filename2[] = {'t','x','.','c','o','f','f'};
	char *fptr2 = &filename2[0];

	char *arg[3] = {};

	exc = exec(fptr2, 3, arg);
	if( exc == -1){
		printf("Failure: problem with passing args");
		exit(0);
	}
	

	return 0;
}
