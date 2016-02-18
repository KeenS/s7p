(ns s7p.slave.log.bidresponse
  (:require
   [clojure.tools.logging :as log]
   [cheshire.core :as json]))

(defn log [arg]
  (log/info (json/generate-string arg)))

