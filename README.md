__tcp-learner__ is a Java/Python tool you can use to automatically learn 
TCP stacks. What does it mean to learn? Well, to obtain a model/state machine
that tells a bit more then the RFC 793 spec. and which fits your TCP stack and
none other. How does it learn? By running tests on the TCP Stack 
(sending inputs / receiving outputs), it learnes about how TCP operates until
it can build a (behavioral model). How much can it learn? Time-stripped TCP
with seq, ack, flags, 0/1 payload and socket calls.

##  Installing ##
The tool can only work reliably on Linux due to some of the Python libraries used.
In addition, the TCP stack cannot be learned locally, it should be done over 
an actual connection. As such, we suggest you do your installation on a 
virtual machine (say Virtual Box) and use that machine to learn your host TCP 
Stack. 

For a quick install and run, clone/download the cav-aec branch of the tool. 
`git clone -b cav-aec https://gitlab.science.ru.nl/pfiteraubrostean/tcp-learner.git`

Make sure you have installed a Java 8 Jdk, Python 2.7, and the Python libraries
Scapy, Pcapy and Impacket. 



## Components ##
* Learner side:
 * _Learner_ Java tool which sends input and receives output strings from a network adapter,
based on this it builds the model. The inputs comprise socket calls ("connect", "close") or packets with flags, sequence, acknowledgement numbers and optionally one byte
payload ("SYN(0,0,0)" or "ACK+PSH(20,30,1)"). Outputs are packets or timeout 
(no packet received)

 * _Network Adapter_ Python tool, transform packet inputs to actual packets and sends them
to _TCP Entity_, or forwards socket command String to the TCP Adapter. Intercepts packet
responses (or timeouts) from the TCP Entity, translates them to strings, sends them
back to _Learner_

* TCP Entity side:
 * _TCP Adapter_ envelops _TCP Entity_, calls corresponding socket calls on it
 * _TCP Entity_ your TCP stack, can either be a Server or a Client
  
##  Running ##
Now, get the TCP Adapter (SutAdapter/socketAdapter.c) and deploy it on the system you want 
to learn (for example your host). Compile it (with any Linux/Windows 
compiler) and use:

`./socketAdapter -a addressOfNetworkAdapter -l portOfNetworkAdapter -p portUsedByTCPEntity`

This should get your TCP Adapter listening for network adapter connections, over which all
socket strings are sent along with system resets. 

The Learner side requires more tweaking. 
1. first, don't allow the OS to interfere to the communication with the TCP Entity by running from a terminal:
`sudo iptables -A OUTPUT -p --tcp-flags RST RST -j DROP`

Then copy the files from the Example directory to the tcp-learner dir. 
2. edit sutinfo.yaml with the alphabet used for learner. All possible inputs are included,
comment out those you don't need. Start off small. (e.g. with only "ACCEPT", "LISTEN" 
and "SYN(V,V,0)"). Don't combine server socket calls with client socket calls!
3. edit config.cfg by setting: 
 * serverIP, serverPort to the IP/port of _TCP Entity_
 * cmdIP, cmdPort to the IP/port of  _TCP Adapter_ 
 * networkInterface (the interface over which communication with the TCP Entity is done)
 * waitime to 0.2 or 0.3 (depending on the network)

4. run the network adapter:
`sudo python Adapter/main.py --configFile config.cfg`
(the adapter should display all the parameters used and start running)

5. (finally) run the java learner. You needn't build it, can just use the distribution:
`java -jar dist/tcp-learner.jar  config.yaml`

There you go! It should be up and running. Use a minimal alphabet at first, so 
that learning is fast. Then experiment with more inputs. 

## Caching ##
After each run, a "cache.ser" is created with outputs for all the tests run. 
This is loaded on a subsequent run and used as cache. (so these tests are not
re-run) It allows you to resume learning from the state it was when you terminated it.

## Handling Complications ##
In case of  non-determinism reported by the learner, tweak the timing parameters. Also,
try changing the mapper from "windows" to "linux".  Non-determinism means
that the same sequence of inputs you received different outputs. For example, assume the tests:
* ACCEPT/TIMEOUT LISTEN/TIMEOUT SYN/SYN+ACK 
* ACCEPT/TIMEOUT LISTEN/TIMEOUT SYN/RST+ACK CLOSE/TIMEOUT

For the sequence ACCEPT LISTEN SYN we obtained over two distinct tests different
outputs (SYN+ACK != RST+ACK)







