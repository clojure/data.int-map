(defproject org.clojure/data.int-map "0.1.1-SNAPSHOT"
  :description "Set and map data structures optimized for integer keys and elements."
  :url "https://github.com/clojure/data.int-map"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :source-paths ["src/main/clojure"]
  :test-paths ["src/test/clojure"]
  :dependencies []
  :profiles {:dev {:dependencies [[org.clojure/clojure "1.7.0-alpha5"]
                                  [collection-check "0.1.5-SNAPSHOT"]
                                  [criterium "0.4.3"]]}}
  :test-selectors {:default (complement :benchmark)
                   :benchmark :benchmark}
  :java-source-paths ["src/main/java"]
  :jvm-opts ^:replace ["-server" "-Xmx1g"]
  :global-vars {*warn-on-reflection* true})
