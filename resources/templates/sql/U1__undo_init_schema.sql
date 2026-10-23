-- ==============================================================
-- Rollback / Undo: U1__undo_init_schema.sql
-- Description: Rollback script for V1__init_schema.sql
-- ==============================================================

DROP TABLE IF EXISTS users CASCADE;
DROP TABLE IF EXISTS schema_version CASCADE;
