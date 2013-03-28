(ns subjson.test.textutils
  (:use clojure.test)
  (:import [su.boptim.al.subjson TextUtils]))

;;
;; Read utilities
;;

(deftest isDigit-test
  (is (= true (TextUtils/isDigit (int \0))))
  (is (= true (TextUtils/isDigit (int \1))))
  (is (= true (TextUtils/isDigit (int \2))))
  (is (= true (TextUtils/isDigit (int \3))))
  (is (= true (TextUtils/isDigit (int \4))))
  (is (= true (TextUtils/isDigit (int \5))))
  (is (= true (TextUtils/isDigit (int \6))))
  (is (= true (TextUtils/isDigit (int \7))))
  (is (= true (TextUtils/isDigit (int \8))))
  (is (= true (TextUtils/isDigit (int \9))))

  (is (= false (TextUtils/isDigit -1)))
  (doseq [i (range 0 16r2F)]
    (is (= false (TextUtils/isDigit i))))
  (doseq [i (range 16r3A 16rFF)]
    (is (= false (TextUtils/isDigit i)))))

(deftest isHexDigit-test
  (is (= false (TextUtils/isHexDigit -1)))
  (doseq [i (range (int \0) (int \9))]
    (is (= true (TextUtils/isHexDigit i))))
  (doseq [i (range (int \a) (int \f))]
    (is (= true (TextUtils/isHexDigit i))))
  (doseq [i (range (int \A) (int \F))]
    (is (= true (TextUtils/isHexDigit i))))
  (doseq [i (range (inc (int \f)) 16rFF)]
    (is (= false (TextUtils/isHexDigit i)))))


(deftest isControlCharacter-test
  (is (= false (TextUtils/isControlCharacter -1)))
  (doseq [i (range 0 16r1F)]
    (is (= true (TextUtils/isControlCharacter i))))
  (doseq [i (range 16r20 16rFF)]
    (is (= false (TextUtils/isControlCharacter i)))))

(deftest isWhitespace-test
  (is (= true (TextUtils/isWhitespace (int \space))))
  (is (= true (TextUtils/isWhitespace (int \tab))))
  (is (= true (TextUtils/isWhitespace (int \return))))
  (is (= true (TextUtils/isWhitespace (int \newline))))
  (doseq [i (range 16r21 16rFF)]
    (is (= false (TextUtils/isWhitespace i)))))
