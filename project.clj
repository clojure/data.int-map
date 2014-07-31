(defproject org.clojure/data.int-map "0.1.0-SNAPSHOT"
  :description "Set and map data structures optimized for integer keys and elements."
  :url "https://github.com/clojure/data.int-map"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :source-paths ["src/main/clojure"]
  :test-paths ["src/test/clojure"]
  :dependencies []
  :profiles {:dev {:dependencies [[org.clojure/clojure "1.6.0"]
                                  [collection-check "0.1.3"]
                                  [primitive-math "0.1.3"]
                                  [criterium "0.4.3"]]}}
  :test-selectors {:default (complement :benchmark)
                   :benchmark :benchmark}
  :jvm-opts ^:replace ["-server"])
