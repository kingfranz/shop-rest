(ns shop-rest.menus
	(:require 	(clj-time		[core     		:as t]
            					[local    		:as l]
            					[coerce   		:as c]
            					[format   		:as f]
            					[periodic 		:as p])
            	(clojure 		[set      		:as set]
            					[pprint   		:as pp]
            					[string   		:as str])
            	(ring.util	 	[response 		:as ring])
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
  								[projects 		:refer :all]
  								[recipes 		:refer :all])
            )
	(:import 	[java.util UUID])
	(:import 	[com.mongodb MongoOptions ServerAddress]))

;;-----------------------------------------------------------------------------

(defn- fix-date
	[m]
	(update m :date #(->> % c/to-date c/from-date)))

(defn- get-menu-by-id
  	[id]
   	(ring/response (mc-find-map-by-id "get-menu-by-id" menus id)))

(defn- get-menu-by-dt
  	[dt]
   	(ring/response (mc-find-one-as-map "get-menu-by-dt" menus {:date dt})))

(defn new-menu
	[entry]
	{:pre [(q-valid? :shop/menu* entry)]
	 :post [(q-valid? :shop/menu %)]}
	(let [entry* (merge entry (mk-std-field))]
		(mc-insert "add-menu" menus entry*)
		(get-menu-by-id (:_id entry*))))

(defn update-menu
	[entry]
	{:pre [(q-valid? :shop/menu entry)]}
	(mc-update-by-id "update-menu" menus (:_id entry)
		{$set (select-keys entry [:entryname :date :tags :recipe])})
 	(get-menu-by-id (:_id entry)))

(defn add-recipe-to-menu
	[menu-dt recipe-id]
	{:pre [(q-valid? :shop/date menu-dt) (q-valid? :shop/_id recipe-id)]}
	(let [recipe (get-recipe recipe-id)]
		(mc-update "add-recipe-to-menu" menus {:date menu-dt}
			{$set {:recipe (select-keys recipe [:_id :entryname])}})
  		(get-menu-by-dt menu-dt)))

(defn remove-recipe-from-menu
	[menu-dt]
	{:pre [(q-valid? :shop/date menu-dt)]}
	(mc-update "remove-recipe-from-menu" menus {:date menu-dt} {$unset {:recipe nil}})
 	(get-menu-by-dt menu-dt))

(defn get-menus
	[from to]
	{:pre [(q-valid? :shop/date from)
           (q-valid? :shop/date to)]}
	(let [db-menus* (mc-find-maps "get-menus" menus {:date {$gte from $lt to}})
		  db-menus  (map fix-date db-menus*)
		  new-menus (set/difference (set (time-range from to (t/days 1)))
		  	                        (set (map :date db-menus)))]
   		(->> new-menus
          	 (map (fn [dt] {:date dt}))
             (concat db-menus)
             (sort-by :date)
             (s/assert :shop/x-menus)
			 (ring/response))))

