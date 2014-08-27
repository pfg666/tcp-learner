__author__ = 'paul'
TCP_IP = '131.174.142.208'
TCP_PORT = 7992
import socket
if __name__ == "__main__":
    s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    s.connect((TCP_IP, TCP_PORT))
    s.close()