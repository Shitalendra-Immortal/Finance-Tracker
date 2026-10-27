-- Personal Finance Tracker — MySQL schema.
-- Run once:  mysql -u root -p < schema.sql
-- (The app also runs this file automatically on first launch.)

CREATE DATABASE IF NOT EXISTS finance_tracker;
USE finance_tracker;

CREATE TABLE IF NOT EXISTS categories (
  id   INT AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(50) NOT NULL UNIQUE,
  type ENUM('INCOME', 'EXPENSE') NOT NULL
);

CREATE TABLE IF NOT EXISTS transactions (
  id          INT AUTO_INCREMENT PRIMARY KEY,
  type        ENUM('INCOME', 'EXPENSE') NOT NULL,
  category_id INT NOT NULL,
  amount      DECIMAL(12, 2) NOT NULL CHECK (amount > 0),
  txn_date    DATE NOT NULL,
  note        VARCHAR(255),
  payment_method VARCHAR(30) NOT NULL DEFAULT 'Cash',
  created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (category_id) REFERENCES categories (id)
);

CREATE TABLE IF NOT EXISTS budgets (
  id          INT AUTO_INCREMENT PRIMARY KEY,
  category_id INT NOT NULL,
  month       CHAR(7) NOT NULL COMMENT 'YYYY-MM',
  amount      DECIMAL(12, 2) NOT NULL CHECK (amount > 0),
  UNIQUE (category_id, month),
  FOREIGN KEY (category_id) REFERENCES categories (id)
);

CREATE TABLE IF NOT EXISTS savings_goals (
  id            INT AUTO_INCREMENT PRIMARY KEY,
  name          VARCHAR(100) NOT NULL,
  target_amount DECIMAL(12, 2) NOT NULL CHECK (target_amount > 0),
  deadline      DATE NOT NULL,
  created_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS goal_contributions (
  id                INT AUTO_INCREMENT PRIMARY KEY,
  goal_id           INT NOT NULL,
  amount            DECIMAL(12, 2) NOT NULL CHECK (amount > 0),
  contribution_date DATE NOT NULL,
  note              VARCHAR(255),
  FOREIGN KEY (goal_id) REFERENCES savings_goals (id) ON DELETE CASCADE
);

-- Money lent to friends. Repayments live separately so partial paybacks
-- accumulate. A loan is NOT a transaction (it is expected back).
CREATE TABLE IF NOT EXISTS loans (
  id          INT AUTO_INCREMENT PRIMARY KEY,
  person_name VARCHAR(100) NOT NULL,
  amount      DECIMAL(12, 2) NOT NULL CHECK (amount > 0),
  lent_date   DATE NOT NULL,
  note        VARCHAR(255),
  created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS loan_repayments (
  id          INT AUTO_INCREMENT PRIMARY KEY,
  loan_id     INT NOT NULL,
  amount      DECIMAL(12, 2) NOT NULL CHECK (amount > 0),
  repaid_date DATE NOT NULL,
  note        VARCHAR(255),
  FOREIGN KEY (loan_id) REFERENCES loans (id) ON DELETE CASCADE
);

-- Starter categories (INSERT IGNORE keeps this safe to re-run)
INSERT IGNORE INTO categories (name, type) VALUES
  ('Salary', 'INCOME'),
  ('Freelance', 'INCOME'),
  ('Interest', 'INCOME'),
  ('Other Income', 'INCOME'),
  ('Food', 'EXPENSE'),
  ('Transport', 'EXPENSE'),
  ('Rent', 'EXPENSE'),
  ('Shopping', 'EXPENSE'),
  ('Entertainment', 'EXPENSE'),
  ('Health', 'EXPENSE'),
  ('Education', 'EXPENSE'),
  ('Other', 'EXPENSE');
