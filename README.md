# s7p

a toy ssp. Sexp version of s6p

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
$ java -cp ./target/s7p-0.0.1-standalone.jar ./request-data.csv
```

then access localhost:8080 and manage DSPs with Web UI

### Master - Slave mode
To start master:


```
lein run -m s7p.master.web -- ./request-data.csv
```

and to start slave(s):

```
lein run -m s7p.slave.main -- tcp://master.host:5558 tcp://master.host:5557
```

or to run built jar:

```
java -cp ./target/s7p-0.0.1-standalone.jar s7p.master.web ./request-data.csv
java -cp ./target/s7p-0.0.1-standalone.jar s7p.slave.main tcp://master.host:5558 tcp://master.host:5557
```
