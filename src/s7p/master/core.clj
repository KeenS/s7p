(ns s7p.master.core
  (:require
   [clojure.core.async :refer [go go-loop timeout close! chan >! <! <!!]]
   [clojure.java.io :as io]
   [cheshire.core :as json]
   [zeromq.zmq :as zmq]
   [s7p.config :refer [dsps dsps-file]]))


(defn load-dsps []
  (when (.exists (io/as-file dsps-file))
    (with-open [in-file (io/reader dsps-file)]
      (let [read (json/parse-stream in-file true)]
        (reset! dsps read)))))

(defn save-dsps []
  (with-open [out-file (io/writer dsps-file)]
    (json/generate-stream @dsps out-file)))

(def qp100ms (atom 200))

(defn timer [ms]
  (let [c (chan)]
    (go-loop [n 0]
      (<! (timeout ms))
      (if (>! c n)
       (recur (+ n 1))))
    c))

(defn start-query [sender reqs]
  (let [timer (timer 100)]
    (go-loop []
      (let [t (<! timer)
            took (take @qp100ms @reqs)]
        (doall
         (doseq [req took]
           (zmq/send-str sender (json/generate-string req))))
        (swap! reqs #(drop @qp100ms %))
        (if (and t (not (empty? took)))
          (recur)
          (println "request done"))))
    timer))

(defn stop-query [queries]
  (close! queries))

(defn create-dsp [pub dsp]
  (swap! dsps conj dsp)
  (zmq/send-str pub (json/generate-string {:command "create-dsp" :data dsp})))

(defn remove-dsp [pub id]
  (swap! dsps (fn [dsps] (remove #(= (:id %) id) dsps)))
  (zmq/send-str pub (json/generate-string {:command "remove-dsp" :data id})))

(defn sync-all-dsp [pub]
  (zmq/send-str pub (json/generate-string {:command "sync-dsps" :data @dsps})))

(defn change-qps [qps]
  (reset! qp100ms (int (Math/ceil (/ qps 10)))))
