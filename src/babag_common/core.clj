(ns babag-common.core
  (:require
    [schema.core :as s]
    [compojure.api.sweet :refer [describe]]
    [digest]
    [clj-uuid :as uuid]))

(defn max-len [len]
  (s/pred (fn [x] (<= (count x) len))))

(defn between-1-and-10 [x]
  (and (integer? x) (>= x 1) (<= x 10)))

(defn plus-and-nine-to-fifteen-digits [x]
  (re-matches #"\+\d{9,15}" x))

(s/defschema PhoneNumber
  (s/both s/Str
          (s/pred plus-and-nine-to-fifteen-digits)))

(s/defschema SmsRequest
  {:id                        s/Str                         ; Not visible in swagger
   :user                      s/Str                         ; Not visible in swagger, extracted from auth header
   :from                      (describe (s/both s/Str (max-len 11))
                                        "Alphanumeric string, visible in FROM field on receiving mobile phone, upto 11 characters")
   :to                        (describe PhoneNumber
                                        "Mobile phone number; format: format +XXXXXX, 11 to 15 digits")
   :content                   (describe s/Str
                                        "Message content. If it exceeds message length, several SMS messages will be sent.")
   (s/optional-key :provider) (describe (s/enum :aws :linkmobility :smsapi-com)
                                        "Provider to use for sending the SMS")
   (s/optional-key :priority) (describe (s/both s/Num (s/pred between-1-and-10))
                                        "Message priority, from 1 to 10")})
(s/defn verify-sms :- s/Str
  ([record :- SmsRequest]
    (s/validate SmsRequest record)))

(s/defschema SmsStatus
  (s/enum :enqueued :delivered-to-provider :provider-failure))

(defn content-to-id [content]
  (str (digest/sha-1 content) "_" (uuid/v4)))
