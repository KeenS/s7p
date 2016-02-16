(ns s7p.slave
  (:gen-class)
  (:require
   [taoensso.carmine :as car :refer (wcar)]
   [taoensso.carmine.message-queue :as car-mq]
   [clojure.data.json :as json]
   [clojure.core.async :refer [<!! close! chan >!!]]
   [s7p.config :as config]
   [s7p.manage :as manage]
   [s7p.core :as core]
))


(defn make-request-handler [ch]
  (car-mq/worker config/redis config/queue-name
                 {:handler
                  (fn [{:keys [message]}]
                    (>!! ch (json/read-str message))
                    {:status :success})}))


(defn -main [& args]
  (let [ch (chan)
        request-handler (make-request-handler ch)
        workers (map (fn [_] (core/worker ch)) (range 64))
        command-listener (car/with-new-pubsub-listener (:spec config/redis)
                           {"create-dsp" (fn [[_ _ msg]] (println "create dsp: " (json/read-str msg)))
                            "delete-dsp" (fn [[_ _ msg]] (println "delete dsp: " (json/read-str msg)))
                            "stop"       (fn [[_ _ msg]] (println "stop:" msg))}
                           (car/subscribe  "create-dsp" "delete-dsp"))]
    (loop []
      (when-let [req (<!! ch)]
        (println req)
        (recur)))))
