(ns defteron.struct
  (:require [camel-snake-kebab.core :as csk])
  (:import (com.google.protobuf Value Value$KindCase NullValue ListValue Struct)))

(defmulti ->Value type)
(defmulti Value-> (fn [x] (.getKindCase x)))

(defn ->Struct [obj]
  (.build (reduce (fn [builder [key- value-]]
                    (.putFields builder (csk/->snake_case_string key-) (->Value value-)))
                  (Struct/newBuilder)
                  obj)))

(defn Struct-> [obj]
  (into {}
        (map (fn [[k v]] [(csk/->kebab-case-keyword k) (Value-> v)]))
        (.getFieldsMap obj)))

(defmethod Value-> Value$KindCase/BOOL_VALUE [x]
  (.getBoolValue x))

(defmethod ->Value java.lang.Boolean [x]
  (-> (Value/newBuilder)
      (.setBoolValue x)
      (.build)))

(defn- convert-number [x]
  (-> (Value/newBuilder)
      (.setNumberValue x)
      (.build)) )

(defmethod ->Value java.lang.Double [x] (convert-number x))
(defmethod ->Value java.lang.Long [x] (convert-number x))
(defmethod ->Value clojure.lang.BigInt [x] (convert-number x))
(defmethod ->Value java.math.BigDecimal [x] (convert-number x))

(defmethod Value-> Value$KindCase/NUMBER_VALUE [x]
  (.getNumberValue x))

(defmethod ->Value java.lang.String [x]
  (-> (Value/newBuilder)
      (.setStringValue x)
      (.build)))

(defmethod Value-> Value$KindCase/STRING_VALUE [x]
  (.getStringValue x))

(defmethod ->Value clojure.lang.PersistentArrayMap [x]
  (-> (Value/newBuilder)
      (.setStructValue (->Struct x))
      (.build)))

(defmethod Value-> Value$KindCase/STRUCT_VALUE [x]
  (Struct-> (.getStructValue x)))

(defmethod ->Value clojure.lang.PersistentVector [x]
  (.build (doto (Value/newBuilder)
            (.setListValue (.build (reduce (fn [builder itm] (.addValues builder (->Value itm)))
                                           (ListValue/newBuilder)
                                           x))))))

(defmethod Value-> Value$KindCase/LIST_VALUE [x]
  (into []
        (map Value->)
        (.getValuesList (.getListValue x))))

(defmethod ->Value nil [x]
  (-> (Value/newBuilder)
      (.setNullValue NullValue/NULL_VALUE)
      (.build)))

(defmethod Value-> Value$KindCase/NULL_VALUE [x] nil)
