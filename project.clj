(defproject s7p "0.0.1"
  :description "a SSP used at intern"
  :url "http://github.com/KeenS/s7p"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/data.csv "0.1.3"]
                 [org.clojure/core.async "0.2.374"]
                 [org.clojure/tools.logging "0.3.1"]
                 [org.slf4j/slf4j-api "1.7.6"]
                 [ch.qos.logback/logback-classic "1.1.5"]
                 [cheshire "5.5.0"]
                 [http-kit "2.1.18"]
                 [javax.servlet/servlet-api "2.5"]
                 [compojure "1.4.0"]
                 [hiccup "1.0.5"]
                 [org.zeromq/jeromq "0.3.5"]
                 [org.zeromq/cljzmq "0.1.4" :exclusions [org.zeromq/jzmq]]]
  :jvm-opts ["-Dclojure.compiler.direct-linking=true" "-Djava.library.path=/usr/local/Cellar/zeromq/4.1.4/lib/:/usr/lib:/usr/local/lib"]
  :aot [s7p.master.web s7p.slave.main]
  )
