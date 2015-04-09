from pcapy import open_live
from impacket.ImpactDecoder import EthDecoder,Dot11WPA2Decoder
from impacket.ImpactPacket import IP, TCP
from interfaceType import InterfaceType
import threading
from response import ConcreteResponse, Timeout

# Tool that monitors communication of a server port and interface, built on the "pcapy" framework.
# It always stores the last response received from the server. The sender tool, in  case scapy did not receive
# a response, can query the tracker, to see if it did detect a packet. This is useful, since scapy does miss
# some responses.
class Tracker(threading.Thread):
    serverPort = 0
    senderPort = 0
    pcap = None
    interface = 'eth0'
    decoder = None
    max_bytes = 1024
    promiscuous = False
    readTimeout = 1 # in milliseconds
    isStopped = False
    lastResponse = None
    lastResponses = dict()
    
    def __init__ (self,interface, serverIp, interfaceType=InterfaceType.Ethernet, readTimeout = 1):
        super(Tracker, self).__init__()
        self.interface = interface
        self.decoder = self.getDecoder(interfaceType) # Wireless not yet supported
        self._stop = threading.Event()
        self.daemon = True
        self.readTimeout = readTimeout
        self.serverIp = serverIp
        self.lastResponse = None
        self.lastResponses = dict()
        self.responseHistory = set()
        
    def getDecoder(self, interfaceType):
        if interfaceType == InterfaceType.Ethernet:
            return EthDecoder()
        else:
            return Dot11WPA2Decoder()
            # if interfaceType == InterfaceType.Wireless:
            #     print "In Tracker.py: Wireless not yet supported, sorry"
            #     exit(0)

    def stop(self):
        self._stop.set()

    def isStopped(self):
        return self._stop.isSet()
    
    # This is method is called periodically by pcapy
    def callback(self,hdr,data):
        if self.isStopped() == True:
            print("Tracker is stopped.")
            #exit(0) # results in a strange warning
        else:
            packet=self.decoder.decode(data)
            l2=packet.child()
            if isinstance(l2,IP):
                l3=l2.child()
    #       Due to the filter used, all packets should use TCP
                src_ip = l2.get_ip_src()
                dst_ip = l2.get_ip_dst()
                tcp_src_port = l3.get_th_sport()
                tcp_dst_port = l3.get_th_dport()
                tcp_syn = l3.get_th_seq()
                tcp_ack = l3.get_th_ack()
                response = self.impacketResponseParse(l3)
                if (response.seq, response.ack, response.flags) not in self.responseHistory:
                    if "S" in response.flags:
                        self.responseHistory.add((response.seq, response.ack, response.flags))
                    self.lastResponses[(tcp_src_port, tcp_dst_port)] = response
                    self.lastResponse = response
    #                print "tracker:" + self.impacketResponseParse(l3).__str__()

    def processResponse(self, response):
        if response is not None:
            self.lastResponse = response
            if response.flags == "SA" and response in self.lastResponses:
                print 'ignoring SA retransmission ' + response.__str__()
            else:
                print 'non SA-ret packet:' + response.__str__()
#                self.lastResponses.append(response)
#                self.lastResponse = response


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
    
    # clears all last responses for all ports (keep that in mind if you have responses on several ports)
    # this is done because when learning, we only care about one port
    def clearLastResponse(self):
        self.lastResponse = None
        self.lastResponses.clear()
    
    def reset(self):
        self.clearLastResponse()
        self.responseHistory.clear()
    
    # fetches the last response from an active port. If no response was sent, then it returns Timeout
    def getLastResponse(self, serverPort, senderPort, requestSN = None):
        lastResponse = self.lastResponses.get((serverPort,senderPort))
        if lastResponse is None:
            lastResponse = Timeout()
        return lastResponse
#        else:
#            if  requestSN is None or requestSN <= self.lastResponse.ack:
#                lastResponse = self.lastResponse
#            else:
#                if len(self.lastResponses) > 0:
#                    for response in reversed(self.lastResponses):
#                        if response.seq <= requestSN:
#                            lastResponse = response
#                            break
        return lastResponse
    
    def run(self):
        self.trackPackets()

    def trackPackets(self):
        self.pcap = open_live(self.interface, self.max_bytes, self.promiscuous, self.readTimeout)
        self.pcap.setfilter("tcp and ip src "+str(self.serverIp))#+" and tcp port " + str(self.serverPort))
        a = self.pcap.loop(0,self.callback)