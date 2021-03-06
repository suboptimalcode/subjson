(ns subjson.benchmarks.core
  (:use perforate.core)
  (:require [clojure.java.io :as io]
            [cheshire.core :as cheshire]
            [clojure.data.json :as cdjson]
            [su.boptim.al.subjson :as subjson-clj])
  (:import [su.boptim.al.subjson SubJson UnsynchronizedStringReader]
           [com.fasterxml.jackson.databind ObjectMapper]))

(set! *warn-on-reflection* true)

(def reps 100)

(defgoal small-parse-test "JSON Parse Speed - Small Object")

(defcase* small-parse-test :subjson
  (fn []
    [(let [json-src (-> "jsonorg_examples/menu.json"
                        io/resource slurp)]
       (fn [] (dotimes [_ reps]
                (SubJson/read (UnsynchronizedStringReader. json-src)))))]))

(defcase* small-parse-test :subjson-clojure
  (fn []
    (let [json-src (-> "jsonorg_examples/menu.json"
                       io/resource slurp)]
      [(fn [] (dotimes [_ reps]
                (subjson-clj/read-string json-src)))])))

(defcase* small-parse-test :data.json
  (fn []
    (let [json-src (-> "jsonorg_examples/menu.json"
                       io/resource slurp)]
      [(fn [] (dotimes [_ reps]
                (cdjson/read-str json-src)))])))

(defcase* small-parse-test :cheshire
  (fn []
    (let [json-src (-> "jsonorg_examples/menu.json"
                       io/resource slurp)]
      [(fn [] (dotimes [_ reps]
                (cheshire/parse-string json-src)))])))

(defcase* small-parse-test :jackson-tree
  (fn []
    (let [json-src (-> "jsonorg_examples/menu.json"
                       io/resource slurp)
          mapper (ObjectMapper.)]
      [(fn [] (dotimes [_ reps]
                (.readTree mapper ^String json-src)))])))

(defcase* small-parse-test :jackson-mapper
  (fn []
    (let [json-src (-> "jsonorg_examples/menu.json"
                       io/resource slurp)
          mapper (ObjectMapper.)]
      [(fn [] (dotimes [_ reps]
                (.readValue mapper ^String json-src Object)))])))



(defgoal large-parse-test "JSON Parse Speed - Large Object")

(defcase* large-parse-test :subjson
  (fn []
    [(let [json-src (-> "jsonorg_examples/web-app.json"
                        io/resource slurp)]
       (fn [] (dotimes [_ reps]
                (SubJson/read (UnsynchronizedStringReader. json-src)))))]))

(defcase* large-parse-test :subjson-clojure
  (fn []
    [(let [json-src (-> "jsonorg_examples/web-app.json"
                        io/resource slurp)]
       (fn [] (dotimes [_ reps]
                (subjson-clj/read-string json-src))))]))

(defcase* large-parse-test :data.json
  (fn []
    (let [json-src ^String (-> "jsonorg_examples/web-app.json"
                               io/resource slurp)]
      [(fn [] (dotimes [_ reps]
                (cdjson/read-str json-src)))])))

(defcase* large-parse-test :cheshire
  (fn []
    (let [json-src ^String (-> "jsonorg_examples/web-app.json"
                               io/resource slurp)]
      [(fn [] (dotimes [_ reps]
                (cheshire/parse-string json-src)))])))

(defcase* large-parse-test :jackson-tree
  (fn []
    (let [json-src (-> "jsonorg_examples/web-app.json"
                       io/resource slurp)
          mapper (ObjectMapper.)]
      [(fn [] (dotimes [_ reps]
                (.readTree mapper ^String json-src)))])))

(defcase* large-parse-test :jackson-mapper
  (fn []
    (let [json-src (-> "jsonorg_examples/web-app.json"
                       io/resource slurp)
          mapper (ObjectMapper.)]
      [(fn [] (dotimes [_ reps]
                (.readValue mapper ^String json-src Object)))])))