(ns defteron.core
  (:require [clojure.string :as str]
            [clojure.set :as set]
            [defteron.struct :as d.struct]
            [defteron.tools :as d.tools]
            [camel-snake-kebab.extras :refer [transform-keys]]
            [camel-snake-kebab.core :as csk])
  (:import (com.google.protobuf Descriptors$FieldDescriptor$Type
                                Descriptors$EnumDescriptor
                                Descriptors$EnumValueDescriptor
                                Descriptors$Descriptor
                                Descriptors$FieldDescriptor
                                Descriptors$FileDescriptor
                                Message
                                Message$Builder
                                MessageOrBuilder
                                Struct
                                ProtocolMessageEnum)))

(set! *warn-on-reflection* true)

(def ^:dynamic *convert-key* d.tools/name-proto->clj)

(defn import-by-name [proto-class]
  (.importClass (the-ns *ns*)
                (clojure.lang.RT/classForName proto-class)))

;; clj -> Proto

(defn- kw->proto [^Descriptors$EnumDescriptor proto-enum kw]
  (.findValueByName proto-enum (name kw)))

(defmacro keyword->proto [proto-enum kw]
  `(~(symbol (name proto-enum) "valueOf") ~(name kw)))

(defn- craft-static-method! [^Descriptors$Descriptor message
                             method-name]
  (let [file (.getFile message)
        file-options (.getOptions file)
        package (.getJavaPackage file-options)
        class-name (str
                     (if (not (.getJavaMultipleFiles file-options))
                       (some->
                         (.getJavaOuterClassname file-options)
                         (str "$")))
                     (.getName message))]

    (import-by-name (str package "." class-name))

    (symbol
      class-name
      method-name)))

(declare *clj->proto)

(defn field->message [field value]
  (let [msg-descr ^Descriptors$Descriptor (.getMessageType ^Descriptors$FieldDescriptor field)
        msg-type-name (.getFullName msg-descr)]

    (if (= "google.protobuf.Struct" msg-type-name)
      (d.struct/->Struct value)
      (*clj->proto (eval (list (craft-static-method! msg-descr "newBuilder")))
                   msg-descr
                   value))))


(defn *clj->proto ^Message [^Message$Builder builder
                   ^Descriptors$Descriptor fields
                   data]
  (.build ^Message$Builder
          (reduce (fn [^Message$Builder b [key- val-]]
                    (let [field ^Descriptors$FieldDescriptor (.findFieldByName fields (d.tools/name-clj->proto key-))]
                      (.setField b field
                                 (cond->> val-
                                   ;; Message
                                   (= Descriptors$FieldDescriptor$Type/MESSAGE (.getType field))
                                   (field->message field)

                                   ;; Enum
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
  (let [full-name ^String (.getFullName proto-enum)
        splitting-point (.lastIndexOf full-name 46) ;; (int \.) => 46
        ns- (str/lower-case (.substring full-name splitting-point))
        value (.substring full-name splitting-point)]
    (keyword ns- value)))

(defn proto->keyword
  "Returns a keyword representation of the proto enum object"
  [proto-enum]
  (proto->kw (if (isa? (.getClass ^Object proto-enum) ProtocolMessageEnum)
               (.getValueDescriptor
                 ^ProtocolMessageEnum proto-enum))))

(declare proto->map)

(defn- match-xformer [^Descriptors$FieldDescriptor descr value]
  (let [descr-type (.getType descr)]
    (cond
      (instance? Struct value) d.struct/Struct->
      (= Descriptors$FieldDescriptor$Type/MESSAGE descr-type) proto->map
      (= Descriptors$FieldDescriptor$Type/ENUM descr-type) proto->kw
      :else identity)))


(defn proto->map
  "Returns a map representation of the proto message object."
  [^MessageOrBuilder proto]
  (persistent! (reduce
    (fn [obj [^Descriptors$FieldDescriptor descr value]]
      (cond-> obj
          (some? value) (assoc! (*convert-key* (.getName descr)) ((match-xformer descr value) value))))
    (transient {})
    (.getAllFields proto))))
