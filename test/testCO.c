#include "stdio.h"
#include "stdlib.h"
#include "syscall.h"

int main(){
	
	char filename[] = {'T','m','p'};
	char *fptr = &filename[0];

	//Make sure that Tmp doesn't exist
	int fd = open(fptr);

	printf("Attempt to open non-existing file");

	if(fd == -1){
		printf("Opened failed");
		fd = creat(fptr);
		printf("Attempt to creat file");
		if (fd == -1){
			printf("Failure: Something went wrong with creat ");
			exit(0);
		}else if (fd != -1){
			printf("Success: Tmp File Created");
		}
	} else if(fd != -1){
		printf("Failure: Open-- Tmp already exists");
		exit(0);
	}
	
	/** Attempting to open illegal filenames**/

	char filename2[] = {' '};
	char *fptr2 = &filename2[0];
	
	printf("Attempting to open an invalid filename");
	fd = open(fptr2);

	if(fd == 0){
		printf("invalid Open failed");
		fd = creat(fptr2);
		if(fd == -1){
			printf("Invalid Creat failed");
		}else if(fd != -1){
			printf("Failure: Invalid Creat");
			exit(0);
		}
			
	} else if(fd != -1){
		printf("Failure: Invalid Open");
		exit(0);
	}
	return 0;
}
