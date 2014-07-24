__author__ = 'paul'
import argparse
import ConfigParser
import sys
from tracker import InterfaceType
from sender import Sender
from networkAdapter import Adapter

class Argument:
    def __init__(self, definition, fullDefinition, type, defaultValue, description):
        self.definition = definition
        self.fullDefinition = fullDefinition
        self.description = description
        self.default = defaultValue
        self.type = type

    
class ArgumentParser:
    adapterArguments = [
        Argument("lcp","localCommunicationPort", int, 18200, "Listening adapter port which the learner connects to")
    ]
    configArguments = [
        Argument("c","useConfig", None, None, "Sets whether the tool will read sender args from a configuration file"),
        Argument("csec","configSection", str, "tcp", "The section in the configuration file."),
        Argument("cfile","configFile", str, "config.cfg", "The configuration file used. Preferably left as the default value.")]
    senderArguments = [
        Argument("np","networkPort", int, 15000,"Active adapter port "),
        Argument("npmin","networkPortMinimum", int, 20000, "Set the minimum boundary and starting number for "
                                                       "the network port"),
        Argument("npmax","networkPortMaximum", int, 40000, "Set the maximum boundary after which it reverts back to "
                                                       "networkPortMinimum"),
        Argument("ut","useTracking", bool, True, "If set, then the tracker is used along with the Scapy tool"),
        Argument("wt","waitTime",float, 0.004, "Sets the time the adapter waits for a response before concluding a timeout"),
        Argument("sip","serverIP", str, "131.174.142.204", "The server TCP"),
        Argument("sport","serverPort", int, 7991, "The server port"),
        Argument("smac","serverMAC", str, "50:46:5D:DE:44:C7", "The server MAC address"),
        Argument("rst","resetMechanism", int, 0, "0 selects reset by sending a valid RST and not changing the port,"
                                                   "1 selects reset by changing the port (+1)"
                                                    "2 selects a hybrid, where a valid RST is sent and the port is changed")]
    parsedValues = {}

    def getArguments(self):
        arguments = []
        arguments.extend(self.senderArguments)
        arguments.extend(self.configArguments)
        arguments.extend(self.adapterArguments)
        return arguments

    # parses arguments received from the command line
    # note networkPortMinimum and Maximum are only used in case port switching reset method is used
    def parseCmdArguments(self,programArguments):
        parser = argparse.ArgumentParser(prog="TCP Learner Adapter", description="Tool that transforms abstract messages"
        "received via a localCommunication into valid tcp/ip packets, sends them over the network, retrieves responses"
        "and transforms them back to abstract messages")
        arguments = self.getArguments()
        for argument in arguments:
            if argument.type is None:
                parser.add_argument("-"+argument.definition, "--"+argument.fullDefinition, action="store_true", help=argument.description)
            else:
                parser.add_argument("-"+argument.definition, "--"+argument.fullDefinition, type=argument.type, default=argument.default, help=argument.description)
        ns = parser.parse_args(programArguments)
        return ns

    def parseArguments(self):
        ns = self.parseCmdArguments(sys.argv[1:])
        cmdValues = ns.__dict__
        if ns.useConfig == True:
            print "parsing config"
            configValues = self.parseConfigArguments(ns.configFile, ns.configSection)
            cmdValues.update(configValues)
        self.parsedValues.clear()
        self.parsedValues.update(cmdValues)

    def parseConfigArguments(self, configFile, configSection):
        values = {}
        for argument in self.senderArguments:
            values.update({argument.definition : argument.default})
        config = ConfigParser.RawConfigParser(defaults=values, allow_no_value=True)
        config.read(configFile)
        for argument in self.getArguments():
            if config.has_option(configSection, argument.definition):
                if argument.type is int:
                    values.update({argument.fullDefinition : config.getint(configSection,argument.definition)})
                elif argument.type is bool:
                    values.update({argument.fullDefinition : config.getboolean(configSection,argument.definition)})
                elif argument.type is float:
                    values.update({argument.fullDefinition : config.getfloat(configSection,argument.definition)})
                else:
                    values.update({argument.fullDefinition : config.get(configSection,argument.definition)[1:-1]})
        return values

    def getValueMapForArguments(self, arguments, parsedValues):
        valueMap = {}
        argumentDefinitions = map(lambda x: {x.fullDefinition: parsedValues.get(x.fullDefinition, x.default)}, arguments)
        for argumentDefinition in argumentDefinitions:
            valueMap.update(argumentDefinition)
        return valueMap

    def buildSender(self):
        values = self.getValueMapForArguments(self.senderArguments, self.parsedValues)
        sender = Sender(**values)
        return sender
    def buildAdapter(self):
        values = self.getValueMapForArguments(self.adapterArguments, self.parsedValues)
        adapter = Adapter(**values)
        return adapter