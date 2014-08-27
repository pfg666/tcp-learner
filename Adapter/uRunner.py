__author__ = 'paul'
import jpype
import os
from sender import *
from response import *
from argparser import *

# Tool used to run a single abstract trace from a file. It communicates directly with the Java mapper. Example of
# an abstract trace:
# SYN(INV, INV)
# ACK(V, V)
# SYN(V, V)SYN(V,V)
# SYN+ACK(V,V)
# FIN+ACK(V,V)
# ACK(V,V)
# SYN+ACK(V,V)
# ACK(INV,V)SYN(V,V)
# SYN+ACK(V,V)
# FIN+ACK(V,V)
# ACK(V,V)
# SYN+ACK(V,V)
# ACK(INV,V)
# ...
# Change the following 4 settings to suit your own setup.
cexPath = "inputTrace.txt" # the path to the file containing the trace to be executed
jvmPath = "/usr/lib/jvm/jdk1.7.0_45/jre/lib/amd64/server/libjvm.so" # path to libjm.so for ubuntu or jvm.dll for windows
learnerProjectBinPath = "-Djava.class.path=../Learner/bin" # path to the java learner setup binaries
portNumberFile = "sn.txt" # used for always generating a new port number. The previously used port number is stored and the
# next will be its increment.
global mapper
mapper= None
global sender
sender = None
global waitTime
waitTime = 0
def startJava():
    jpype.startJVM(jvmPath, "-ea",learnerProjectBinPath)
def stopJava():
    jpype.shutdownJVM()

# get mapper instance
def getMapper():
    global mapper
    if mapper is None:
        Mapper = jpype.JClass("sut.interfacing.Mapper")
        mapper = Mapper()
    return mapper

# get sender singleton (quite uninspired)
def getSender():
    global sender
    if sender is None:
        argumentParser = ArgumentParser()
        argumentParser.parseArguments()
        sender = argumentParser.buildSender()
    return sender

def processRequest(flags, syn, ack):
    return getMapper().processOutgoingRequest(flags, syn, ack)

def processResponse(response):
    if type(response) is ConcreteResponse:
        #flags = jpype.JString(str(response.flags))
        responseString = getMapper().processIncomingResponseComp(response.flags, str(response.seq), str(response.ack))
    else:
        responseString = response.serialize().upper()
    return responseString

def sendConcreteRequest(concreteRequest):
    if concreteRequest == "UNDEFINED":
        return Undefined()
    parts = concreteRequest.split()
    flags = str(parts[0])
    syn = long(parts[1])
    ack = long(parts[2])
    return getSender().sendInput(flags, syn, ack)

# gets a new port number, an increment of the old. Replaces it in the portNumber file.
def getNextPort():
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

def validReset():
    validSeq = getMapper().getNextValidSeq()
    getSender().sendValidReset(validSeq)

# resets by changing ports on the sender.
def reset():
    getMapper().setDefault()
    getSender().networkPort = getNextPort()
    print "Running on ", str(getSender().networkPort)

# executes the trace at path. Starting '#' is used to comment the lines. Parsing ends once an endline is hit.
# Step is 2, so that on a normal trace log, the response line is ignored.
def executeTraceFile(filePath, step=2):
    count = 0
    ack = 0
    reset()
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

        line = line.replace("(",",");
        line = line.replace(")",",");
        if len(line) < 2:
            break;
        parts = line.split(",")
        if len(parts) == 4:
            flags = parts[0]
            syn = parts[1]
            ack = parts[2]
            print flags + " " + syn + " " + ack
            concreteRequest = processRequest(flags, syn, ack)
            concreteResponse = sendConcreteRequest(concreteRequest)
            abstractResponse = processResponse(concreteResponse)
            print abstractResponse
        print "\n"
        global waitTime
        time.sleep(waitTime)

if __name__ == "__main__":
    startJava()
    for i in range(0,1):
        executeTraceFile(cexPath, 2)
    stopJava()