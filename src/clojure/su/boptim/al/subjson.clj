(ns su.boptim.al.subjson
  (:refer-clojure :exclude [read read-string])
  (:import [su.boptim.al.subjson SubJson BuildPolicy ValueInterpreter
            ValueInterpreter$ValueType]
           [clojure.lang ITransientVector ITransientMap]
           [java.io Reader Writer]))

(defprotocol IBox
  (box-get [box])
  (box-set [box newVal]))

(deftype Box [^:unsynchronized-mutable val]
  IBox
  (box-get [box] val)
  (box-set [box newVal] (set! val newVal)))

;;
;; Read
;;

;; We're going to keep objects inside a Box. This is just a type that
;; will let us stick values into a reference that is not slowed down
;; by volatile/CAS/thread-local/etc. It's essentially a deftype with a
;; single unsynchronized-mutable field you can set or get, and nothing
;; more.
;;
;; Because Clojure transients cannot be "bashed in place," we need to
;; reassign them to the Box each time we perform an update, since the
;; BuildPolicy interface does not let us return new values, it expects
;; modifications to an identity. Also note that we need to check for
;; transient vectors/maps with a direct check against the class, since
;; vector? and map? will return false for transients.
(deftype ClojureBuildPolicy []
  BuildPolicy
  ;; Arrays
  (isArray [this box] (instance? ITransientVector
                                 (.box-get ^Box box)))
  (startArray [this] (Box. (transient [])))
  (arrayAppend [this box val] (.box-set ^Box box
                                        (conj! (.box-get ^Box box)
                                               val)))
  (finishArray [this box] (persistent! (.box-get ^Box box)))
  ;; Objects
  (isObject [this obj] (instance? ITransientMap
                                  (.box-get ^Box obj)))
  (startObject [this] (Box. (transient {})))
  (objectInsert [this box key val] (.box-set ^Box box
                                             (assoc! (.box-get ^Box box)
                                                     key val)))
  (finishObject [this box] (persistent! (.box-get ^Box box)))
  ;; Primitives
  (makeNull [this] nil)
  (makeBoolean [this b] b)
  (makeString [this s] s)
  (makeNumber [this n] n))

(def ^BuildPolicy clojure-build-policy (ClojureBuildPolicy.))

(defn read
  "Read a json value from the argument and return the value, made out of
   Clojure objects, that the json represents. The argument must be a
   java.io.Reader. When the function returns, the Reader will not have
   been closed, and the next character read will be the first character
   after the end of the json value."
  [^Reader json-src]
  (SubJson/read json-src clojure-build-policy))

(defn read-string
  "Read a json value from the argument and return the value, made out of
   Clojure objects, that the json represents. The argument must be a
   String."
  [^String json-src]
  (SubJson/read json-src clojure-build-policy))

;;
;; Write
;;

(deftype ClojureValueInterpreter []
  ValueInterpreter
  (categorize [this obj]
    (cond (nil? obj) ValueInterpreter$ValueType/TYPE_NULL
          (instance? Boolean obj) ValueInterpreter$ValueType/TYPE_BOOLEAN
          (string? obj) ValueInterpreter$ValueType/TYPE_STRING
          (integer? obj) ValueInterpreter$ValueType/TYPE_INTEGER
          (float? obj) ValueInterpreter$ValueType/TYPE_REAL
          (vector? obj) ValueInterpreter$ValueType/TYPE_ARRAY
          (map? obj) ValueInterpreter$ValueType/TYPE_OBJECT
          :else (throw (IllegalArgumentException.
                        (str "Could not categorize the given object" obj
                             "into a JSON value type.")))))
  ;; Conversions
  (asBoolean [this obj] obj)
  (asString [this obj] obj)
  (asInteger [this obj] obj)
  (asReal [this obj] obj)
  ;; Iterators
  (arrayIterator [this obj] (.iterator ^Iterable obj))
  (objectIterator [this obj] (.iterator ^Iterable obj)))

(def ^ValueInterpreter clojure-value-interpreter (ClojureValueInterpreter.))

(defn write
  "Take a Writer and a Clojure value that represents a json value, and write
   the json representation of that value to the Writer. Clojure values that
   represent json objects are made from vectors, maps with string keys,
   strings, numbers, booleans, and nil."
  ([^Writer out json-value]
     (write out json-value true))
  ([^Writer out json-value pretty?]
     (SubJson/write out json-value pretty?)))