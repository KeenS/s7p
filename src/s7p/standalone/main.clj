(ns s7p.standalone.main
  (:gen-class)
  (:require
   [s7p.slave.main :as slave]
   [s7p.master.web :as master]))

(defn -main [& args]
  (apply master/-main args)
  (slave/-main "tcp://127.0.0.1:5557" "tcp://127.0.0.1:5558"))
