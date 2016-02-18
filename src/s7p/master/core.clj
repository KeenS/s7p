(ns s7p.master.core
  (:require
   [clojure.core.async :refer [go go-loop timeout close! chan >! <! <!!]]
   [cheshire.core :as json]
   [zeromq.zmq :as zmq]
   [s7p.config :as config]))


(def qp100ms 100)

(defn timer [ms]
  (let [c (chan)]
    (go-loop [n 0]
      (>! c n)
      (<! (timeout ms))
      (recur (+ n 1)))
    c))

(defn enqueue-query-timer [sender reqs]
  (let [t (timer 100)]
   (go-loop [reqs reqs]
     (println (<! t))
     (doall
      (doseq [req (take qp100ms reqs)]
        (zmq/send-str sender (json/generate-string req))))
     (recur (drop qp100ms reqs)))))

(defn create-dsp [pub dsp]
  (zmq/send-str pub (json/generate-string {:command "create-dsp" :data dsp})))

(defn remove-dsp [pub id]
  (zmq/send-str pub (json/generate-string {:command "remove-dsp" :data id})))
