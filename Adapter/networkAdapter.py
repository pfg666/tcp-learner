__author__ = 'paul,ramon'
import socket
from select import select
import time
import sys
import inspect
import argparse
import signal
from scapy.all import *
from sender import Sender
import interfaceType



# The adapter exposes the sender functionality over sockets.
# It  reads packet strings from a socket and sends them to the sender
#     serializes responses retrieved by the sender and writes them back to the socket
# Currently, the adapter 
class Adapter:
    learnerSocket = None
    serverSocket = None
    sender = None
    data = None

    def __init__(self, localCommunicationPort = 18200):
        self.localCommunicationPort = localCommunicationPort

    # returns a new socket to the mapper/learner
    def setUpSocket(self, commPort):
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
            except IOError:
                print "Error closing"
                sys.exit(1)
            try:
                print(self.serverSocket)
                if self.serverSocket is not None:
                    print "Closing gateway server socket"
                    self.serverSocket.close()
            except IOError:
                print "Error closing"
                sys.exit(1)
            if self.sender is not None and self.sender.isTracking() is True:
                print "Stopping monitoring thread"
                self.sender.stopTracking()

            print "Have a nice day"
            sys.exit(1)
        except KeyboardInterrupt:
            sys.exit(1)
            
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
             

    # accepts input from the learner, and process it. Sends network packets, looks at the
    # response, extracts the relevant parameters and sends them back to the learner
    def handleInput(self, sender):
        self.sender = sender
        while (True):
            input1 = self.receiveInput()
            seqNr = 0
            ackNr = 0
            if input1 == "reset":
                print "reset"
                print "********** reset **********"
                self.sender.refreshNetworkPort()
               # seqNr = int(self.receiveInput())
               # sender.sendValidReset(seqNr)
            else:
                if input1 == "exit":
                    self.closeSockets()
                    return
                else:
                    print "*****"
                    if input1 != "nil":
                        seqNr = self.receiveNumber()
                        ackNr = self.receiveNumber()
                        print (" " +input1 + " " + str(seqNr) + " " + str(ackNr))
                        response = sender.sendInput(input1, seqNr, ackNr);
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
        origSigInt = signal.getsignal(signal.SIGINT)
        self.setUpSocket(self.localCommunicationPort)
        self.handleInput(sender)

