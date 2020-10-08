(ns bench.defteron
  (:require [defteron.core :refer [map->proto keyword->proto]])
  (:import (defteron Proto$Header Proto$Size)))

(defn serialize-enum-only []
  (.toByteArray (.build (doto (Proto$Header/newBuilder)
                          (.setMsgSize (keyword->proto Proto$Size :defteron.Size/large))))))

(defn serialize-msg []
  (.toByteArray (map->proto Proto$Header {:msg-size :defteron.Size/large
                                          :data "Some data"
                                          :meta ["a" "really" "short" "list"]})) )
