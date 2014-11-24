__author__ = 'paul'
try:
    import argparse
    has_argparse = True
except ImportError:
    print 'argparse is not available, will use command line interface only'
    has_argparse = False
import sys
import interfaceType
from ConfigParser import RawConfigParser
from sender import Sender
from networkAdapter import Adapter
from args import Argument
# for the arguments of each components
import args

class ArgumentParser:
<<<<<<< HEAD
    adapterArguments = [
        Argument("lcp","localCommunicationPort", int, 18200, "Listening adapter port which the learner connects to")
    ]
    configArguments = [
        Argument("c","useConfig", None, None, "Sets whether the tool will read sender args from a configuration file"),
        Argument("csec","configSection", str, "tcp", "The section in the configuration file."),
        Argument("cfile","configFile", str, "config.cfg", "The configuration file used. Preferably left as the default value.")]
    senderArguments = [
        Argument("ni","networkInterface", str, "eth0","The net interface through which the client communicates"),
        Argument("np","networkPort", int, 15000,"Active adapter port "),
        Argument("npmin","networkPortMinimum", int, 20000, "Set the minimum boundary and starting number for "
                                                       "the network port"),
        Argument("npmax","networkPortMaximum", int, 40000, "Set the maximum boundary after which it reverts back to "
                                                       "networkPortMinimum"),
        Argument("pnf","portNumberFile", str, "sn.txt", "File with the port number"),
        Argument("ut","useTracking", bool, True, "If set, then the tracker is used along with the Scapy tool"),
        Argument("wt","waitTime",float, 0.06, "Sets the time the adapter waits for a response before concluding a timeout"),
        Argument("sip","serverIP", str, "10.42.0.42", "The TCP server"),
        Argument("sport","serverPort", int, 20000, "The server port"),
        # Argument("saport" , "serverAdapterPort", int, 19999, "the server adapter port"),
        Argument("smac","serverMAC", str, "00:0C:29:22:B9:6B", "The server MAC address"),
        Argument("rst","resetMechanism", int, 0, "0 selects reset by sending a valid RST and not changing the port,"
                                                   "1 selects reset by changing the port (+1)"
                                                    "2 selects a hybrid, where a valid RST is sent and the port is changed")]
    parsedValues = {}

=======
    
    configValues = None
    # reads config values for arg parser only once
    def getConfigValues(self):
        if self.configValues is None:
            self.configValues = self.parseCmdArguments(sys.argv[1:], args.configArguments, fillWithDefault=True)
        return self.configValues
    
    # fetches the whole list of arguments
>>>>>>> bb8f98b991127c40d26b3a6665e19265cf5752b7
    def getArguments(self):
        arguments = []
        arguments.extend(args.senderArguments)
        arguments.extend(args.configArguments)
        arguments.extend(args.adapterArguments)
        return arguments
    
    # parses the settable arguments from command line, then from config file if option is given
    def parseArguments(self, settableArguments):
        parsedValues = {}
        configOptions = self.getConfigValues()
        # if reading from config file is enabled, stamp argument values read from config file to parsedValuesMap
        if configOptions["useConfig"] == True:
            global has_argparse
            if has_argparse == False:
                print "cannot use the configuration parser because the \"argparse\" module couldn't be located"
                exit()
            configValues = self.parseConfigArguments(configOptions["configFile"], configOptions["configSection"], settableArguments, fillWithDefault=True)
            parsedValues.update(configValues)
            
        # stamp cmd  values read from cmd line to map (they will overwrite options set via config)
        if configOptions["useConfig"] == True:
            cmdValues = self.parseCmdArguments(sys.argv[1:], settableArguments, fillWithDefault=False)
        else:
            cmdValues = self.parseCmdArguments(sys.argv[1:], settableArguments, fillWithDefault=True)
        parsedValues.update(cmdValues)
        return parsedValues
        

    # parses arguments received from the command line
    # note networkPortMinimum and Maximum are only used in case port switching reset method is used
    def parseCmdArguments(self,cmdOptions, settableArguments, fillWithDefault=False):
        parser = argparse.ArgumentParser(prog="TCP Learner Adapter", description="Tool that transforms abstract messages"
        "received via a localCommunication into valid tcp/ip packets, sends them over the network, retrieves responses"
        "and transforms them back to abstract messages")
        for argument in settableArguments:
            if argument.type is None:
                parser.add_argument("-"+argument.definition, "--"+argument.fullDefinition, action="store_const", const=True, default=False, help=argument.description)
            else:
<<<<<<< HEAD
                parser.add_argument("-"+argument.definition, "--"+argument.fullDefinition, type=argument.type, default=argument.default, help=argument.description)
        ns = parser.parse_args(programArguments)
        return ns

    def parseArguments(self):
        global has_argparse
        ns = self.parseCmdArguments(sys.argv[1:])
        cmdValues = ns.__dict__
        if ns.useConfig == True:
            if has_argparse != True:
                print "cannot use the configuration parser because the \"argparse\" module couldn't be located"
                exit()
            print "parsing config"
            configValues = self.parseConfigArguments(ns.configFile, ns.configSection)
            cmdValues.update(configValues)
        self.parsedValues.clear()
        self.parsedValues.update(cmdValues)
=======
                if fillWithDefault == True:
                    parser.add_argument("-"+argument.definition, "--"+argument.fullDefinition, type=argument.type, default = argument.default, help=argument.description)
                else: 
                    parser.add_argument("-"+argument.definition, "--"+argument.fullDefinition, type=argument.type, help=argument.description)
        ns, unknown = parser.parse_known_args(cmdOptions)
        reducedValues = dict((k, v) for k, v in vars(ns).iteritems() if v is not None) # build dict from namespace without None values
        return reducedValues
>>>>>>> bb8f98b991127c40d26b3a6665e19265cf5752b7

    # parses arguments received via a configuration file using the argparse module (see https://docs.python.org/2.7/library/argparse.html)
    def parseConfigArguments(self, configFile, configSection, settableArguments, fillWithDefault=False):
        values = {}
        config = RawConfigParser(defaults=values, allow_no_value=True)
        config.read(configFile)
        for argument in settableArguments:
            definition = None
            if config.has_option(configSection, argument.fullDefinition):
                definition = argument.fullDefinition
            elif config.has_option(configSection, argument.definition):
                definition = argument.definition
            if definition is not None:
                if argument.type is int:
                    values.update({argument.fullDefinition : config.getint(configSection,definition)})
                elif argument.type is bool:
<<<<<<< HEAD
                    print argument.definition
                    values.update({argument.fullDefinition : config.getboolean(configSection,argument.definition)})
=======
                    values.update({argument.fullDefinition : config.getboolean(configSection,definition)})
>>>>>>> bb8f98b991127c40d26b3a6665e19265cf5752b7
                elif argument.type is float:
                    values.update({argument.fullDefinition : config.getfloat(configSection,definition)})
                else:
                    values.update({argument.fullDefinition : config.get(configSection,definition)[1:-1]})
            elif fillWithDefault==True:
                values.update({argument.fullDefinition : argument.default})
        return values

    # receives a list of arguments and a map of argument definitions to values. Selects only definition - value pairs that
    # are relevant to the set of given arguments 
    # no longer needed
    def getValueMapForArguments(self, arguments, parsedValues):
        valueMap = {}
        argumentDefinitions = map(lambda x: {x.fullDefinition: parsedValues.get(x.fullDefinition, x.default)}, arguments)
        for argumentDefinition in argumentDefinitions:
            valueMap.update(argumentDefinition)
        return valueMap

    # builds the sender component of the learning setup
    def buildSender(self):
        values = self.parseArguments(args.senderArguments)
        sender = Sender(**values)
        return sender
    
    # builds the adapter component of the learning setup
    def buildAdapter(self):
        values = self.parseArguments(args.adapterArguments)
        # values = self.getValueMapForArguments(self.adapterArguments, values)
        adapter = Adapter(**values)
        return adapter
