__author__ = 'paul,ramon'

"""
   run a sequence of abstract inputs from a file using the Java Mapper in the learner project. Currently, it runs the trace in "trace.txt"

   usage: sudo python debug.py [traceRunner param] [sender param] [actionSender param]  
   
    Example:
    
    file trace.txt with:
    SYN(INV, INV)
    ACK(V, V)
    SYN(V, V)
    ...
    
"""

import signal
from argparser import ArgumentParser
from sender import Sender
from traceRunner import TraceRunner

# debug main. Uses the trace runner to run individual traces
if __name__ == "__main__":
    print "==Preparation=="
    argumentParser = ArgumentParser()
    
#     print "\n==Sender Setup=="
#     sender = argumentParser.buildSender()
#     print str(sender)
    
    print "\n==Trace Runner Setup=="
    runner = argumentParser.buildTraceRunner()
    print str(runner)
    
    print "\n==Action Sender Setup=="
    actionSender = argumentParser.buildActionSender()
    print str(actionSender)

    runner.executeTraceFile(actionSender, "trace.txt")
    