(ns defteron.core-test
  (:require [clojure.test :refer :all]
            [defteron.core :refer :all])
  (:import (defteron Proto$Message Proto$Header Proto$Size)
           (com.google.protobuf Value
                                Value$KindCase
                                NullValue
                                ListValue
                                Struct)))

(set! *warn-on-reflection* true)

(def sample-header (.build (doto (Proto$Header/newBuilder)
                             (.setMsgSize Proto$Size/large)
                             (.setData "Amazing")
                             (.addAllMeta ["asdf" "qwer"]))))
(def sample-message (.build (doto (Proto$Message/newBuilder)
                              (.setHeader sample-header)
                              (.setData (.build (doto (Struct/newBuilder)
                                                  (.putFields
                                                    "some"
                                                    (.build
                                                      (.setListValue
                                                        (Value/newBuilder)
                                                        (.addAllValues
                                                          (ListValue/newBuilder)
                                                          (map
                                                            (fn [x]
                                                              (-> (Value/newBuilder)
                                                                  (.setStringValue x)
                                                                  (.build)))
                                                            ["Arbitrary" "data" "structure"])))))
                                                  (.putFields
                                                    "with"
                                                    (.build
                                                      (.setStructValue
                                                        (Value/newBuilder)
                                                        (doto (Struct/newBuilder)
                                                          (.putFields
                                                            "nested_data"
                                                            (.build (.setNumberValue (Value/newBuilder) 100)))
                                                          (.putFields
                                                            "opinionated_serializers"
                                                            (.build (.setBoolValue (Value/newBuilder) true)))
                                                          (.putFields
                                                            "all_types"
                                                            (.build (.setNullValue (Value/newBuilder) NullValue/NULL_VALUE)))))))))))))

(deftest protobuf->clojure

  (testing "Enums can turn into namespaced keywords"
    (is (= :defteron.size/large (proto->keyword Proto$Size/large))))

  (testing "Messages can be turned into maps"
    (is (= {:msg-size :defteron.size/large
            :data "Amazing"
            :meta ["asdf" "qwer"]}
           (proto->map sample-header))))

  (testing "Arbitrary data goes into Structs"
    (is (= {:header {:msg-size :defteron.size/large
                     :data "Amazing"
                     :meta ["asdf" "qwer"]}
            :data {:some ["Arbitrary" "data" "structure"]
                   :with {:nested-data 100.0
                          :opinionated-serializers true
                          :all-types nil}}}
           (proto->map sample-message)))))

(deftest clojure->protobuf
  (testing "Keywords can be turned into enums"
    (is (= (keyword->proto Proto$Size :defteron.size/large)
           Proto$Size/large)))

(.getValueDescriptor ^ProtocolMessageEnum Proto$Size/large)

  (testing "Maps can be turned into messages"
    (is (= sample-header
           (map->proto  Proto$Header
                       {:msg-size :defteron.size/large
                        :data "Amazing"
                        :meta ["asdf" "qwer"]}))))

  (testing "Maps can be turned into structs"
    (is (= sample-message
           (map->proto Proto$Message
                       {:header {:msg-size :defteron.size/large
                                 :data "Amazing"
                                 :meta ["asdf" "qwer"]}
                        :data {:some ["Arbitrary"
                                      'data
                                      :structure]
                               :with {:nested-data 100
                                      :opinionated-serializers true
                                      :all-types nil}} })))))

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
