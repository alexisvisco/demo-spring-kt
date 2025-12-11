ifneq (,$(wildcard ./.env))
    include .env
    export
endif

db-migrate:
	@./gradlew bootRun --args="db migrate" -x processAot --rerun-tasks

db-rollback:
	@./gradlew bootRun --args="db rollback --count=1" -x processAot --rerun-tasks

db-new-migration:
	@./gradlew bootRun --args="db generate $(name)" -x processAot --rerun-tasks

db-new-migration-kt:
	@./gradlew bootRun --args="db generate $(name) --kt" -x processAot --rerun-tasks

db-status:
	@./gradlew bootRun --args="db status" -x processAot --rerun-tasks
