# s7p

a toy SSP. Sexp version of s6p

## Usage
S7p is a Clojure project so [leiningen](http://leiningen.org/) is required.

S7p uses `/opt/s7p` as working and log file dir. You need to create it before you can run s7p.

```
$ sudo mkdir /opt/s7p
$ sudo chown $(whoami) /opt/s7p
```

### Stand alone mode
simply run:

```
$ lein run ./request-data.csv
```

or, if you want to make jar,

```
$ lein uberjar
$ java -jar ./target/s7p-0.0.1-standalone.jar ./simulation_data.csv
```

then access localhost:8080 and manage DSPs, start/stop requesting with Web UI

### Master - Slave mode
To start master:


```
lein run -m s7p.master.web -- ./simulation_data.csv
```

and to start slave(s):

```
lein run -m s7p.slave.main -- tcp://master.host:5558 tcp://master.host:5557
```

then access port 8080 at master host.

or to run built jar:

```
java -cp ./target/s7p-0.0.1-standalone.jar s7p.master.web ./simulation_data.csv
java -cp ./target/s7p-0.0.1-standalone.jar s7p.slave.main tcp://master.host:5558 tcp://master.host:5557
```

# App Organization
The master and slaves are connected with 2 channels based on ZeroMQ. One channel is request data channel (push-pull type)
and the other is controll channel (publish-subscribe type).
When you add a DSP from Web UI, master publishes to all the slaves via the controll
channel that "register a new DSP to the DSP list" and similary in remove. When you start
requesting, the master reads request data from the given csv file and enqueue them
into request channel and one of the slaves dequeue it, then request the registered DSPs.
Because master - slave communication is mono-directional, you can add or restart any
number of slaves before and after the master starts up but data syncing (the 'sync' button)
is needed to sync the DSP lists.

![app organization](images/s7p.png)

# Note

Currently, master pushes request data in file only once. As `simulation_data.csv` contains only
10,000 request data, master will stop after it pushes 10,000 requests to ZeroMQ.
If you want to start requesting again, you need to restart the master.
