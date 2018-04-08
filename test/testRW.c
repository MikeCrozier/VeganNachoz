#include "stdlib.h"
#include "stdio.h"
#include "syscall.h"

int main(){
	
	/**start by trying to read and write to file that is not opened
	**/

	char buffer1[24];
	char *buff1 = &buffer1[0];

	//reading from a file in file descriptor 4
	/**This may have to change talk to joel about it
	since we need to make sure that there are is a 
	file in filedescriptor 4**/
	int bytesRead = read(4, buff1, 256);

	if(bytesRead < 0){
		printf("Success: nothing read");
	}

	//reading from a fd out of bounds (>16)
	bytesRead = read(20, buff1, 256);
	if(bytesRead < 0){
		printf("Success: nothing read");
	}

	//writing to a file that isn't open
	char buffer2[] = {'t','s','t'};
	char *buff2 = &buffer2[0];

	//getting the number of bits writen.. should be 1 per char (spaces included
	int bytesWriten = write(4, buff2, 3);

	if(bytesWriten < 0 ){
		printf("Success: nothing written");
	}
	printf("Bytes writen: %d", bytesWriten);	

	//writting to a fd that is out of bounds (>16)
	bytesWriten = write(20, buff2, 3);
	if(bytesWriten < 0){
		printf("Success: out of bound, nothing written");		
	}

	/**Opening and file and reading and writing to it**/

	printf("opening file to reand and write to it");

	char filename[] = {'t','s','t'};
	char *fptr = &filename[0];

	int fd = open(fptr);

	bytesRead = read(fd, buff1, 24);
	if(bytesRead < 0){
		printf("Failure: nothing read");
		exit(0);
	} 
	printf("bytesRead: %d", bytesRead);

	bytesWriten = write(fd, buff2, 256);
	if(bytesWriten < 0){
		printf("Failure: nothing written");
		exit(0);
	}
	return 0;
}
