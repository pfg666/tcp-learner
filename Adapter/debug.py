__author__ = 'paul,ramon'
import signal
from argparser import ArgumentParser
from sender import Sender
from traceRunner import TraceRunner

# debug main. Uses the trace runner to run individual traces
if __name__ == "__main__":
    print "==Preparation=="
    argumentParser = ArgumentParser()
    
    print "\n==Sender Setup=="
    sender = argumentParser.buildSender()
    print str(sender)
    
    print "\n==Trace Runner Setup=="
    runner = argumentParser.buildTraceRunner()
    print str(runner)

    runner.executeTraceFile(sender, "trace.txt")
    