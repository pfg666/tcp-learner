tcp-learner is a Java/Python tool you can use to automatically learn 
TCP stacks. What does it mean to learn? Well, to obtain a model/state machine
that tells a bit more then the RFC 793 spec. and which fits your TCP stack and
none other. 

#  Installing #
The tool can only work reliably on Linux due to some of the Python libraries used.
In addition, the TCP stack cannot be learned locally, it should be done over 
an actual connection. As such, we suggest you do your installation on a 
virtual machine (say Virtual Box) and use that machine to learn your host TCP 
Stack. 

For a quick install and run, clone/download the cav-aec branch of the tool. 
Make sure you have installed a Java 8 Jdk, Python 2.7, and the Python libraries
Scapy, Pcapy and Impacket. 

# Components #
Learner side:
_Learner_ Java tool which sends input and receives output strings from a network adapter,
based on this it builds the model. The inputs comprise socket calls ("connect", "close") or packets with flags, sequence, acknowledgement numbers and optionally one byte
payload ("SYN(0,0,0)" or "ACK+PSH(20,30,1)"). Outputs are packets or timeout 
(no packet received)

_Network Adapter_ Python tool, transform packet inputs to actual packets and sends them
to _TCP Entity_, or forwards socket command String to the TCP Adapter. Intercepts packet
responses (or timeouts) from the TCP Entity, translates them to strings, sends them
back to _Learner_

TCP Entity side:
_TCP Adapter_ envelops _TCP Entity_, calls corresponding socket calls on it
_TCP Entity_ your TCP stack
  
#  Running #
Now, get the TCP Adapter (SutAdapter/socketAdapter.c) and deploy it on the system you want 
to learn (for example your host). Compile it (with any Linux/Windows 
compiler) and use:

`./socketAdapter -a addressOfNetworkAdapter -l portOfNetworkAdapter -p portUsedBySut`

This should get your TCP Adapter listening for upcoming 
