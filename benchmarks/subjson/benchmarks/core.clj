(ns subjson.benchmarks.core
  (:use perforate.core)
  (:require [clojure.java.io :as io]
            [cheshire.core :as cheshire])
  (:import [su.boptim.al.subjson SubJson LightStringReader]
           [com.fasterxml.jackson.databind ObjectMapper]))

(defgoal small-parse-test "JSON Parse Speed - Small Object")

(defcase* small-parse-test :subjson
  (fn []
    (let [json-src (-> "jsonorg_examples/menu.json"
                       io/resource slurp)]
      [(fn [] (SubJson/parse (LightStringReader. json-src)))])))

(defcase* small-parse-test :cheshire
  (fn []
    (let [json-src (-> "jsonorg_examples/menu.json"
                       io/resource slurp)]
      [(fn [] (cheshire/parse-string json-src))])))

(defcase* small-parse-test :jackson
  (fn []
    (let [json-src (-> "jsonorg_examples/menu.json"
                       io/resource slurp)
          mapper (ObjectMapper.)]
      [(fn [] (.readTree mapper json-src))])))

(defgoal large-parse-test "JSON Parse Speed - Large Object")

(defcase* large-parse-test :subjson
  (fn []
    (let [json-src (-> "jsonorg_examples/web-app.json"
                       io/resource slurp)]
      [(fn [] (SubJson/parse (LightStringReader. json-src)))])))

(defcase* large-parse-test :cheshire
  (fn []
    (let [json-src (-> "jsonorg_examples/web-app.json"
                       io/resource slurp)]
      [(fn [] (cheshire/parse-string json-src))])))

(defcase* large-parse-test :jackson
  (fn []
    (let [json-src (-> "jsonorg_examples/web-app.json"
                       io/resource slurp)
          mapper (ObjectMapper.)]
      [(fn [] (.readTree mapper json-src))])))