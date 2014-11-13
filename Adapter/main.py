__author__ = 'paul,ramon'
import signal
from argparser import ArgumentParser
from sender import Sender

global adapter
adapter = None

# routine used to close the server socket, so you don't have to
def signal_handler(sign, frame):
    signal.signal(sign, origSigInt)
    print 'You pressed Ctrl+C, meaning you want to stop the system!'
    adapter.closeSockets()
    signal.signal(sign, signal_handler)
     # sets up the close server socket routine
def setupSignalHandler():
    print "Setting up SIG Handlers"
    signal.signal(signal.SIGINT, signal_handler)

# main method. An initial local port can be given as parameter for the program
if __name__ == "__main__":
    print "running"
    origSigInt = signal.getsignal(signal.SIGINT)
    setupSignalHandler()
    argumentParser = ArgumentParser()
    argumentParser.parseArguments()
    sender = argumentParser.buildSender()
    print "sender setup"
    print sender.__dict__
    adapter = argumentParser.buildAdapter()
    adapter.startAdapter(sender)