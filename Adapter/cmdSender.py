
import socket

class CmdAdapter:
    cmdSocket = None
    sender = None
    def __init__(self, cmdIp = "192.168.56.1", cmdPort=5000):
        self.cmdPort = cmdPort
        self.cmdIp = cmdIp
        
    # returns a new socket to the mapper/learner
    def setUpSocket(self):
        self.cmdSocket = socket.create_connection((self.cmdIp, self.cmdPort))
        self.cmdSocket.setsockopt(socket.IPPROTO_TCP, socket.TCP_NODELAY, 1)
        print "python connected to server Adapter at " + cmdIp + " " + (str(cmdPort))
        self.listenForServerPort()
        
    
    def closeSockets(self):
        try:
            print(self.cmdSocket)
            if self.cmdSocket is not None:
                print "Closing gateway server command socket"
                self.cmdSocket.send("exit\n")
                self.cmdSocket.close()
        except IOError:
            print "Error closing CmdAdapter " + IOError.message
    
    # fetches a server port
    def listenForServerPort(self):
        newPortFound = False
        newPortString = ""
        while True:
            newPortString = self.cmdSocket.recv(1024)
            for word in newPortString.split(): # TODO check if this really always works with a stream
                print "recvd " + word
                if newPortFound:
                    self.serverPort = int(word)
                    print "next server port: " + word
                    return
                if word == "port":
                    newPortFound = True
    
    def handleInput(self):
        self.sender = sender
        while (True):
            input1 = self.receiveInput()
            seqNr = 0
            ackNr = 0
            if input1 == "reset":
                self.sendReset()
            elif input1 == "exit":
                self.closeSockets()
                return
            else:
                print "*****"
                if input1 in ["listen", "accept", "closeconnection", "closeserver"]:
                    print(" " + input1)
                    self.cmdSocket.send(input1 + "\n") # TODO race-condition here, might go wrong: 
                    response = sender.captureResponse() # response might arrive before sender is ready
                    
    def startAdapter(self, sender):
        self.setUpSocket(self.cmdIp, self.cmdPort)
    


    