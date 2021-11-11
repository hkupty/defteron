(ns tooling.release
  (:require [clojure.java.io :as io]
            [clojure.string :as str])
  (:import (org.eclipse.jgit.api
             Git)
           (org.eclipse.jgit.lib Constants)
           (org.apache.maven.model Model)
           (org.apache.maven.model.io.xpp3 MavenXpp3Reader MavenXpp3Writer)))

;; Extract data from git

(def git (Git/open (.getAbsoluteFile (io/file "."))))

(defn head [] (.resolve (.getRepository git) Constants/HEAD))

(defn commit-theme [message]
  (keyword (last (re-find (re-matcher #"^(\w+):"
              message)))))

(defn xf-count-themes [v-map]
  (fn [xf]
    (fn
      ([] (xf))
      ([result] (xf result))
      ([result item]
       (vswap! v-map update (commit-theme (.getShortMessage item)) (fnil inc 0))
       (xf result item)))))

(defn xf-get-changelog [v-coll]
  (fn [xf]
    (fn
      ([] (xf))
      ([result] (xf result))
      ([result item]
       (let [msg (.getShortMessage item)]
         (vswap! v-coll update (commit-theme msg) (fnil conj []) (last (str/split msg #": " 2))))
       (xf result item)))))

(defn process-commits [v-map changelog]
  (into []
        (comp (xf-count-themes v-map)
              (xf-get-changelog changelog))
        (iterator-seq
          (.iterator (.call (.addRange (.log git)
                                       (.getObjectId (last (-> git
                                                               (.tagList)
                                                               (.call))))
                                       (head)))))))
;; Pom related tasks


(defn read-pom []
  (.read (MavenXpp3Reader.) (io/reader (io/file "pom.xml"))))

(defn write-pom! [file]
  (.write (MavenXpp3Writer.)
          (io/writer (io/file "pom.xml"))
          file))

(defn get-version [str-version]
  (into []
        (map #(Integer/parseInt %))
        (str/split str-version #"\.")))

(defn major [version] (-> version
                          (update 0 inc)
                          (assoc 1 0)
                          (assoc 2 0)))
(defn minor [version] (-> version
                          (update 1 inc)
                          (assoc 2 0)))
(defn bugfix [version] (update version 2 inc))

(defn bump-version [version]
  (str/join "." (let [themes (process-commits (volatile! {}) (volatile! {}))
                      version (get-version version)]
                  (cond
                    (pos? (:breaking themes 0)) (major version)
                    (pos? (:feature themes 0)) (minor version)
                    (pos? (:fix themes 0)) (bugfix version)
                    :else version))))

(defn update-version! []
  (let [base (read-pom)]
    (.setVersion base (bump-version (.getVersion base)))
    (write-pom! base)))


(defn changelog []
  (let [chg (volatile! {})]
    (process-commits (volatile! {}) chg)
    (run! (fn [[k v]]
            (some-> k
                    (name)
                    (str ":")
                   (println))
            (run! println v))
          @chg)))

(defn -main [cmd]
  (case cmd
   "changelog" (changelog)
   "bump" (update-version!)))
