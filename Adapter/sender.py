__author__ = 'ramon, paul'
import socket
import time
import sys
import subprocess
import threading
from scapy.all import *
from response import *
from tracker import Tracker, InterfaceType

seqVar = 0
ackVar = 0
ethController = "50:46:5D:DE:44:C7"
#ethController = "08:00:27:26:8D:6B"
ethServerIp = "131.174.142.204"
#ethServerIp = "131.174.142.157"
wController = "DC:85:DE:8D:55:58"
vboxController = "08:00:27:00:F8:FD"
vboxServerIp = "192.168.56.1"

class Sender:
    # information of the SUT
    def __init__(self, serverMAC=ethController, serverIP = ethServerIp, serverPort = 7991,
                 networkInterface="eth1", networkInterfaceType=InterfaceType.Ethernet, networkPort=15000, networkPortMinimum=20000,
                 networkPortMaximum=40000, useTracking=False,
                 isVerbose=0, waitTime=0.004, resetMechanism=0):
        self.serverIP = serverIP
        self.serverPort = serverPort
        self.serverMAC = serverMAC
        self.networkPort = networkPort
        self.networkPortMinimum = networkPortMinimum
        self.networkPortMaximum = networkPortMaximum
        #time to wait for a response from the SUT before concluding a timeout
        self.waitTime = waitTime
        #set verbosity (0/1)
        self.resetMechanism = resetMechanism
        self.isVerbose = isVerbose
        self.useTracking = useTracking
        if self.useTracking == True:
            self.tracker = Tracker(networkInterface, self.serverPort, )
            self.tracker.start()
        else:
            self.tracker = None

    # chooses a new port to send packets from
    def refreshNetworkPort(self):
        print("previous local port: " + str(self.networkPort))
        if self.networkPort == self.networkPortMaximum or self.networkPort < self.networkPortMinimum:
            self.networkPort = self.networkPortMinimum
        else:
            self.networkPort = self.networkPort + 1
        print("next local port: " + str(self.networkPort)+"\n")

    # send a packet onto the network with the given parameters, and return the response packet
    # uses two scapy to create and send packets, while responses are gathered first through scapy's response, should
    # scapy return None, then a tracker is used to retrieve whatever packets scapy has missed (in case it did)
    def sendPacket(self,flagsSet, seqNr, ackNr):
        captureMethod = ""
        if self.useTracking == True :
            self.tracker.clearLastResponse()
        #if self.isVerbose == 1 :
        print "" +flagsSet + " " + str(seqNr) + " " + str(ackNr)

        pIP = IP(dst=self.serverIP, flags="DF")
        pTCP = TCP(sport=self.networkPort,
                   dport=self.serverPort,
                   seq=seqNr,
                   ack=ackNr,
                   flags=flagsSet)
        response = None
        if "P" in flagsSet or "p" in flagsSet:
            # this adds payload to the packet when the push-flag has been set: not used yet.
            p = pIP / pTCP / Raw(load="cc")
            scapyResponse = sr1(p, timeout=self.waitTime, verbose=self.isVerbose)
        else:
            p = pIP / pTCP
            scapyResponse = sr1(p, timeout = self.waitTime, verbose = self.isVerbose)
        if scapyResponse is not None:
            response = self.scapyResponseParse(scapyResponse)
            captureMethod = "scapy"
        else:
            response = None
            if self.useTracking == True:
                # timeout case, return the response (if caught) by the tracker and missed by scappy
                time.sleep(self.waitTime)
                response = self.tracker.getLastResponse(self.networkPort)
                if type(response) is not Timeout:
                    captureMethod = "tracker"
        if captureMethod != "":
            captureMethod = "("+captureMethod+")"
        print response.serialize() + "  "+captureMethod
        if self.useTracking == True:
            self.tracker.clearLastResponse()
        return response

    # transforms a scappy TCP response packet into an abstract response
    def scapyResponseParse(self, scapyResponse):
        flags = scapyResponse[TCP].flags
        seq = scapyResponse[TCP].seq
        ack = scapyResponse[TCP].ack
        response = ConcreteResponse(self.intToFlags(flags), seq, ack)
        return response

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
        return sniffedPackets

    # sends input over the network to the server
    def sendInput(self, input1, seqNr, ackNr):
        # add the MAC-address of the server to scapy's ARP-table to use LAN
        # used every iteration, otherwise the entry somehow
        # w disappears after a while
        conf.netcache.arp_cache[self.serverIP] = self.serverMAC
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

if __name__ == "__main__":
    sender = Sender(useTracking=True, isVerbose=0, networkPortMinimum=20000, waitTime=0.1)
    seq = 50
    sender.refreshNetworkPort()
    sender.sendInput("SP", seq, 1) #SA svar seq+1 | SYN_REC
    sender.sendInput("A", seq + 1, seqVar + 1) #A svar+1 seq+2 | CLOSE_WAIT
    #sender.sendInput("AP", seq+1, seqVar+1)
   # sender.sendInput("AP", seq+3, seqVar+1)
   # sender.sendInput("AP", seq+5, seqVar+1)
   # sender.sendInput("AP", seq+7, seqVar+1)
    sender.stopTracking()
