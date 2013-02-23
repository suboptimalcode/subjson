(ns subjson.benchmarks.buffers
  (:use perforate.core)
  (:require [clojure.java.io :as io])
  (:import [su.boptim.al.subjson LightStringReader]
           [java.io StringReader PushbackReader BufferedReader]))

(defgoal iterate-buffer "Test various ways to iterate through input")

(defcase* iterate-buffer :lightstringreader
  (fn []
    (let [test-str (apply str (repeat 10 "ABCDEF"))
          lsr (LightStringReader. test-str)]
      [(fn []
         (loop [cnt (int 0)]
           (let [c (int (.read lsr))]
             (if (== -1 c)
               lsr
               (recur (+ cnt c))))))])))

(defcase* iterate-buffer :stringreader
  (fn []
    (let [test-str (apply str (repeat 10 "ABCDEF"))
          sr (StringReader. test-str)]
      [(fn []
         (loop [cnt (int 0)]
           (let [c (int (.read sr))]
             (if (== -1 c)
               sr
               (recur (+ cnt c))))))])))

(defcase* iterate-buffer :pushbackreader
  (fn []
    (let [test-str (apply str (repeat 10 "ABCDEF"))
          pr (PushbackReader. (StringReader. test-str))]
      [(fn []
         (loop [cnt (int 0)]
           (let [c (int (.read pr))]
             (if (== -1 c)
               pr
               (recur (+ cnt c))))))])))

(defcase* iterate-buffer :bufferedpushbackreader
  (fn []
    (let [test-str (apply str (repeat 10 "ABCDEF"))
          bpr (PushbackReader. (BufferedReader. (StringReader. test-str)))]
      [(fn []
         (loop [cnt (int 0)]
           (let [c (int (.read bpr))]
             (if (== -1 c)
               bpr
               (recur (+ cnt c))))))])))