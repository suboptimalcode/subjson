(ns subjson.benchmarks.string-copies
  (:use perforate.core)
  (:require [clojure.java.io :as io])
  (:import [subjson.perftests StringCopies]))

(defgoal string-copy "Create a copy of a string")

(defcase string-copy :stringbuilder-chars
  []
  (let [s "abcdefghijklmnopqrstuvwxyz"]
    (dotimes [_ 1000] (StringCopies/copyStringSBChars s))))

(defcase string-copy :stringbuilder-str
  []
  (let [s "abcdefghijklmnopqrstuvwxyz"]
    (dotimes [_ 1000] (StringCopies/copyStringSBStr s))))


(defcase string-copy :arraycopy
  []
  (let [s (.toCharArray "abcdefghijklmnopqrstuvwxyz")]
    (dotimes [_ 1000] (StringCopies/copyStringCharArray s))))

