(ns s7p.config)

(def someurl "")

(def dsps (atom
           [{:id "1"
             :url someurl
             :winnotice someurl}
            {:id "2"
             :url someurl
             :winnotice someurl}
            {:id "3"
             :url someurl
             :winnotice someurl}
            {:id "4"
             :url someurl
             :winnotice someurl}
            {:id "5"
             :url someurl
             :winnotice someurl}
            {:id "6"
             :url someurl
             :winnotice someurl}
            {:id "7"
             :url someurl
             :winnotice someurl}
            ]))

(def advertisers [{:id "1"} {:id "2"} {:id "3"} {:id "4"}])

(def req-addr "tcp://*:5557")
(def command-addr "tcp://*:5558")
