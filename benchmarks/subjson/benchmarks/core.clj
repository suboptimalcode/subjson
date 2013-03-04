(ns subjson.benchmarks.core
  (:use perforate.core)
  (:require [clojure.java.io :as io]
            [cheshire.core :as cheshire])
  (:import [su.boptim.al.subjson SubJson LightStringReader]
           [com.fasterxml.jackson.databind ObjectMapper]))

(set! *warn-on-reflection* true)

(defgoal small-parse-test "JSON Parse Speed - Small Object")

(defcase* small-parse-test :subjson
  (fn []
    (let [json-src (-> "jsonorg_examples/menu.json"
                       io/resource slurp)]
      [(fn [] (dotimes [_ 10]
                (SubJson/parse (LightStringReader. json-src))))])))

(defcase* small-parse-test :cheshire
  (fn []
    (let [json-src (-> "jsonorg_examples/menu.json"
                       io/resource slurp)]
      [(fn [] (dotimes [_ 10]
                (cheshire/parse-string json-src)))])))

(defcase* small-parse-test :jackson-tree
  (fn []
    (let [json-src (-> "jsonorg_examples/menu.json"
                       io/resource slurp)
          mapper (ObjectMapper.)]
      [(fn [] (dotimes [_ 10]
                (.readTree mapper ^String json-src)))])))


(defcase* small-parse-test :jackson-mapper
  (fn []
    (let [json-src (-> "jsonorg_examples/menu.json"
                       io/resource slurp)
          mapper (ObjectMapper.)]
      [(fn [] (dotimes [_ 10]
                (.readValue mapper ^String json-src Object)))])))

(defgoal large-parse-test "JSON Parse Speed - Large Object")

(defcase* large-parse-test :subjson
  (fn []
    (let [json-src (-> "jsonorg_examples/web-app.json"
                       io/resource slurp)]
      [(fn [] (dotimes [_ 10]
                (SubJson/parse (LightStringReader. ^String json-src))))])))

(defcase* large-parse-test :cheshire
  (fn []
    (let [json-src ^String (-> "jsonorg_examples/web-app.json"
                               io/resource slurp)]
      [(fn [] (dotimes [_ 10]
                (cheshire/parse-string json-src)))])))

(defcase* large-parse-test :jackson-tree
  (fn []
    (let [json-src (-> "jsonorg_examples/web-app.json"
                       io/resource slurp)
          mapper (ObjectMapper.)]
      [(fn [] (dotimes [_ 10]
                (.readTree mapper ^String json-src)))])))

(defcase* large-parse-test :jackson-mapper
  (fn []
    (let [json-src (-> "jsonorg_examples/web-app.json"
                       io/resource slurp)
          mapper (ObjectMapper.)]
      [(fn [] (dotimes [_ 10]
                (.readValue mapper ^String json-src Object)))])))