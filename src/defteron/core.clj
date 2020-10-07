(ns defteron.core
  (:require [clojure.string :as str]
            [clojure.set :as set]
            [defteron.struct :as d.struct]
            [camel-snake-kebab.extras :refer [transform-keys]]
            [camel-snake-kebab.core :as csk])
  (:import (com.google.protobuf Descriptors$FieldDescriptor$Type
                                Message
                                MessageOrBuilder
                                ProtocolMessageEnum
                                Struct
                                ProtocolMessageEnum)))

(def ^:dynamic *convert-key* csk/->kebab-case-keyword)

(defn- proto-method [proto method]
  (.invoke
    (.getMethod proto
                method
                (into-array Class []))
    nil
    (into-array Object [])))

(defn- enum-from-class [klazz value]
  (.invoke (.getMethod klazz "valueOf" (into-array Class [String]))
           nil
           (into-array String [value])))

;; clj -> Proto

(defn keyword->proto [proto-enum kw]
  (cond
    (isa? proto-enum ProtocolMessageEnum) (enum-from-class proto-enum (name kw))
    :else (->> kw (name) (.findValueByName proto-enum))))

(defn map->proto
  "Get fields from protobuf object's descriptor as clojure keywords"
  [proto-class data]
  (let [builder (proto-method proto-class "newBuilder")]
    (.build (reduce (fn [b field]
                      (let [is-enum? (= Descriptors$FieldDescriptor$Type/ENUM (.getType field))
                            value (cond->> (-> field (.getName) (*convert-key*) data)
                                    is-enum? (keyword->proto (.getEnumType field)))]
                        (cond-> b
                          (some? value) (.setField field value))))
                    builder
                    (.getFields (proto-method proto-class "getDescriptor"))))))

;; Proto -> clj
(defn proto->keyword
  "Returns a keyword representation of the proto enum object"
  [proto-enum]
  (let [proto-enum (cond-> proto-enum
                     (isa? (.getClass proto-enum) ProtocolMessageEnum) (.getValueDescriptor))
        full-name (str/split (.getFullName proto-enum)
                             #"\.")
        value (last full-name)
        ns- (str/join "." (butlast full-name))]
    (keyword ns- value)))

(defn proto->map
  "Returns a map representation of the proto message object."
  [proto]
  (reduce
    (fn [obj [descr value]]
      (type value)
      (cond->> value
        (= Descriptors$FieldDescriptor$Type/ENUM (.getType descr)) (proto->keyword)
        true (assoc obj (*convert-key* (.getName descr)))))
    {}
    (.getAllFields proto)))
