__author__ = 'paul,ramon'
import signal
from argparser import ArgumentParser
from sender import Sender

"""
   listens on a port for actions, packet strings or reset, forwards them to the sender component,
   retrieves responses, processes them back to strings and writes them to the socket

   usage: sudo python main.py [networkAdapter param] [sender param] [actionSender param] 
     
   For parameters of each component, see args.py
   
   for learning:
       run the C TCP server adapter (which by itself runs a TCP server)
       run this program which opens a connection to the adapter and starts the listener
       run the learner tool and commence learning of the TCP server implementation
    
"""


global adapter
adapter = None

# routine used to close the server socket, so you don't have to
def signal_handler(sign, frame):
    signal.signal(sign, origSigInt)
    print "\n==Processing Interrupt=="
    print 'You pressed Ctrl+C, meaning you want to stop the system!'
    adapter.closeSockets()
    signal.signal(sign, signal_handler)
    
     # sets up the close server socket routine
def setupSignalHandler():
    print "Setting up SIG Handlers"
    signal.signal(signal.SIGINT, signal_handler)

# main method. An initial local port can be given as parameter for the program
if __name__ == "__main__":
    print "==Preparation=="
    origSigInt = signal.getsignal(signal.SIGINT)
    setupSignalHandler()
    argumentParser = ArgumentParser()
    sender = argumentParser.buildSender()
    print "\n==Sender Setup=="
    print vars(sender)
    adapter = argumentParser.buildAdapter()
    print "\n==Adapter Setup=="
    print vars(adapter)
    print "\n==Starting Adapter=="
    adapter.startAdapter(sender)