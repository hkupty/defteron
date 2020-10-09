(ns defteron.core-test
  (:require [clojure.test :refer :all]
            [defteron.core :refer :all])
  (:import (defteron Proto$Header Proto$Size)))

(set! *warn-on-reflection* true)


(deftest protobuf->clojure
  (let [sample-header (.build (doto (Proto$Header/newBuilder)
                                (.setMsgSize Proto$Size/large)
                                (.setData "Amazing")
                                (.addAllMeta ["asdf" "qwer"])))]

    (testing "Enums can turn into namespaced keywords"
      (is (= :defteron.size/large (proto->keyword Proto$Size/large))))

    (testing "Messages can be turned into maps"
      (is (= {:msg-size :defteron.size/large
              :data "Amazing"
              :meta ["asdf" "qwer"]}
             (proto->map sample-header))))))

(deftest clojure->protobuf
  (let [sample-header (.build (doto (Proto$Header/newBuilder)
                                (.setMsgSize Proto$Size/large)
                                (.setData "Amazing")
                                (.addAllMeta ["asdf" "qwer"])))]

    (testing "Keywords can be turned into enums"
      (is (= (keyword->proto Proto$Size :defteron.size/large)
             Proto$Size/large)))

    (testing "Maps can be turned into messages"
      (is (= sample-header
             (map->proto  Proto$Header
                         {:msg-size :defteron.size/large
                          :data "Amazing"
                          :meta ["asdf" "qwer"]}))))))

(deftest roudtrip
  (let [proto-msg (.build (doto (Proto$Header/newBuilder)
                            (.setMsgSize Proto$Size/large)
                            (.setData "Amazing")
                            (.addAllMeta ["asdf" "qwer"])))
        clj-msg {:msg-size :defteron.size/large
                 :data "Amazing"
                 :meta ["asdf" "qwer"]}]

    (testing "Clojure -> Proto -> Clojure"
      (is (= (proto->map (map->proto Proto$Header clj-msg))
             clj-msg)))

    (testing "Messages can be turned into maps"
      (is (= (map->proto Proto$Header (proto->map proto-msg))
             proto-msg)))))
