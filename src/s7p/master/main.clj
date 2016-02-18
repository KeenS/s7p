(ns s7p.master.main
  (:gen-class)
  (:require
   [clojure.data.csv :as csv]
   [clojure.java.io :as io]
   [clojure.core.async :refer [go-loop timeout close! chan >! <! <!!]]
   [cheshire.core :as json]
   [zeromq.zmq :as zmq]
   [s7p.config :as config]
   [s7p.master.core :as core]))


(defn to-req [line]
  (let [fp (line 1)]
   {:req
    {:site       (line 0)
     :floorPrice (if (= "NA" fp) nil (Integer. fp))
     :device     (line 2)
     :user       (line 3)
     ;; testing
     :test       0}
    :result      (mapv #(Integer. %) (subvec line 4))}))


;; (defn -main [& args]
;;   (let [context (zmq/zcontext 1)]
;;     (with-open [in-file (io/reader (first args))
;;                 pub (doto (zmq/socket context :pub)
;;                       (zmq/bind config/command-addr))
;;                 sender (doto (zmq/socket context :push)
;;                          (zmq/bind config/req-addr))]
;;       (let [reqs (map to-req (drop 1 (csv/read-csv in-file)))]
;;         (let [t (core/enqueue-query-timer sender reqs)]
;;           ;; (core/create-dsp pub (json/generate-string {:id "1" :url config/someurl}))
;;           ;; (core/remove-dsp pub (json/generate-string {:id "1"}))
;;           (<!! t)
;;           )))))
