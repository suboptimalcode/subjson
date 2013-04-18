(ns subjson.test.subjson-clojure
  (:use clojure.test)
  (:require [su.boptim.al.subjson :as subjson]
            [clojure.string :as str]
            [clojure.java.io :as io])
  (:import [su.boptim.al.subjson SubJson]
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
            json-val (subjson/read-string json-src)]
        (is (= json-val
               (let [sw (StringWriter.)]
                 (subjson/write sw json-val pretty-print?)
                 (subjson/read-string (.toString sw))))))))

;; Check the pretty printing by reading examples that are pretty-printed
;; the way we like and trying to print them out to get identical results.
;; Maps present an obvious problem, so restrict ourselves to single-key maps.
(def prettyprinting_examples ["pretty_printed1" "pretty_printed2"
                              "pretty_printed3" "pretty_printed4"
                              "pretty_printed5"])

(deftest prettyprinted_examples-test
  (doseq [example-name prettyprinting_examples]
    (let [json-src (-> (str "prettyprinting_examples/" example-name ".json")
                       io/resource slurp str/trim)
          json-val (subjson/read-string json-src)]
      (is (= json-src
             (let [sw (StringWriter.)]
               (subjson/write sw json-val true)
               (.toString sw))))
      (is (= json-src
             (subjson/write-string json-val true))))))

;; Similarly for compact-printed examples.
(def compactprinting_examples ["compact_printed1" "compact_printed2"
                               "compact_printed3" "compact_printed4"
                               "compact_printed5"])

(deftest compactprinted_examples-test
  (doseq [example-name compactprinting_examples]
      (let [json-src (-> (str "compactprinting_examples/" example-name ".json")
                         io/resource slurp str/trim)
            json-val (subjson/read-string json-src)]
        (is (= json-src
               (let [sw (StringWriter.)]
                 (subjson/write sw json-val false)
                 (.toString sw))))
        (is (= json-src
               (subjson/write-string json-val false))))))
