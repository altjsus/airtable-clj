(ns airtable-clj.core
  (:require [clj-http.client :as http]
            [cheshire.core :as json]
            [airtable-clj.util :refer [camelize-keyword
                                       parse-time
                                       make-url
                                       request-headers
                                       handle-api-error]]))

;; TODO: :as :json (strict?) for response coercion

(defn- format-record [record]
  {:id (record "id")
   :fields (record "fields")
   :created-time (parse-time (record "createdTime"))})

(defn- format-deletion [response]
  {:id (response "id")})

(defn- record-not-found? [response]
  (let [error-type (-> (:body response)
                       json/parse-string
                       (get-in ["error" "type"]))]
    (and (= 404 (:status response))
         (= "MODEL_ID_NOT_FOUND" error-type))))

(def ^:private select-options
  (->> [:fields
        :filter-by-formula
        :max-records
        :page-size
        :view
        :cell-format
        :time-zone
        :user-locale]
       (map (fn [param] [param (camelize-keyword param)]))
       (into {})))

(defn select
  "Select records from an base."
  [{:keys [api-key] :as options}]
  (let [url (make-url options)
        query-params (->> (select-keys options (keys select-options))
                          (map (fn [[k v]]
                                 [(k select-options)
                                  (if (keyword? v) (name v) v)]))
                          (into {}))
        http-options (cond-> {:headers (request-headers api-key)
                              :throw-exceptions true
                              :multi-param-style :array}
                       (seq query-params) (assoc :query-params query-params))
        response (http/get url http-options)
        _ (handle-api-error response)
        body (json/parse-string (:body response))]
    {:records (map format-record (body "records"))
     :offset (body "offset")}))

(defn retrieve
  "Retrieve a single record from a base."
  [{:keys [api-key record-id throw-if-not-found?] :as options}]
  (let [url (make-url options record-id)
        response (http/get url {:headers (request-headers api-key)
                                :throw-exceptions false})]
    (when-not (and (not throw-if-not-found?) (record-not-found? response))
      (handle-api-error response)
      (-> response :body json/parse-string format-record))))

(defn create
  "Create a record in a base."
  [{:keys [api-key fields typecast?] :as options}]
  (let [url (make-url options)
        body (cond-> {"fields" fields}
               typecast? (assoc "typecast" true))
        response (http/post url {:headers (request-headers api-key)
                                 :content-type :json
                                 :body (json/generate-string body)})]
    (handle-api-error response)
    (-> response :body json/parse-string format-record)))

(defn modify
  "Update a record in a base."
  [{:keys [api-key record-id fields clobber? typecast?] :as options}]
  (let [url (make-url options record-id)
        body (cond-> {"fields" fields}
               typecast? (assoc "typecast" true))
        http-fn (if clobber? http/put http/patch)
        response (http-fn url {:headers (request-headers api-key)
                               :content-type :json
                               :body (json/generate-string body)})]
    (handle-api-error response)
    (-> response :body json/parse-string format-record)))

(defn delete
  "Delete a record from a base."
  [{:keys [api-key record-id throw-if-not-found?] :as options}]
  (let [url (make-url options record-id)
        response (http/delete url {:headers (request-headers api-key)
                                   :throw-exceptions false})]
    (when-not (and (not throw-if-not-found?) (record-not-found? response))
      (handle-api-error response)
      (-> response :body json/parse-string format-deletion))))
