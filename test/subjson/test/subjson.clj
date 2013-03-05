(ns subjson.test.subjson
  (:use clojure.test)
  (:require [clojure.java.io :as io])
  (:import [su.boptim.al.subjson SubJson ISpeedReader StringISpeedReader]
           [java.lang.reflect Method]))

(defn get-private-static-method
  "Given a method name, returns a closure that will call that static method
   with the arguments given to the closure."
  [method-name]
  (let [method (.getDeclaredMethod SubJson method-name
                                   (into-array Class [ISpeedReader]))]
    (.setAccessible method true)
    (fn [& args]
      (.invoke method nil ;; static methods only
               (to-array args)))))

;;
;; Parse utilities
;;

(deftest isDigit-test
  (is (= true (SubJson/isDigit (int \0))))
  (is (= true (SubJson/isDigit (int \1))))
  (is (= true (SubJson/isDigit (int \2))))
  (is (= true (SubJson/isDigit (int \3))))
  (is (= true (SubJson/isDigit (int \4))))
  (is (= true (SubJson/isDigit (int \5))))
  (is (= true (SubJson/isDigit (int \6))))
  (is (= true (SubJson/isDigit (int \7))))
  (is (= true (SubJson/isDigit (int \8))))
  (is (= true (SubJson/isDigit (int \9))))

  (is (= false (SubJson/isDigit -1)))
  (doseq [i (range 0 16r2F)]
    (is (= false (SubJson/isDigit i))))
  (doseq [i (range 16r3A 16rFF)]
    (is (= false (SubJson/isDigit i)))))

(deftest isHexDigit-test
  (is (= false (SubJson/isHexDigit -1)))
  (doseq [i (range (int \0) (int \9))]
    (is (= true (SubJson/isHexDigit i))))
  (doseq [i (range (int \a) (int \f))]
    (is (= true (SubJson/isHexDigit i))))
  (doseq [i (range (int \A) (int \F))]
    (is (= true (SubJson/isHexDigit i))))
  (doseq [i (range (inc (int \f)) 16rFF)]
    (is (= false (SubJson/isHexDigit i)))))


(deftest isControlCharacter-test
  (is (= false (SubJson/isControlCharacter -1)))
  (doseq [i (range 0 16r1F)]
    (is (= true (SubJson/isControlCharacter i))))
  (doseq [i (range 16r20 16rFF)]
    (is (= false (SubJson/isControlCharacter i)))))

(deftest isWhitespace-test
  (is (= true (SubJson/isWhitespace (int \space))))
  (is (= true (SubJson/isWhitespace (int \tab))))
  (is (= true (SubJson/isWhitespace (int \return))))
  (is (= true (SubJson/isWhitespace (int \newline))))
  (doseq [i (range 16r21 16rFF)]
    (is (= false (SubJson/isWhitespace i)))))

;;
;; Ignoring whitespace
;;

;; Map of strings to the next character their reader should read
;; after any whitespace.
(def whitespaces {" " -1
                 "  " -1
                 "      " -1
                 "" -1
                 "  ab" \a
                 "\t" -1
                 "\ta" \a
                 " \t1" \1
                 "\r\n" -1
                 "abc" \a
                 "\rab" \a})

(def skipWhitespace (get-private-static-method "skipWhitespace"))

(deftest parse-test--whitespace
  (doseq [[ws-str next-char] whitespaces]
    (let [lsr (StringISpeedReader. ws-str)]
      (is (= (int next-char) (do (skipWhitespace lsr)
                                 (.read lsr)))))))

;;
;; Parsing numbers
;;

(def json-numbers {"0" 0
                   "-0" 0
                   "1" 1
                   "-1" -1
                   "100" 100
                   "-100" -100
                   "-0e1" 0.0
                   "0e+1" 0.0
                   "-12e-1" -1.2
                   "-12e+1" -120.0
                   "10E1" 100.0
                   "10e1" 100.0
                   "0.00000001" 0.00000001
                   "-1.1" -1.1
                   "100.2" 100.2
                   "100.232e3" 100232.0
                   "100.232e+3" 100232.0
                   "100.232E+3" 100232.0
                   "100.232e-3" 0.100232
                   "100.232E-3" 0.100232})

(def not-json-numbers ["00" "01" "001" "00.1" ".1" "1."])

(def parseNumber (get-private-static-method "parseNumber"))

(deftest parse-test--numbers
  (doseq [[num-str num] json-numbers]
    (is (= num (parseNumber (StringISpeedReader. num-str)))))
  (doseq [not-num not-json-numbers]
    (is (thrown? Exception
                 (parseNumber (StringISpeedReader. not-num)))))

  ;; Check that it still works from main json parse
  (doseq [[num-str num] json-numbers]
    (is (= num (SubJson/parse (StringISpeedReader. num-str)))))
  (doseq [not-num not-json-numbers]
    (is (thrown? Exception
                 (SubJson/parse (StringISpeedReader. not-num)))))

  ;; Check that it still works from main json parse with leading/trailing ws
  (doseq [[num-str num] json-numbers]
    (is (= num (SubJson/parse (StringISpeedReader. (str " \t"
                                                        num-str
                                                        "\r\n"))))))
  (doseq [not-num not-json-numbers]
    (is (thrown? Exception
                 (SubJson/parse (StringISpeedReader. (str " \t"
                                                          not-num
                                                          "\r\n")))))))

;;
;; Parsing booleans
;;

(def bools {"true" true
            "true " true
            "true," true
            "false" false
            "false " false
            "false," false})

(def not-booleans ["True" "TRUE" "tRue" "trUe" "truE" "t" "tr" "tru"
                   "False" "FALSE" "fAlse" "faLse" "falSe" "falsE" "f" "fa"
                   "fals" "null" "Roger"])

(def parseBoolean (get-private-static-method "parseBoolean"))

(deftest parse-test--booleans
  (doseq [[bool-src bool-val] bools]
    (is (= bool-val (parseBoolean (StringISpeedReader. bool-src)))))
  (doseq [not-bool not-booleans]
    (is (thrown? Exception
                 (parseBoolean (StringISpeedReader. not-bool)))))

  ;; Check that it still works from main json parse
  (doseq [[bool-src bool-val] bools]
    (is (= bool-val (SubJson/parse (StringISpeedReader. bool-src)))))

  ;; Check that it still works from main json parse with leading/trailing ws
  (doseq [[bool-src bool-val] bools]
    (is (= bool-val (SubJson/parse (StringISpeedReader. (str " \t"
                                                             bool-src
                                                             "\r\n")))))))

;;
;; Parsing null
;;

;; Map is of source strings to what read() should return just after.
(def nulls {"null" -1
            "null "  \space
            "null," \,})

(def not-nulls ["Null" "NULL" "nUll" "nuLl" "nulL" "n" "nu" "nul" "true"
                "Burt"])

(def parseNull (get-private-static-method "parseNull"))

(deftest parse-test--null
  (doseq [[null-src next-chr] nulls]
    (let [lsr (StringISpeedReader. null-src)]
      (is (= (int next-chr) (do (parseNull lsr)
                                (.read lsr))))))
  (doseq [not-null not-nulls]
    (is (thrown? Exception (parseNull (StringISpeedReader. not-null)))))

  ;; Check that it still works from the main json parse
  (doseq [[null-src next-chr] nulls]
    (is (= nil (SubJson/parse (StringISpeedReader. null-src)))))
  (doseq [[null-src next-chr] nulls]
    (is (= nil (SubJson/parse (StringISpeedReader. (str " \t"
                                                        null-src
                                                        "\r\n")))))))

;;
;; Parsing strings
;;

(def strings {"\"a\"" "a"
              "\"ab\"" "ab"
              "\"abc\"" "abc"
              "\"\\\\\"" "\\"
              "\"\\\"\"" "\""
              "\"a\\\"b\"" "a\"b"
              "\"a\\/b\\/c/\"" "a/b/c/"
              "\"/\\b\"" "/\b"
              "\"\\f\\n\\r\\t\"" "\f\n\r\t"
              "\"\\u0030\\u0031\\u0032\"" "012"
              "\"\\u00312345\"" "12345"})

(def not-strings ["hi" "1923" "\"unclosed..." "\"\\u11\""
                  "\"\\ummff\"" "\"\\.notanescape\""])

(def parseString (get-private-static-method "parseString"))

(deftest parse-test--string
  (doseq [[string-src string-value] strings]
    (is (= string-value (parseString (StringISpeedReader. string-src)))))
  (doseq [not-string not-strings]
    (is (thrown? Exception (parseString (StringISpeedReader. not-string)))))

  ;; Check that it still works from the main json parse
  (doseq [[string-src string-value] strings]
    (is (= string-value (SubJson/parse (StringISpeedReader. string-src)))))
   (doseq [[string-src string-value] strings]
     (is (= string-value (SubJson/parse (StringISpeedReader. (str " \t"
                                                                  string-src
                                                                  "\r\n")))))))

;;
;; Parsing arrays
;;

(def arrays {"[]" []
             "[1]" [1]
             "[1,2]" [1,2]
             "[1,2,3]" [1,2,3]
             "[ 1,2,3]" [1,2,3]
             "[1 ,2,3]" [1,2,3]
             "[1, 2,3]" [1,2,3]
             "[1,2 , 3]" [1,2,3]
             "[1,2,3 ]" [1,2,3]
             "[\"one\", \"two\", \"3\"]" ["one", "two", "3"]
             "[true, false, null]" [true, false, nil]
             "[ null , true , false , 123, 45.67e+89, \"ten\"]"
             [nil, true, false, 123, 45.67e+89, "ten"]
             "[[]]" [[]]
             "[[[[[[[[[[1, 2.0, false, null]]]]]]]]]]"
             [[[[[[[[[[1 2.0 false nil]]]]]]]]]]
             "[{\"a\":[1, 2,3]}, {\"b\":[true]}]"
             [{"a" [1 2 3]} {"b" [true]}]})

(def not-arrays ["[" "[1" "[1,2" "[1 2]" "[\"hi\"" "[\"hi\",true"
                 "[null, \"hi\"" "[\"hi\" \"there\"" "[}" "[1,}" "[1,2}"])

(deftest parse-test--array
  (doseq [[arr-src arr] arrays]
    (is (= arr (SubJson/parse (StringISpeedReader. arr-src)))))
  (doseq [not-arr not-arrays]
    (is (thrown? Exception (SubJson/parse (StringISpeedReader. not-arr))))))

;;
;; Parsing objects
;;

(def objects {"{}" {}
              "{\"\":1}" {"" 1}
              "{\"\" :1}" {"" 1}
              "{\"\": 1}" {"" 1}
              "{\"\":1 }" {"" 1}
              "{\"1\":1,\"2\":2}" {"1" 1 "2" 2}
              "{\"1\":1 ,\"2\":2}" {"1" 1 "2" 2}
              "{\"1\":1, \"2\":2}" {"1" 1 "2" 2}
              "{\"1\" : 1 , \"2\" : 2}" {"1" 1 "2" 2}
              "{\"null\":null, \"true\": true ,\"false\" :false}"
              {"null" nil "true" true "false" false}
              "{ } " {}
              "{\"\":1} " {"" 1}
              "{\"a\":{}}" {"a" {}}
              "{\"a\":{\"b\":\"c\"}}" {"a" {"b" "c"}}
              "{\"a\":{\"b\":{\"c\":\"d\"}}}" {"a" {"b" {"c" "d"}}}
              "{\"a\": {\"b\":\"c\"},\"d\":\"e\"}" {"a" {"b" "c"} "d" "e"}
              "{\"a\":{\"b\":0,\"c\":1},\"d\":{\"e\":4,\"f\":false} }"
              {"a" {"b" 0 "c" 1} "d" {"e" 4 "f" false}}
              "{\"a\":[{\"b\":{\"c\":\"d\"},\"e\":4}],\"f\":false}"
              {"a" [{"b" {"c" "d"} "e" 4}] "f" false}})

(def not-objects ["{" "}" "{1:1}" "{\"}" "{\"1\":}"
                  "{\"1\" 1}" "{\"1\":1 \"2\":2}" "{]" "{\"a\":" "{\"a\":1"
                  "{\"a\":1," "{\"a\":]" "{\"a\":1]" "{\"a\":1,]"])

(deftest parse-test--object
  (doseq [[obj-src obj] objects]
    (is (= obj (SubJson/parse (StringISpeedReader. obj-src)))))
  (doseq [not-obj not-objects]
    (is (thrown? Exception (SubJson/parse (StringISpeedReader. not-obj))))))

;;
;; "Full" examples
;;

(def jsonorg_examples ["glossary" "menu" "widget" "web-app" "menu2"])

(deftest test_jsonorg_examples
  (doseq [example-name jsonorg_examples]
    (let [json-src (-> (str "jsonorg_examples/" example-name ".json")
                       io/resource slurp)
          edn-src (-> (str "jsonorg_examples/" example-name ".edn")
                      io/resource slurp)]
      (is (= (SubJson/parse (StringISpeedReader. json-src))
             (read-string edn-src))))))
