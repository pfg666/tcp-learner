tcp-learner is a Java/Python tool you can use to automatically learn 
TCP stacks. What does it mean to learn? Well, to obtain a model/state machine
that tells a bit more then the RFC 793 spec. and which fits your TCP stack and
none other.

  Installing
The tool can only work reliably on Linux due to some of the Python libraries used.
In addition, the TCP stack cannot be learned locally, it should be done over 
an actual connection. As such, we suggest downloading 
For a quick install and run, clone/download the cav-aec branch of the tool. 
Make sure you have installed a Java 8 Jdk, Python 2.7, and the Python libraries
Scapy, Pcapy and Impacket. Also make sure that they are included in the path.

For this experiment you will also need a TCP stack. Localhost might work but 
we suggest running VirtualBox. Actually, we sugge

  
  Running
Now, get the SutAdapter (socketAdapter.c) and deploy it on the system you want 
to learn to learn. Compile it (with any Linux/Windows compiler) and use:

./socketAdapter -a addressOfNetworkAdapter -l portOfNetworkAdapter -p portUsedBySut

Now on to the 
