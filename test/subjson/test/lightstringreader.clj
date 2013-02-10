(ns subjson.test.lightstringreader
  (:use clojure.test)
  (:import su.boptim.al.subjson.LightStringReader))

;; Last two characters are a surrogate pair (two 16-bit values that
;; represent a single Unicode character).
(def test-string "ABC\uD801\uDC00")

(deftest read-test
  (let [r (LightStringReader. test-string)]
    (is (= (int \A) (.read r)))
    (is (= (int \B) (.read r)))
    (is (= (int \C) (.read r)))
    (is (= (int 16rD801) (.read r)))
    (is (= (int 16rDC00) (.read r)))
    (is (= -1 (.read r)))
    ;; Check that additional reads are still -1.
    (is (= -1 (.read r)))))

(deftest move-test
  (let [r (LightStringReader. test-string)]
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

