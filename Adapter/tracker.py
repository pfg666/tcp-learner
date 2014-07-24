from pcapy import open_live
from impacket.ImpactDecoder import EthDecoder
from impacket.ImpactPacket import IP, TCP, UDP, ICMP
from response import *
import threading
import sys


class InterfaceType:
    Wireless, Ethernet = range(0,2)
    @staticmethod
    def getDecoder(interfaceType):
        if interfaceType == InterfaceType.Ethernet:
            return EthDecoder()
        else:
            if interfaceType == InterfaceType.Wireless:
                print "In Tracker.py: Wireless not yet supported, sorry"
                exit(0)

# Tool that monitors communication of a server port and interface, built on the "pcapy" framework.
# It always stores the last response received from the server. The sender tool, in  case scapy did not receive
# a response, can query the tracker, to see if it did detect a packet. This is useful, since scapy does miss
# some responses.
class Tracker(threading.Thread):
    port = 0
    interface = 'eth0'
    decoder = None
    max_bytes = 1024
    promiscuous = False
    readTimeout = 10 # in milliseconds
    isStopped = False
    lastResponses = dict()
    
    def __init__ (self,interface,  port, interfaceType=InterfaceType.Ethernet, readTimeout = 1):
        super(Tracker, self).__init__()
        self.interface = interface
        self.port = port        
        self.decoder = InterfaceType.getDecoder(interfaceType) # Wireless not yet supported
        self._stop = threading.Event()
        self.daemon = True
        self.readTimeout = readTimeout
        print "stopping state"
        print self._stop.isSet()

    def stop(self):
        self._stop.set()

    def isStopped(self):
        return self._stop.isSet()
        
    # This is method is called periodically by pcapy
    def callback(self,hdr,data):
        if self.isStopped() == True:
            print("Tracker was stopped at ")
            sys.exit(1)

        packet=self.decoder.decode(data)
        l2=packet.child()
        if isinstance(l2,IP):
            l3=l2.child()
#       Due to the filter used, all packets should use TCP
            src_ip = l2.get_ip_src()
            dst_ip = l2.get_ip_dst()
            tcp_dst_port = l3.get_th_sport()
            tcp_src_port = l3.get_th_dport()
            tcp_syn = l3.get_th_seq()
            tcp_ack = l3.get_th_ack()
            if l3.get_th_sport() == self.port:
                self.lastResponses[l3.get_th_dport()] = self.impacketResponseParse(l3)
#                print "tracker:" + self.impacketResponseParse(l3).serialize()


    # MAKE SURE the order of checking/appending characters is the same here as it is in the sender
    def impacketResponseParse(self, tcpPacket):
        response = None
        if isinstance(tcpPacket, TCP):
            tcp_syn = tcpPacket.get_th_seq()
            tcp_ack = tcpPacket.get_th_ack()

            flags = 'F' if tcpPacket.get_FIN() == 1 else ''
            flags += 'S' if tcpPacket.get_SYN() == 1 else ''
            flags += 'R' if tcpPacket.get_RST() == 1 else ''
            flags += 'P' if tcpPacket.get_PSH() == 1 else ''
            flags += 'A' if tcpPacket.get_ACK() == 1 else ''
            response = ConcreteResponse(flags, tcp_syn, tcp_ack)
        return response
    
    def clearLastResponse(self):
        self.lastResponses.clear()
    
    def getLastResponse(self, localPort):
        lastResponse = self.lastResponses.get(localPort)
        if lastResponse is None:
            lastResponse = Timeout()
        return lastResponse
        
    def run(self):
        self.trackPackets()

    def trackPackets(self):
        pcap = open_live(self.interface, self.max_bytes, self.promiscuous, self.readTimeout)
        pcap.setfilter("tcp port " + str(self.port))
        pcap.loop(0,self.callback)