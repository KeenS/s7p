(ns s7p.core
  (:require [clojure.data.json :as json]
            [org.httpkit.client :as http]))



(def options {:timeout 100             ; ms
              :body (json/write-str {:b {:c 5} :e {:f 6}})
              :keepalive 3000})

(defn -main []
 (let [urls ["http://dsp-no-bid.compe"]
       ;; send the request concurrently (asynchronously)
       futures (doall (map (fn [url] (http/post url options)) urls))]
   (with-async-results [resps futures]
     (-> resps
         (pick-winner)
         (winnotice)))))
