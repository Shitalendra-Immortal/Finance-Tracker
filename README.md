# Personal Finance Tracker

A desktop app to track income, expenses, budgets, savings goals and monthly
spending. Built with **Java 17 · JavaFX · JDBC · MySQL** — a practical
project for database queries and real data analysis.

## Features

- **Dark, card-based UI** — one standardized token set across every screen
  (background #0B0F14, cards #151D27, borders #263241, accent #3B82F6),
  driven by a compact sidebar with icons instead of a tab bar; no feature changes
  beyond the dashboard items below, only the look.
- **Dashboard** — a greeting header with month selector and a prominent
  Add button; four stat cards (net cash flow, total income, total
  expenses, savings rate) with small color-coded icons and month-over-month
  lines worded per card when the current month is empty ("No activity
  yet" for net, "No income recorded" for income, "No expenses recorded"
  for expenses) and "New activity" when the previous month was zero,
  with expense lines saying whether spending rose or fell (and a note
  that current-month comparisons are month-to-date); a standing note
  that figures cover the selected month only, not a bank balance, and
  "Not available yet" under a dashed savings rate when there is no
  income; quick actions for Set
  budget and Export report; a compact alerts panel (one icon, one short
  message, one action) that shows the closest budget to its limit when
  all budgets are fine; a cash-flow chart over the last 3, 6, or 12
  months with exact per-month tooltips, the selected month outlined,
  and quiet months muted; a donut spending chart with
  the total in the middle and a legend, replaced by an honest sentence
  when a single category (or none) makes a chart meaningless; a budget
  overview with progress bars; and recent transactions with a View all
  link and rows that jump to the transaction on double-click. Spacing
  follows an 8-point layout scale (4 for micro gaps), cards share one
  radius and border, and dashboard cards stretch with the window.
  Amounts always carry a +/− sign and a label, never color alone.
- **Indian money formatting** — every amount in the app displays as
  ₹1,000.00 or ₹1,00,000.00 (lakh grouping), negatives as −₹900.00.
  Calculations stay BigDecimal; formatting happens only at display time.
  The tracker opens on November 2026 while that month is upcoming or
  current, with October data intact and still selectable.
- **Transactions** — add, edit and delete income/expense entries with
  category, amount, date, payment method (Cash, UPI, Card, Bank Transfer,
  Other) and note. Search across notes and categories, filter by type,
  category and month, and sort by clicking any table header.
- **CSV import** — import bank-exported CSV files: pick a file, map the
  columns (with header names auto-detected), preview exactly what will be
  imported, and confirm before anything saves. Duplicates (same date,
  amount, category and note) are skipped, bad rows are listed and left out,
  and the import saves all-or-nothing in one database transaction.
- **Budgets** — set a monthly limit per expense category, see used and
  remaining amounts. Status turns `WARNING` at 80% spent and `OVER BUDGET`
  at 100%, and adding an expense that crosses a budget pops up an alert.
  Budgets never carry forward on their own — a "Copy to next month" button
  copies them only when you ask.
- **Smart spending alerts** — the dashboard warns not only on budget
  limits but when a category's spending surges versus your own recent
  months: 50% or more above your 3-month average, and at least ₹500 more.
  The comparison is month-to-date versus full previous months, so alerts
  are conservative early in a month.
- **Reports** — weekly (Monday–Sunday), monthly and yearly category-wise
  expense breakdowns as a pie chart plus a ranked table, with the biggest
  spending categories highlighted. Export all transactions to CSV.
- **Savings goals** — create goals (laptop, trip, emergency fund) with a
  target amount and deadline, add contributions, watch progress, and see
  the even monthly amount needed to hit the target on time.
- **Loans** — money lent to friends, by name: amount, date and note, with
  partial repayments recorded per loan. The table shows what each person
  has returned, what they still owe, and whether the loan is OPEN or
  SETTLED, plus a total still owed across open loans. Lent money is
  tracked separately from transactions: it is expected back, so it never
  counts as an expense in reports.

## Requirements

- JDK 17 or newer (`java -version` to check)
- Maven 3.8+ (`mvn -version` to check)
- MySQL 8 running locally

## Setup

1. **Point the app at your MySQL.**
   Open `src/main/resources/db.properties` and set your MySQL password
   (and user/host if yours differ). The file is gitignored, so it never
   reaches GitHub. There is a `db.properties.example` to copy from.

2. **Create the database (optional — the app does this itself).**
   ```bash
   mysql -u root -p < src/main/resources/schema.sql
   ```
   If you skip this, the app runs `schema.sql` on first launch, which
   creates the `finance_tracker` database, the tables, and 12 starter
   categories (Salary, Food, Transport, Rent, ...). Databases created by
   v1 are migrated automatically (the `payment_method` column is added).

3. **Run it.**
   ```bash
   mvn javafx:run
   ```
   On Windows you can also double-click `run.bat`.

4. **Run the tests (no MySQL needed).**
   ```bash
   mvn test
   ```

## Technical decisions

- **Money is BigDecimal, never float or double.** All amounts are stored,
  calculated and compared as `BigDecimal` (two-decimal scale, HALF_UP).
  Charts convert to plain numbers for drawing only — never for math.
  Balance is always recomputed from the transaction rows, never adjusted
  incrementally, so editing an expense cannot double-count it.
- **Architecture is layered.** UI (`controller`) → business rules
  (`service`) → SQL (`dao`, the repositories) → MySQL, with plain data
  objects (`model`, the entities) passed between layers. The controller
  never writes SQL and never implements a business rule; services own
  validation, budget thresholds, alerts, balances and report logic.
  One documented exception: `SavingsGoal` exposes progress and monthly
  needed, and `Loan` exposes remaining and status, as pure derivations
  from their own fields, so the table can bind to them — no database
  access happens there.
- **Dates are calendar dates.** Transaction dates, deadlines and
  contribution dates are MySQL `DATE` plus `java.time.LocalDate` in the
  user's local timezone — no time component, so no timezone conversion
  ever applies to them. The JDBC URL pins `serverTimezone=UTC`, which
  only affects the `TIMESTAMP` columns (`created_at`).
- **Validation rejects bad input before it reaches the database:**
  negative or zero amounts, missing categories, categories of the wrong
  type, missing dates, malformed numbers, months that are not real
  calendar months, notes over 255 characters, and past goal deadlines.
  Every rejection is a plain message, not an exception dump.
- **Errors are friendly.** The UI never shows raw database messages or
  stack traces. Connection problems, data conflicts and other database
  errors each get a plain explanation; the technical detail goes to the
  console only, for debugging.
- **Database integrity.** Primary keys everywhere, foreign keys from
  transactions, budgets and contributions to their parents, `CHECK`
  constraints on positive amounts, unique category names, and one budget
  per category per month. Contributions disappear with their goal
  (`ON DELETE CASCADE`).
- **Import and recurring safety.** CSV import saves all-or-nothing in one
  database transaction, and recurring generation advances each entry's
  next due date as it creates an occurrence, so neither path can silently
  duplicate entries.

## Security

This is a single-user desktop app by design — there is no login, so there
are no user passwords anywhere in the code. The only secret is the MySQL
password: it lives only in `db.properties`, which is gitignored and never
hardcoded. If user accounts are ever added, their passwords must be stored
with a proper password-hashing algorithm (BCrypt), never in plaintext.

Privacy: the app collects nothing beyond your transactions, budgets, goals
and settings. Error output never includes transaction notes, amounts or
passwords, and CSV import previews stay on your own screen — nothing is
uploaded anywhere. There is no server, no account, and no tracking.

## Backup

CSV export is for portability (transactions only), not a full backup.
For a complete backup including budgets and goals, dump the database:

```bash
mysqldump -u root -p finance_tracker > finance_tracker_backup.sql
```

Restore it with:

```bash
mysql -u root -p finance_tracker < finance_tracker_backup.sql
```

## Project structure

```
src/main/java/com/financetracker/
  Main.java                 JavaFX entry point, DB init + error dialog
  db/Database.java          Reads db.properties, opens connections, runs schema.sql + migration
  model/                    Transaction, Category, Budget, BudgetStatus,
                            CategorySummary, MonthTotal, SavingsGoal,
                            Loan (entities)
  dao/                      CategoryDao, TransactionDao, BudgetDao,
                            ReportDao, SavingsGoalDao, LoanDao
                            (repositories, SQL only)
  service/                  TransactionService, BudgetService, ReportService,
                            GoalService, ImportService, LoanService
                            (business rules + validation)
  util/Money.java           The single place money is parsed and formatted
  util/CsvParser.java       CSV parsing (quotes, commas, line breaks)
  controller/MainController.java   UI only, for the sidebar views
  util/DashboardMetrics.java  Month-over-month change, savings rate, greeting
src/test/java/com/financetracker/
  util/MoneyTest.java
  util/CsvParserTest.java
  util/DashboardMetricsTest.java  MoM change, savings rate, greeting hours
  service/TransactionServiceTest.java   Balance example, edit recalculation, validation
  service/BudgetServiceTest.java        80%/100% thresholds, month validation
  service/GoalServiceTest.java          Monthly needed, progress, goal validation
  service/ReportServiceTest.java        Week boundaries, top categories, surge rule
  service/ImportServiceTest.java        Duplicates, validation, sign/type rules
  service/LoanServiceTest.java          Remaining/status, loan + repayment validation
src/main/resources/
  fxml/main.fxml            Sidebar + 6 views (Overview, Transactions, Budgets,
                            Reports, Savings Goals, Loans)
  css/style.css             Standardized token theme (see header comment)
  fonts/fa-solid-900.ttf    FontAwesome Free solid icons, sidebar nav (CC BY 4.0)
  schema.sql                Tables + starter categories
  db.properties             Your local MySQL settings (gitignored)
```
