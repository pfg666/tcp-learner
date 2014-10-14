__author__ = 'paul,ramon'
import socket
import time
import sys
import subprocess
import threading
import platform
from interfaceType import InterfaceType
from scapy.all import *
from response import *

# variables used to retain last sequence/acknowledgment sent
seqVar = 0
ackVar = 0

# the sender sends packets with configurable parameters to a server and retrieves responses
class Sender:
    # information of the SUT
    def __init__(self, serverMAC, serverIP, serverPort = 7991,
                 networkInterface="eth1", networkInterfaceType=InterfaceType.Ethernet, networkPort=15000, networkPortMinimum=20000,
                 networkPortMaximum=40000, portNumberFile = "sn.txt", useTracking=False,
                 isVerbose=0, waitTime=0.006, resetMechanism=0):
        # data on sender and server needed to send packets 
        self.serverIP = serverIP
        self.serverPort = serverPort
        self.serverMAC = serverMAC
        self.networkInterface = networkInterface
        self.networkPort = networkPort
        self.networkPortMinimum = networkPortMinimum
        self.networkPortMaximum = networkPortMaximum
        self.portNumberFile = portNumberFile;
        
        # time to wait for a response from the server before concluding a timeout
        self.waitTime = waitTime
        
        # use tracking mechanism to aid Scapy
        self.useTracking = useTracking
        
        # TODO: need to remove this, the client should decide on the reset mechanism.
        self.resetMechanism = resetMechanism
        
        #set verbosity (0/1)
        self.isVerbose = isVerbose
        
        if self.useTracking == True:
            # todo see if this limitation is correct 
            if platform.system() != "Linux":
                print("Tracker is not compatible with Windows can only function on Linux")
                self.useTracking = False
            else:
                from tracker import Tracker
                self.tracker = Tracker(self.networkInterface, self.serverPort, self.serverIP)
                self.tracker.start()
        else:
            self.tracker = None

    # chooses a new port to send packets from
    def refreshNetworkPort(self):
        print("previous local port: " + str(self.networkPort))
        self.networkPort = self.getNextPort()
        print("next local port: " + str(self.networkPort)+"\n")
        return self.networkPort

    # gets a new port number, an increment of the old. Write new number over the old number in the portNumber file.
    def getNextPort(self):
        f = open(self.portNumberFile,"a+")
        f.seek(0)
        line = f.readline()
        if line == '' or int(line) < self.networkPortMinimum:
            networkPort = self.networkPortMinimum
        else:
            networkPort = (int(line)+1)%self.networkPortMaximum
        f.closed
        f = open(self.portNumberFile, "w")
        f.write(str(networkPort))
        f.closed
        return networkPort

    # send a packet onto the network with the given parameters, and return the response packet
    # uses scapy to create and send packets
    # response packets are gathered first through scapy's response
    # should scapy return None, then a tracker is used to retrieve whatever packets scapy has missed (in case it did)
    # TODO Why does scapy miss some packets?
    def sendPacket(self,flagsSet, seqNr, ackNr):
        if self.useTracking == True :
            self.tracker.clearLastResponse()
        #if self.isVerbose == 1 :
        packet = self.createPacket(flagsSet, seqNr, ackNr)
        response = self.sendPacketAndRetrieveResponse(packet)
        return response
    
    # function that creates packet from data strings/integers
    def createPacket(self, tcpFlagsSet, seqNr, ackNr, destIP = None, destPort = None, srcPort = None,
                     ipFlagsSet="DF", data="cc"):
        if destIP is None:
            destIP = self.serverIP
        if destPort is None:
            destPort = self.serverPort
        if srcPort is None:
            srcPort = self.networkPort
        print "" +tcpFlagsSet + " " + str(seqNr) + " " + str(ackNr)
        pIP = IP(dst=destIP, flags=ipFlagsSet)
        pTCP = TCP(sport=srcPort,
        dport=destPort,
        seq=seqNr,
        ack=ackNr,
        flags=tcpFlagsSet)
        if "P" in tcpFlagsSet or "p" in tcpFlagsSet:
            p = pIP / pTCP / Raw(load="cc")
        else:
            p = pIP / pTCP 
        return p
    
    # sends packets and ensures both reception tools are used so as to retrieve the response when such response is given
    def sendPacketAndRetrieveResponse(self, packet):
        # we need first to clear the last response cached in the tracker 
        if self.useTracking == True :
            self.tracker.clearLastResponse()
        #todo find a more elegant way of finding the client IP?
        self.clientIP = packet[IP].src
        # consider adding the parameter: iface="ethx" if you don't receive a response. Also consider increasing the wait time
        scapyResponse = sr1(packet, timeout=self.waitTime, iface=self.networkInterface, verbose=self.isVerbose)
        captureMethod = ""
        if scapyResponse is not None:
            response = self.scapyResponseParse(scapyResponse)
            captureMethod = "scapy"
        else:
            response = None
            if self.useTracking == True:
                # timeout case, return the response (if caught) by the tracker and missed by scapy
                response = self.tracker.getLastResponse(self.networkPort)
                if type(response) is not Timeout:
                    captureMethod = "tracker"
            else:
                response = Timeout()

        if captureMethod != "":
            captureMethod = "("+captureMethod+")"
        print response.serialize() + "  "+captureMethod
        if self.useTracking == True:
            self.tracker.clearLastResponse()
        # if scapyResponse is not None:
        #    self.bijectionCheck(scapyResponse, response)
        return response
    
    # transforms a scapy TCP response packet into an abstract response
    def scapyResponseParse(self, scapyResponse):
        flags = scapyResponse[TCP].flags
        seq = scapyResponse[TCP].seq
        ack = scapyResponse[TCP].ack
        concreteResponse = ConcreteResponse(self.intToFlags(flags), seq, ack)
        return concreteResponse
    
    # check if we can reproduce the response packet from the received response 
    def bijectionCheck(self, packet, concreteMessage):
        # first, initialize information that we should now about the sender/server
        sourceIP = packet[IP].src
        sourcePort = packet[TCP].sport
        destIP = packet[IP].dst
        destPort = packet[TCP].dport
        win = packet[TCP].window
        options = packet[TCP].options
        
        # build packets
        result = IP(src=sourceIP,dst=destIP) / TCP(sport=sourcePort, dport=destPort,flags=concreteMessage.flags, seq=concreteMessage.seq, ack=concreteMessage.ack, window=win, options=options)
        del result.chksum
        result = result.__class__(str(result))
        result[TCP].show()
        packet[TCP].show() 
        return True

    # check whether there is a 1 at the given bit-position of the integer
    def checkForFlag(self, x, flagPosition):
        if x & 2 ** flagPosition == 0:
            return False
        else:
            return True

    # the flags-parameter of a network packets is returned as an int, this function converts
    # it to a string (such as "FA" if the Fin-flag and Ack-flag have been set)
    # MAKE SURE the order of checking/appending characters is the same here as it is in the tracker
    def intToFlags(self, x):
        result = ""
        if self.checkForFlag(x, 0):
            result = result + "F"
        if self.checkForFlag(x, 1):
            result = result + "S"
        if self.checkForFlag(x, 2):
            result = result + "R"
        if self.checkForFlag(x, 3):
            result = result + "P"
        if self.checkForFlag(x, 4):
            result = result + "A"
        return result

    # tells whether tracking is still active
    def isTracking(self):
        return self.useTracking and (not self.tracker.isStopped())

    # stops the tracking thread (so you don't have to)
    def stopTracking(self):
        self.tracker.stop()

    # uses scapy packet sniffer to sniff whatever TCP/IP packets there are on the network that are of interest
    def sniffPackets(self):
        sniffedPackets = sniff(lfilter=lambda x: IP in x and x[IP].src == self.serverIP and
                                                 TCP in x and x[TCP].dport == self.networkPort,
                               timeout=self.waitTime)
        print 'sniffed'
        return sniffedPackets

    # sends input over the network to the server
    def sendInput(self, input1, seqNr, ackNr):
        # add the MAC-address of the server to scapy's ARP-table to use LAN
        # used every iteration, otherwise the entry somehow
        # w disappears after a while
        conf.netcache.arp_cache[self.serverIP] = self.serverMAC
        conf.sniff_promisc=False

        response = None
        timeBefore = time.time()
        if input1 != "nil":
            response = self.sendPacket(input1, seqNr, ackNr)
        else:
            sniffed = self.sniffPackets()
            if len(sniffed) > 0:
                response = self.scapyResponseParse(sniffed[0])
            else:
                response = Timeout()
        timeAfter = time.time()
        timeSpent = timeAfter - timeBefore
        if timeSpent < self.waitTime:
            time.sleep(self.waitTime - timeSpent)
        if type(response) is not Timeout:
            global seqVar, ackVar
            seqVar = response.seq;
            ackVar = response.ack;
        return response

    # resets by way of a valid reset. Requires a valid sequence number. Avoids problems encountered with the maximum
    # number of connections allowed on a port.
    def sendValidReset(self,seq):
        if self.resetMechanism == 0 or self.resetMechanism == 2:
            self.sendInput("R", seq, 0)
            if self.useTracking == True:
                self.tracker.clearLastResponse()
        if self.resetMechanism == 1 or self.resetMechanism == 2:
            self.sendReset()

    # resets the connection by changing the port number. Be careful, on some OSes (Win 8) upon hitting a certain number of
    # connections opened on a port, packets are sent to close down connections, which affects learning. TCP configurations
    # can be altered, but I'd say in case learning involves many queries, use the other method.
    def sendReset(self):
        self.refreshNetworkPort()
        if self.useTracking == True:
            self.tracker.clearLastResponse()

# example on how to run the sender
if __name__ == "__main__":
    print "main test"
    sender = Sender(serverMAC="08:00:27:23:AA:AF", serverIP="131.174.142.227", serverPort=8000, useTracking=False, isVerbose=0, networkPortMinimum=20000, waitTime=1)
    seq = 50
    sender.refreshNetworkPort()
    sender.sendInput("S", seq, 1) #SA svar seq+1 | SYN_REC
    sender.sendInput("A", seq + 1, seqVar + 1) #A svar+1 seq+2 | CLOSE_WAIT
    sender.sendInput("FA", seq + 1, seqVar + 1)
