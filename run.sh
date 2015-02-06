# setting the required routing rules
sudo iptables --flush
sudo iptables -A OUTPUT -p tcp --tcp-flags RST RST -j DROP
# running the server, python adapter and finally the learner
sleep 1;sudo python Adapter/main.py &
java -cp "TCPServer/bin"  Main 20000 "localhost" &
echo "waiting for adapter and server to run"; sleep 2;java -cp Learner/lib/*:Learner/dist/TCPLearner.jar learner.Main input/config.yaml;gnome-open output/learnresult.pdf
# killing the server
killall java



