(ns defteron.tools)

(defn name-clj->proto [^clojure.lang.Keyword kw]
  (.replace (name kw) \- \_))

(defn name-proto->clj [^String s]
  (keyword (.replace s \_ \-)))
