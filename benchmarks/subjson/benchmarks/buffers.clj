(ns subjson.benchmarks.buffers
  (:use perforate.core)
  (:require [clojure.java.io :as io])
  (:import [su.boptim.al.subjson UnsynchronizedStringReader]
           [subjson.perftests ParseToQuotes]
           [java.io StringReader PushbackReader
            BufferedReader CharArrayReader]))

;; http://stackoverflow.com/questions/8894258/fastest-way-to-iterate-over-all-the-chars-in-a-string

(set! *warn-on-reflection* true)

(def strreps 1000)

(defgoal find-endquote "Test various ways to search through a series of
                        chars to find the end-quote character's index.")

(defcase* find-endquote :string-as-char-array
  (fn []
    (let [test-str ^String (str (apply str (repeat strreps "ABCDEF")) "\"")]
      [(fn []
         (dotimes [_ 100] (ParseToQuotes/findQuote test-str)))])))

(defcase* find-endquote :char-array
  (fn []
    (let [test-str ^String (str (apply str (repeat strreps "ABCDEF")) "\"")]
      [(let [char-array (.toCharArray test-str)]
         (fn []
           (let [char-array (.toCharArray test-str)]
             (dotimes [_ 100] (ParseToQuotes/findQuote char-array)))))])))

(defcase* find-endquote :bufferedreader
  (fn []
    (let [test-str (str (apply str (repeat strreps "ABCDEF")) "\"")]
      [(let [test-str (str (apply str (repeat strreps "ABCDEF")) "\"")]
         (fn []
           (dotimes [_ 100]
             (ParseToQuotes/findQuote (BufferedReader.
                                       (CharArrayReader. (.toCharArray test-str)))))))])))

(defcase* find-endquote :unsynchronizedstringreader
  (fn []
    [(let [test-str (str (apply str (repeat strreps "ABCDEF")) "\"")]
       (fn []
         (dotimes [_ 100]
           (ParseToQuotes/findQuote (UnsynchronizedStringReader. test-str)))))]))

