"""
   run a sequence of abstract inputs from a file using the Java Mapper in the learner project.

   usage: python traceRunner.py [--runs(-r) runs_num] [--hops(-h) skip_lines_numb] [--jvmPath(-jvm) path_to_jvm_so] --traceFile(-tf) trace_file  
   
   trace_file is a path to a file containing a sequence of abstract inputs. The trace runner
   reads an abstract input from every line. 
   
   runNum (r) is the number of times the trace is run. 
   
   skipNum (s) is the number of lines skipped after each abstract input read. 
   (useful if your trace also contains output symbols)
   
    Example:
    
    file trace.txt with:
    SYN(INV, INV)
    ACK(V, V)
    SYN(V, V)
    ...
    
"""

__author__ = 'paul'
import jpype
import os
from sender import *
from response import *


class TraceRunner:

    def __init__(self, traceFile, jvmPath, runNum, skipNum):
        self.traceFile = traceFile # the path to the file containing the trace to be executed
        self.jvmPath = jvmPath # path to libjm.so for ubuntu or jvm.dll for windows
        self.runNum = runNum
        self.skipNum = skipNum
        
    waitTime = 0.1
    learnerProjectBinPath = "-Djava.class.path=../Learner/bin" # path to the java learner setup binaries
    mapClass = "sutInterface.tcp.TCPMapper" # mapper class used
    # expected mapper interface:
    #     processOutgoingRequest(string, int, int)
    #     processIncomingResponseComp()
    #     processIncomingTimeout()
        
    def startJava(self):
        jpype.startJVM(jvmPath, "-ea",learnerProjectBinPath)
    
    def stopJava(self):
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
            responseString = self.getMapper().processIncomingResponseComp(response.flags, str(response.seq), str(response.ack))
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
    
    # gets a new port number, an increment of the old. Replaces it in the portNumber file.
    def getNextPort(self):
        f = open(portNumberFile,"a+")
        f.seek(0)
        line = f.readline()
        if line == '':
            networkPort = getSender().networkPortMinimum
        else:
            networkPort = (int(line)+1)%getSender().networkPortMaximum
        f.closed
        f = open(portNumberFile, "w")
        f.write(str(networkPort))
        f.closed
        return networkPort
    
    def validReset(self):
        validSeq = self.getMapper().getNextValidSeq()
        self.getSender().sendValidReset(validSeq)
    
    # resets by changing ports on the sender.
    def reset():
        self.getMapper().setDefault()
        self.getSender().sendReset()
    
    # executes the trace at path. Starting '#' is used to comment the lines. Parsing ends once an endline is hit.
    # Step is 2, so that on a normal trace log, the response line is ignored.
    def executeTraceFile(sender, filePath):
        self.sender = sender
        step = self.skipNum
        count = 0
        ack = 0
        reset()
        print filePath
        for line in open(filePath, "r"):
            if line == "\n":
                return
            count = count + 1
            if line[len(line)-1] == "\n":
                line = line[:len(line)-1]
            if line == "reset":
                validReset()
                count = 0
                continue
            if count % step != 1:
                continue
            if line[0] == "#":
                continue
         #   print getMapper().state()
            print line#,"  ", getMapper().state()
    
            line = line.replace("(",",");
            line = line.replace(")",",");
            if len(line) < 2:
                break;
            parts = line.split(",")
            if len(parts) == 4:
                flags = parts[0]
                syn = parts[1]
                ack = parts[2]
    
                concreteRequest = self.processRequest(flags, syn, ack)
                print concreteRequest
                concreteResponse = self.sendConcreteRequest(concreteRequest)
                abstractResponse = self.processResponse(concreteResponse)
                print abstractResponse#,"  ", getMapper().state()
                print self.getMapper().getState()
            print "\n"
            global waitTime
            time.sleep(waitTime)
