(ns subjson.test.subjson-clojure
  (:use clojure.test)
  (:require [su.boptim.al.subjson :as subjson]
            [clojure.java.io :as io])
  (:import [su.boptim.al.subjson ClojureBuildPolicy SubJson]
           [java.io StringReader StringWriter]))

;;
;; "Full" examples
;;

(def jsonorg_examples ["glossary" "menu" "widget" "web-app" "menu2"])

(deftest parse_jsonorg_examples-test
  (doseq [example-name jsonorg_examples]
    (let [json-src (-> (str "jsonorg_examples/" example-name ".json")
                       io/resource slurp)
          edn-src (-> (str "jsonorg_examples/" example-name ".edn")
                      io/resource slurp)]
      (is (= (subjson/read (StringReader. json-src))
             (read-string edn-src)))))
  ;; Test String version
  (doseq [example-name jsonorg_examples]
    (let [json-src (-> (str "jsonorg_examples/" example-name ".json")
                       io/resource slurp)
          edn-src (-> (str "jsonorg_examples/" example-name ".edn")
                      io/resource slurp)]
      (is (= (subjson/read-string json-src)
             (read-string edn-src))))))

(deftest print-jsonorg_examples-test
  ;; Test String version
  (doseq [example-name jsonorg_examples
          pretty-print? [true false]]
      (let [json-src (-> (str "jsonorg_examples/" example-name ".json")
                         io/resource slurp)
            json-val (subjson/read json-src)]
        (is (= json-val
               (let [sw (StringWriter.)]
                 (subjson/write sw json-val pretty-print?)
                 (subjson/read-string (.toString sw))))))))