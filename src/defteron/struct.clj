(ns defteron.struct
  (:require [defteron.tools :as d.tools])
  (:import (com.google.protobuf Value
                                Value$KindCase
                                NullValue
                                ListValue
                                Struct)))

(def ^:dynamic to-struct-key-fn d.tools/name-proto->clj)
(def ^:dynamic from-struct-key-fn d.tools/name-clj->proto)

;; Reading Structs and Values

(defmulti ->Value type)
(defn ->Struct [obj]
  (.build (reduce (fn [builder [key- value-]]
                    (.putFields builder (to-struct-key-fn key-) (->Value value-)))
                  (Struct/newBuilder)
                  obj)))

;; Helper functions
(defn- convert-number [x]
  (-> (Value/newBuilder)
      (.setNumberValue x)
      (.build)))

(defn- convert-string [x]
  (-> (Value/newBuilder)
      (.setStringValue x)
      (.build)))

(defn- convert-collection [coll]
  (.build (.setListValue
            (Value/newBuilder)
            (.addAllValues
              (ListValue/newBuilder)
              (map ->Value coll)))))

;; 1 = Nil
(defmethod ->Value nil [x]
  (-> (Value/newBuilder)
      (.setNullValue NullValue/NULL_VALUE)
      (.build)))

;; 2 = Number
(defmethod ->Value java.lang.Double [x] (convert-number x))
(defmethod ->Value java.lang.Long [x] (convert-number x))
(defmethod ->Value clojure.lang.BigInt [x] (convert-number x))
(defmethod ->Value java.math.BigDecimal [x] (convert-number x))

;; 3 = String
(defmethod ->Value clojure.lang.Keyword [x] (convert-string (name x)))
(defmethod ->Value clojure.lang.Symbol [x] (convert-string (name x)))
(defmethod ->Value java.lang.String [x] (convert-string x))

;; 4 = Boolean
(defmethod ->Value java.lang.Boolean [x]
  (-> (Value/newBuilder)
      (.setBoolValue x)
      (.build)))

;; 5 = Map
(defmethod ->Value clojure.lang.PersistentArrayMap [x]
  (-> (Value/newBuilder)
      (.setStructValue (->Struct x))
      (.build)))

;; 6 = Collection
(defmethod ->Value clojure.lang.PersistentVector [x] (convert-collection x))
(defmethod ->Value clojure.lang.PersistentList [x] (convert-collection x))
(defmethod ->Value clojure.lang.PersistentHashSet [x] (convert-collection x))


;; Writing Structs and Values
(defmulti Value-> (fn [x] (.getKindCase x)))

(defn Struct-> [obj]
  (into {}
        (map (fn [[k v]] [(from-struct-key-fn k) (Value-> v)]))
        (.getFieldsMap obj)))

;; 1 = Nil
(defmethod Value-> Value$KindCase/NULL_VALUE [x] nil)

;; 2 = Number
(defmethod Value-> Value$KindCase/NUMBER_VALUE [x]
  (.getNumberValue x))

;; 3 = String
(defmethod Value-> Value$KindCase/STRING_VALUE [x]
  (.getStringValue x))


;; 4 = Boolean
(defmethod Value-> Value$KindCase/BOOL_VALUE [x]
  (.getBoolValue x))

;; 5 = Map
(defmethod Value-> Value$KindCase/STRUCT_VALUE [x]
  (Struct-> (.getStructValue x)))


;; 6 = Collection
(defmethod Value-> Value$KindCase/LIST_VALUE [x]
  (into []
        (map Value->)
        (.getValuesList (.getListValue x))))
