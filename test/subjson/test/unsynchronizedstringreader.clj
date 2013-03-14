(ns subjson.test.unsynchronizedstringreader
  (:use clojure.test)
  (:import su.boptim.al.subjson.UnsynchronizedStringReader
           [java.io Reader StringReader]))

;; Last two characters are a surrogate pair (two 16-bit values that
;; represent a single Unicode character).
(def test-string1 "ABC\uD801\uDC00")

(def test-string2 "abcdefghijklmnopqrstuvwxyz")

;; We want to make sure the code works the same for our reader and
;; standard ones.
(def reader-makers [#(UnsynchronizedStringReader. %)
                    #(StringReader. %)])

(deftest read-test
  (doseq [make-rdr reader-makers]
    (let [r ^Reader (make-rdr test-string1)]
      (is (= (int \A) (.read r)))
      (is (= (int \B) (.read r)))
      (is (= (int \C) (.read r)))
      (is (= (int 16rD801) (.read r)))
      (is (= (int 16rDC00) (.read r)))
      (is (= -1 (.read r)))
      ;; Check that additional reads are still -1.
      (is (= -1 (.read r))))))

(deftest move-test
  (doseq [make-rdr reader-makers]
    (let [r ^Reader (make-rdr test-string1)]
      (.skip r 3)
      (.mark r 3)
      (is (= (int 16rD801) (.read r))) ;; High surrogate pair char.
      (.reset r)                       ;; Move back 1 code point.
      (is (= (int 16rD801) (.read r)))
      (is (= (int 16rDC00) (.read r)))
      (.skip r 5)
      (is (= -1 (.read r))))))

(deftest bulk-read-test
  (doseq [make-rdr reader-makers]
    (let [r ^Reader (make-rdr test-string2)
          chrs (char-array (count test-string2))]
      (.mark r 4)
      (.read r chrs 0 0)
      (is (= "" (String. chrs 0 0)))
      (.read r)
      (.reset r)
      (is (= (int \a) (.read r)))
      (.skip r 2)
      (is (= (int \d) (.read r)))
      (.mark r 3)
      (.skip r 3)
      (.reset r)
      (.read r chrs 0 3)
      (is (= "efg" (String. chrs 0 3)))
      (is (= (int \h) (.read r)))
      ;; Test can move backwards while recording.
      (.mark r 7)
      (.skip r 6)
      (is (= (int \o) (.read r)))
      (.reset r)
      (.read r chrs 0 5)
      (is (= "ijklm" (String. chrs 0 5))))))
