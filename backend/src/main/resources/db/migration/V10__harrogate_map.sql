-- Seed data: Harrogate landmarks map (15 streets). Coordinates are best-effort approximations —
-- verify/fine-tune against a real map before relying on them for GPS proximity checks.
-- image_clue_url is left NULL: upload a photo per street via the admin app's existing
-- "Upload Image" flow (POST /api/images/upload) once real, appropriately-licensed photos are sourced.

INSERT INTO public.game_map
(id, "name", created_at, updated_at, postcode_area)
VALUES('1a7cc5ee-17a3-4fce-a74d-7aa2493149dc'::uuid, 'Harrogate', now(), now(), 'HG1');

INSERT INTO public.street
(id, game_map_id, "name", price, rental_price, colour, latitude, longitude, image_clue_url, created_at)
VALUES('7b635b4b-bb9d-49a6-b9c5-7fd7cf72b484'::uuid, '1a7cc5ee-17a3-4fce-a74d-7aa2493149dc'::uuid, 'The Stray', 200.00, 70.00, 'brown', 53.9900, -1.5340, NULL, now());
INSERT INTO public.street
(id, game_map_id, "name", price, rental_price, colour, latitude, longitude, image_clue_url, created_at)
VALUES('623015e1-b9f7-458b-b7e4-97bfaaa6623d'::uuid, '1a7cc5ee-17a3-4fce-a74d-7aa2493149dc'::uuid, 'Tewit Well', 200.00, 70.00, 'brown', 53.9887, -1.5300, NULL, now());
INSERT INTO public.street
(id, game_map_id, "name", price, rental_price, colour, latitude, longitude, image_clue_url, created_at)
VALUES('59054658-fb7c-4ca6-b949-094a60114460'::uuid, '1a7cc5ee-17a3-4fce-a74d-7aa2493149dc'::uuid, 'St Wilfrid''s Church', 300.00, 100.00, 'light_blue', 53.9989, -1.5478, NULL, now());
INSERT INTO public.street
(id, game_map_id, "name", price, rental_price, colour, latitude, longitude, image_clue_url, created_at)
VALUES('92879c99-e541-4e9c-b802-27ed7fff71b8'::uuid, '1a7cc5ee-17a3-4fce-a74d-7aa2493149dc'::uuid, 'Montpellier Hill', 300.00, 100.00, 'light_blue', 53.9919, -1.5456, NULL, now());
INSERT INTO public.street
(id, game_map_id, "name", price, rental_price, colour, latitude, longitude, image_clue_url, created_at)
VALUES('a5b33381-1b28-4094-9270-26f92dd967e6'::uuid, '1a7cc5ee-17a3-4fce-a74d-7aa2493149dc'::uuid, 'Montpellier Quarter', 400.00, 130.00, 'pink', 53.9917, -1.5451, NULL, now());
INSERT INTO public.street
(id, game_map_id, "name", price, rental_price, colour, latitude, longitude, image_clue_url, created_at)
VALUES('b275ec84-30f1-4c82-bb03-ad3ec071201b'::uuid, '1a7cc5ee-17a3-4fce-a74d-7aa2493149dc'::uuid, 'Cenotaph', 400.00, 130.00, 'pink', 53.9918, -1.5391, NULL, now());
INSERT INTO public.street
(id, game_map_id, "name", price, rental_price, colour, latitude, longitude, image_clue_url, created_at)
VALUES('b7b747e9-ee77-4c57-b202-7b8fbfa3c232'::uuid, '1a7cc5ee-17a3-4fce-a74d-7aa2493149dc'::uuid, 'Victoria Shopping Centre', 500.00, 170.00, 'orange', 53.9920, -1.5423, NULL, now());
INSERT INTO public.street
(id, game_map_id, "name", price, rental_price, colour, latitude, longitude, image_clue_url, created_at)
VALUES('f54acd45-c2cb-4fc3-b52f-f93d1d5ec206'::uuid, '1a7cc5ee-17a3-4fce-a74d-7aa2493149dc'::uuid, 'Harrogate Theatre', 500.00, 170.00, 'orange', 53.9924, -1.5389, NULL, now());
INSERT INTO public.street
(id, game_map_id, "name", price, rental_price, colour, latitude, longitude, image_clue_url, created_at)
VALUES('1c3bd89c-df9a-4408-b5bb-a945f6e17e22'::uuid, '1a7cc5ee-17a3-4fce-a74d-7aa2493149dc'::uuid, 'Valley Gardens', 600.00, 200.00, 'red', 53.9891, -1.5449, NULL, now());
INSERT INTO public.street
(id, game_map_id, "name", price, rental_price, colour, latitude, longitude, image_clue_url, created_at)
VALUES('bf139318-3e4a-4fad-898b-2a15c303acde'::uuid, '1a7cc5ee-17a3-4fce-a74d-7aa2493149dc'::uuid, 'Mercer Art Gallery', 600.00, 200.00, 'red', 53.9942, -1.5418, NULL, now());
INSERT INTO public.street
(id, game_map_id, "name", price, rental_price, colour, latitude, longitude, image_clue_url, created_at)
VALUES('a4fac65e-ffa9-4e17-a064-8b6f037c7c89'::uuid, '1a7cc5ee-17a3-4fce-a74d-7aa2493149dc'::uuid, 'Harrogate Station', 700.00, 230.00, 'yellow', 53.9928, -1.5412, NULL, now());
INSERT INTO public.street
(id, game_map_id, "name", price, rental_price, colour, latitude, longitude, image_clue_url, created_at)
VALUES('a078bc9d-dc6f-46c1-ac8c-095d39768725'::uuid, '1a7cc5ee-17a3-4fce-a74d-7aa2493149dc'::uuid, 'Royal Hall', 700.00, 230.00, 'yellow', 53.9946, -1.5382, NULL, now());
INSERT INTO public.street
(id, game_map_id, "name", price, rental_price, colour, latitude, longitude, image_clue_url, created_at)
VALUES('a6f7622c-34e5-432f-b2dc-080438605223'::uuid, '1a7cc5ee-17a3-4fce-a74d-7aa2493149dc'::uuid, 'Turkish Baths', 800.00, 270.00, 'green', 53.9929, -1.5403, NULL, now());
INSERT INTO public.street
(id, game_map_id, "name", price, rental_price, colour, latitude, longitude, image_clue_url, created_at)
VALUES('4198ef77-e8a7-404d-b65f-bb8924af8c2e'::uuid, '1a7cc5ee-17a3-4fce-a74d-7aa2493149dc'::uuid, 'Royal Pump Room', 800.00, 270.00, 'green', 53.9925, -1.5427, NULL, now());
INSERT INTO public.street
(id, game_map_id, "name", price, rental_price, colour, latitude, longitude, image_clue_url, created_at)
VALUES('e00d49f2-c71b-4616-a83d-10cd3992ce6c'::uuid, '1a7cc5ee-17a3-4fce-a74d-7aa2493149dc'::uuid, 'Bettys', 900.00, 300.00, 'dark_blue', 53.9915, -1.5398, NULL, now());
