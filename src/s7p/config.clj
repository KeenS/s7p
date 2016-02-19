(ns s7p.config)

(def dsps (atom []))

(def advertisers [{:id "1" :CPC 200 :budget 10000000}
                  {:id "2" :CPC 133 :budget 6000000}
                  {:id "3" :CPC 100 :budget 6000000}
                  {:id "4" :CPC 80 :budget 4000000}
                  {:id "5" :CPC 67 :budget 4000000}
                  {:id "6" :CPC 57 :budget 4000000}
                  {:id "7" :CPC 50 :budget 2000000}
                  {:id "8" :CPC 44 :budget 2000000}
                  {:id "9" :CPC 40 :budget 1000000}
                  {:id "10" :CPC 36 :budget 1000000}])

(def req-addr "tcp://*:5557")
(def command-addr "tcp://*:5558")
