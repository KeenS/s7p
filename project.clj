(defproject s7p "0.0.1"
  :description "a SSP used at intern"
  :url "http://github.com/KeenS/s7p"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [cheshire "5.5.0"]
                 [org.clojure/data.csv "0.1.3"]
                 [org.clojure/core.async "0.2.374"]
                 [http-kit "2.1.18"]
                 [org.zeromq/jeromq "0.3.5"]
                 [org.zeromq/cljzmq "0.1.4" :exclusions [org.zeromq/jzmq]]]
  :jvm-opts ["-Dclojure.compiler.direct-linking=true" "-Djava.library.path=/usr/local/Cellar/zeromq/4.1.4/lib/:/usr/lib:/usr/local/lib"]
  :aot [s7p.master.main s7p.slave.main]
  )
