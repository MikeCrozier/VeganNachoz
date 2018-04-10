#include "syscall.h"
#include "stdio.h"
#include "stdlib.h"
int main()
{
	// printf() displays the string inside quotation
	printf("Hello, World!");
	halt();
	return 0;
}

