import socket
from select import select
import time
import sys
import inspect
import argparse
import signal
from scapy.all import *
from sender import Sender
from tracker import InterfaceType

global data
data = ""


class Adapter:
    mapperSocket = None
    serverSocket = None
    sender = None

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
        self.mapperSocket = clientSocket

    def closeSockets(self):
        print self.serverSocket
        print self.mapperSocket
        try:
            try:
                if self.mapperSocket is not None:
                    print "Closing local server socket"
                    self.mapperSocket.close()
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
    # gets a string from the socket, up until a space/newline
    def receiveInput(self):
        inputstring = '';
        finished = False
        while not finished:
            if not data:
                try:
                    ready = select([self.mapperSocket], [self.mapperSocket], [], 3)
                    if ready[0]:
                        data = self.mapperSocket.recv(1024)
                except IOError:
                    print "No output received from client, closing"
                    self.mapperSocket.close();
                    sys.exit()
            else:
                c = data[0]
                global data
                data = data[1:]
                if c == '\n' or c == ' ':
                    finished = True
                else:
                    inputstring = inputstring + c
        return inputstring

    # accept input from the learner, and process it. Sends network packets, looks at the
    # response, extracts the relevant parameters and sends them back to the learner
    def handleInput(self, sender):
        self.sender = sender
        while (True):
            input1 = self.receiveInput()
            seqNr = 0
            ackNr = 0
            if input1 != "reset":
                print "*****"
                if input1 != "nil":
                    seqNr = int(self.receiveInput())
                    ackNr = int(self.receiveInput())
                    print (" " +input1 + " " + str(seqNr) + " " + str(ackNr))
                    response = sender.sendInput(input1, seqNr, ackNr);

                if response is not None:
                    print ' ' + response.serialize()
                    self.sendOutput(response.serialize())
                else:
                    print "timeout"
                    self.sendOutput("timeout")
            else:
                print "reset"
                print "********** reset **********"
                seqNr = int(self.receiveInput())
                sender.sendValidReset(seqNr)

    # sends a string to the learner, and simply adds a newline to denote the end of the string
    def sendOutput(self, outputString):
        self.mapperSocket.send(outputString + "\n")

    def startAdapter(self, sender):
        print "listening on "+str(self.localCommunicationPort)
        origSigInt = signal.getsignal(signal.SIGINT)
        self.setUpSocket(self.localCommunicationPort)
        self.handleInput(sender)

