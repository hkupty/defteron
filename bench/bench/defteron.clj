(ns bench.defteron
  (:require [defteron.core :refer [proto->map
                                   map->proto
                                   keyword->proto]])
  (:import (defteron Proto$Header Proto$Size)))

(defn serialize-enum-only []
  (.toByteArray (.build (doto (Proto$Header/newBuilder)
                          (.setMsgSize (keyword->proto Proto$Size :defteron.size/large))))))

(defn serialize-msg []
  (.toByteArray (map->proto Proto$Header {:msg-size :defteron.size/large
                                          :data "Some data"
                                          :meta ["a" "really" "short" "list"]})))

(defn deserialize-msg [msg]
  (proto->map msg))
