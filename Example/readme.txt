config.cfg is supplied to the Adapter, config.yaml to the Learner.  
You have to adjust config.cfg to your 

In order to run using these setup files, from the project directory run:
sudo python Adapter/main.py -c -cfile Example/config.cfg -csec "tcp"
Within eclipse, run the Learner using:
Example/config.yaml 
as parameter
