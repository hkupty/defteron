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

(defn deserialize-msg [msg]
  ;; Not necessarily a fair comparison,
  ;; since bean will produce a different result,
  ;; but I could see bean being used in production
  ;; so it should be "ok" to check against something
  ;; being used the same way, while not producing the same result.
  (bean msg))
