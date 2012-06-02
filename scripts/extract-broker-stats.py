#!/usr/bin/python

# Scan a set of logfile bundles, extracting the final broker statistics
# from each one. Result is an entry from each file of the form
#   gameId brokername:nnn.nnn ...
# containing an entry for each broker that was supposed to log into the
# game. If a broker was supposed to log in and did not, its score will
# be represented as "xxx". If the broker data record is not in the file,
# then all broker scores will be "nnn".

import sys,re
#import argparse
import subprocess


def openTgzFile (tgzFile, fn):
    tar = subprocess.Popen(["/bin/tar", "xzOf", tgzFile, fn],
                           stdout=subprocess.PIPE)
    return tar.stdout


def extractBrokers (stream):
    authBrokersRe = re.compile(': Authorized brokers: \[')
    brokerNameRe = re.compile('([\w ]+)')
    brokerNames = []

    for line in stream:
        mq = authBrokersRe.search(line)
        if mq:
            mb = mq
            while mb:
                mb = brokerNameRe.search(line, mb.end())
                if mb:
                    brokerNames.append(mb.group(1))
    return brokerNames


def extractStats (stream):
    brokerDataRe = re.compile('Final balance \(brokername:balance\)')
    brokerBalanceRe = re.compile(' ([\w ]+):(\d+\.\d*)')
    result = []

    count = 0
    for line in stream:
        mq = brokerDataRe.search(line)
        if mq:
            count += 1
            mb = mq
            while mb:
                mb = brokerBalanceRe.search(line, mb.end())
                if mb:
                    result.append([mb.group(1), mb.group(2)])
    return result

def filterStats (stats, brokers):
    # stats is [[broker-name, amt], ...]
    # brokers is [broker-name, ...]
    # include stats with names matching brokers
    # add stats entries for missing brokers
    filteredStats = []
    for stat in stats:
        if stat[0] in brokers:
            broker = stat[0]
            filteredStats.append(stat)
            index = brokers.index(broker)
            brokers = brokers[0:index] + brokers[index+1:]
    for broker in brokers:
        filteredStats.append([broker, 0.0])
    return filteredStats

# -----------------
# top-level

print sys.argv

stream = openTgzFile("game-1-logs.tar.gz", "log/init.trace")
brokers = extractBrokers(stream)
stream.close()
#print brokers

stream = openTgzFile("game-1-logs.tar.gz", "log/powertac-sim-.trace")
stats = extractStats(stream)
stream.close()
#print stats

stats = filterStats(stats, brokers)
print stats

