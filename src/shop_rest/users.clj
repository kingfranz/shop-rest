(ns shop-rest.users
	(:require 	(shop-rest			[utils			:refer :all]
                          			[db 			:refer :all]
                             		[spec       	:refer :all])
   				(clj-time			[core     		:as t]
            						[local    		:as l]
            						[coerce   		:as c]
            						[format   		:as f]
            						[periodic 		:as p])
            	(clojure 			[set      		:as set]
            						[pprint   		:as pp]
            						[string   		:as str])
            	(ring.util	 		[response 		:as ring])
            	(clojure.spec 		[alpha          :as s])
             	(cheshire 			[core     		:refer :all])
            	(taoensso 			[timbre   		:as log])
				(cemerick 			[friend      	:as friend])
            	(cemerick.friend 	[workflows 	 	:as workflows]
                             		[credentials 	:as creds])
            	(monger 			[core     		:as mg]
            						[credentials 	:as mcr]
            						[collection 	:as mc]
            						[joda-time  	:as jt]
            						[operators 		:refer :all])
            ))

;;-----------------------------------------------------------------------------

(defn get-user
	[uname]
 	{:pre [(q-valid? :shop/username uname)]}
  	;(log/trace "get-user")
	(let [udata (mc-find-one-as-map "get-user" users
					{:username {$regex (str "^" (str/trim uname) "$") $options "i"}})]
		(if (seq udata)
			(as-> udata $
				  (update $ :roles #(->> % (map keyword) set))
				  (update $ :created str)
    			  (s/assert (s/nilable :shop/user-db) $)
    			  (ring/response $))
   			(ring/not-found))))

;;-----------------------------------------------------------------------------

(defn get-user-by-id
	[uid]
 	{:pre [(q-valid? :shop/_id uid)]}
	;(log/trace "get-user-by-id")
	(let [udata (mc-find-map-by-id "get-user-by-id" users uid)]
		(if (seq udata)
			(as-> udata $
				  (update $ :roles #(->> % (map keyword) set))
				  (update $ :created str)
    			  (s/assert (s/nilable :shop/user-db) $)
    			  (ring/response $))
   			(ring/not-found))))

;;-----------------------------------------------------------------------------

(defn get-users
	[]
 	(->> (mc-find-maps "get-users" users {})
		 (map (fn [u] (update u :roles #(->> % (map keyword) set))))
		 (map (fn [u] (update u :created str)))
   		 (s/assert (s/* :shop/user-db))
   		 (ring/response)))

;;-----------------------------------------------------------------------------

(defn- count-chars
	[pw c-class]
	(if (nil? (re-find c-class pw))
		(throw (ex-info (str "PW must contain at least one of " c-class) {:cause :password}))
		pw))

(defn- verify-passwd
	[pw]
	(if (not= (str/trim pw) pw)
		(throw (ex-info "PW can't begin or end with space" {:cause :password}))
		(if (< (count pw) 8)
			(throw (ex-info "PW must be 8 chars or more" {:cause :password}))
			(-> pw
				(count-chars #"[a-zåäö]")
				(count-chars #"[A-ZÅÄÖ]")
				(count-chars #"[0-9]")
				(count-chars #"[.*!@#$%^&()=+-]")
				))))

(defn create-user
	[username passwd roles]
 	{:pre [(q-valid? :shop/username username)
           (q-valid? :shop/password passwd)
           (q-valid? :shop/roles roles)]}
	(when (some? (get-user username))
		(throw (ex-info "duplicate username" {:cause :username})))
	(let [user (merge {:username (str/trim username)
					   :password (creds/hash-bcrypt (verify-passwd passwd))
					   :roles    roles} (mk-std-field))]
		(mc-insert "create-user" users user)
		(get-user-by-id (:_id user))))

;;-----------------------------------------------------------------------------

(defn set-user-password
	[uid passwd]
 	{:pre [(q-valid? :shop/_id uid)
           (q-valid? :shop/password passwd)]}
	(mc-update-by-id "set-user-password" users uid
		{$set {:password (creds/hash-bcrypt (verify-passwd passwd))}})
 	(get-user-by-id uid))

;;-----------------------------------------------------------------------------

(defn set-user-roles
	[uid roles]
 	{:pre [(q-valid? :shop/_id uid)
           (q-valid? :shop/roles roles)]}
	(mc-update-by-id "set-user-roles" users uid
    	{$set {:roles roles}})
 	(get-user-by-id uid))

;;-----------------------------------------------------------------------------

(defn set-user-property
	[uid prop-key prop-val]
 	{:pre [(q-valid? :shop/_id uid)
           (q-valid? keyword? prop-key)
           (q-valid? map? prop-val)]}
	(mc-update-by-id "set-user-property" users uid
		{$set {:properties {prop-key prop-val}}})
 	(get-user-by-id uid))

;;-----------------------------------------------------------------------------

;{:us u :pw p :properties {:home {:list-type :tree} :lists {}}}
