(ns s7p.master.core
  (:require
   [clojure.core.async :refer [go go-loop timeout close! chan >! <! <!!]]
   [cheshire.core :as json]
   [zeromq.zmq :as zmq]
   [s7p.config :refer [dsps]]))


(def qp100ms (atom 100))

(defn timer [ms]
  (let [c (chan)]
    (go-loop [n 0]
      (<! (timeout ms))
      (if (>! c n)
       (recur (+ n 1))))
    c))

(defn start-query [sender reqs]
  (let [timer (timer 100)]
    (go-loop [reqs reqs]
      (let [t (<! timer)]
        (println t)
        (doall
         (doseq [req (take @qp100ms reqs)]
           (zmq/send-str sender (json/generate-string req))))
        (if t
          (recur (drop qp100ms reqs)))))
    timer))

(defn stop-query [queries]
  (close! queries))

(defn create-dsp [pub dsp]
  (swap! dsps conj dsp)
  (zmq/send-str pub (json/generate-string {:command "create-dsp" :data dsp})))

(defn remove-dsp [pub id]
  (swap! dsps (fn [dsps] (remove #(= (:id %) id) dsps)))
  (zmq/send-str pub (json/generate-string {:command "remove-dsp" :data id})))

(defn change-qps [qps]
  (reset! qp100ms (/ qps 10)))
