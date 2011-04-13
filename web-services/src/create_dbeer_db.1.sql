
create table database_versions(pkuid integer primary key autoincrement,
                date integer not null,
                comments text
                );
insert into database_versions (date, comments) values (datetime('now'), "initial revision");

-- keeps track of data imports for us.
create table data_updates(pkuid integer primary key autoincrement,
                date integer not null,
                username text,
                bars_created integer,
                bars_removed integer,
                bars_modified integer,
                source_file text,
                status text
                );

-- bars table, imported from osm, and added to by my own users
CREATE TABLE bars(PKUID integer primary key autoincrement,
                name text not null,
                osmid integer,
                type text,
                created integer not null,
                updated integer,
                deleted integer,
                geometry blob not null
                );
SELECT RecoverGeometryColumn('bars', 'geometry', 4326, 'POINT', 2);
create index idx_bar_osmid on bars(osmid);

-- pricings, entirely my own.
CREATE TABLE pricings(PKUID integer primary key autoincrement,
                date integer not null,
                barid integer not null,
                drink_type integer not null,
                price real not null,
                geometry blob not null,
                host text,
                user_agent text,
                userid text
                );
SELECT RecoverGeometryColumn('pricings', 'geometry', 4326, 'POINT', 2);
create index idx_pricing_barid on pricings(barid);