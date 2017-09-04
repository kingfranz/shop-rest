(ns shop-rest.spec
  	(:require 	(clj-time 		[core     :as t]
            					[local    :as l]
            					[coerce   :as c]
            					[format   :as f]
            					[periodic :as p])
            	(clojure.spec 	[alpha    :as s])
             	(clojure 		[string   :as str]
            					[set      :as set])))

;;-----------------------------------------------------------------------------

; "242be596-a391-4405-a1c0-fa7c3a1aa5c9"
(defonce uuid-regex
	#"\p{XDigit}{8}-\p{XDigit}{4}-\p{XDigit}{4}-\p{XDigit}{4}-\p{XDigit}{12}")

(s/def :shop/string    		(and string? seq))
(s/def :shop/strings   		(s/* :shop/string))
(s/def :shop/date      		#(instance? org.joda.time.DateTime %))
(s/def :shop/_id       		#(and (string? %) (re-matches uuid-regex %)))
(s/def :shop/created   		:shop/date)
(s/def :shop/entryname 		(and string? seq))
(s/def :shop/entrynamelc 	(and string? seq))
(s/def :shop/numof     		(and int? pos?))
(s/def :shop/parent    		(s/nilable :shop/_id))
(s/def :list/parent    		(s/nilable (s/keys :req-un [:shop/_id :shop/entryname :list/parent])))
(s/def :list/kast    		boolean?)
(s/def :shop/amount    		(s/nilable number?))
(s/def :shop/unit      		(s/nilable string?))
(s/def :shop/price     		(s/nilable number?))
(s/def :shop/text      		(s/nilable string?))
(s/def :menu/recipe    		(s/keys :req-un [:shop/_id :shop/entryname]))
(s/def :recipe/item    		(s/keys :req-un [:shop/text] :opt-un [:shop/unit :shop/amount]))
(s/def :recipe/items   		(s/* :recipe/item))
(s/def :shop/priority  		(s/int-in 1 6))
(s/def :shop/finished  		(s/nilable :shop/date))
(s/def :shop/url       		(s/nilable string?))
(s/def :shop/cleared   		(s/nilable :shop/date))
(s/def :shop/std-keys  		(s/keys :req-un [:shop/_id :shop/created]))
(s/def :shop/username  		:shop/string)
(s/def :shop/roles     		(s/every keyword? :kind set?))
(s/def :shop/password  		:shop/string)
(s/def :shopuser/created 	:shop/string)
(s/def :shopuser/properties (s/map-of keyword? map?))

;;-----------------------------------------------------------------------------

(s/def :shop/list*   (s/keys :req-un [:shop/entryname]
							 :opt-un [:shop/items :list/parent :list/last]))

(s/def :shop/list    (s/merge :shop/list* :shop/std-keys))
(s/def :shop/lists*  (s/* :shop/list*))
(s/def :shop/lists   (s/* :shop/list))

;;-----------------------------------------------------------------------------

(s/def :shop/item*   (s/keys :req-un [:shop/entryname]
							 :opt-un [:shop/tags :shop/finished :shop/numof :shop/url
							 		  :shop/amount :shop/unit :shop/price
							 		  :shop/parent]))

(s/def :shop/item    (s/merge :shop/item* :shop/std-keys :shop/entrynamelc))
(s/def :shop/items*  (s/* :shop/item*))
(s/def :shop/items   (s/* :shop/item))

;;-----------------------------------------------------------------------------

(s/def :shop/menu*     (s/keys :req-un [:shop/entryname :shop/date]
							   :opt-un [:menu/recipe]))

(s/def :shop/menu      (s/merge :shop/menu* :shop/std-keys))
(s/def :shop/menus*    (s/* :shop/menu*))
(s/def :shop/menus     (s/* :shop/menu))
(s/def :shop/fill-menu (s/keys :req-un [:shop/date]))
(s/def :shop/x-menus   (s/+ (s/or :full :shop/menu :fill :shop/fill-menu)))

;;-----------------------------------------------------------------------------

(s/def :shop/project*   (s/keys :req-un [:shop/entryname :shop/priority]
						        :opt-un [:shop/finished :shop/tags :shop/cleared]))

(s/def :shop/project    (s/merge :shop/project* :shop/std-keys))
(s/def :shop/projects*  (s/* :shop/project*))
(s/def :shop/projects   (s/* :shop/project))

;;-----------------------------------------------------------------------------

(s/def :shop/recipe*  (s/keys :req-un [:shop/entryname]
							  :opt-un [:recipe/items :shop/url :shop/text]))

(s/def :shop/recipe   (s/merge :shop/recipe* :shop/std-keys :shop/entrynamelc))
(s/def :shop/recipes* (s/* :shop/recipe*))
(s/def :shop/recipes  (s/* :shop/recipe))

;;-----------------------------------------------------------------------------

(s/def :shop/tag*  (s/keys :req-un [:shop/entryname]))

(s/def :shop/tag   (s/merge :shop/tag* :shop/std-keys :shop/entrynamelc))
(s/def :shop/tags* (s/* :shop/tag*))
(s/def :shop/tags  (s/* :shop/tag))

;;-----------------------------------------------------------------------------

(s/def :shop/user    (s/keys :req-un [:shop/_id :shopuser/created
									  :shop/username :shop/roles]
							 :opt-un [:shopuser/properties]))
(s/def :shop/user-db (s/merge :shop/user :shop/password))
