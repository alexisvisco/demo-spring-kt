
DATABASE_URL="postgres://postgres:postgres@localhost:6789/postgres?sslmode=disable"

db-migrate:
	dbmate --url $(DATABASE_URL) up

db-rollback:
	dbmate --url $(DATABASE_URL) rollback

db-new-migration:
	dbmate new $(name)
