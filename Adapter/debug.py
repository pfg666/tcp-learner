__author__ = 'paul,ramon'
import signal
from argparser import ArgumentParser
from sender import Sender
from traceRunner import TraceRunner

global adapter
adapter = None

# main method. An initial local port can be given as parameter for the program
if __name__ == "__main__":
    print "==Preparation=="
    argumentParser = ArgumentParser()
    
    print "\n==Sender Setup=="
    sender = argumentParser.buildSender()
    
    print "\n==Trace Runner Setup=="
    adapter = argumentParser.buildTraceRunner()
    