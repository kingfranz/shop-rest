(ns shop-rest.recipes
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
  								[items			:refer :all]
  								[lists 			:refer :all]
  								;[menus 		:refer :all]
  								[projects 		:refer :all])
            )
	(:import 	[java.util UUID])
	(:import 	[com.mongodb MongoOptions ServerAddress]))

;;-----------------------------------------------------------------------------

(defn get-recipe-names
	[]
	(ring/response (mc-find-maps "get-recipe-names" recipes {} {:_id true :entryname true})))

(defn get-recipes
	[]
	(->> (mc-find-maps "get-recipes" recipes)
         (s/assert :shop/recipes)
         (ring/response)))

(defn get-recipe
	[id]
	{:pre [(q-valid? :shop/_id id)]}
	(if-let [result (mc-find-one-as-map "get-recipe" recipes {:_id id})]
   		(->> result
             (s/assert :shop/recipe)
             (ring/response))
     	(ring/not-found)))

(defn new-recipe
	[entry]
	{:pre [(q-valid? :shop/recipe* entry)]}
	(add-tags (:tags entry))
	(let [entrynamelc (mk-enlc (:entryname entry))
		  db-entry    (get-by-enlc recipes entrynamelc)
		  entry*      (-> entry
		  			      (merge {:entrynamelc entrynamelc} (mk-std-field))
		  			      (update :entryname str/trim))]
		(if (some? db-entry)
			(ring/response db-entry)
			(do
				(mc-insert "new-recipe" recipes entry*)
				(get-recipe (:_id entry*))))))

(defn update-recipe
	[recipe*]
	{:pre [(q-valid? :shop/recipe* recipe*)]}
	(let [entrynamelc (mk-enlc (:entryname recipe*))
		  recipe (-> recipe*
		  			 (assoc :entrynamelc entrynamelc)
		  			 (update :entryname str/trim))
		  db-entry (get-by-enlc recipes entrynamelc)]
		(if (some? db-entry)
			(if (= (:_id db-entry) (:_id recipe))
				(mc-update-by-id "update-recipe" recipes (:_id recipe)
					{$set (select-keys recipe [:url :items :text])})
				(throw (ex-info "duplicate name" {:cause "dup"})))
			(do
				(mc-update-by-id "update-recipe" recipes (:_id recipe)
					{$set (select-keys recipe [:entryname :entrynamelc :url :items :text])})
				; now update the recipe in menus
				(mc-update "update-recipe" menus {:recipe._id (:_id recipe)}
					{$set {:recipe (select-keys recipe [:_id :entryname])}}
					{:multi true})))
		(get-recipe (:_id recipe))))

