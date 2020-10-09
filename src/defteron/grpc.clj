(ns defteron.grpc
  (:require [camel-snake-kebab.core :as csk]
            [defteron.core :as d]
            [clojure.string :as str])
  (:import
    (io.grpc Server
             Channel
             ServerMethodDefinition
             MethodDescriptor
             BindableService)
    (io.grpc.protobuf ProtoMethodDescriptorSupplier)))

(def ^:private dcl (clojure.lang.DynamicClassLoader.))
(def ^:private svc-catalog (atom {}))

(defn introspect-svc [grpc-package svc]
  (into {}
      (comp (map (fn [^ServerMethodDefinition method] (.getMethodDescriptor method)))
            (map (fn [^MethodDescriptor descr]
                   (let [method-descr (.getMethodDescriptor
                                        ^ProtoMethodDescriptorSupplier (.getSchemaDescriptor descr))]
                     [(-> (.getFullMethodName descr) (str/split  #"/") (last) (csk/->kebab-case-keyword))
                    {:input (.loadClass ^ClassLoader dcl (str grpc-package "." (.getFullName (.getInputType method-descr))))
                     :output (.loadClass ^ClassLoader dcl (str grpc-package "." (.getFullName (.getOutputType method-descr))))}]))))
      (seq (.getMethods svc))))


(defn register-service-description [grpc-package svc]
  (let [bind (.bindService ^BindableService svc)
        svc-descr (.getServiceDescriptor bind)]
    (swap! svc-catalog assoc (.getName svc-descr) (introspect-svc grpc-package bind))))

(defn- map->proto
  "Reflective version of map->proto"
  [proto-class data]
  (d/*clj->proto (.invoke (.getMethod proto-class "newBuilder" (into-array Class []))
                          nil
                          (into-array Object []))
                  (.invoke (.getMethod proto-class "getDescriptor" (into-array Class []))
                          nil
                          (into-array Object []) )
                  ~data))

(defn new-client [klazz channel]
  ;; TODO Perform reflections on instantiation only
  (let [client (.invoke (.getMethod klazz "newBlockingStub" (into-array Class [Channel]))
                        nil
                        (into-array Object [channel]))
        service-name (.get (.getField klazz "SERVICE_NAME") klazz)]
    (fn [method data]
      (let [input (get-in @svc-catalog [service-name method :input])]
        (d/proto->map (.invoke (.getMethod (.getClass client)
                                           (csk/->camelCaseString method)
                                           (into-array Class [input]))
                               client
                               (into-array Object [(map->proto input data)])))))))
