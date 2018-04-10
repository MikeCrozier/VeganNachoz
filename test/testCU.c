#include "stdio.h"
#include "stdlib.h"
#include "syscall.h"

int main(){

	/**creating tests for close and unlink**/

	//trying to close a file that is out of bounds/doesn't exist
	char filename[] = {'n','o','t',' ','h','e','r','e'};
	char *fptr = &filename[0];

	int cls = close(fptr);
	if(cls != -1){
		printf("Failure: close did NOT return -1");
		exit(0);
	}else if (cls == -1){
		printf("Success: close returned -1");
	}

	//Unlinking a file that was never created/opened
	int unlk = unlink(fptr);
	if(unlk != -1){
		printf("Failure: Unlink did NOT return -1");
		exit(0);
	}else if(unlk == -1){
		printf("Success: Unlink returned -1");
	}

	//opening filname to be close it then unlink it
	int fd = open(fptr);
	
	if(fd == -1){
		printf("Failure to open");
		exit(0);
	}

	cls = close(fptr);
	if(cls == -1){
		printf("Failure to close");
	}
	
	
	return 0;
}
