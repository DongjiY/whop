.PHONY: start up down logs

start: up

up:
	docker compose up --build

down:
	docker compose down

logs:
	docker compose logs -f
