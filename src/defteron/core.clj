(ns defteron.core
  (:require [clojure.string :as str]
            [clojure.set :as set]
            [defteron.struct :as d.struct]
            [camel-snake-kebab.extras :refer [transform-keys]]
            [camel-snake-kebab.core :as csk])
  (:import (com.google.protobuf Descriptors$FieldDescriptor$Type
                                Descriptors$EnumDescriptor
                                Descriptors$EnumValueDescriptor
                                Descriptors$Descriptor
                                Descriptors$FieldDescriptor
                                Message
                                Message$Builder
                                MessageOrBuilder
                                Struct
                                ProtocolMessageEnum)))

(def ^:dynamic *convert-key* csk/->kebab-case-keyword)

;; clj -> Proto
(defn- kw->proto [^Descriptors$EnumDescriptor proto-enum kw]
  (.findValueByName proto-enum (name kw)))

(defmacro keyword->proto [proto-enum kw]
  `(~(symbol (name proto-enum) "valueOf") ~(name kw)))

(defn *clj->proto [^Message$Builder builder
                   ^Descriptors$Descriptor fields
                   data]
  (.build ^Message$Builder
          (reduce (fn [^Message$Builder b [key- val-]]
                    (let [field ^Descriptors$FieldDescriptor (.findFieldByName fields (csk/->snake_case_string key-))]
                      (.setField b field
                                 (cond->> val-
                                   (= Descriptors$FieldDescriptor$Type/ENUM (.getType field))
                                   (kw->proto (.getEnumType field))))))
                  builder
                  data)))

(defmacro map->proto
  "Get fields from protobuf object's descriptor as clojure keywords"
  [proto-class data]
  (let [class-name (name proto-class)]
    `(*clj->proto (~(symbol class-name "newBuilder"))
                  (~(symbol class-name "getDescriptor"))
                  ~data)))

;; Proto -> clj
(defn- proto->kw
  "Returns a keyword representation of the proto enum object"
  [^Descriptors$EnumValueDescriptor proto-enum]
  (let [full-name (str/split (.getFullName proto-enum)
                             #"\.")
        value (last full-name)
        ns- (str/join "." (butlast full-name))]
    (keyword ns- value)))

(defn proto->keyword
  "Returns a keyword representation of the proto enum object"
  [proto-enum]
  (proto->kw (if (isa? (.getClass ^Object proto-enum) ProtocolMessageEnum)
               (.getValueDescriptor
                 ^ProtocolMessageEnum proto-enum))))

(defn proto->map
  "Returns a map representation of the proto message object."
  [^MessageOrBuilder proto]
  (reduce
    (fn [obj [^Descriptors$FieldDescriptor descr value]]
      (cond->> value
        (= Descriptors$FieldDescriptor$Type/ENUM (.getType descr)) (proto->kw)
        true (assoc obj (*convert-key* (.getName descr)))))
    {}
    (.getAllFields proto)))
