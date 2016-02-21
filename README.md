# s7p

a toy ssp. Sexp version of s6p

## Usage

to start master


```
lein run -m s7p.master.web -- ./request-data.csv
```

to start slave,

```
lein run -m s7p.slave.main -- tcp://master.host:5558 tcp://master.host:5557
```


then access localhost:8080 and manage DSPs with Web UI
