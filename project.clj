(defproject subjson "0.1.0"
  :description "A simple JSON parser, easily bound to JVM languages."
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.4.0"]]

  :source-paths ["src" "src/clojure"]
  :java-source-paths ["src/java"]

  :profiles {:test {:resource-paths ["resources" "test/resources"]}}
  )
