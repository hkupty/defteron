(ns defteron.core
  (:require [clojure.string :as str]
            [clojure.set :as set]
            [defteron.struct :as d.struct]
            [camel-snake-kebab.extras :refer [transform-keys]]
            [camel-snake-kebab.core :as csk])
  (:import (com.google.protobuf Descriptors$FieldDescriptor$Type
                                Message
                                Struct
                                ProtocolMessageEnum)))

(defn- map-val
  ([fn] (map (juxt key (comp fn val))))
  ([fn coll] (into {} (map-val fn) coll)))

(defn- *proto->fields
  "Get fields from protobuf object's descriptor as clojure keywords"
  [descr]
  (map (fn [field]
         (keyword (.getName field)))
       (.getFields descr)))


(defn- get-descriptor [proto method]
  (.invoke
    (.getMethod (.getClass proto)
                method
                (into-array Class []))
    nil
    (into-array Object [])))

(defn- *proto-fields-descr [proto] (get-descriptor proto "getDescriptor"))

(defn- oneofs-xform [oneof-descriptor]
  (let [oneof-field (csk/->camelCaseKeyword (str (.getName oneof-descriptor) "Case"))
        possible-oneof-fields (into #{}
                                    (map #(csk/->camelCaseKeyword (.getName %)))
                                    (.getFields oneof-descriptor))]
    (fn [proto-obj]
      (->> (get proto-obj oneof-field)
           (.toString)
           (csk/->camelCaseKeyword)
           (disj possible-oneof-fields)
           (apply dissoc proto-obj)))))

(defn proto->keyword
  "Returns a keyword representation of the proto enum object"
  [proto-enum & {:keys [namespaced?] :or {namespaced? true}}]
  (let [full-name (str/split (.getFullName (.getValueDescriptor proto-enum))
                             #"\.")
        value (last full-name)
        ns- (str/join "." (butlast full-name))]

    (cond->> []
      namespaced? (conj ns-)
      true (conj (csk/->kebab-case-string value))
      true (apply keyword))))

(defn proto->map
  "Returns a map representation of the proto message object."
  [proto]
  (let [fields-descr (*proto-fields-descr proto)
        field-oneofs (.getOneofs fields-descr)
        all-oneof-xforms (map oneofs-xform field-oneofs)
        fields (*proto->fields fields-descr)]
    (into {}
          (map-val (fn [val-]
                     (let [is-a? (fn [x] (some? ((supers (type val-)) x)))]
                       (cond
                         (is-a? Enum) (proto->keyword val-)
                         (is-a? Message) (proto->map val-)
                         :else val-))))
          (select-keys (reduce
                         (fn [acc xform]
                           (xform acc))
                         (bean proto)
                         all-oneof-xforms)
                       fields))))
