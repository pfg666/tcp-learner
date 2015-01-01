#from socketAdapter import SocketAdapter
import socket

# extends sender functionality with higher level commands
class ActionSender:
    cmdSocket = None
    sender = None
    actions = ["listen", "accept", "closeconnection", "closeserver"]
    def __init__(self, cmdIp = "192.168.56.1", cmdPort=5000, sender = None):
        self.cmdPort = cmdPort
        self.cmdIp = cmdIp
        self.sender = sender
        
    def __str__(self):
        ret =  "ActionSender with parameters: " + str(self.__dict__)
        if self.sender is not None:
            ret  = ret + "\n" + str(self.sender)
        return ret
        
    # returns a new socket to the mapper/learner
    def setUpSocket(self):
        if self.cmdSocket is None:
            cmdSocket = socket.create_connection((self.cmdIp, self.cmdPort))
            cmdSocket.setsockopt(socket.IPPROTO_TCP, socket.TCP_NODELAY, 1)
            cmdSocket.settimeout(2)
            print "python connected to server Adapter at " + self.cmdIp + " " + (str(self.cmdPort))
            #self.cmdSocket = SocketAdapter(cmdSocket)
            self.cmdSocket = cmdSocket
        self.listenForServerPort()
        
    
    def closeSockets(self):
        try:
            print(self.cmdSocket)
            if self.cmdSocket is not None:
                print "Closing gateway server command socket"
                self.cmdSocket.send("exit\n")
                self.cmdSocket.close()
        except IOError:
            print "Error closing ActionSender " + IOError.message
    
    # fetches a server port
    def listenForServerPort(self):
        newPortFound = False
        newPortString = ""
        while True:
            newPortString = self.cmdSocket.recv(1024)
            print "received " + newPortString
            for word in newPortString.split(): # TODO check if this really always works with a stream
                if newPortFound:
                    self.serverPort = int(word)
                    print "next server port: " + word
                    return
                if word == "port":
                    newPortFound = True
                    
    def sendReset(self):
        if self.cmdSocket is None:
            self.setUpSocket()
        print "reset"
        print "********** reset **********"
        self.cmdSocket.send("reset\n")
        self.listenForServerPort()
        self.sender.setServerPort(self.serverPort)
    
    def isAction(self, inputString):
        return inputString in self.actions
    
    def sendAction(self, inputString):
        if self.isAction(inputString):
            self.cmdSocket.send(inputString + "\n") # TODO race-condition here, might go wrong: 
            response = self.sender.captureResponse() # response might arrive before sender is ready
        else:
            print inputString + " not a valid action ( it is not one of: " + str(self.actions) + ")"
        return response
                
    def sendInput(self, input1, seqNr, ackNr):
        return self.sender.sendInput(input1, seqNr, ackNr)
    


    