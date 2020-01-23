# airtable-clj

Clojure interface to [Airtable's API](https://airtable.com/api).

## Usage

```clojure
(ns my-project.uses-airtable
  (:require [airtable-clj.core :as airtable]
            [environ.core :refer [env]))

(def api-key "key")
(def base "base")
(def table "My Table")

(def five-records
  (airtable/select {:api-key api-key
                    :base base
                    :table table
                    :max-records 5}))

(def just-one-record
  (airtable/retrieve {:api-key api-key
                      :base base
                      :table table
                      :record-id "rec123"}))

(airtable/create {:api-key api-key
                  :base base
                  :table table
                  :fields {"Foo" "boo"
                           "Bing" "bong"}
                  :typecast? false})
```

## License

Copyright © 2018 Evan Hahn

Distributed under the MIT License.
