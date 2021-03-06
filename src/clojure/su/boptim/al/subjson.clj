(ns su.boptim.al.subjson
  (:refer-clojure :exclude [read read-string])
  (:import [su.boptim.al.subjson SubJson FromJsonPolicy ToJsonPolicy
            ToJsonPolicy$ValueType]
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
(deftype ClojureFromJsonPolicy []
  FromJsonPolicy
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

(def ^FromJsonPolicy clojure-fromjson-policy (ClojureFromJsonPolicy.))

(defn read
  "Read a json value from the argument and return the value, made out of
   Clojure objects, that the json represents. The argument must be a
   java.io.Reader. When the function returns, the Reader will not have
   been closed, and the next character read will be the first character
   after the end of the json value."
  [^Reader json-src]
  (SubJson/read json-src clojure-fromjson-policy))

(defn read-string
  "Read a json value from the argument and return the value, made out of
   Clojure objects, that the json represents. The argument must be a
   String."
  [^String json-src]
  (SubJson/read json-src clojure-fromjson-policy))

;;
;; Write
;;

(deftype ClojureToJsonPolicy []
  ToJsonPolicy
  (categorize [this obj]
    (cond (nil? obj) ToJsonPolicy$ValueType/TYPE_NULL
          (instance? Boolean obj) ToJsonPolicy$ValueType/TYPE_BOOLEAN
          (string? obj) ToJsonPolicy$ValueType/TYPE_STRING
          (integer? obj) ToJsonPolicy$ValueType/TYPE_INTEGER
          (float? obj) ToJsonPolicy$ValueType/TYPE_REAL
          (vector? obj) ToJsonPolicy$ValueType/TYPE_ARRAY
          (map? obj) ToJsonPolicy$ValueType/TYPE_OBJECT
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

(def ^ToJsonPolicy clojure-tojson-policy (ClojureToJsonPolicy.))

(defn write
  "Take a Writer and a Clojure value that represents a json value, and write
   the json representation of that value to the Writer. Clojure values that
   represent json objects are made from vectors, maps with string keys,
   strings, numbers, booleans, and nil. Optional last argument can be set
   to true (default) for pretty-printing, or false for compact printing."
  ([^Writer out json-value]
     (write out json-value true))
  ([^Writer out json-value pretty?]
     (SubJson/write out json-value pretty? clojure-tojson-policy)))

(defn write-string
  "Return a String that contains the json encoding of the Clojure values
   given as the first argument. Clojure values that represent json objects
   are made from vectors, maps with string keys, strings, numbers, booleans,
   and nil. Optional last argument can be set to true (default) for
   pretty-printing, or false for compact printing."
  ([json-value]
     (write-string json-value true))
  ([json-value ^Boolean pretty?]
     (SubJson/writeToString json-value pretty? clojure-tojson-policy)))
