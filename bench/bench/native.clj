(ns bench.native
  (:import (defteron Proto$Header Proto$Size)))

(defn serialize-enum-only []
  (.toByteArray (.build (doto (Proto$Header/newBuilder)
                          (.setMsgSize Proto$Size/large)))))
(defn serialize-msg []
  (.toByteArray (.build (doto (Proto$Header/newBuilder)
                          (.setMsgSize Proto$Size/large)
                          (.setData "Some data")
                          (.addAllMeta ["a" "really" "short" "list"])))))
