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

def combineGames (brokerData):
    # entries are games, containing [[broker-name, score], ...]
    # track stats separately by game size
    # all the games of a particular size i are a 'competition'
    # competition record is [n, s, pi, {broker: [m, m^2, cnt, z], ...} 

    zscores = dict()
    for bd in brokerData:
        size = len(bd)
        if not size in zscores:
            zscores[size] = [1, 0.0, 0.0, dict()]
            for brokerRecord in bd:
                score = brokerRecord[1]
                zscores[size][3][brokerRecord[0]] = [score, score * score, 1]
        else:
            #print zscores
            szRecord = zscores[size]
            szRecord[0] += 1 # one more game of this size
            for brokerRecord in bd:
                brokerName = brokerRecord[0]
                score = brokerRecord[1]
                if brokerName in szRecord[3]:
                    br = szRecord[3][brokerName]
                    br[0] += score
                    br[1] += score * score
                    br[2] += 1
                else:
                    szRecord[3][brokerName] = [score, score * score, 1]
    return zscores


def computeZScores (data):
    # takes data from combineGames, computes z scores
    # first, compute pi and s values for each competition
    for key in data:
        record = data[key]
        count = 0
        sum = 0.0
        sumsq = 0.0
        for broker in record[3]:
            brokerRecord = record[3][broker]
            sum += brokerRecord[0]
            sumsq += brokerRecord[1]
            count += brokerRecord[2]
        record[2] = sum / count
        record[1] = sqrt(sumsq / count)
        for broker in record[3]:
            brokerRecord = record[3][broker]
            pi = brokerRecord[0] / brokerRecord[2]
            z = (pi - record[2]) / record[1]
            print broker, key, z
            #brokerRecord.append()


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

result = combineGames([[["a", 1.2], ["b", 2.1]],
                       [["b", 3.2], ["c", 2.2]],
                       [["a", 1.3], ["b", 2.2], ["c", 1.0]],
                       [["a", 1.6], ["c", 1.1]],
                       [["a", 1.3], ["b", 2.2], ["c", 1.0]]])

result = computeZScores(result)
print result
