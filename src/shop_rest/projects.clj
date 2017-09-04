(ns shop-rest.projects
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
  								[lists 			:refer :all])
            )
	(:import 	[java.util UUID])
	(:import 	[com.mongodb MongoOptions ServerAddress]))

;;-----------------------------------------------------------------------------

(defn get-projects
	[]
	{:post [(q-valid? :shop/projects %)]}
	(ring/response (mc-find-maps "get-projects" projects {:cleared nil})))

(defn get-active-projects
	[]
	(->> (mc-find-maps "get-active-projects" projects
			{:finished nil}
			{:_id true :entryname true :priority true :tags true})
		 (sort-by :priority)
   		 (ring/response)))

(defn get-project
	[id]
	{:pre [(q-valid? :shop/_id id)]
	 :post [(q-valid? :shop/project %)]}
	(ring/response (mc-find-one-as-map "get-project" projects {:_id id})))

(defn new-project
	[entry]
	{:pre [(q-valid? :shop/project* entry)]
	 :post [(q-valid? :shop/project %)]}
	(add-tags (:tags entry))
	(let [entry* (merge entry (mk-std-field))]
		(mc-insert "new-project" projects entry*)
		(get-project (:_id entry*))))

(defn finish-project
	[project-id]
	{:pre [(q-valid? :shop/_id project-id)]}
	(mc-update-by-id "finish-project" projects project-id {$set {:finished (l/local-now)}})
 	(get-project project-id))

(defn unfinish-project
	[project-id]
	{:pre [(q-valid? :shop/_id project-id)]}
	(mc-update-by-id "unfinish-project" projects project-id {$set {:finished nil}})
 	(get-project project-id))

(defn update-project
	[proj]
	{:pre [(q-valid? :shop/project* proj)]}
	(mc-update-by-id "update-project" projects (:_id proj)
		{$set (select-keys proj [:entryname :priority :finished :tags])})
 	(get-project (:_id proj)))

(defn clear-projects
	[]
	(mc-update clear-projects projects
		{:finished {$type "date"}}
		{$set {:cleared (now)}}
		{:multi true})
 	{:status 204})

