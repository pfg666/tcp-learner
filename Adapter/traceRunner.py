from response import ConcreteResponse, Timeout, Undefined
import time
import jpype

waitTime = 0.1
learnerProjectBinPath = "-Djava.class.path=../bin" # path to the java learner setup binaries
mapClass = "sutInterface.tcp.TCPMapper" # mapper class used

class TraceRunner:

    def __init__(self, jvmPath, runNum, skipNum):
        self.jvmPath = jvmPath # path to libjm.so for ubuntu or jvm.dll for windows
        self.runNum = runNum # the number of times the trace is run. 
        self.skipNum = skipNum # the number of lines skipped after each abstract input read
        self.mapper = None
        
    def __str__(self):
        return "Trace Runner with parameters: " + str(self.__dict__)
        
    # expected mapper interface:
    #     processOutgoingRequest(string, int, int)
    #     processIncomingResponseComp()
    #     processIncomingTimeout()
        
    def startJava(self):
        print "starting JVM with parameters: " + "-ea " + learnerProjectBinPath
        jpype.startJVM(self.jvmPath, "-ea",learnerProjectBinPath)
    
    def stopJava(self):
        print "shutting down JVM"
        jpype.shutdownJVM()
    
    # get mapper instance
    def getMapper(self):
        if self.mapper is None:
            Mapper = jpype.JClass(mapClass)
            self.mapper = Mapper()
        return self.mapper
    
    # get sender singleton (quite uninspired)
    def getSender(self):
        return self.sender
    
    def processRequest(self,flags, syn, ack):
        return self.getMapper().processOutgoingRequest(flags, syn, ack)
    
    def processResponse(self, response):
        if type(response) is ConcreteResponse:
            responseString = self.getMapper().\
            processIncomingResponseComp(response.flags, str(response.seq), str(response.ack))
        else:
            if type(response) is Timeout:
                self.getMapper().processIncomingTimeout()
            responseString = response.serialize().upper()
        return responseString
    
    def sendConcreteRequest(self, concreteRequest):
        if concreteRequest == "UNDEFINED":
            return Undefined()
        parts = concreteRequest.split()
        flags = str(parts[0])
        syn = long(parts[1])
        ack = long(parts[2])
        return self.getSender().sendInput(flags, syn, ack)
    
    def validReset(self):
        validSeq = self.getMapper().getNextValidSeq()
        self.getSender().sendValidReset(validSeq)
    
    # resets by changing ports on the sender.
    def reset(self):

        print self.getMapper()
        self.getMapper().setDefault()
        
        self.getSender().sendReset()
    
    # executes the trace at path. Starting '#' is used to comment the lines.
    def executeTraceFile(self, sender, tracePath):
        self.startJava()
        self.sender = sender
        count = 0
        
        self.reset()
        for line in open(tracePath, "r"):
            # we ignore comments
            if line[0] == "#":
                continue
            if count>0:
                count -= 1
                continue
            self.processLine(line)
            # after each processed line we skip the following skipNum lines
            count = self.skipNum
        self.getSender().shutdown()
        self.stopJava()
    
    def processLine(self, line):
        # in this case we have a normal message
        line = line.replace("(",",");
        line = line.replace(")",",");
        parts = line.split(",")
        # in this case we have a message
        if len(parts) == 4:
            flags = parts[0]
            syn = parts[1]
            ack = parts[2]

            concreteRequest = self.processRequest(flags, syn, ack)
            concreteResponse = self.sendConcreteRequest(concreteRequest)
            abstractResponse = self.processResponse(concreteResponse)
            print self.getMapper().getState()
            
        # in this case we either have reset or  a higher method call
        elif len(parts) == 1: 
            line = line.lower().replace("\n","") # removes excess baggage
            if line == "reset":
                self.getSender().sendReset()
            elif "sendAction" in dir(self.getSender()) and "isAction" in dir(self.getSender()):
                if self.getSender().isAction(line):
                    self.getSender().sendAction(line)
                else:
                    print "invalid command encountered: " + line
                    exit(-1)
            else:
                print "the sender of type " + str(type(self.getSender())) + ""\
                " does not implement both sendAction and isAction methods"
                exit(-1)  
        else: 
            print "invalid command encountered: " + line
            exit(-1)
        time.sleep(waitTime)
                
                
    