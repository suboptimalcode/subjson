(ns subjson.test.stringispeedreader
  (:use clojure.test)
  (:import su.boptim.al.subjson.StringISpeedReader))

;; Last two characters are a surrogate pair (two 16-bit values that
;; represent a single Unicode character).
(def test-string1 "ABC\uD801\uDC00")

(def test-string2 "abcdefghijklmnopqrstuvwxyz")

(deftest read-test
  (let [r (StringISpeedReader. test-string1)]
    (is (= (int \A) (.read r)))
    (is (= (int \B) (.read r)))
    (is (= (int \C) (.read r)))
    (is (= (int 16rD801) (.read r)))
    (is (= (int 16rDC00) (.read r)))
    (is (= -1 (.read r)))
    ;; Check that additional reads are still -1.
    (is (= -1 (.read r)))))

(deftest move-test
  (let [r (StringISpeedReader. test-string1)]
    (.move r 3)  ;; Move 3 chars
    (is (= (int 16rD801) (.read r))) ;; High surrogate pair char.
    (.move r -1) ;; Move back 1 code point.
    (is (= (int 16rD801) (.read r)))
    (is (= (int 16rDC00) (.read r)))
    (.move r -3) ;; Move back past double-wide code point.
    (is (= (int \C) (.read r)))
    (is (thrown? IndexOutOfBoundsException
                 (.move r -4)))
    (is (thrown? IndexOutOfBoundsException
                 (.move r 5)))))

(deftest recording-test
  (let [r (StringISpeedReader. test-string2)]
    (.startRecording r)
    (is (= "" (.copyRecording r)))
    (.startRecording r)
    (.read r)
    (is (= "a" (.copyRecording r)))
    (.move r 2)
    (is (= (int \d) (.read r)))
    (.startRecording r)
    (is (true? (.isRecording r)))
    (.move r 3)
    (is (true? (.isRecording r)))
    (is (= "efg" (.copyRecording r)))
    (.endRecording r)
    (is (false? (.isRecording r)))
    (is (= (int \h) (.read r)))
    ;; Test can move backwards while recording.
    (.startRecording r)
    (.move r 6)
    (is (= (int \o) (.read r)))
    (.move r -2)
    (is (= "ijklm" (.copyRecording r)))))
