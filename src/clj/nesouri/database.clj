(ns nesouri.database
  (:require [black.water.korma :refer [decorate-korma!]]
            [korma.core :refer :all]
            [korma.db :refer :all]))

;; monkey-patch broken grouping when joining belongs-to
(in-ns 'korma.sql.utils)
(defn left-assoc [vs]
  (loop [ret "" [v & vs] vs]
    (cond
      (nil? v) ret
      (nil? vs) (str ret v)
      :else (recur (str ret v) vs))))
(in-ns 'nesouri.database)

;(decorate-korma!)

(defdb db (sqlite3 {:db "/home/daniel/Development/home/nesouri-db/metadata.db"}))

(declare authors games systems tracks)

(defentity credits)

;; (fields :_id :name :abbrev :url)
(defentity systems
  (pk :system_id)
  (entity-fields [:name :system]))

;; (fields :person_id :name)
(defentity persons
  (pk :person_id)
  (many-to-many games :credits {:lfk :person_id
                                :rfk :game_id}))

(defentity developers
  (table :organizations :developers)
  (pk :organization_id)
  (entity-fields [:name :developer]))

(defentity publishers
  (table :organizations :publishers)
  (pk :organization_id)
  (entity-fields [:name :publisher]))

(defentity tracks
  (belongs-to games {:fk :game_id})
  (entity-fields :track :title))

(defentity game_links
  (table :webpages_games_view :game_links)
  (entity-fields [:name :name] [:url :url]))

(defentity games
  (pk :game_id)
  (belongs-to systems {:fk :system_id})
  (has-many tracks {:fk :game_id })
  (has-many game_links {:fk :game_id})
  (belongs-to developers {:fk :developer_id})
  (belongs-to publishers {:fk :publisher_id})
  (many-to-many persons :credits {:lfk :game_id
                                  :rfk :person_id}))

(defentity releases
  (table :games :releases)
  (belongs-to publishers {:fk :publisher_id})
  (entity-fields :game_id :title :date :year :publisher_id))

(defentity publications
  (table :games :publications)
  (belongs-to developers {:fk :developer_id})
  (entity-fields :game_id :title :date :year :developer_id))

(defentity organizations
  (pk :organization_id)
  (has-many releases {:fk :developer_id})
  (has-many publications {:fk :publisher_id}))


(defn list-games [lim off]
  (select games (fields :game_id :title)
          (order :title)
          (limit lim) (offset off)))

(defn show-artist [artist_id]
  (let [result (first (select persons
                              (with games (fields :game_id :title :year :date :developer_id :publisher_id)
                                    (order :year) (order :date)
                                    (with developers)
                                    (with publishers))
                              (where {:persons.person_id artist_id})))]
    (when-not result
      (throw (IllegalArgumentException. (str "artist_id '" artist_id "' not found."))))
    result))

(defn show-organization [organization_id]
  (let [result (first (select organizations
                              (with releases (order :date)
                                    (with publishers))
                              (with publications (order :date)
                                    (with developers))
                              (where {:organization_id organization_id})))]
    result))

(defn show-game [game_id]
  (let [result (first (select games
                              (with systems)
                              (with persons)
                              (with developers)
                              (with publishers)
                              (with tracks (order :track))
                              (with game_links)
                              (where {:games.game_id game_id})))
        ;; Nasty hack.. prehaps possible to step through tracks
        ;; and inject missing ones in the stream as they are ordered
        pad-missing (fn [tracks total-tracks]
                      (for [x (range 1 (+ total-tracks 1))]
                        (or (first (filter #(= x (:track %)) tracks))
                            { :track x :title "Unknown"})))]
    (when-not result
      (throw (IllegalArgumentException. (str "game_id '" game_id "' not found."))))
    (update-in result [:tracks] pad-missing (:total_tracks result))))

(defn random-track []
  ;; No support for ORDER BY random() in Korma
  (let [total-games (:count  (first (select games
                                      (aggregate (count :game_id) :count))))
        game_id (+ (rand-int total-games) 1)
        total-tracks (:total_tracks (first (select games
                                                   (fields :total_tracks)
                                                   (where {:game_id game_id}))))
        track_id (+ (rand-int total-tracks) 1)]
    {:game_id game_id :track track_id}))
