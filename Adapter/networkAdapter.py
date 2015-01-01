__author__ = 'paul,ramon'
import socket
from select import select
import time
import sys
import signal


# The adapter exposes the sender functionality over sockets.
# It  reads packet strings from a socket and sends them to the sender
#     serializes responses retrieved by the sender and writes them back to the socket
# Currently, the adapter 
class Adapter:
    learnerSocket = None
    serverSocket = None
    cmdSocket = None
    sender = None
    data = None
    continous = None

    def __init__(self, localCommunicationPort = 18200, cmdIp = "192.168.56.1", cmdPort=5000, 
                 continuous=True):
        self.localCommunicationPort = localCommunicationPort
        self.cmdPort = cmdPort
        self.cmdIp = cmdIp
        self.continuous = continuous

    # returns a new socket to the mapper/learner
    #def setUpSocket(self, commPort, cmdIp, cmdPort):
    def setUpSocket(self, commPort, cmdIp, cmdPort):
        # create an INET, STREAMing socket
        self.serverSocket = socket.socket(
            socket.AF_INET, socket.SOCK_STREAM)
        # bind the socket to a public host and a well-known port
        self.serverSocket.bind(('localhost', commPort))
        # become a server socket
        self.serverSocket.listen(1)

        # accept connections from outside
        (clientSocket, address) = self.serverSocket.accept()
        print "python server: address connected: " + str(address)
        self.learnerSocket = clientSocket
        self.cmdSocket = socket.create_connection((cmdIp, cmdPort))
        self.cmdSocket.setsockopt(socket.IPPROTO_TCP, socket.TCP_NODELAY, 1)
        print "python connected to server Adapter at " + cmdIp + " " + (str(cmdPort))
        self.listenForServerPort()
        
    # closes all open sockets
    # TODO doesn't work all the time 
    def closeSockets(self):
        print self.serverSocket
        print self.learnerSocket
        try:
            try:
                if self.learnerSocket is not None:
                    print "Closing local server socket"
                    self.learnerSocket.close()
            except IOError as e:
                print "Error closing learner socket " + e
            try:
                print(self.serverSocket)
                if self.serverSocket is not None:
                    print "Closing gateway server socket"
                    self.serverSocket.close()
            except IOError as e:
                print "Error closing network adapter socket " + e
            try:
                print(self.cmdSocket)
                if self.cmdSocket is not None:
                    print "Closing gateway server command socket"
                    self.cmdSocket.send("exit\n")
                    self.cmdSocket.close()
            except IOError as e:
                print "Error closing command socket"
            if self.sender is not None and self.sender.isTracking() is True:
                print "Stopping monitoring thread"
                self.sender.stopTracking()

            print "Have a nice day"
            time.sleep(1)
            sys.exit(1)
        except KeyboardInterrupt:
            sys.exit(1)
    
    def closeLearnerSocket(self):
        try:
            if self.learnerSocket is not None:
                print "Closing local server socket"
                self.learnerSocket.close()
        except IOError as e:
                print "Error closing learner socket " + e
                
    # reads string from socket until it reads a space/newline
    def receiveInput(self):
        inputstring = '';
        finished = False
        while not finished:
            if not self.data:
                try:
                    ready = select([self.learnerSocket], [], [], 3)
                    if ready[0]:
                        self.data = self.learnerSocket.recv(1024)
                    else:
                        self.fault("Learner socket has been unreadable for too long")
                except IOError:
                    self.fault("No output received from client, closing")
            else:
                c = self.data[0]
                self.data = self.data[1:]
                if c == '\n' or c == ' ':
                    finished = True
                else:
                    inputstring = inputstring + c
        return inputstring
    
    def receiveNumber(self):
        inputString = self.receiveInput()
        if inputString.isdigit() == False:
            self.fault("Received "+inputString + " but expected a number")
        else:
            return int(inputString)
             

    def listenForServerPort(self):
        newPortFound = False
        newPortString = ""
        while True:
            newPortString = self.cmdSocket.recv(1024)
            for word in newPortString.split(): # TODO check if this really always works with a stream
                print "recvd " + word
                if newPortFound:
                    self.serverPort = int(word)
                    print "next server port: " + word
                    return
                if word == "port":
                    newPortFound = True
                    
    def sendReset(self):
        print "********** reset **********"
        self.cmdSocket.send("reset\n")
        self.listenForServerPort()
        self.sender.setServerPort(self.serverPort)

    # accepts input from the learner, and process it. Sends network packets, looks at the
    # response, extracts the relevant parameters and sends them back to the learner
    def handleInput(self, sender):
        self.sender = sender
        while (True):
            input1 = self.receiveInput()
            seqNr = 0
            ackNr = 0
            if input1 == "reset":
                self.sendReset()
            elif input1 == "exit":
                msg = "Received exit signal " +  "(continuous" +  "=" + self.continuous + ") :"  
                if self.continuous == False:
                    msg = msg + " Closing all sockets"
                    self.closeSockets()
                else:
                    msg = msg + " Closing only learner socket (so we are ready we a new session)"
                    self.closeLearnerSocket()
                return
            else:
                print "*****"
                if input1 in ["listen", "accept", "closeconnection", "closeserver"]:
                    print(" " + input1)
                    self.cmdSocket.send(input1 + "\n") # TODO race-condition here, might go wrong: 
                    response = sender.captureResponse() # response might arrive before sender is ready
                elif input1 != "nil":
                    seqNr = self.receiveNumber()
                    ackNr = self.receiveNumber()
                    print (" " +input1 + " " + str(seqNr) + " " + str(ackNr))
                    response = sender.sendInput(input1, seqNr, ackNr);
                else:
                    print(" nil")
                    response = sender.captureResponse()
                
                if response is not None:
                    print ' ' + response.serialize()
                    self.sendOutput(response.serialize())
                else:
                    print "timeout"
                    self.sendOutput("timeout")

    # sends a string to the learner, and simply adds a newline to denote the end of the string
    def sendOutput(self, outputString):
        self.learnerSocket.send(outputString + "\n")
    
    # prints error message, closes sockets and terminates
    def fault(self, msg):
        print '===FAULT EXIT WITH MESSAGE==='
        print msg
        self.closeSockets()
        sys.exit()
    
    # start adapter by list
    def startAdapter(self, sender):
        print "listening on "+str(self.localCommunicationPort)
        signal.getsignal(signal.SIGINT)
        self.setUpSocket(self.localCommunicationPort, self.cmdIp, self.cmdPort)
        self.handleInput(sender)

