(ns subjson.benchmarks.buffers
  (:use perforate.core)
  (:require [clojure.java.io :as io])
  (:import [su.boptim.al.subjson LightStringReader ISpeedReader
            StringISpeedReader UnsynchronizedStringReader]
           [subjson.perftests ParseToQuotes]
           [java.io StringReader PushbackReader BufferedReader CharArrayReader]))

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

(defcase* find-endquote :stringreader
  (fn []
    (let [test-str  (str (apply str (repeat strreps "ABCDEF")) "\"")]
      [(fn []
         (let [test-str (str (apply str (repeat strreps "ABCDEF")))]
           (dotimes [_ 100]
             (ParseToQuotes/findQuote (StringReader. test-str)))))])))

(defcase* find-endquote :bufferedreader
  (fn []
    (let [test-str (str (apply str (repeat strreps "ABCDEF")) "\"")]
      [(let [test-str (str (apply str (repeat strreps "ABCDEF")) "\"")]
         (fn []
           (dotimes [_ 100]
             (ParseToQuotes/findQuote (BufferedReader.
                                       (CharArrayReader. (.toCharArray test-str)))))))])))

#_(defcase* find-endquote :lightstringreader
  (fn []
    (let [test-str (str (apply str (repeat strreps "ABCDEF")) "\"")
          lsr (LightStringReader. test-str)]
      [(fn []
         (let [lsr (LightStringReader. test-str)]
           (dotimes [_ 100]
             (ParseToQuotes/findQuote lsr)
             (.move lsr (int -6000)))))])))

(defcase* find-endquote :lightstringreader
  (fn []
    (let [test-str (str (apply str (repeat strreps "ABCDEF")) "\"")]
      [(fn []
         (dotimes [_ 100]
           (ParseToQuotes/findQuote (LightStringReader. test-str))))])))

(defcase* find-endquote :unsynchronizedstringreader
  (fn []
    [(let [test-str (str (apply str (repeat strreps "ABCDEF")) "\"")]
       (fn []
         (dotimes [_ 100]
           (ParseToQuotes/findQuote (UnsynchronizedStringReader. test-str)))))]))

(defcase* find-endquote :stringispeedreader
  (fn []
    (let [test-str (str (apply str (repeat strreps "ABCDEF")) "\"")]
      [(fn []
         (dotimes [_ 100]
           (ParseToQuotes/findQuoteAsReader (StringISpeedReader. test-str))))])))

(defgoal move "Moving one of these readers")

(defcase* move :lightstringreader
  (fn []
    (let [test-str (str (apply str (repeat strreps "ABCDEF")) "\"")
          lsr (LightStringReader. test-str)]
      [(fn []
         (dotimes [_ 1000]
           (.move lsr 1)
           (.move lsr -1)))])))

