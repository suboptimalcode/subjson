(defproject subjson "0.2.0"
  :description "A simple JSON parser, easily bound to JVM languages."
  :url "http://github.com/suboptimal/subjson"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.4.0"]
                 [criterium "0.3.2-SNAPSHOT"]]
  :plugins [[perforate "0.3.1"]
            [lein-javadoc "0.1.0"]]

  :source-paths ["src" "src/clojure"]
  :java-source-paths ["src/java"]
  :javadoc-opts {:package-names ["su.boptim.al.subjson"]}

  ;; Need to get Leiningen to not run with the TieredCompilation level
  ;; set to 1, which defeats many optimization and makes benchmark
  ;; results unrealistic.
  :jvm-opts ^:replace []

  :profiles {:test {:resource-paths ["resources" "test/resources"]
                    :java-source-paths ["benchmarks/subjson/java"]}
             :subjson0.1 {:dependencies [[subjson "0.1.0"]]}
             :data.json {:dependencies [[org.clojure/data.json "0.2.1"]]}
             :cheshire5.0 {:dependencies [[cheshire "5.0.2"]]}
             :jackson2.1 {:dependencies [[com.fasterxml.jackson.core/jackson-core "2.1.3"]
                                         [com.fasterxml.jackson.core/jackson-databind "2.1.3"]]}}
  :perforate {:environments [{:name :current
                              :profiles [:test :cheshire5.0 :jackson2.1
                                         :data.json]
                              :namespaces [subjson.benchmarks.core]}
                             {:name :subjson0.1
                              :profiles [:test :subjson0.1
                                         :cheshire5.0 :jackson2.1]
                              :namespaces [subjson.benchmarks.core]}
                             {:name buffers
                              :profiles [:test]
                              :namespaces [subjson.benchmarks.buffers]}
                             {:name string-copies
                              :profiles [:test]
                              :namespaces [subjson.benchmarks.string-copies]}]}
  )
