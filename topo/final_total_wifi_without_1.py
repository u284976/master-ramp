#!/usr/bin/python

from mininet.log import setLogLevel, info, error
from mn_wifi.net import Mininet_wifi
from mn_wifi.node import Station, OVSKernelAP
from mn_wifi.cli import CLI
from mn_wifi.link import wmediumd, adhoc
from mn_wifi.wmediumdConnector import interference
from subprocess import call

from mininet.term import makeTerms

import threading
import inspect
import ctypes
import time


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
                           position='300.0,300.0,0',range=1)
    sta2 = net.addStation('sta2', ip='10.0.0.2/24',
                           position='100.0,100.0,0')
    sta3 = net.addStation('sta3', ip='10.0.0.3/24',
                           position='100.0,500.0,0')
    sta4 = net.addStation('sta4', ip='10.0.0.4/24',
                           position='400.0,100.0,0')
    sta5 = net.addStation('sta5', ip='10.0.0.5/24',
                           position='5.0,300.0,0')
    sta6 = net.addStation('sta6', ip='10.0.0.6/24',
                           position='270.0,600.0,0')
    sta7 = net.addStation('sta7', ip='10.0.0.7/24',
                           position='450.0,500.0,0')
    sta8 = net.addStation('sta8', ip='10.0.0.8/24',
                           position='562.0,300.0,0')
    sta9 = net.addStation('sta9', ip='10.0.0.9/24',
                           position='750.0,300.0,0')
    sta10 = net.addStation('sta10', ip='10.0.0.10/24',
                           position='900.0,400.0,0')
    sta11 = net.addStation('sta11', ip='10.0.0.11/24',
                           position='900.0,200.0,0')

    info("*** Configuring Propagation Model\n")
    net.setPropagationModel(model="logDistance", exp=3)

    info("*** Configuring wifi nodes\n")
    net.configureWifiNodes()

    info( '*** Add links\n')

    net.addLink(sta1, sta2)
    net.addLink(sta1, sta3)
    net.addLink(sta1, sta4)
    net.addLink(sta1, sta5)
    net.addLink(sta1, sta8)

    net.addLink(sta2, cls=adhoc, intf='sta2-wlan0',
                ssid='adhocNet', mode='g', channel=5)
    net.addLink(sta3, cls=adhoc, intf='sta3-wlan0',
                ssid='adhocNet', mode='g', channel=5)
    net.addLink(sta4, cls=adhoc, intf='sta4-wlan0',
                ssid='adhocNet', mode='g', channel=5)
    net.addLink(sta5, cls=adhoc, intf='sta5-wlan0',
                ssid='adhocNet', mode='g', channel=5)
    net.addLink(sta6, cls=adhoc, intf='sta6-wlan0',
                ssid='adhocNet', mode='g', channel=5)
    net.addLink(sta7, cls=adhoc, intf='sta7-wlan0',
                ssid='adhocNet', mode='g', channel=5)
    net.addLink(sta8, cls=adhoc, intf='sta8-wlan0',
                ssid='adhocNet', mode='g', channel=5)
    net.addLink(sta9, cls=adhoc, intf='sta9-wlan0',
                ssid='adhocNet', mode='g', channel=5)
    net.addLink(sta10, cls=adhoc, intf='sta10-wlan0',
                ssid='adhocNet', mode='g', channel=5)
    net.addLink(sta11, cls=adhoc, intf='sta11-wlan0',
                ssid='adhocNet', mode='g', channel=5)

    net.plotGraph(max_x=1000, max_y=1000)

    info( '*** Starting network\n')
    net.build()
    info( '*** Starting controllers\n')
    for controller in net.controllers:
        controller.start()

    info( '*** Starting switches/APs\n')

    info( '*** Post configure nodes\n')

    sta1.cmd("ifconfig sta1-wlan0 0")
    sta1.cmd("ifconfig sta1-eth1 10.0.12.1/24")
    sta1.cmd("ifconfig sta1-eth2 10.0.13.1/24")
    sta1.cmd("ifconfig sta1-eth3 10.0.14.1/24")
    sta1.cmd("ifconfig sta1-eth4 10.0.15.1/24")
    sta1.cmd("ifconfig sta1-eth5 10.0.18.1/24")

    sta2.cmd("ifconfig sta2-eth1 10.0.12.2/24")
    sta3.cmd("ifconfig sta3-eth1 10.0.13.3/24")
    sta4.cmd("ifconfig sta4-eth1 10.0.14.4/24")
    sta5.cmd("ifconfig sta5-eth1 10.0.15.5/24")
    sta8.cmd("ifconfig sta8-eth1 10.0.18.8/24")
    
    CLI.do_openXterm = openXterm
    CLI.do_execute = execute
    CLI.do_stopTest = stopTest
    CLI(net)
    net.stop()
    
Threads = []
activeQueue = [2,3,4,5,6,7,8,9,10,11]
def execute(self, line):
    args = line.split()
    for i in range(0,len(activeQueue)):
        info("i = " + str(i) + ", node = " + str(activeQueue[i]) +"\n")
        node = self.mn['sta'+str(activeQueue[i])]
        node.cmd("cd ramp/" + str(activeQueue[i]) + "-node/")
        
        Threads.append(MyThread(node))
        Threads[i].start()
        time.sleep(1)
        # thread = MyThread(node)
        # thread.start()
    

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