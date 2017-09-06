(ns shop-rest.items
	(:require 	(clj-time		[core     		:as t]
            					[local    		:as l]
            					[coerce   		:as c]
            					[format   		:as f]
            					[periodic 		:as p])
            	(clojure 		[set      		:as set]
            					[pprint   		:as pp]
            					[string   		:as str])
            	(clojure.spec 	[alpha          :as s])
             	(cheshire 		[core     		:refer :all])
            	(taoensso 		[timbre   		:as log])
            	(monger 		[core     		:as mg]
            					[credentials	:as mcr]
            					[collection 	:as mc]
            					[joda-time  	:as jt]
            					[operators 		:refer :all])
            	(shop-rest		[utils     		:refer :all]
            					[spec       	:refer :all]
                 				[db 			:refer :all]
            			 		[tags 			:refer :all])
            )
	(:import 	[java.util UUID])
	(:import 	[com.mongodb MongoOptions ServerAddress]))

;;-----------------------------------------------------------------------------

(defn get-item-names
	[]
	(->> (mc-find-maps "get-item-names" items {} {:_id true :entryname true})
         (ring/response)))

(defn get-items
	[]
	(->> (mc-find-maps "get-items" items {})
         (s/assert :shop/items)
         (ring/response)))

(defn get-item
	[id]
	{:pre [(q-valid? :shop/_id id)]}
	(if-let [result (mc-find-one-as-map "get-item" items {:_id id})]
         (->> result
              (s/assert :shop/item)
              (ring/response))
         (ring/not-found)))

(defn new-item
	[entry]
	{:pre [(q-valid? :shop/item* entry)]}
	(add-tags (:tags entry))
	(let [entry* (as-> entry $
                       (merge $ (mk-std-field))
                       (assoc $ :entrynamelc (mk-enlc (:entryname $))))]
		(mc-insert "add-item" items entry*)
		(add-item-usage nil (:_id entry*) :create 0)
		(get-item (:_id entry*))))

(defn update-item
	[entry]
	{:pre [(q-valid? :shop/item* entry)]}
	(add-item-usage nil (:_id entry) :update 0)
	(let [entry* (assoc entry :entrynamelc (mk-enlc (:entryname entry)))]
   		(mc-update-by-id "update-item" items (:_id entry*)
			{$set (select-keys entry* [:entryname :entrynamelc :unit :url
                             		   :amount :price :tags :parent])})
     	(get-item (:_id entry*))))

(defn delete-item
	[item-id]
	{:pre [(q-valid? :shop/_id item-id)]}
	(add-item-usage nil item-id :delete 0)
	(mc-remove-by-id "delete-item" items item-id)
 	(-> (ring/response {})
        (ring/status 204)))

