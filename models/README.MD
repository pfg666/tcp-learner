This folder contains all experiments done. For each operating system, there is a 
folder containing two sub-folders, one for the client, another for the server.
These sub-folders represent the output of the corresponding experiment. 

Each sub-folder is named after the convention:
&lt;operating system&gt;&#x96;&lt;role&gt;&#x96;&lt;time of experiment&gt;&#x96;&lt;alphabet&gt;&#x96;&lt;random queries&gt;[&#x96;&lt;k confidence&gt;]

Sub-folders contain experimental data. The files of importance are:
 - learnresult.dot : the model learned (in dot format)
 - tmp-learnedresult&lt;num&gt;.dot :  the hypotheses learned 
 - statistics.txt : statistics gathered after running the experiment
 - input : a folder containing the input used for the experiment