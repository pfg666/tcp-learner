__author__ = 'paul,ramon'
import signal
from argparser import ArgumentParser
from sender import Sender

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