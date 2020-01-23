(ns airtable-clj.util
  (:require [clojure.string :as string]
            [cheshire.core :as json]
            [environ.core :refer [env]])
  (:import [java.net URL]
           [java.text SimpleDateFormat]
(def api-version "0.1.0")
(def project-version (env :airtable-clj-version))
(def user-agent (str "airtable-clj/" project-version))

(defn request-headers [api-key]
  {"X-API-Version" api-version
   "Authorization" (str "Bearer " api-key)
   "User-Agent" user-agent})

(def ^:private date-format
  (SimpleDateFormat. "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"))
(defn parse-time [s]
  (-> (.parse date-format s) .getTime))

(defn make-url
  ([{:keys [endpoint-url base table]}]
   (let [base-url (URL. (or endpoint-url "https://api.airtable.com/"))
         protocol (.getProtocol base-url)
         host (.getHost base-url)
         port (.getPort base-url)
         path (str "/v0/" base "/" table)]
     (-> (URL. protocol host port path) str)))
  ([options suffix]
   (str (make-url options) "/" suffix)))

(defn camelize-keyword [k]
  (-> (name k)
      (string/replace #"-(\w)" (fn ([[_ c]] (string/upper-case c))))))

(defn maybe-parse-json [s]
  (try
    (json/parse-string s)
    (catch JsonParseException _ nil)))

(defn handle-api-error [response]
  (let [status (:status response)
        body (maybe-parse-json (:body response))
        api-error (get body "error")
        api-error-message (if (string? api-error)
                            api-error
                            (get api-error "message"))
        error-message (cond
                        api-error-message api-error-message
                        (= status 401) "Unauthorized. Check your API key."
                        (= status 403) "Forbidden."
                        (= status 404) "Not found."
                        (= status 413) "Request body is too large."
                        (= status 422) "Request cannot be processed."
                        (= status 429) "Too many requests. Please try again later."
                        (= status 503) "Service unavailable."
                        (>= status 500) "Server error."
                        (>= status 400) "Bad request.")]
    (if error-message
      (throw (ex-info error-message {:type (get-in body ["error" "type"])
                                     :response response})))))
