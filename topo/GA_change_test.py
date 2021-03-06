#!/usr/bin/python

from mininet.log import setLogLevel, info
from mn_wifi.net import Mininet_wifi
from mn_wifi.node import Station, OVSKernelAP
from mn_wifi.cli import CLI
from mn_wifi.link import wmediumd
from mn_wifi.wmediumdConnector import interference
from subprocess import call
from mininet.term import makeTerms

import threading
import inspect
import ctypes
import time

"""
              sta3
            /      \    
sta1---sta2---sta4---sta6--sta7
            \      /
              sta5


"""

def myNetwork():

    net = Mininet_wifi(topo=None,
                       build=False,
                       link=wmediumd,
                       wmediumd_mode=interference,
                       ipBase='10.0.0.0/24')

    info( '*** Adding controller\n' )
    info( '*** Add switches/APs\n')

    info( '*** Add hosts/stations\n')
    sta1 = net.addStation('sta1', ip='10.0.0.1/24',
                           position='100,200,0', range = 1)
    sta2 = net.addStation('sta2', ip='10.0.0.2/24',
                           position='200,200,0', range = 1)
    sta3 = net.addStation('sta3', ip='10.0.0.3/24',
                           position='300,300,0', range = 1)
    sta4 = net.addStation('sta4', ip='10.0.0.4/24',
                           position='300,200,0', range = 1)
    sta5 = net.addStation('sta5', ip='10.0.0.5/24',
                           position='300,100,0', range = 1)
    sta6 = net.addStation('sta6', ip='10.0.0.6/24',
                           position='400,250,0', range = 1)
    sta7 = net.addStation('sta7', ip='10.0.0.7/24',
                           position='400,150,0', range = 1)

    info("*** Configuring Propagation Model\n")
    net.setPropagationModel(model="logDistance", exp=3)

    info("*** Configuring wifi nodes\n")
    net.configureWifiNodes()

    info( '*** Add links\n')
    net.addLink(sta1, sta2)

    net.addLink(sta2, sta3)
    net.addLink(sta2, sta4)
    net.addLink(sta2, sta5)

    net.addLink(sta3, sta6)
    net.addLink(sta3, sta7)

    net.addLink(sta4, sta6)
    net.addLink(sta4, sta7)

    net.addLink(sta5, sta6)
    net.addLink(sta5, sta7)

    # net.addLink(sta3, sta6,bw=2)
    # net.addLink(sta3, sta7,bw=2)

    # net.addLink(sta4, sta6,bw=5)
    # net.addLink(sta4, sta7,bw=5)

    # net.addLink(sta5, sta6,bw=10)
    # net.addLink(sta5, sta7,bw=10)

    net.addLink(sta6, sta7)

    # net.plotGraph(max_x=1000, max_y=1000)

    info( '*** Starting network\n')
    net.build()
    info( '*** Starting controllers\n')
    for controller in net.controllers:
        controller.start()

    info( '*** Starting switches/APs\n')

    info( '*** Post configure nodes\n')

    sta1.cmd("ifconfig sta1-eth1 10.0.12.1/24")

    sta2.cmd("ifconfig sta2-eth1 10.0.12.2/24")
    sta2.cmd("ifconfig sta2-eth2 10.0.23.2/24")
    sta2.cmd("ifconfig sta2-eth3 10.0.24.2/24")
    sta2.cmd("ifconfig sta2-eth4 10.0.25.2/24")

    sta3.cmd("ifconfig sta3-eth1 10.0.23.3/24")
    sta3.cmd("ifconfig sta3-eth2 10.0.36.3/24")
    sta3.cmd("ifconfig sta3-eth3 10.0.37.3/24")
    
    sta4.cmd("ifconfig sta4-eth1 10.0.24.4/24")
    sta4.cmd("ifconfig sta4-eth2 10.0.46.4/24")
    sta4.cmd("ifconfig sta4-eth3 10.0.47.4/24")

    sta5.cmd("ifconfig sta5-eth1 10.0.25.5/24")
    sta5.cmd("ifconfig sta5-eth2 10.0.56.5/24")
    sta5.cmd("ifconfig sta5-eth3 10.0.57.5/24")

    sta6.cmd("ifconfig sta6-eth1 10.0.36.6/24")
    sta6.cmd("ifconfig sta6-eth2 10.0.46.6/24")
    sta6.cmd("ifconfig sta6-eth3 10.0.56.6/24")
    sta6.cmd("ifconfig sta6-eth4 10.0.67.6/24")

    sta7.cmd("ifconfig sta7-eth1 10.0.37.7/24")
    sta7.cmd("ifconfig sta7-eth2 10.0.47.7/24")
    sta7.cmd("ifconfig sta7-eth3 10.0.57.7/24")
    sta7.cmd("ifconfig sta7-eth4 10.0.67.7/24")

    


    CLI.do_execute = execute
    CLI.do_openXterm = openXterm
    # CLI.do_stopThread = stopThread
    CLI.do_stopTest = stopTest
    CLI(net)
    net.stop()

Threads = []
activeQueue = [2,3,4,5,6,7]
def execute(self, line):
    for i in range(0,len(activeQueue)):
        info("i = " + str(i) + ", node = " + str(activeQueue[i]) +"\n")
        node = self.mn['sta'+str(activeQueue[i])]
        node.cmd("cd ramp/" + str(activeQueue[i]) + "-node/")
        
        Threads.append(MyThread(node))
        Threads[i].start()
        time.sleep(3)

def stopThread(self, line):
    for t in Threads:
        stop_thread(t)

def stop_thread(thread):
    _async_raise(thread.ident, SystemExit)

def _async_raise(tid, exctype):
    """raises the exception, performs cleanup if needed"""
    tid = ctypes.c_long(tid)
    if not inspect.isclass(exctype):
        exctype = type(exctype)
    res = ctypes.pythonapi.PyThreadState_SetAsyncExc(tid, ctypes.py_object(exctype))
    if res == 0:
        raise ValueError("invalid thread id")
    elif res != 1:
        # """if it returns a number greater than one, you're in trouble,
        # and you should call it again with exc=NULL to revert the effect"""
        ctypes.pythonapi.PyThreadState_SetAsyncExc(tid, None)
        raise SystemError("PyThreadState_SetAsyncExc failed")

class MyThread(threading.Thread):
    def __init__(self, node):
        threading.Thread.__init__(self)
        self.node = node
    def run(self):
        self.node.cmd("sh start.sh")

# send ctrl+c to all node's cmd
def stopTest(self, line):
    for t in Threads:
        stop_thread(t)
    for node in self.mn.values():
        node.sendInt()

def openXterm(self, line):
    term = 'xterm'
    args = line.split()
    if not args:
        error( 'usage: openXterm m n ...\n' )
    else:
        for i in range(int(args[0]),int(args[1])+1):
            arg = 'sta' + str(i)
            if arg not in self.mn:
                error( "node '%s' not in network\n" % arg )
            else:
                node = self.mn[ arg ]
                self.mn.terms += makeTerms( [ node ], term = term )


if __name__ == '__main__':
    setLogLevel( 'info' )
    myNetwork()

