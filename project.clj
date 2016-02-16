(defproject s7p "0.0.1"
  :description "a SSP used at intern"
  :url "http://github.com/KeenS/s7p"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/data.json "0.2.6"]
                 [org.clojure/core.async "0.2.374"]
                 [http-kit "2.1.18"]]
  :main s7p.core)
