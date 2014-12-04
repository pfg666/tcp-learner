class Argument:
    def __init__(self, definition, fullDefinition, type, defaultValue, description):
        self.definition = definition
        self.fullDefinition = fullDefinition
        self.description = description
        self.default = defaultValue
        self.type = type

# list of arguments for each component in the setup

adapterArguments = [
    Argument("lcp","localCommunicationPort", int, 18200, "Listening adapter port which the learner connects to")
]

configArguments = [
    Argument("c","useConfig", None, None, "Sets whether the tool will read sender args from a configuration file"),
    Argument("csec","configSection", str, "tcp", "The section in the configuration file."),
    Argument("cfile","configFile", str, "../config.cfg", "The configuration file used. Preferably left as the default value.")]

senderArguments = [
    Argument("ni","networkInterface", str, "eth0","The net interface through which the client communicates"),
    Argument("sp","senderPort", int, 15000,"Active adapter port "),
    Argument("spmin","senderPortMinimum", int, 20000, "Set the minimum boundary and starting number for "
                                                   "the network port"),
    Argument("spmax","senderPortMaximum", int, 40000, "Set the maximum boundary after which it reverts back to "
                                                   "networkPortMinimum"),
    Argument("v","isVerbose", bool, True, "If true then more text will be displayed"),
    Argument("pnf","portNumberFile", str, "sn.txt", "File with the port number"),
    Argument("ut","useTracking", bool, True, "If set, then the tracker is used along with the Scapy tool"),
    Argument("wt","waitTime",float, 0.06, "Sets the time the adapter waits for a response before concluding a timeout"),
    Argument("sip","serverIP", str, "192.168.56.1", "The TCP server"),
    Argument("sport","serverPort", int, 20000, "The server port"),
    Argument("smac","serverMAC", str, None, "The server MAC address"),
    Argument("rst","resetMechanism", int, 0, "0 selects reset by sending a valid RST and not changing the port,"
                                               "1 selects reset by changing the port (+1)"
                                                "2 selects a hybrid, where a valid RST is sent and the port is changed")]

runnerArguments = [
    Argument("tf","traceFile", str, None,"file containing a sequence of abstract inputs. The trace runner"
                                               "reads an abstract input from every line"),
    Argument("r","runNum", int, 0,"The number of times the trace is run"),
    Argument("s","skipNum", int, 0,"The number of lines skipped after each abstract input is read"),
    Argument("j","jvmPath", str, "/usr/lib/jvm/java-7-openjdk-i386/jre/lib/amd64/server/libjvm.so" ,"Path to libjvm.so")]
