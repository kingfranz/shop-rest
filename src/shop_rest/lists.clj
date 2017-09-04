(ns shop-rest.lists
	(:require 	(clj-time		[core     		:as t]
            					[local    		:as l]
            					[coerce   		:as c]
            					[format   		:as f]
            					[periodic 		:as p])
            	(clojure 		[set      		:as set]
            					[pprint   		:as pp]
            					[string   		:as str])
            	(ring.util 		[response 		:as ring])
            	(clojure.spec 	[alpha          :as s])
             	(cheshire 		[core     		:refer :all])
            	(taoensso 		[timbre   		:as log])
            	(monger 		[core     		:as mg]
            					[credentials 	:as mcr]
            					[collection 	:as mc]
            					[joda-time  	:as jt]
            					[operators 		:refer :all])
            	(shop-rest		[utils       	:refer :all]
            					[spec       	:refer :all]
            					[db 			:refer :all]
            					[tags 			:refer :all]
  								[items			:refer :all])
            )
	(:import 	[java.util UUID])
	(:import 	[com.mongodb MongoOptions ServerAddress]))

;;-----------------------------------------------------------------------------

(defn get-list
	[listid]
	{:pre [(q-valid? :shop/_id listid)]}
	(->> (mc-find-one-as-map "get-list" lists {:_id listid})
      	 (s/assert :shop/list)
      	 (ring/response)))

(defn get-lists
	[]
	(->> (mc-find-maps "get-lists" lists)
      	 (s/assert :shop/lists)
      	 (ring/response)))

(defn get-list-names
	[]
	(->> (mc-find-maps "get-list-names" lists {} {:_id true :entryname true})
      	 (s/assert :shop/strings)
         (ring/response)))

(defn new-list
	[entry]
	{:pre [(q-valid? :shop/list* entry)]}
	(let [entry* (merge entry (mk-std-field))]
		(mc-insert "add-list" lists entry*)
		(get-list (:_id entry*))))

(defn update-list
	[entry]
	{:pre [(q-valid? :shop/list* entry)]}
	(mc-update-by-id "update-list" lists (:_id entry)
		{$set (select-keys entry [:entryname :parent :last])})
 	(get-list (:_id entry)))

(defn delete-list
	[list-id]
	{:pre [(q-valid? :shop/_id list-id)]}
	(mc-remove-by-id "delete-list" lists list-id)
	(doseq [mlist (mc-find-maps "delete-list" lists {} {:_id true :parent true})
	  :let [np (some->> mlist :parent :parent)]
	  :when (= (some->> mlist :parent :_id) list-id)]
		(mc-update-by-id "delete-list" lists (:_id mlist) {$set {:parent np}}))
 	{:status 204})

(defn get-lists-with-count
	[]
	(ring/response (mc-aggregate "get-lists-with-count" lists
		[{$project {:_id true
		 		    :entryname true
		 		    :parent true
         			:last true
		 		    :count {
		 		    	$cond [{$gt ["$items" nil]}
					           {$size {
					           		"$filter" {
					           			:input "$items"
					                    :as "item"
					                    :cond {$not [{$gt ["$$item.finished" nil]}]}}}}
					           0]}}}])))

(defn finish-list-item
	[list-id item-id]
	{:pre [(q-valid? :shop/_id list-id) (q-valid? :shop/_id item-id)]}
	(add-item-usage list-id item-id :finish 0)
	(mc-update "finish-list-item" lists
		{:_id list-id :items._id item-id}
		{$set {:items.$.finished (l/local-now)}})
 	(get-list list-id))

(defn unfinish-list-item
	[list-id item-id]
	{:pre [(q-valid? :shop/_id list-id) (q-valid? :shop/_id item-id)]}
	(add-item-usage list-id item-id :unfinish 0)
	(mc-update "unfinish-list-item" lists
		{:_id list-id :items._id item-id}
		{$set {:items.$.finished nil}})
 	(get-list list-id))

(defn del-finished-list-items
	[list-id]
	{:pre [(q-valid? :shop/_id list-id)]}
	(let [a-list (get-list list-id)
		  clean (remove #(some? (:finished %)) (:items a-list))]
		(mc-update-by-id "del-finished-list-items" lists list-id
			{$set {:items clean}}))
 	(get-list list-id))

(defn- remove-item
	[list-id item-id]
	(add-item-usage list-id item-id :remove 0)
	(mc-update "remove-item" lists
		{:_id list-id}
		{$pull {:items {:_id item-id}}}))

(defn- mod-item
	[list-id item-id num-of]
	(add-item-usage list-id item-id :mod num-of)
	(mc-update "mod-item" lists
		{:_id list-id :items._id item-id}
		{$inc {:items.$.numof num-of}}))

(defn find-list-by-name
	[e-name]
	{:pre [(q-valid? :shop/string e-name)]}
	(->> (mc-find-one-as-map "find-list-by-name" lists {:entryname e-name})
         (s/assert :shop/list)
         (ring/response)))

(defn- find-item
	[list-id item-id]
	{:pre [(q-valid? :shop/_id list-id)
		   (q-valid? :shop/_id item-id)]}
	(some->> (mc-find-one-as-map "find-item" lists {:_id list-id :items._id item-id} {:items.$ 1})
			 :items
			 first))

(defn- list-id-exists?
	[id]
	{:pre [(q-valid? :shop/_id id)]}
	(ring/response (= (get (mc-find-map-by-id "list-id-exists?" lists id {:_id true}) :_id) id)))

(defn item->list
	[list-id item-id num-of]
	{:pre [(q-valid? :shop/_id list-id)
		   (q-valid? :shop/_id item-id)
		   (q-valid? int? num-of)]}
	; make sure it's a valid list
	(when-not (list-id-exists? list-id)
		(throw (ex-info "unknown list" {:cause :invalid})))
	; find the item if it's already in the list
	(if-let [item (find-item list-id item-id)]
		; yes it was
		(do
			(if (or (zero? num-of) (<= (+ (:numof item) num-of) 0))
				(finish-list-item list-id item-id)
				(do
      				(when (some? (:finished item))
            			(unfinish-list-item list-id item-id))
      				(mod-item list-id item-id num-of))))
		; no, we need to add it
		(when (pos? num-of)
			(add-item-usage list-id item-id :add-to num-of)
			(mc-update-by-id "item->list" lists list-id
				{$addToSet {:items (assoc (get-item item-id) :numof num-of)}})))
 	(get-list list-id))

