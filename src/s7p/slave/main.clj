(ns s7p.slave.main
  (:gen-class)
  (:require
   [clojure.core.async :refer [thread close! chan >!! <!!]]
   [clojure.tools.logging :as log]
   [cheshire.core :as json]
   [zeromq.zmq :as zmq]
   [s7p.config :as config]
   [s7p.slave.manage :as manage]
   [s7p.slave.core :as core]))


(defn make-request-handler [reciever ch]
  (thread
    (loop []
      (let [json (zmq/receive-str reciever)
            req (json/parse-string json true)]
        (log/info json)
        (>!! ch req)
        (recur)))))

(defn make-command-listener [sub]
  (thread
    (loop []
      (let [cmd (-> (zmq/receive-str sub)
                    (json/parse-string true))]
        (case (:command cmd)
          "create-dsp" (println (:data cmd))
          "remove-dsp" (println (:data cmd))))
      (recur))))


(defn -main [& args]
  (let [ch (chan)
        context (zmq/zcontext 1)
        workers (manage/make-workers ch 1024)
        [command-addr req-addr] args
        ]
    (with-open [sub (doto (zmq/socket context :sub)
                      (zmq/connect command-addr)
                      (zmq/subscribe ""))
                receiver (doto (zmq/socket context :pull)
                           (zmq/connect req-addr))]
      (let [request-handler  (make-request-handler receiver ch)
            command-listener (make-command-listener sub)]
        (<!! command-listener)
        (<!! request-handler)))))