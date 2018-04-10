#include "stdlib.h"
#include "stdio.h"
#include "syscall.h"

int main(){

	/**testing join cases to make sure it cannot be broken**/

	//trying to join a non existant pid
	int jn = join(50,null);
	if(jn == 0){
		printf("Failure: successful join was not suppose to happen");
		exit(0);
	}

	//trying to properly join
	char filen[] = {'t','x','.','c','o','f','f'};
	char *fptr = filen[0];
	
	int ex = exec(fptr,0,null);
	
	jn = join(ex,null);
	if(jn != 0 ){
		printf("Failure: error in joining");
		exit(0);
	}
	// testing to see if the same pid can join this process(shouldn't be able to)
	jn = join(ex,null);
	if(jn == 0){
		printf("Failure: there is two joins with the same pid");
	}

	return 0;
}
