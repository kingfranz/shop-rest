(ns shop-rest.db
	(:require 	(shop-rest			[utils			:refer :all]
                          			[spec       	:as spec])
   				(clj-time			[core     		:as t]
            						[local    		:as l]
            						[coerce   		:as c]
            						[format   		:as f]
            						[periodic 		:as p])
            	(clojure 			[set      		:as set]
            						[pprint   		:as pp]
            						[string   		:as str])
            	(clojure.spec 		[alpha          :as s])
             	(cheshire 			[core     		:refer :all])
				(cemerick 			[friend      	:as friend])
            	(cemerick.friend 	[workflows 	 	:as workflows]
                             		[credentials 	:as creds])
            	(taoensso 			[timbre   		:as log])
            	(monger 			[core     		:as mg]
            						[credentials 	:as mcr]
            						[collection 	:as mc]
            						[joda-time  	:as jt]
            						[operators 		:refer :all])
             	(environ 			[core 			:refer [env]])
            )
	(:import 	[java.util UUID])
	(:import 	[com.mongodb MongoOptions ServerAddress]))

;;-----------------------------------------------------------------------------

; mongo --port 27017 -u "mongoadmin" -p "Benq.fp731" --authenticationDatabase "admin"
; db.createUser({user:"shopper",pwd:"kAllE.kUlA399",roles:[{role:"readWrite",db:"shopdb"}]})

(defonce db-conn (mg/connect-with-credentials (env :database-ip)
							(mcr/create (env :database-user)
                   						(env :database-db)
                         				(env :database-pw)
                             )))
(defonce shopdb (mg/get-db db-conn (env :database-db)))

(defonce sessions   "sessions")
(defonce item-usage "item-usage")
(defonce users      "users")
(defonce recipes    "recipes")
(defonce menus      "menus")
(defonce items      "items")
(defonce projects   "projects")
(defonce lists      "lists")
(defonce tags       "tags")

;;-----------------------------------------------------------------------------

(defn mk-id
	[]
 	{:post [(q-valid? :shop/_id %)]}
	(str (java.util.UUID/randomUUID)))

(defn mk-std-field
	[]
 	{:post [(q-valid? :shop/std-keys %)]}
	{:_id (mk-id) :created (now)})

;;-----------------------------------------------------------------------------

;monger.collection$find_one_as_map@5f2b4e24users
(defn fname
	[s]
 	{:post [(q-valid? :shop/string %)]}
	(second (re-matches #"^[^$]+\$(.+)@.+$" (str s))))

(defn- do-mc
	[mc-func caller tbl & args]
	(log/trace (apply str caller ": " (fname mc-func) " " tbl " " (first args)))
	(let [ret (apply mc-func shopdb tbl (first args))]
		(log/trace caller "returned:" (pr-str ret))
		ret))

(defn mc-aggregate
	[func tbl & args]
	(do-mc mc/aggregate func tbl args))

(defn mc-find-maps
	[func tbl & args]
	(do-mc mc/find-maps func tbl args))

(defn mc-find-one-as-map
	[func tbl & args]
	(do-mc mc/find-one-as-map func tbl (vec args)))

(defn mc-find-map-by-id
	[func tbl & args]
	(do-mc mc/find-map-by-id func tbl args))

(defn mc-insert
	[func tbl & args]
	(do-mc mc/insert func tbl args))

(defn mc-insert-batch
	[func tbl & args]
	(do-mc mc/insert-batch func tbl args))

(defn mc-update
	[func tbl & args]
	(do-mc mc/update func tbl args))

(defn mc-update-by-id
	[func tbl & args]
	(do-mc mc/update-by-id func tbl args))

(defn mc-remove-by-id
	[func tbl & args]
	(do-mc mc/remove-by-id func tbl args))

;;-----------------------------------------------------------------------------

(defn mk-enlc
	[en]
 	{:pre [(q-valid? :shop/string en)]
     :post [(q-valid? :shop/string %)]}
	(-> en str/trim str/lower-case (str/replace #"[ \t-]+" " ")))

(defn get-by-enlc
	[tbl en]
 	{:pre [(q-valid? :shop/string tbl) (q-valid? :shop/string en)]
     :post [(q-valid? (s/nilable map?) %)]}
	(mc-find-one-as-map "get-by-enlc" tbl {:entrynamelc en}))

;;-----------------------------------------------------------------------------

(defn add-item-usage
	[list-id item-id action numof]
 	{:pre [(q-valid? (s/nilable :shop/_id) list-id)
           (q-valid? :shop/_id item-id)
           (q-valid? keyword? action)
           (q-valid? number? numof)]}
	(mc-insert "add-item-usage" item-usage
		(merge {:listid list-id :itemid item-id :action action :numof numof}
			   (mk-std-field))))

;;-----------------------------------------------------------------------------

