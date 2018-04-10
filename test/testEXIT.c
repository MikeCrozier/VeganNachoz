#include "stdlib.h"
#include "stdio.h"
#include "syscall.h"

int main(){
	
	/** testing exit commands.
		lots of testing has been done in previous code 
		now we are just covering all our bases
	**/

	//tring a argument that is over maxFileD
	exit(23);
	//this should stop the machine and send an error

	return 0;
}
