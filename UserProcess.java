package nachos.userprog;

import nachos.machine.*;
import nachos.threads.*;
import nachos.userprog.*;

import java.io.EOFException;

/**
 * Encapsulates the state of a user process that is not contained in its
 * user thread (or threads). This includes its address translation state, a
 * file table, and information about the program being executed.
 *
 * <p>
 * This class is extended by other classes to support additional functionality
 * (such as additional syscalls).
 *
 * @see	nachos.vm.VMProcess
 * @see	nachos.network.NetProcess
 */
public class UserProcess {
    /**
     * Allocate a new process.
     */
    public UserProcess() {
	int numPhysPages = Machine.processor().getNumPhysPages();
	pageTable = new TranslationEntry[numPhysPages];
	for (int i=0; i<numPhysPages; i++)
	    pageTable[i] = new TranslationEntry(i,i, true,false,false,false);
    }
    
    /**
     * Allocate and return a new process of the correct class. The class name
     * is specified by the <tt>nachos.conf</tt> key
     * <tt>Kernel.processClassName</tt>.
     *
     * @return	a new process of the correct class.
     */
    public static UserProcess newUserProcess() {
	return (UserProcess)Lib.constructObject(Machine.getProcessClassName());
    }

    /**
     * Execute the specified program with the specified arguments. Attempts to
     * load the program, and then forks a thread to run it.
     *
     * @param	name	the name of the file containing the executable.
     * @param	args	the arguments to pass to the executable.
     * @return	<tt>true</tt> if the program was successfully executed.
     */
    public boolean execute(String name, String[] args) {
	if (!load(name, args))
	    return false;
	
	new UThread(this).setName(name).fork();

	return true;
    }

    /**
     * Save the state of this process in preparation for a context switch.
     * Called by <tt>UThread.saveState()</tt>.
     */
    public void saveState() {
    }

    /**
     * Restore the state of this process after a context switch. Called by
     * <tt>UThread.restoreState()</tt>.
     */
    public void restoreState() {
	Machine.processor().setPageTable(pageTable);
    }

    /**
     * Read a null-terminated string from this process's virtual memory. Read
     * at most <tt>maxLength + 1</tt> bytes from the specified address, search
     * for the null terminator, and convert it to a <tt>java.lang.String</tt>,
     * without including the null terminator. If no null terminator is found,
     * returns <tt>null</tt>.
     *
     * @param	vaddr	the starting virtual address of the null-terminated
     *			string.
     * @param	maxLength	the maximum number of characters in the string,
     *				not including the null terminator.
     * @return	the string read, or <tt>null</tt> if no null terminator was
     *		found.
     */
    public String readVirtualMemoryString(int vaddr, int maxLength) {
	Lib.assertTrue(maxLength >= 0);

	byte[] bytes = new byte[maxLength+1];

	int bytesRead = readVirtualMemory(vaddr, bytes);

	for (int length=0; length<bytesRead; length++) {
	    if (bytes[length] == 0)
		return new String(bytes, 0, length);
	}

	return null;
    }

    /**
     * Transfer data from this process's virtual memory to all of the specified
     * array. Same as <tt>readVirtualMemory(vaddr, data, 0, data.length)</tt>.
     *
     * @param	vaddr	the first byte of virtual memory to read.
     * @param	data	the array where the data will be stored.
     * @return	the number of bytes successfully transferred.
     */
    public int readVirtualMemory(int vaddr, byte[] data) {
	return readVirtualMemory(vaddr, data, 0, data.length);
    }

    /**
     * Transfer data from this process's virtual memory to the specified array.
     * This method handles address translation details. This method must
     * <i>not</i> destroy the current process if an error occurs, but instead
     * should return the number of bytes successfully copied (or zero if no
     * data could be copied).
     *
     * @param	vaddr	the first byte of virtual memory to read.
     * @param	data	the array where the data will be stored.
     * @param	offset	the first byte to write in the array.
     * @param	length	the number of bytes to transfer from virtual memory to
     *			the array.
     * @return	the number of bytes successfully transferred.
     */
    public int readVirtualMemory(int vaddr, byte[] data, int offset,
				 int length) {
	Lib.assertTrue(offset >= 0 && length >= 0 && offset+length <= data.length);

	Processor proc = Machine.processor();
	byte[] memory = proc.getMemory();
	
	int vPage = proc.pageFromAddress(vaddr);
	int addrOffset = proc.offsetFromAddress(vaddr);
	
	/* for now, just assume that virtual addresses equal physical addresses
	if (vaddr < 0 || vaddr >= memory.length)
	    return 0;
	*/
	
	
	TranslationEntry tEntry = pageTable[vPage]; //put virtual page into tEntry
	tEntry.used = true; //tEntry is used
	
	int pPage = tEntry.ppn; //get physical page number
	
	if(pPage < 0 || pPage >= proc.getNumPhysPages()) //if page number is invalid
		return 0;
	
	int pAddr = (pPage*pageSize) + addrOffset; //physical address
	
	
	int amount = Math.min(length, memory.length-vaddr);
	System.arraycopy(memory, vaddr, data, offset, amount);

	return amount;
    }

    /**
     * Transfer all data from the specified array to this process's virtual
     * memory.
     * Same as <tt>writeVirtualMemory(vaddr, data, 0, data.length)</tt>.
     *
     * @param	vaddr	the first byte of virtual memory to write.
     * @param	data	the array containing the data to transfer.
     * @return	the number of bytes successfully transferred.
     */
    public int writeVirtualMemory(int vaddr, byte[] data) {
	return writeVirtualMemory(vaddr, data, 0, data.length);
    }

    /**
     * Transfer data from the specified array to this process's virtual memory.
     * This method handles address translation details. This method must
     * <i>not</i> destroy the current process if an error occurs, but instead
     * should return the number of bytes successfully copied (or zero if no
     * data could be copied).
     *
     * @param	vaddr	the first byte of virtual memory to write.
     * @param	data	the array containing the data to transfer.
     * @param	offset	the first byte to transfer from the array.
     * @param	length	the number of bytes to transfer from the array to
     *			virtual memory.
     * @return	the number of bytes successfully transferred.
     */
    public int writeVirtualMemory(int vaddr, byte[] data, int offset,
				  int length) {
	Lib.assertTrue(offset >= 0 && length >= 0 && offset+length <= data.length);

	Processor proc = Machine.processor();
	byte[] memory = proc.getMemory();
	
	int vPage = proc.pageFromAddress(vaddr);
	int addrOffset = proc.offsetFromAddress(vaddr);
	
	/* for now, just assume that virtual addresses equal physical addresses
	if (vaddr < 0 || vaddr >= memory.length)
	    return 0;
	*/
	
	TranslationEntry tEntry = pageTable[vPage]; //tEntry holds virtual page from pageTable
	tEntry.used = true; //tEntry is used
	
	int pPage = tEntry.ppn; //pPage is tEntry's physical page number
	
	if(pPage < 0 || pPage >= proc.getNumPhysPages()) //if page is invalid
		return 0;
	
	int pAddr = (pPage*pageSize) + addrOffset; //get physical address
	tEntry.dirty = true; //mark tEntry as being written to

	int amount = Math.min(length, memory.length-vaddr); 
	System.arraycopy(data, offset, memory, vaddr, amount);

	return amount;
    }

    /**
     * Load the executable with the specified name into this process, and
     * prepare to pass it the specified arguments. Opens the executable, reads
     * its header information, and copies sections and arguments into this
     * process's virtual memory.
     *
     * @param	name	the name of the file containing the executable.
     * @param	args	the arguments to pass to the executable.
     * @return	<tt>true</tt> if the executable was successfully loaded.
     */
    private boolean load(String name, String[] args) {
	Lib.debug(dbgProcess, "UserProcess.load(\"" + name + "\")");
	
	OpenFile executable = ThreadedKernel.fileSystem.open(name, false);
	if (executable == null) {
	    Lib.debug(dbgProcess, "\topen failed");
	    return false;
	}

	try {
	    coff = new Coff(executable);
	}
	catch (EOFException e) {
	    executable.close();
	    Lib.debug(dbgProcess, "\tcoff load failed");
	    return false;
	}

	// make sure the sections are contiguous and start at page 0
	numPages = 0;
	for (int s=0; s<coff.getNumSections(); s++) {
	    CoffSection section = coff.getSection(s);
	    if (section.getFirstVPN() != numPages) {
		coff.close();
		Lib.debug(dbgProcess, "\tfragmented executable");
		return false;
	    }
	    numPages += section.getLength();
	}

	// make sure the argv array will fit in one page
	byte[][] argv = new byte[args.length][];
	int argsSize = 0;
	for (int i=0; i<args.length; i++) {
	    argv[i] = args[i].getBytes();
	    // 4 bytes for argv[] pointer; then string plus one for null byte
	    argsSize += 4 + argv[i].length + 1;
	}
	if (argsSize > pageSize) {
	    coff.close();
	    Lib.debug(dbgProcess, "\targuments too long");
	    return false;
	}

	// program counter initially points at the program entry point
	initialPC = coff.getEntryPoint();	

	// next comes the stack; stack pointer initially points to top of it
	numPages += stackPages;
	initialSP = numPages*pageSize;

	// and finally reserve 1 page for arguments
	numPages++;

	if (!loadSections())
	    return false;

	// store arguments in last page
	int entryOffset = (numPages-1)*pageSize;
	int stringOffset = entryOffset + args.length*4;

	this.argc = args.length;
	this.argv = entryOffset;
	
	for (int i=0; i<argv.length; i++) {
	    byte[] stringOffsetBytes = Lib.bytesFromInt(stringOffset);
	    Lib.assertTrue(writeVirtualMemory(entryOffset,stringOffsetBytes) == 4);
	    entryOffset += 4;
	    Lib.assertTrue(writeVirtualMemory(stringOffset, argv[i]) ==
		       argv[i].length);
	    stringOffset += argv[i].length;
	    Lib.assertTrue(writeVirtualMemory(stringOffset,new byte[] { 0 }) == 1);
	    stringOffset += 1;
	}
	
	pageTable = new TranslationEntry[numPages]; //make pageTable
	
	for(int i=0; i< numPages; i++){ //for each index in pageTable
		int physPage = UserKernel.getPage(); //get a free physical page
		Lib.assertTrue(physPage >= 0); //Ensures physical page number is valid
		Lib.assertTrue(physPage < Machine.processor().getNumPhysPages());
		pageTable[i] = new TranslationEntry(i, physPage, true, false, false, false);//put mew translation entry into pageTable
	}

	return true;
    }

    /**
     * Allocates memory for this process, and loads the COFF sections into
     * memory. If this returns successfully, the process will definitely be
     * run (this is the last step in process initialization that can fail).
     *
     * @return	<tt>true</tt> if the sections were successfully loaded.
     */
    protected boolean loadSections() {
	if (numPages > Machine.processor().getNumPhysPages()) {
	    coff.close();
	    Lib.debug(dbgProcess, "\tinsufficient physical memory");
	    return false;
	}

	// load sections
	for (int s=0; s<coff.getNumSections(); s++) {
	    CoffSection section = coff.getSection(s);
	    
	    Lib.debug(dbgProcess, "\tinitializing " + section.getName()
		      + " section (" + section.getLength() + " pages)");

	    for (int i=0; i<section.getLength(); i++) {
		int vpn = section.getFirstVPN()+i;

		/* for now, just assume virtual addresses=physical addresses
		section.loadPage(i, vpn);*/
		
		
		TranslationEntry tEntry = pageTable[vpn]; //tEntry is pageTable[virtual page number]
		tEntry.readOnly= section.isReadOnly(); //tEntry's readonly values is sections readonly value
	
		int pPage = tEntry.ppn;//get physical page number
		
		section.loadPage(i, pPage);
	    }
	}
	
	return true;
    }

    /**
     * Release any resources allocated by <tt>loadSections()</tt>.
     */
    protected void unloadSections() {
		//loops through all possible page numbers
		for(int i=0; i< numPages; i++)
		{
			UserKernel.addPage(pageTable[i].ppn);//adds page to pageTable
			pageTable[i].valid = false;//marks page invalid
		}
    }    

    /**
     * Initialize the processor's registers in preparation for running the
     * program loaded into this process. Set the PC register to point at the
     * start function, set the stack pointer register to point at the top of
     * the stack, set the A0 and A1 registers to argc and argv, respectively,
     * and initialize all other registers to 0.
     */
    public void initRegisters() {
	Processor processor = Machine.processor();

	// by default, everything's 0
	for (int i=0; i<processor.numUserRegisters; i++)
	    processor.writeRegister(i, 0);

	// initialize PC and SP according
	processor.writeRegister(Processor.regPC, initialPC);
	processor.writeRegister(Processor.regSP, initialSP);

	// initialize the first two argument registers to argc and argv
	processor.writeRegister(Processor.regA0, argc);
	processor.writeRegister(Processor.regA1, argv);
    }

    /**
     * Handle the halt() system call. 
     */
    private int handleHalt() {

	Machine.halt();
	
	Lib.assertNotReached("Machine.halt() did not halt machine!");
	return 0;
    }
	
	/*
    The Creat method accepts an address as an argument and attempts to open the file linked to the address, if 
    no file is available to open, it will create a file. If however I file is linked to the address, the 
    method will check to see if the file has been set to be unlinked. If it has, the method will not open the 
    file and an error message will be produced.
     */

    private int handleCreat(int address) {
        // private int handleCreate() {                                 
        Lib.debug(dbgProcess, "handleCreat()");                   

        // address is address of filename 
        String filename = readVirtualMemoryString(address, maxStringLen);  

        Lib.debug(dbgProcess, "Filename: "+filename);                     

        // invoke open through stubFilesystem
        OpenFile returnValue  = UserKernel.fileSystem.open(filename, true);   

        if (returnValue == null) {  
            Lib.debug(dbgProcess, "No file named "+filename+" found.");
            return -1;                                                
        }                                                             
        else {                                                        
            int fileHandle = findEmptyFileDescriptor();                
            if (fileHandle < 0){
        Lib.debug(dbgProcess, "No room for file.");
                return -1;  
        }
        if (fd[fileHandle].toKill == true){
            Lib.debug(dbgProcess, "File flagged to be removed.");
            return -1;
        }
            else {                                                    
                fd[fileHandle].fileName = filename;                   
                fd[fileHandle].file = returnValue;                    
                fd[fileHandle].IOpos = 0;                             
                return fileHandle;                                    
            }                                                          
        }                                                             
    }                                                                 

    /*
    The Open method does the exact same functionality as the Creat method, however, it will only open a file 
    not create one if there is no file linked to the address input
     */

    private int handleOpen(int address) {
        Lib.debug(dbgProcess, "[UserProcess.handleOpen] Start");      

        Lib.debug(dbgProcess, "[UserProcess.handleOpen] address: "+address+"\n");   

        // address is address of filename 
        String filename = readVirtualMemoryString(address, maxStringLen); 

        Lib.debug(dbgProcess, "filename: "+filename);            

        // invoke open through stubFilesystem, truncate flag is set to false
        OpenFile returnValue  = UserKernel.fileSystem.open(filename, false); 

        if (returnValue == null) {  
        Lib.debug(dbgProcess, "No file named "+filename+" found.");
            return -1;                                  
        }                                               
        else {                                          
            int fileHandle = findEmptyFileDescriptor();  
            if (fileHandle < 0){
        Lib.debug(dbgProcess, "No room for file.");
                return -1;  
        }
        if (fd[fileHandle].toKill == true){
            Lib.debug(dbgProcess, "File flagged to be removed.");
            return -1;
        }
            else {                                  
                fd[fileHandle].fileName = filename; 
                fd[fileHandle].file = returnValue;  
                fd[fileHandle].IOpos = 0;           
                return fileHandle;                  
            }                                        
        }                                           
    }                                               
 

    /*
    The Read method will take in 3 arguments, the file descriptor, the buffer address, and the buffer size. 
    Before anything can be done we must confirm that all three of these arguments are not going to cause any 
    problems. Afterwards we will need to create a buffer array. Then we can use the read method in the 
    SyncConsole class to read through the StubFileSystem. Then we need to write this to virtual memory. If the 
    read was successful, the number of bytes that was read through the method will be returned. If an error 
    occurs during the process, it will return -1
     */

    private int handleRead(int descriptor, int bufferAddress, int bufferSize) {          
        Lib.debug(dbgProcess, "handleRead()");     
         
        int handler = descriptor;                  
        int bufaddr = bufferAddress;               
        int bufsize = bufferSize;                  

        Lib.debug(dbgProcess, "handle: " + handler);                   
        Lib.debug(dbgProcess, "buf address: " + bufaddr);               
        Lib.debug(dbgProcess, "buf size: " + bufsize);                

        // get data regarding to file descriptor
        if (handler < 0 || handler > maxFileD                         
                || fd[handler].file == null)                          
            return -1;                                                

        FileDescriptor fd2 = fd[handler];                             
        byte[] buff = new byte[bufsize];                              

        // invoke read through stubFilesystem
        int returnValue = fd2.file.read(fd2.IOpos, buff, 0, bufsize); 

        if (returnValue < 0) {                                    
            return -1;                                            
        }                                                         
        else {                                                    
            int number = writeVirtualMemory(bufaddr, buff);       
            fd2.IOpos = fd2.IOpos + number;                       
            return returnValue;                                         
        }                                                               
    }                                                                   
    
    /*
    The Write method is very similar to the read method, mainly because for the write method to execute, it 
    requires the same executions as the read method. However, with the write method, instead of file.read, we 
    will use file.write to write the input.
     */

     private int handleWrite(int descriptor, int bufferAddress, int bufferSize) {
        Lib.debug(dbgProcess, "handleWrite()");    
         
        int handler = descriptor;                  
        int bufaddr = bufferAddress;               
        int bufsize = bufferSize;                    

        Lib.debug(dbgProcess, "handle: " + handler);                      
        Lib.debug(dbgProcess, "buf address: " + bufaddr);                  
        Lib.debug(dbgProcess, "buf size: " + bufsize);                   

        // get data regarding to file descriptor
        if (handler < 0 || handler > maxFileD                            
                || fd[handler].file == null)                             
            return -1;                                                   

        FileDescriptor fd2 = fd[handler];                                

        byte[] buff = new byte[bufsize];                                   

        int bytesRead = readVirtualMemory(bufaddr, buff);                

        // invoke read through stubFilesystem                            
        int returnValue = fd2.file.write(fd2.IOpos, buff, 0, bytesRead); 

        if (returnValue < 0) {                                      
            return -1;                                              
        }                                                           
        else {                                                      
            fd2.IOpos = fd2.IOpos + returnValue;                    
            return returnValue;                                     
        }                                                           
    }

    /*
     The Close method requires an address of a file as its only argument. The method frees up the use of a 
     file descriptor by closing the current file. The method will check the address and ensure that in meets 
     the FileDescriptor parameters and will result in an error if it does not. The file descriptor that is
     used to refer to a file will be closed and made available for other files. The method will check to see 
     if the file has been set by Unlink to be removed. If it has been set to remove, the resources used by the 
     recently closed file will become available for another. If the close method was successful, the results 
     will return 0, and if an error occurs it will return -1.
     */

    private int handleClose(int address) {                          
        Lib.debug(dbgProcess, "handleClose()");                     
        
        int handler = address;                                      
        if (address < 0 || address >= maxFileD)                     
            return -1;                                              

        boolean returnValue = true;                                 

        FileDescriptor fd2 = fd[handler];                           

        fd2.IOpos = 0;                                              
        fd2.file.close();                                           

        // remove this file if necessary                            
        if (fd2.toKill) {                                               
            returnValue = UserKernel.fileSystem.remove(fd2.fileName);     
            fd2.toKill = false;                         
        }                                              

        fd2.fileName = "";
        fd2.file = null;
        fd2.IOpos = 0;

        return returnValue ? 0 : -1;                   
    }                                                  

    /*
    This Unlink methods purpose is to delete a file from the file system. This is done by locating the file
     and determining if any process is currently using the file. If no process is using it, the file is 
     removed and the memory space allocated for that file is now available for another file. If the file has 
     is being used by a process, the file will be set to be removed, and will be removed once the processes 
     are done. If the file was successfully deleted, the results returned will be 0, and if an error had 
     occurred during the process, the result will return -1
     */

    private int handleUnlink(int address) {
        Lib.debug(dbgProcess, "handleUnlink()");

        boolean returnValue = true;

        // address is address of filename 
        String filename = readVirtualMemoryString(address, maxFileD); 

        Lib.debug(dbgProcess, "filename: " + filename);                  

        int fileHandler = findFileDescriptorByName(filename);     
        if (fileHandler < 0) {                      
           /* invoke open through stubFilesystem, truncate flag is set to false
             If no processes have the file open, the file is deleted immediately 
             and the space it was using is made available for reuse.
            */
            returnValue = UserKernel.fileSystem.remove(fd[fileHandler].fileName); 
        }                           
        else {                      
            // If the file is in use, set toKill to true to be removed once the file is done being used
             fd[fileHandler].toKill = true;   
        } 

        return returnValue ? 0 : -1;                                          
    }


    private static final int
        syscallHalt = 0,
	syscallExit = 1,
	syscallExec = 2,
	syscallJoin = 3,
	syscallCreate = 4,
	syscallOpen = 5,
	syscallRead = 6,
	syscallWrite = 7,
	syscallClose = 8,
	syscallUnlink = 9;

    /**
     * Handle a syscall exception. Called by <tt>handleException()</tt>. The
     * <i>syscall</i> argument identifies which syscall the user executed:
     *
     * <table>
     * <tr><td>syscall#</td><td>syscall prototype</td></tr>
     * <tr><td>0</td><td><tt>void halt();</tt></td></tr>
     * <tr><td>1</td><td><tt>void exit(int status);</tt></td></tr>
     * <tr><td>2</td><td><tt>int  exec(char *name, int argc, char **argv);
     * 								</tt></td></tr>
     * <tr><td>3</td><td><tt>int  join(int pid, int *status);</tt></td></tr>
     * <tr><td>4</td><td><tt>int  creat(char *name);</tt></td></tr>
     * <tr><td>5</td><td><tt>int  open(char *name);</tt></td></tr>
     * <tr><td>6</td><td><tt>int  read(int fd, char *buffer, int size);
     *								</tt></td></tr>
     * <tr><td>7</td><td><tt>int  write(int fd, char *buffer, int size);
     *								</tt></td></tr>
     * <tr><td>8</td><td><tt>int  close(int fd);</tt></td></tr>
     * <tr><td>9</td><td><tt>int  unlink(char *name);</tt></td></tr>
     * </table>
     * 
     * @param	syscall	the syscall number.
     * @param	a0	the first syscall argument.
     * @param	a1	the second syscall argument.
     * @param	a2	the third syscall argument.
     * @param	a3	the fourth syscall argument.
     * @return	the value to be returned to the user.
     */
    public int handleSyscall(int syscall, int a0, int a1, int a2, int a3) {
	switch (syscall) {
	case syscallHalt:
	    return handleHalt();


	default:
	    Lib.debug(dbgProcess, "Unknown syscall " + syscall);
	    Lib.assertNotReached("Unknown system call!");
	}
	return 0;
    }

    /**
     * Handle a user exception. Called by
     * <tt>UserKernel.exceptionHandler()</tt>. The
     * <i>cause</i> argument identifies which exception occurred; see the
     * <tt>Processor.exceptionZZZ</tt> constants.
     *
     * @param	cause	the user exception that occurred.
     */
    public void handleException(int cause) {
	Processor processor = Machine.processor();

	switch (cause) {
	case Processor.exceptionSyscall:
	    int result = handleSyscall(processor.readRegister(Processor.regV0),
				       processor.readRegister(Processor.regA0),
				       processor.readRegister(Processor.regA1),
				       processor.readRegister(Processor.regA2),
				       processor.readRegister(Processor.regA3)
				       );
	    processor.writeRegister(Processor.regV0, result);
	    processor.advancePC();
	    break;				       
				       
	default:
	    Lib.debug(dbgProcess, "Unexpected exception: " +
		      Processor.exceptionNames[cause]);
	    Lib.assertNotReached("Unexpected exception");
	}
    }
	
		/**
	* The exit system call will take a process and terminate it and anything that process had open,
	* this will also be performed if the process if abnormally abrupted. The exit status of this 
	* process has to be transferred to the parent that way the parent won’t call the join system call.
	* If the root thread calls this system call Machine.halt will be called
	*  
	* @param status  status integer is the exit status of the thread that is using the system call
	*/
	private void handleExit(int status){
		for(int i = 2; i < maxFileD; i++){
			if(fd[i] != null)
					handleClose(i); //closes all open files after the root processes
		}
		unloadSections();
		if(processID == 0){ //machine process ID
			Lib.debug(dbgProcess, "\tTerminated");
			Machine.halt();
		}
		else KThread.finish(); //finishes any thread that isnt the machine process
	}
	
	/**
	* The process who calls this system call will join the the terminated process, if it is still running join will wait 
	* until it is exited, if there is an error it will return -1. The process that joins has to be the parent of the process
	* that uses the system call, and can only be joined once.
	*
	* @param ProcessID  is a globally unique positive integer that will be assigned to every process when they are created to keep track of them
	* @param status  is the exit status of the thread it is running that will be terminated
	*/
	private int handleJoin(int ProcessID, int status){
		UserProcess child = children.get(ProcessID); //sets the user process to that of the specified child join
		child.statLock.acquire(); //acquire lock
		
		Integer childStatus = child.eStat; //exit status
		if (childStatus == null){
			statLock.acquire();
			child.statLock.release();
			jCond.sleep();
			statLock.release();
			child.statLock.acquire();
			childStatus = child.eStat; 
		}
		child.statLock.release();
		children.remove(ProcessID);
		
		byte[] statusBytes = new byte[4];
		for (int j = 0; j < 4; j++) 
			statusBytes[j] = (byte) (waitingStatus >>> j * 8); //byte offset
		writeVirtualMemory(status, statusBytes); //write to the virtual memory
		return 0; //returns 0 as there is no error
	}

	/**
	* The execute system call will overwrite the current process with a new one via the file passed and will 
	* return the new processID. 
	*
	* @param file  the ID of the file to be executed
	*/
	private int handleExec(int file, int cLength){
		String fileName = readVirtualMemoryString(file, maxStringLen); //reads the system for the file being executed
		if(fileName == null || fileName.isEmpty()) //error handling
			return -1;
		int[] vAddr = new int[cLength];
		String[] vStrings = new String[cLength];
		for (int i = 0; i < cLength; i++){
			String s = readVirtualMemoryString(vAddr[i], maxStringLen); //virtual address of the file that is executed
			if (s == null || s.isEmpty()) 
				return -1;
			vStrings[i] = s;
		}
		UserProcess newProcess = new UserProcess(); //creating new process for the new execution
		newProcess.processID = ++idCounter; //ID counter increases
		newProcess.parent = this;
		this.children.add(newProcess);
		newProcess.execute(fileName, vStrings); //executes
		int retPID = newProcess.processID;
		return retPID; //the new Process ID
	}
	
	// File Descriptor constructor
    private class FileDescriptor{
            public FileDescriptor(){}
            private String fileName = "";
            private OpenFile file = null;
            private int IOpos = 0;
            private boolean toKill = false;
        FileDescriptor(int filePointer){    
        }
       
    } 


//method to find empty file descriptor location
    private int findEmptyFileDescriptor(){
        for (int i = 0; i < maxFileD; i++){
            if(fd[i].file == null){
                return i;
            }
        }
        return -1;
    }
    
// method for finding file descriptor by filename
    private int findFileDescriptorByName(String file){
        for (int i = 0; i < maxFileD; i++){
            if (fd[i].filename == file)
            return 1;
        }
        return -1;
    }

	private static final int maxStringLen = 256;
	public int processID; 
	public UserProcess parent;
	private Integer waitingStatus;
	private static int idCounter = 0; //counter for process ID
    public static final int maxFileD = 16; 	// Max number of file descriptors 
	private FileDescriptor fd[] = new FileDescriptor[maxFileD]; 
	protected Lock statLock; 	//condition locks
	protected Condition jCond;
	protected Integer eStat; 	//exit status
	
    /** The program being run by this process. */
    protected Coff coff;

    /** This process's page table. */
    protected TranslationEntry[] pageTable;
    /** The number of contiguous pages occupied by the program. */
    protected int numPages;

    /** The number of pages in the program's stack. */
    protected final int stackPages = 8;
    
    private int initialPC, initialSP;
    private int argc, argv;
	
    private static final int pageSize = Processor.pageSize;
    private static final char dbgProcess = 'a';
}
