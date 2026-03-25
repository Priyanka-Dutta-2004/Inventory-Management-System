-- =========================================
-- Inventory Management System Database
-- PostgreSQL Schema Reference
-- =========================================

-- ENUM TYPES

CREATE TYPE user_status AS ENUM ('ACTIVE', 'INACTIVE');

CREATE TYPE request_status AS ENUM (
    'PENDING',
    'APPROVED',
    'REJECTED'
);

CREATE TYPE asset_status AS ENUM (
    'AVAILABLE',
    'IN_USE',
    'UNDER_MAINTENANCE',
    'DECOMMISSIONED'
);

CREATE TYPE ticket_status AS ENUM (
    'OPEN',
    'IN_PROGRESS',
    'RESOLVED',
    'CLOSED'
);

CREATE TYPE warranty_status AS ENUM (
    'ACTIVE',
    'EXPIRED'
);

CREATE TYPE ticket_priority AS ENUM (
    'LOW',
    'MEDIUM',
    'HIGH',
    'CRITICAL'
);

CREATE TYPE notification_type AS ENUM (
    'REQUEST_UPDATE',
    'MAINTENANCE_ALERT',
    'WARRANTY_ALERT',
    'GENERAL'
);

CREATE TYPE asset_condition AS ENUM (
    'GOOD',
    'DAMAGED',
    'REPAIR_REQUIRED'
);

-- ROLES

CREATE TABLE roles (
                       role_id SERIAL PRIMARY KEY,
                       role_name VARCHAR(50) UNIQUE NOT NULL
);

CREATE TABLE permissions (
                             permission_id SERIAL PRIMARY KEY,
                             permission_name VARCHAR(100) UNIQUE NOT NULL
);

CREATE TABLE role_permissions (
                                  role_id INT REFERENCES roles(role_id) ON DELETE CASCADE,
                                  permission_id INT REFERENCES permissions(permission_id) ON DELETE CASCADE,
                                  PRIMARY KEY (role_id, permission_id)
);

-- DEPARTMENTS

CREATE TABLE departments (
                             department_id SERIAL PRIMARY KEY,
                             department_name VARCHAR(100)
);

-- USERS

CREATE TABLE users (
                       user_id SERIAL PRIMARY KEY,
                       name VARCHAR(100) NOT NULL,
                       email VARCHAR(120) UNIQUE NOT NULL,
                       password_hash TEXT NOT NULL,
                       phone_number VARCHAR(20),
                       status user_status DEFAULT 'ACTIVE',
                       role_id INT REFERENCES roles(role_id),
                       department_id INT REFERENCES departments(department_id),
                       created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ASSET CATEGORIES

CREATE TABLE asset_categories (
                                  category_id SERIAL PRIMARY KEY,
                                  category_name VARCHAR(100)
);

-- ASSETS

CREATE TABLE assets (
                        asset_id SERIAL PRIMARY KEY,
                        asset_tag VARCHAR(100) UNIQUE NOT NULL,
                        name VARCHAR(120) NOT NULL,
                        serial_number VARCHAR(120),
                        purchase_date DATE,
                        status asset_status DEFAULT 'AVAILABLE',
                        location VARCHAR(120),
                        category_id INT REFERENCES asset_categories(category_id)
);

-- WARRANTIES

CREATE TABLE warranties (
                            warranty_id SERIAL PRIMARY KEY,
                            asset_id INT UNIQUE REFERENCES assets(asset_id) ON DELETE CASCADE,
                            start_date DATE,
                            expiry_date DATE,
                            provider VARCHAR(120),
                            status warranty_status
);

-- ASSET REQUESTS

CREATE TABLE asset_requests (
                                request_id SERIAL PRIMARY KEY,
                                request_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                status request_status DEFAULT 'PENDING',
                                requested_by INT REFERENCES users(user_id),
                                approved_by INT REFERENCES users(user_id),
                                asset_category VARCHAR(100),
                                remarks TEXT
);

-- ASSET ALLOCATIONS

CREATE TABLE asset_allocations (
                                   allocation_id SERIAL PRIMARY KEY,
                                   asset_id INT REFERENCES assets(asset_id),
                                   user_id INT REFERENCES users(user_id),
                                   allocated_date DATE,
                                   expected_return_date DATE,
                                   return_date DATE,
                                   condition_on_return asset_condition
);

-- MAINTENANCE TICKETS

CREATE TABLE maintenance_tickets (
                                     ticket_id SERIAL PRIMARY KEY,
                                     issue_description TEXT,
                                     priority ticket_priority,
                                     status ticket_status DEFAULT 'OPEN',
                                     created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                     resolved_date TIMESTAMP,
                                     asset_id INT REFERENCES assets(asset_id),
                                     reported_by INT REFERENCES users(user_id),
                                     assigned_to INT REFERENCES users(user_id)
);

-- NOTIFICATIONS

CREATE TABLE notifications (
                               notification_id SERIAL PRIMARY KEY,
                               message TEXT,
                               type notification_type,
                               sent_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                               recipient_id INT REFERENCES users(user_id),
                               read_status BOOLEAN DEFAULT FALSE
);

-- AUDIT LOGS

CREATE TABLE audit_logs (
                            log_id SERIAL PRIMARY KEY,
                            action VARCHAR(120),
                            performed_by INT REFERENCES users(user_id),
                            entity_type VARCHAR(50),
                            entity_id INT,
                            timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- CHAT MESSAGES

CREATE TABLE chat_messages (
                               message_id SERIAL PRIMARY KEY,
                               sender_id INT REFERENCES users(user_id),
                               receiver_id INT REFERENCES users(user_id),
                               message TEXT,
                               sent_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ASSET HISTORY

CREATE TABLE asset_history (
                               history_id SERIAL PRIMARY KEY,
                               asset_id INT REFERENCES assets(asset_id),
                               action VARCHAR(100),
                               performed_by INT REFERENCES users(user_id),
                               action_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- INDEXES

CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_asset_status ON assets(status);
CREATE INDEX idx_ticket_status ON maintenance_tickets(status);
CREATE INDEX idx_notifications_user ON notifications(recipient_id);

-- DEFAULT ROLES

INSERT INTO roles (role_name) VALUES
                                  ('ADMIN'),
                                  ('MANAGER'),
                                  ('EMPLOYEE'),
                                  ('IT_STAFF');