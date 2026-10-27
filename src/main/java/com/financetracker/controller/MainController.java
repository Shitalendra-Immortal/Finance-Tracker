package com.financetracker.controller;

import com.financetracker.dao.BudgetDao;
import com.financetracker.dao.CategoryDao;
import com.financetracker.dao.LoanDao;
import com.financetracker.dao.ReportDao;
import com.financetracker.dao.SavingsGoalDao;
import com.financetracker.dao.TransactionDao;
import com.financetracker.model.BudgetStatus;
import com.financetracker.model.Category;
import com.financetracker.model.CategorySummary;
import com.financetracker.model.Loan;
import com.financetracker.model.MonthTotal;
import com.financetracker.model.SavingsGoal;
import com.financetracker.model.Transaction;
import com.financetracker.service.BudgetService;
import com.financetracker.service.GoalService;
import com.financetracker.service.ImportService;
import com.financetracker.service.LoanService;
import com.financetracker.service.ReportService;
import com.financetracker.service.TransactionService;
import com.financetracker.util.CsvParser;
import com.financetracker.util.DashboardMetrics;
import com.financetracker.util.Money;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.control.cell.ProgressBarTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * UI only: reads widgets, calls services, shows results.
 * No SQL here, no business rules here — those live in the service layer.
 */
public class MainController {

    private final TransactionService transactionService =
            new TransactionService(new TransactionDao(), new CategoryDao());
    private final BudgetService budgetService = new BudgetService(new BudgetDao());
    private final ReportService reportService = new ReportService(new ReportDao());
    private final GoalService goalService = new GoalService(new SavingsGoalDao());
    private final ImportService importService = new ImportService(new TransactionDao());
    private final LoanService loanService = new LoanService(new LoanDao());

    /** -1 means the form is adding; otherwise it holds the id being edited. */
    private int editingTxnId = -1;

    /** Same pattern for the savings-goal form; the saved amount guards the new target. */
    private int editingGoalId = -1;
    private BigDecimal editingGoalSaved = BigDecimal.ZERO;

    /** Donut slice colors, in order — matches the .default-colorN.chart-pie rules in style.css. */
    private static final String[] DONUT_COLORS = {
            "#3B82F6", "#34D399", "#F87171", "#FBBF24",
            "#A78BFA", "#22D3EE", "#FB923C", "#94A3B8"};

    // ---- Navigation (compact sidebar drives the hidden tab pane) ----
    @FXML private TabPane mainTabPane;
    @FXML private Button navOverviewButton;
    @FXML private Button navTransactionsButton;
    @FXML private Button navBudgetsButton;
    @FXML private Button navReportsButton;
    @FXML private Button navGoalsButton;
    @FXML private Button navLoansButton;

    // ---- Dashboard ----
    @FXML private ComboBox<String> dashboardMonthBox;
    @FXML private Label greetingLabel;
    @FXML private Label overviewSubtitleLabel;
    @FXML private Button dashboardAddButton;
    @FXML private Label incomeLabel;
    @FXML private Label expenseLabel;
    @FXML private Label balanceLabel;
    @FXML private Label incomeDeltaLabel;
    @FXML private Label expenseDeltaLabel;
    @FXML private Label balanceDeltaLabel;
    @FXML private Label savingsRateLabel;
    @FXML private Label savingsCaptionLabel;
    @FXML private Label netIconLabel;
    @FXML private Label incomeIconLabel;
    @FXML private Label expenseIconLabel;
    @FXML private Label savingsIconLabel;
    @FXML private Label alertIconLabel;
    @FXML private Button alertActionButton;
    @FXML private ComboBox<String> cashflowRangeBox;
    @FXML private Label cashflowSubtitleLabel;
    @FXML private Button quickBudgetButton;
    @FXML private Button quickExportButton;
    @FXML private Label alertLabel;
    @FXML private Label comparisonNoteLabel;
    @FXML private Label cashflowNoteLabel;
    @FXML private StackPane donutPane;
    @FXML private Circle donutHole;
    @FXML private Label donutTotalValueLabel;
    @FXML private VBox spendingLegendBox;
    @FXML private Label spendingEmptyLabel;
    @FXML private PieChart dashboardChart;
    @FXML private VBox budgetOverviewBox;
    @FXML private Label budgetEmptyLabel;
    @FXML private Button viewAllButton;
    @FXML private TableView<Transaction> recentTable;
    @FXML private TableColumn<Transaction, LocalDate> recentDateCol;
    @FXML private TableColumn<Transaction, String> recentNoteCol;
    @FXML private TableColumn<Transaction, String> recentCategoryCol;
    @FXML private TableColumn<Transaction, BigDecimal> recentAmountCol;
    @FXML private BarChart<String, Number> comparisonChart;
    @FXML private CategoryAxis comparisonXAxis;
    @FXML private NumberAxis comparisonYAxis;

    // ---- Transactions ----
    @FXML private ComboBox<String> typeBox;
    @FXML private ComboBox<Category> categoryBox;
    @FXML private TextField amountField;
    @FXML private DatePicker datePicker;
    @FXML private TextField noteField;
    @FXML private ComboBox<String> paymentBox;
    @FXML private Button addButton;
    @FXML private Button cancelEditButton;
    @FXML private ComboBox<String> filterTypeBox;
    @FXML private ComboBox<Category> filterCategoryBox;
    @FXML private ComboBox<String> filterMonthBox;
    @FXML private Button applyFilterButton;
    @FXML private TextField searchField;
    @FXML private Button searchButton;
    @FXML private Button showAllButton;
    @FXML private Button editButton;
    @FXML private Button deleteButton;
    @FXML private Button importButton;
    @FXML private TableView<Transaction> txnTable;
    @FXML private TableColumn<Transaction, Integer> idCol;
    @FXML private TableColumn<Transaction, LocalDate> dateCol;
    @FXML private TableColumn<Transaction, String> typeCol;
    @FXML private TableColumn<Transaction, String> categoryCol;
    @FXML private TableColumn<Transaction, BigDecimal> amountCol;
    @FXML private TableColumn<Transaction, String> paymentCol;
    @FXML private TableColumn<Transaction, String> noteCol;

    // ---- Budgets ----
    @FXML private ComboBox<Category> budgetCategoryBox;
    @FXML private TextField budgetMonthField;
    @FXML private TextField budgetAmountField;
    @FXML private Button setBudgetButton;
    @FXML private Button loadBudgetsButton;
    @FXML private Button copyBudgetsButton;
    @FXML private Button deleteBudgetButton;
    @FXML private TableView<BudgetStatus> budgetTable;
    @FXML private TableColumn<BudgetStatus, String> budgetCategoryCol;
    @FXML private TableColumn<BudgetStatus, BigDecimal> limitCol;
    @FXML private TableColumn<BudgetStatus, BigDecimal> spentCol;
    @FXML private TableColumn<BudgetStatus, BigDecimal> remainingCol;
    @FXML private TableColumn<BudgetStatus, String> statusCol;

    // ---- Reports ----
    @FXML private ComboBox<String> periodBox;
    @FXML private ComboBox<String> reportMonthBox;
    @FXML private ComboBox<String> reportYearBox;
    @FXML private Button loadReportButton;
    @FXML private Button exportButton;
    @FXML private Label reportPeriodLabel;
    @FXML private Label reportTopLabel;
    @FXML private Label reportTotalLabel;
    @FXML private PieChart expenseChart;
    @FXML private TableView<CategorySummary> reportTable;
    @FXML private TableColumn<CategorySummary, String> reportCategoryCol;
    @FXML private TableColumn<CategorySummary, BigDecimal> reportTotalCol;

    // ---- Savings goals ----
    @FXML private TextField goalNameField;
    @FXML private TextField goalTargetField;
    @FXML private DatePicker goalDeadlinePicker;
    @FXML private Button addGoalButton;
    @FXML private TextField contributionAmountField;
    @FXML private Button contributeButton;
    @FXML private Button deleteGoalButton;
    @FXML private Button editGoalButton;
    @FXML private Button cancelGoalEditButton;
    @FXML private TableView<SavingsGoal> goalTable;
    @FXML private TableColumn<SavingsGoal, String> goalNameCol;
    @FXML private TableColumn<SavingsGoal, BigDecimal> goalTargetCol;
    @FXML private TableColumn<SavingsGoal, BigDecimal> goalSavedCol;
    @FXML private TableColumn<SavingsGoal, BigDecimal> goalRemainingCol;
    @FXML private TableColumn<SavingsGoal, Double> goalProgressCol;
    @FXML private TableColumn<SavingsGoal, BigDecimal> goalMonthlyCol;
    @FXML private TableColumn<SavingsGoal, LocalDate> goalDeadlineCol;

    // ---- Loans ----
    @FXML private TextField loanPersonField;
    @FXML private TextField loanAmountField;
    @FXML private DatePicker loanDatePicker;
    @FXML private TextField loanNoteField;
    @FXML private Button addLoanButton;
    @FXML private TextField repaymentAmountField;
    @FXML private Button recordRepaymentButton;
    @FXML private Button deleteLoanButton;
    @FXML private Label loanSummaryLabel;
    @FXML private TableView<Loan> loanTable;
    @FXML private TableColumn<Loan, String> loanPersonCol;
    @FXML private TableColumn<Loan, BigDecimal> loanAmountCol;
    @FXML private TableColumn<Loan, BigDecimal> loanReturnedCol;
    @FXML private TableColumn<Loan, BigDecimal> loanRemainingCol;
    @FXML private TableColumn<Loan, LocalDate> loanDateCol;
    @FXML private TableColumn<Loan, String> loanStatusCol;

    @FXML
    public void initialize() {
        // Month pickers: one future month first, then the current month, then the 11 before it.
        // November 2026 is selectable right now, and it is the opening month (see defaultMonth).
        List<String> months = monthsForPickers();
        String openingMonth = defaultMonth().toString();
        dashboardMonthBox.setItems(FXCollections.observableArrayList(months));
        dashboardMonthBox.setValue(openingMonth);
        reportMonthBox.setItems(FXCollections.observableArrayList(months));
        reportMonthBox.setValue(openingMonth);
        List<String> filterMonths = new ArrayList<>();
        filterMonths.add("All");
        filterMonths.addAll(months);
        filterMonthBox.setItems(FXCollections.observableArrayList(filterMonths));
        filterMonthBox.setValue("All");

        // Year picker: current year and the 4 before it.
        List<String> years = new ArrayList<>();
        int thisYear = YearMonth.now().getYear();
        for (int i = 0; i < 5; i++) {
            years.add(String.valueOf(thisYear - i));
        }
        reportYearBox.setItems(FXCollections.observableArrayList(years));
        reportYearBox.setValue(years.get(0));

        // Transaction form defaults.
        typeBox.setItems(FXCollections.observableArrayList("EXPENSE", "INCOME"));
        typeBox.setValue("EXPENSE");
        paymentBox.setItems(FXCollections.observableArrayList(
                "Cash", "UPI", "Card", "Bank Transfer", "Other"));
        paymentBox.setValue("Cash");
        datePicker.setValue(LocalDate.now());
        filterTypeBox.setItems(FXCollections.observableArrayList("All", "EXPENSE", "INCOME"));
        filterTypeBox.setValue("All");
        periodBox.setItems(FXCollections.observableArrayList("Monthly", "Yearly", "Weekly"));
        periodBox.setValue("Monthly");
        budgetMonthField.setText(openingMonth);
        goalDeadlinePicker.setValue(LocalDate.now().plusYears(1));
        loanDatePicker.setValue(LocalDate.now());

        // Table columns read the matching getters (e.g. "categoryName" -> getCategoryName()).
        idCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        dateCol.setCellValueFactory(new PropertyValueFactory<>("date"));
        typeCol.setCellValueFactory(new PropertyValueFactory<>("type"));
        categoryCol.setCellValueFactory(new PropertyValueFactory<>("categoryName"));
        amountCol.setCellValueFactory(new PropertyValueFactory<>("amount"));
        paymentCol.setCellValueFactory(new PropertyValueFactory<>("paymentMethod"));
        noteCol.setCellValueFactory(new PropertyValueFactory<>("note"));

        recentDateCol.setCellValueFactory(new PropertyValueFactory<>("date"));
        recentNoteCol.setCellValueFactory(new PropertyValueFactory<>("note"));
        recentNoteCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String note, boolean empty) {
                super.updateItem(note, empty);
                if (empty) {
                    setText(null);
                } else {
                    Transaction txn = getTableRow().getItem();
                    setText(note == null || note.isBlank()
                            ? (txn == null ? null : txn.getCategoryName()) : note);
                }
            }
        });
        recentCategoryCol.setCellValueFactory(new PropertyValueFactory<>("categoryName"));
        recentAmountCol.setCellValueFactory(new PropertyValueFactory<>("amount"));

        budgetCategoryCol.setCellValueFactory(new PropertyValueFactory<>("categoryName"));
        limitCol.setCellValueFactory(new PropertyValueFactory<>("limit"));
        spentCol.setCellValueFactory(new PropertyValueFactory<>("spent"));
        remainingCol.setCellValueFactory(new PropertyValueFactory<>("remaining"));
        statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));

        reportCategoryCol.setCellValueFactory(new PropertyValueFactory<>("categoryName"));
        reportTotalCol.setCellValueFactory(new PropertyValueFactory<>("total"));

        goalNameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        goalTargetCol.setCellValueFactory(new PropertyValueFactory<>("targetAmount"));
        goalSavedCol.setCellValueFactory(new PropertyValueFactory<>("savedAmount"));
        goalRemainingCol.setCellValueFactory(new PropertyValueFactory<>("remaining"));
        goalProgressCol.setCellValueFactory(new PropertyValueFactory<>("progress"));
        goalProgressCol.setCellFactory(ProgressBarTableCell.forTableColumn());
        goalMonthlyCol.setCellValueFactory(new PropertyValueFactory<>("monthlyNeeded"));
        goalDeadlineCol.setCellValueFactory(new PropertyValueFactory<>("deadline"));

        loanPersonCol.setCellValueFactory(new PropertyValueFactory<>("personName"));
        loanAmountCol.setCellValueFactory(new PropertyValueFactory<>("amount"));
        loanReturnedCol.setCellValueFactory(new PropertyValueFactory<>("returnedAmount"));
        loanRemainingCol.setCellValueFactory(new PropertyValueFactory<>("remaining"));
        loanDateCol.setCellValueFactory(new PropertyValueFactory<>("lentDate"));
        loanStatusCol.setCellValueFactory(new PropertyValueFactory<>("status"));

        // Donut: no built-in labels or legend (a custom legend is built per refresh),
        // and the center hole tracks the chart size so it never swallows a narrow chart.
        dashboardChart.setLabelsVisible(false);
        dashboardChart.setLegendVisible(false);
        donutHole.radiusProperty().bind(Bindings.min(
                dashboardChart.widthProperty(), dashboardChart.heightProperty()).multiply(0.27));

        // Amounts carry their meaning without color alone: a +/− sign plus the label.
        installAmountCell(amountCol);
        installAmountCell(recentAmountCol);
        installCategoryBadgeCell(categoryCol);
        installCategoryBadgeCell(recentCategoryCol);

        // Every other money column renders Indian formatting, never a raw BigDecimal.
        installMoneyCell(limitCol);
        installMoneyCell(spentCol);
        installMoneyCell(remainingCol);
        installMoneyCell(reportTotalCol);
        installMoneyCell(goalTargetCol);
        installMoneyCell(goalSavedCol);
        installMoneyCell(goalRemainingCol);
        installMoneyCell(goalMonthlyCol);
        installMoneyCell(loanAmountCol);
        installMoneyCell(loanReturnedCol);
        installMoneyCell(loanRemainingCol);

        // Compact empty states instead of giant blank tables.
        txnTable.setPlaceholder(emptyState("No transactions yet.",
                "Add your first transaction", () -> amountField.requestFocus()));
        budgetTable.setPlaceholder(emptyState("No budgets set yet.",
                "Create your first budget", () -> budgetCategoryBox.requestFocus()));

        // Edit and Delete only make sense with a transaction selected.
        editButton.setDisable(true);
        deleteButton.setDisable(true);
        txnTable.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldSel, newSel) -> {
                    boolean none = newSel == null;
                    editButton.setDisable(none);
                    deleteButton.setDisable(none);
                });

        // Budget Delete and goal Edit likewise need a selected row.
        deleteBudgetButton.setDisable(true);
        editGoalButton.setDisable(true);
        budgetTable.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldSel, newSel) -> deleteBudgetButton.setDisable(newSel == null));
        goalTable.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldSel, newSel) -> editGoalButton.setDisable(newSel == null));

        // Wiring.
        typeBox.setOnAction(e -> loadCategoriesForType());
        dashboardMonthBox.setOnAction(e -> loadDashboard());
        reportMonthBox.setOnAction(e -> loadReport());
        reportYearBox.setOnAction(e -> loadReport());
        periodBox.setOnAction(e -> loadReport());
        addButton.setOnAction(e -> onAddOrUpdateTransaction());
        cancelEditButton.setOnAction(e -> resetEditState());
        searchButton.setOnAction(e -> onSearch());
        showAllButton.setOnAction(e -> showAllTransactions());
        editButton.setOnAction(e -> onEditTransaction());
        deleteButton.setOnAction(e -> onDeleteTransaction());
        applyFilterButton.setOnAction(e -> onApplyFilter());
        setBudgetButton.setOnAction(e -> onSetBudget());
        loadBudgetsButton.setOnAction(e -> loadBudgets());
        copyBudgetsButton.setOnAction(e -> onCopyBudgets());
        deleteBudgetButton.setOnAction(e -> onDeleteBudget());
        loadReportButton.setOnAction(e -> loadReport());
        exportButton.setOnAction(e -> onExportCsv());
        addGoalButton.setOnAction(e -> onAddGoal());
        contributeButton.setOnAction(e -> onContribute());
        deleteGoalButton.setOnAction(e -> onDeleteGoal());
        editGoalButton.setOnAction(e -> onEditGoal());
        cancelGoalEditButton.setOnAction(e -> resetGoalEditState());
        importButton.setOnAction(e -> onImportCsv());
        addLoanButton.setOnAction(e -> onAddLoan());
        recordRepaymentButton.setOnAction(e -> onRecordRepayment());
        deleteLoanButton.setOnAction(e -> onDeleteLoan());

        // Sidebar navigation and dashboard quick actions.
        installSidebarIcons();
        installStatIcons();
        cashflowRangeBox.setItems(FXCollections.observableArrayList(
                "Last 3 months", "Last 6 months", "Last 12 months"));
        cashflowRangeBox.setValue("Last 6 months");
        cashflowRangeBox.setOnAction(e -> loadDashboard());
        // Both alert actions land on the Budgets tab: creating one or viewing them.
        alertActionButton.setOnAction(e -> showView(2));
        navOverviewButton.setOnAction(e -> showView(0));
        navTransactionsButton.setOnAction(e -> showView(1));
        navBudgetsButton.setOnAction(e -> showView(2));
        navReportsButton.setOnAction(e -> showView(3));
        navGoalsButton.setOnAction(e -> showView(4));
        navLoansButton.setOnAction(e -> showView(5));
        dashboardAddButton.setOnAction(e -> { showView(1); amountField.requestFocus(); });
        quickBudgetButton.setOnAction(e -> showView(2));
        quickExportButton.setOnAction(e -> onExportCsv());
        viewAllButton.setOnAction(e -> showView(1));
        recentTable.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                Transaction selected = recentTable.getSelectionModel().getSelectedItem();
                showView(1);
                if (selected != null) {
                    txnTable.getItems().stream()
                            .filter(t -> t.getId() == selected.getId())
                            .findFirst()
                            .ifPresent(t -> { txnTable.getSelectionModel().select(t); txnTable.scrollTo(t); });
                }
            }
        });

        // First paint.
        loadCategoriesForType();
        loadExpenseCategoriesForBudgets();
        loadFilterCategories();
        showAllTransactions();
        loadDashboard();
        loadBudgets();
        loadReport();
        loadGoals();
        loadLoans();
        showView(0);
    }

    // ---------------- Dashboard ----------------

    private void loadDashboard() {
        String month = dashboardMonthBox.getValue();
        if (month == null) {
            return;
        }
        try {
            YearMonth selected = YearMonth.parse(month);
            BigDecimal income = transactionService.sumByTypeAndMonth("INCOME", month);
            BigDecimal expense = transactionService.sumByTypeAndMonth("EXPENSE", month);
            BigDecimal net = income.subtract(expense);
            String prevMonth = selected.minusMonths(1).toString();
            BigDecimal prevIncome = transactionService.sumByTypeAndMonth("INCOME", prevMonth);
            BigDecimal prevExpense = transactionService.sumByTypeAndMonth("EXPENSE", prevMonth);
            BigDecimal prevNet = prevIncome.subtract(prevExpense);

            // Header: time-of-day greeting; the subtitle follows the selected month.
            greetingLabel.setText(DashboardMetrics.greetingForHour(LocalTime.now().getHour()) + "!");
            overviewSubtitleLabel.setText("Your financial overview · "
                    + selected.format(DateTimeFormatter.ofPattern("MMMM yyyy")));

            incomeLabel.setText(Money.format(income));
            expenseLabel.setText(Money.format(expense));
            balanceLabel.setText(Money.format(net));
            boolean monthEmpty = income.compareTo(BigDecimal.ZERO) == 0
                    && expense.compareTo(BigDecimal.ZERO) == 0;
            setDelta(balanceDeltaLabel, net, prevNet, DashboardMetrics.DeltaKind.NET, monthEmpty);
            setDelta(incomeDeltaLabel, income, prevIncome, DashboardMetrics.DeltaKind.INCOME,
                    income.compareTo(BigDecimal.ZERO) == 0);
            setDelta(expenseDeltaLabel, expense, prevExpense, DashboardMetrics.DeltaKind.EXPENSE,
                    expense.compareTo(BigDecimal.ZERO) == 0);
            savingsRateLabel.setText(DashboardMetrics.savingsRate(income, net)
                    .map(rate -> rate.toPlainString() + "%").orElse("—"));
            // The rate is undefined without income; say so instead of leaving the
            // standing "share of income saved" caption under a dash.
            savingsCaptionLabel.setText(income.compareTo(BigDecimal.ZERO) == 0
                    ? "Not available yet" : "share of income saved");
            // The comparison is month-to-date only while the selected month is still running.
            boolean currentMonth = selected.equals(YearMonth.now());
            comparisonNoteLabel.setVisible(currentMonth);
            comparisonNoteLabel.setManaged(currentMonth);

            // Compact status panel: one icon, one short message, one direct action.
            // "No budgets" and "all budgets fine" are different states: with budgets
            // set, the panel shows the actual closest-to-limit status, not a generic line.
            List<String> alerts = new ArrayList<>(budgetService.getAlerts(month));
            alerts.addAll(reportService.spendingSurgeAlerts(month));
            List<BudgetStatus> statuses = budgetService.getStatuses(month);
            if (!alerts.isEmpty()) {
                setAlertState("\uf071", "alert-icon-warn", null); // triangle-exclamation
                String first = alerts.get(0);
                alertLabel.setText(alerts.size() > 1
                        ? first + " (+" + (alerts.size() - 1) + " more)" : first);
                alertActionButton.setText("View budgets");
                alertActionButton.setVisible(true);
                alertActionButton.setManaged(true);
            } else if (statuses.isEmpty()) {
                setAlertState("\uf05a", "alert-icon-info", "alert-text-info"); // circle-info
                alertLabel.setText("No budgets set for this month yet.");
                alertActionButton.setText("Create budget");
                alertActionButton.setVisible(true);
                alertActionButton.setManaged(true);
            } else {
                setAlertState("\uf058", "alert-icon-ok", "alert-text-ok"); // circle-check
                alertLabel.setText(budgetStatusSummary(statuses));
                alertActionButton.setVisible(false);
                alertActionButton.setManaged(false);
            }

            // Spending breakdown: a donut with a legend once there is a real breakdown,
            // an honest sentence when one category (or none) makes a chart meaningless.
            List<CategorySummary> breakdown = reportService.monthlyReport(month);
            if (breakdown.size() < 2) {
                donutPane.setVisible(false);
                donutPane.setManaged(false);
                spendingLegendBox.setVisible(false);
                spendingLegendBox.setManaged(false);
                spendingEmptyLabel.setVisible(true);
                spendingEmptyLabel.setManaged(true);
                if (breakdown.isEmpty()) {
                    spendingEmptyLabel.setText("No spending recorded this month yet.");
                } else {
                    CategorySummary only = breakdown.get(0);
                    spendingEmptyLabel.setText("All recorded spending this month belongs to "
                            + only.getCategoryName() + " (" + Money.format(only.getTotal()) + ").");
                }
            } else {
                donutPane.setVisible(true);
                donutPane.setManaged(true);
                spendingLegendBox.setVisible(true);
                spendingLegendBox.setManaged(true);
                spendingEmptyLabel.setVisible(false);
                spendingEmptyLabel.setManaged(false);
                List<PieChart.Data> slices = new ArrayList<>();
                for (CategorySummary row : breakdown) {
                    slices.add(new PieChart.Data(row.getCategoryName(), row.getTotal().doubleValue()));
                }
                dashboardChart.setData(FXCollections.observableArrayList(slices));
                donutTotalValueLabel.setText(Money.format(expense));
                spendingLegendBox.getChildren().clear();
                for (int i = 0; i < breakdown.size(); i++) {
                    CategorySummary row = breakdown.get(i);
                    BigDecimal pct = expense.compareTo(BigDecimal.ZERO) == 0 ? BigDecimal.ZERO
                            : row.getTotal().multiply(BigDecimal.valueOf(100))
                                    .divide(expense, 1, RoundingMode.HALF_UP);
                    HBox legendRow = new HBox(8);
                    legendRow.setAlignment(Pos.CENTER_LEFT);
                    Circle dot = new Circle(5, Color.web(DONUT_COLORS[i % DONUT_COLORS.length]));
                    Label name = new Label(row.getCategoryName());
                    HBox.setHgrow(name, javafx.scene.layout.Priority.ALWAYS);
                    Label pctLabel = new Label(pct.toPlainString() + "%");
                    pctLabel.getStyleClass().add("delta-neutral");
                    Label amount = new Label(Money.format(row.getTotal()));
                    legendRow.getChildren().addAll(dot, name, pctLabel, amount);
                    spendingLegendBox.getChildren().add(legendRow);
                }
            }

            // Budget overview: one progress row per budget of the month.
            budgetOverviewBox.getChildren().clear();
            if (statuses.isEmpty()) {
                budgetEmptyLabel.setText("No budgets set for this month — set one on the Budgets tab to track spending against limits.");
                budgetEmptyLabel.setVisible(true);
                budgetEmptyLabel.setManaged(true);
            } else {
                budgetEmptyLabel.setVisible(false);
                budgetEmptyLabel.setManaged(false);
                for (BudgetStatus status : statuses) {
                    BigDecimal limit = status.getLimit();
                    BigDecimal spent = status.getSpent();
                    double progress = limit.compareTo(BigDecimal.ZERO) == 0 ? 0.0
                            : Math.min(1.0, spent.doubleValue() / limit.doubleValue());
                    BigDecimal pct = limit.compareTo(BigDecimal.ZERO) == 0 ? BigDecimal.ZERO
                            : spent.multiply(BigDecimal.valueOf(100))
                                    .divide(limit, 1, RoundingMode.HALF_UP);
                    HBox row = new HBox(8);
                    row.setAlignment(Pos.CENTER_LEFT);
                    Label category = new Label(status.getCategoryName());
                    category.setPrefWidth(120);
                    ProgressBar bar = new ProgressBar(progress);
                    bar.setMaxWidth(Double.MAX_VALUE);
                    HBox.setHgrow(bar, javafx.scene.layout.Priority.ALWAYS);
                    String barColor = switch (status.getStatus()) {
                        case "WARNING" -> "#FBBF24";
                        case "OVER BUDGET" -> "#F87171";
                        default -> "#3B82F6";
                    };
                    bar.setStyle("-fx-accent: " + barColor + ";");
                    Label spentLimit = new Label(Money.format(spent) + " / " + Money.format(limit));
                    spentLimit.setPrefWidth(150);
                    spentLimit.setAlignment(Pos.CENTER_RIGHT);
                    Label pctLabel = new Label(pct.toPlainString() + "%");
                    pctLabel.setPrefWidth(50);
                    pctLabel.setAlignment(Pos.CENTER_RIGHT);
                    row.getChildren().addAll(category, bar, spentLimit, pctLabel);
                    budgetOverviewBox.getChildren().add(row);
                }
            }

            // Most recent 8 transactions of the month.
            List<Transaction> recent = transactionService.findByMonth(month);
            recentTable.setItems(FXCollections.observableArrayList(
                    recent.subList(0, Math.min(8, recent.size()))));

            // Income vs expenses over the selected range ending at the selected month, oldest first,
            // so changing the month (or the range) updates this chart like every other dashboard view.
            int range = cashflowRange();
            List<String> chartMonths = trailingMonths(selected, range);
            List<MonthTotal> totals = reportService.monthlyTotals(chartMonths);
            long monthsWithData = totals.stream()
                    .filter(t -> t.getIncome().compareTo(BigDecimal.ZERO) != 0
                            || t.getExpense().compareTo(BigDecimal.ZERO) != 0)
                    .count();
            cashflowSubtitleLabel.setText("Income vs expenses · Last " + range + " months");
            if (monthsWithData < chartMonths.size()) {
                cashflowNoteLabel.setText("Only " + monthsWithData + " of these " + range + " months have transactions so far"
                        + " — the quiet months will fill in as you keep logging.");
                cashflowNoteLabel.setVisible(true);
                cashflowNoteLabel.setManaged(true);
            } else {
                cashflowNoteLabel.setVisible(false);
                cashflowNoteLabel.setManaged(false);
            }
            XYChart.Series<String, Number> incomeSeries = new XYChart.Series<>();
            incomeSeries.setName("Income");
            XYChart.Series<String, Number> expenseSeries = new XYChart.Series<>();
            expenseSeries.setName("Expenses");
            for (MonthTotal total : totals) {
                incomeSeries.getData().add(new XYChart.Data<>(total.getMonth(), total.getIncome().doubleValue()));
                expenseSeries.getData().add(new XYChart.Data<>(total.getMonth(), total.getExpense().doubleValue()));
            }
            comparisonChart.getData().setAll(incomeSeries, expenseSeries);
            // Bar nodes, tooltips, and tick labels only exist after layout: outline the
            // selected month's bars, give every bar an exact tooltip, and mute quiet
            // months' axis labels. Best effort — if the nodes are not reachable, the
            // note above still carries the same information.
            Platform.runLater(() -> finishCashflowChart(totals, selected.toString()));
        } catch (SQLException e) {
            showError("Could not load the dashboard", e);
        }
    }

    /** Sidebar icons from the bundled FontAwesome Free solid font (CC BY 4.0).
     *  If the font fails to load, the buttons simply stay text-only. */
    private void installSidebarIcons() {
        try (InputStream in = getClass().getResourceAsStream("/fonts/fa-solid-900.ttf")) {
            if (in != null) {
                Font.loadFont(in, 14);
            }
        } catch (IOException e) {
            return;
        }
        if (!Font.getFamilies().contains("Font Awesome 6 Free Solid")) {
            return;
        }
        setNavIcon(navOverviewButton, "\uf015");      // house
        setNavIcon(navTransactionsButton, "\uf362");  // right-left arrows
        setNavIcon(navBudgetsButton, "\uf555");       // wallet
        setNavIcon(navReportsButton, "\uf080");       // bar chart
        setNavIcon(navGoalsButton, "\uf140");         // bullseye
        setNavIcon(navLoansButton, "\uf4c0");         // hand holding dollar
    }

    private void setNavIcon(Button button, String glyph) {
        Label icon = new Label(glyph);
        icon.setFont(Font.font("Font Awesome 6 Free Solid", 14));
        button.setGraphic(icon);
    }

    /** The bundled icon font is usable only once it actually loaded. */
    private boolean iconsLoaded() {
        return Font.getFamilies().contains("Font Awesome 6 Free Solid");
    }

    /** Small FontAwesome glyphs on the four stat cards and the alerts panel.
     *  If the font fails to load, the labels simply stay empty and the titles carry it. */
    private void installStatIcons() {
        if (!iconsLoaded()) {
            return;
        }
        setStatIcon(netIconLabel, "\uf24e");      // scale-balanced
        setStatIcon(incomeIconLabel, "\uf0ab");   // circle-arrow-down
        setStatIcon(expenseIconLabel, "\uf0aa");  // circle-arrow-up
        setStatIcon(savingsIconLabel, "\uf4d3");  // piggy-bank
        alertIconLabel.setFont(Font.font("Font Awesome 6 Free Solid", 16));
    }

    private void setStatIcon(Label label, String glyph) {
        label.setText(glyph);
        label.setFont(Font.font("Font Awesome 6 Free Solid", 16));
    }

    /** Sidebar navigation: select a tab and mark its button selected. */
    private void showView(int index) {
        mainTabPane.getSelectionModel().select(index);
        updateNavSelection(index);
    }

    private void updateNavSelection(int index) {
        Button[] buttons = { navOverviewButton, navTransactionsButton, navBudgetsButton,
                navReportsButton, navGoalsButton, navLoansButton };
        for (int i = 0; i < buttons.length; i++) {
            if (i == index) {
                if (!buttons[i].getStyleClass().contains("nav-button-selected")) {
                    buttons[i].getStyleClass().add("nav-button-selected");
                }
            } else {
                buttons[i].getStyleClass().remove("nav-button-selected");
            }
        }
    }

    /** Month-over-month line from DashboardMetrics: text and tone applied together. */
    private void setDelta(Label label, BigDecimal current, BigDecimal previous,
            DashboardMetrics.DeltaKind kind, boolean currentEmpty) {
        DashboardMetrics.DeltaLine line = DashboardMetrics.deltaLine(current, previous, currentEmpty, kind);
        label.setText(line.text());
        label.getStyleClass().removeAll("delta-good", "delta-bad", "delta-neutral");
        label.getStyleClass().add(switch (line.tone()) {
            case GOOD -> "delta-good";
            case BAD -> "delta-bad";
            case NEUTRAL -> "delta-neutral";
        });
    }

    /** One alert-panel state: glyph (blank when the icon font failed to load),
     *  icon color, and the message color override (null keeps the warning yellow). */
    private void setAlertState(String glyph, String iconClass, String textClass) {
        alertIconLabel.setText(iconsLoaded() ? glyph : "");
        alertIconLabel.getStyleClass().removeAll("alert-icon-warn", "alert-icon-ok", "alert-icon-info");
        alertIconLabel.getStyleClass().add(iconClass);
        alertLabel.getStyleClass().removeAll("alert-text-ok", "alert-text-info");
        if (textClass != null) {
            alertLabel.getStyleClass().add(textClass);
        }
    }

    /** One-line actual budget status: how close the closest budget is to its limit. */
    private String budgetStatusSummary(List<BudgetStatus> statuses) {
        BudgetStatus closest = null;
        BigDecimal closestPct = BigDecimal.ZERO;
        for (BudgetStatus status : statuses) {
            BigDecimal pct = status.getLimit().compareTo(BigDecimal.ZERO) == 0 ? BigDecimal.ZERO
                    : status.getSpent().multiply(BigDecimal.valueOf(100))
                            .divide(status.getLimit(), 1, RoundingMode.HALF_UP);
            if (closest == null || pct.compareTo(closestPct) > 0) {
                closest = status;
                closestPct = pct;
            }
        }
        if (closest == null) {
            return "All budgets look fine this month.";
        }
        return "All budgets within limits — closest: " + closest.getCategoryName()
                + " at " + closestPct.toPlainString() + "%.";
    }

    /** Months shown in the cash-flow chart, from the range selector (default 6). */
    private int cashflowRange() {
        String value = cashflowRangeBox == null ? null : cashflowRangeBox.getValue();
        if (value != null && value.startsWith("Last ")) {
            try {
                return Integer.parseInt(value.substring(5, value.indexOf(' ', 5)));
            } catch (NumberFormatException | IndexOutOfBoundsException e) {
                return 6;
            }
        }
        return 6;
    }

    /** Post-layout cash-flow decoration: exact tooltips, the selected month outlined,
     *  quiet months muted on the axis. Never breaks the dashboard if nodes are missing. */
    private void finishCashflowChart(List<MonthTotal> totals, String selectedMonth) {
        try {
            Set<String> quietMonths = new HashSet<>();
            for (MonthTotal total : totals) {
                if (total.getIncome().compareTo(BigDecimal.ZERO) == 0
                        && total.getExpense().compareTo(BigDecimal.ZERO) == 0) {
                    quietMonths.add(total.getMonth());
                }
            }
            for (XYChart.Series<String, Number> series : comparisonChart.getData()) {
                for (XYChart.Data<String, Number> data : series.getData()) {
                    Node node = data.getNode();
                    if (node == null) {
                        continue;
                    }
                    MonthTotal total = totals.stream()
                            .filter(t -> t.getMonth().equals(data.getXValue()))
                            .findFirst().orElse(null);
                    if (total == null) {
                        continue;
                    }
                    String monthLabel = YearMonth.parse(total.getMonth())
                            .format(DateTimeFormatter.ofPattern("MMM yyyy"));
                    String tip = quietMonths.contains(total.getMonth())
                            ? monthLabel + "\nNo activity recorded"
                            : monthLabel + "\nIncome: " + Money.format(total.getIncome())
                                    + "\nExpenses: " + Money.format(total.getExpense());
                    Tooltip.install(node, new Tooltip(tip));
                    if (total.getMonth().equals(selectedMonth)
                            && !node.getStyleClass().contains("current-month-bar")) {
                        node.getStyleClass().add("current-month-bar");
                    }
                }
            }
            for (Node tick : comparisonXAxis.lookupAll("Text")) {
                if (tick instanceof Text text && quietMonths.contains(text.getText())) {
                    text.setFill(Color.web("#64748B"));
                }
            }
        } catch (RuntimeException e) {
            // Chart decoration only — the chart and its note already carry the data.
        }
    }

    /** Signed, colored amounts: + for income, − for expense; color only reinforces the sign. */
    private void installAmountCell(TableColumn<Transaction, BigDecimal> col) {
        col.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(BigDecimal amount, boolean empty) {
                super.updateItem(amount, empty);
                getStyleClass().removeAll("income-value", "expense-value");
                if (empty || amount == null) {
                    setText(null);
                    return;
                }
                Transaction txn = getTableRow().getItem();
                boolean income = txn != null && "INCOME".equals(txn.getType());
                setText((income ? "+" : "−") + Money.format(amount));
                getStyleClass().add(income ? "income-value" : "expense-value");
            }
        });
    }

    /** Plain money columns: Indian formatting, no sign logic (these are magnitudes). */
    private <S> void installMoneyCell(TableColumn<S, BigDecimal> col) {
        col.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(BigDecimal amount, boolean empty) {
                super.updateItem(amount, empty);
                setText(empty || amount == null ? null : Money.format(amount));
            }
        });
    }

    /** A compact empty state: a muted line plus one prominent action. */
    private VBox emptyState(String message, String actionText, Runnable action) {
        Label label = new Label(message);
        label.getStyleClass().add("hint-text");
        Button button = new Button(actionText);
        button.getStyleClass().add("primary-button");
        button.setOnAction(e -> action.run());
        VBox box = new VBox(8, label, button);
        box.setAlignment(Pos.CENTER);
        return box;
    }

    /** Category names render as pills instead of plain text. */
    private void installCategoryBadgeCell(TableColumn<Transaction, String> col) {
        col.setCellFactory(c -> new TableCell<>() {
            private final Label badge = new Label();
            {
                badge.getStyleClass().add("category-badge");
            }
            @Override
            protected void updateItem(String name, boolean empty) {
                super.updateItem(name, empty);
                setText(null);
                if (empty || name == null) {
                    setGraphic(null);
                } else {
                    badge.setText(name);
                    setGraphic(badge);
                }
            }
        });
    }

    // ---------------- Transactions ----------------

    private void loadCategoriesForType() {
        try {
            List<Category> cats = transactionService.findCategoriesByType(typeBox.getValue());
            categoryBox.setItems(FXCollections.observableArrayList(cats));
            if (!cats.isEmpty()) {
                categoryBox.setValue(cats.get(0));
            }
        } catch (SQLException e) {
            showError("Could not load categories", e);
        }
    }

    private void loadExpenseCategoriesForBudgets() {
        try {
            List<Category> cats = transactionService.findCategoriesByType("EXPENSE");
            List<Category> withAll = new ArrayList<>();
            withAll.add(new Category(-1, "All", "EXPENSE"));
            withAll.addAll(cats);
            budgetCategoryBox.setItems(FXCollections.observableArrayList(withAll));
            if (!cats.isEmpty()) {
                budgetCategoryBox.setValue(cats.get(0));
            }
        } catch (SQLException e) {
            showError("Could not load expense categories", e);
        }
    }

    private void loadFilterCategories() {
        try {
            List<Category> cats = new ArrayList<>();
            cats.add(new Category(-1, "All", ""));
            cats.addAll(transactionService.findAllCategories());
            filterCategoryBox.setItems(FXCollections.observableArrayList(cats));
            filterCategoryBox.setValue(cats.get(0));
        } catch (SQLException e) {
            showError("Could not load categories", e);
        }
    }

    private void onAddOrUpdateTransaction() {
        try {
            Category category = categoryBox.getValue();
            BigDecimal amount = Money.parse(amountField.getText());
            LocalDate date = datePicker.getValue();
            String note = noteField.getText().trim();
            String payment = paymentBox.getValue();
            if (editingTxnId != -1) {
                transactionService.updateTransaction(editingTxnId, typeBox.getValue(),
                        category, amount, date, note, payment);
                resetEditState();
            } else {
                transactionService.addTransaction(typeBox.getValue(),
                        category, amount, date, note, payment);
                if ("EXPENSE".equals(typeBox.getValue())) {
                    Optional<String> warning = budgetService.checkAfterAdd(
                            category.getId(), YearMonth.from(date).toString());
                    warning.ifPresent(this::showWarning);
                }
            }
            amountField.clear();
            noteField.clear();
            showAllTransactions();
            loadDashboard();
            loadBudgets();
            loadReport();
        } catch (IllegalArgumentException e) {
            showWarning(e.getMessage());
        } catch (SQLException e) {
            showError("Could not save the transaction", e);
        }
    }

    private void onEditTransaction() {
        Transaction selected = txnTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showWarning("Select a transaction in the table first.");
            return;
        }
        // Setting the type reloads the category list, then we pick the row's category.
        typeBox.setValue(selected.getType());
        for (Category cat : categoryBox.getItems()) {
            if (cat.getId() == selected.getCategoryId()) {
                categoryBox.setValue(cat);
                break;
            }
        }
        amountField.setText(Money.format(selected.getAmount()));
        datePicker.setValue(selected.getDate());
        noteField.setText(selected.getNote() == null ? "" : selected.getNote());
        paymentBox.setValue(selected.getPaymentMethod() == null ? "Cash" : selected.getPaymentMethod());
        editingTxnId = selected.getId();
        addButton.setText("Update");
        cancelEditButton.setVisible(true);
    }

    private void resetEditState() {
        editingTxnId = -1;
        addButton.setText("Add");
        cancelEditButton.setVisible(false);
        amountField.clear();
        noteField.clear();
        datePicker.setValue(LocalDate.now());
        typeBox.setValue("EXPENSE");
        paymentBox.setValue("Cash");
    }

    private void onApplyFilter() {
        try {
            String type = "All".equals(filterTypeBox.getValue()) ? null : filterTypeBox.getValue();
            Category category = filterCategoryBox.getValue();
            int categoryId = (category == null) ? -1 : category.getId();
            String month = "All".equals(filterMonthBox.getValue()) ? null : filterMonthBox.getValue();
            txnTable.setItems(FXCollections.observableArrayList(
                    transactionService.findFiltered(type, categoryId, month)));
        } catch (SQLException e) {
            showError("Filter failed", e);
        }
    }

    private void onSearch() {
        String keyword = searchField.getText() == null ? "" : searchField.getText().trim();
        try {
            List<Transaction> rows = keyword.isEmpty()
                    ? transactionService.findAll()
                    : transactionService.search(keyword);
            txnTable.setItems(FXCollections.observableArrayList(rows));
        } catch (SQLException e) {
            showError("Search failed", e);
        }
    }

    private void showAllTransactions() {
        try {
            txnTable.setItems(FXCollections.observableArrayList(transactionService.findAll()));
        } catch (SQLException e) {
            showError("Could not load transactions", e);
        }
    }

    private void onDeleteTransaction() {
        Transaction selected = txnTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showWarning("Select a transaction in the table first.");
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        styleDialog(confirm.getDialogPane());
        confirm.setTitle("Delete transaction");
        confirm.setHeaderText(null);
        confirm.setContentText("Delete this transaction?");
        Optional<ButtonType> answer = confirm.showAndWait();
        if (answer.isPresent() && answer.get() == ButtonType.OK) {
            try {
                transactionService.deleteTransaction(selected.getId());
                showAllTransactions();
                loadDashboard();
                loadBudgets();
                loadReport();
            } catch (SQLException e) {
                showError("Could not delete the transaction", e);
            }
        }
    }

    // ---------------- Budgets ----------------

    private void onSetBudget() {
        try {
            Category category = budgetCategoryBox.getValue();
            String month = budgetMonthField.getText().trim();
            BigDecimal amount = Money.parse(budgetAmountField.getText());
            if (category != null && category.getId() == -1) {
                // "All": the same limit for every expense category at once.
                for (Category cat : budgetCategoryBox.getItems()) {
                    if (cat.getId() != -1) {
                        budgetService.setBudget(cat, month, amount);
                    }
                }
            } else {
                budgetService.setBudget(category, month, amount);
            }
            budgetAmountField.clear();
            loadBudgets();
            loadDashboard();
        } catch (IllegalArgumentException e) {
            showWarning(e.getMessage());
        } catch (SQLException e) {
            showError("Could not save the budget", e);
        }
    }

    private void onCopyBudgets() {
        String month = budgetMonthField.getText().trim();
        if (!month.matches("\\d{4}-\\d{2}")) {
            showWarning("Enter the month to copy from as YYYY-MM.");
            return;
        }
        String nextMonth = YearMonth.parse(month).plusMonths(1).toString();
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        styleDialog(confirm.getDialogPane());
        confirm.setTitle("Copy budgets");
        confirm.setHeaderText(null);
        confirm.setContentText("Copy the " + month + " budgets to " + nextMonth
                + "? Budgets never carry forward on their own — this only happens because you asked.");
        Optional<ButtonType> answer = confirm.showAndWait();
        if (answer.isPresent() && answer.get() == ButtonType.OK) {
            try {
                budgetService.copyToNextMonth(month);
                showInfo("Budgets copied to " + nextMonth + ".");
                loadBudgets();
                loadDashboard();
            } catch (IllegalArgumentException e) {
                showWarning(e.getMessage());
            } catch (SQLException e) {
                showError("Could not copy budgets", e);
            }
        }
    }

    private void onDeleteBudget() {
        BudgetStatus selected = budgetTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showWarning("Select a budget in the table first.");
            return;
        }
        Category category = null;
        for (Category cat : budgetCategoryBox.getItems()) {
            if (cat.getId() != -1 && cat.getName().equals(selected.getCategoryName())) {
                category = cat;
                break;
            }
        }
        if (category == null) {
            showWarning("That category is no longer available.");
            return;
        }
        String month = budgetMonthField.getText().trim();
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        styleDialog(confirm.getDialogPane());
        confirm.setTitle("Delete budget");
        confirm.setHeaderText(null);
        confirm.setContentText("Delete the budget for '" + selected.getCategoryName()
                + "' for " + month + "?");
        Optional<ButtonType> answer = confirm.showAndWait();
        if (answer.isPresent() && answer.get() == ButtonType.OK) {
            try {
                budgetService.deleteBudget(category, month);
                loadBudgets();
                loadDashboard();
            } catch (SQLException e) {
                showError("Could not delete the budget", e);
            }
        }
    }

    private void loadBudgets() {
        String month = budgetMonthField.getText().trim();
        if (!month.matches("\\d{4}-\\d{2}")) {
            return;
        }
        try {
            budgetTable.setItems(FXCollections.observableArrayList(budgetService.getStatuses(month)));
        } catch (SQLException e) {
            showError("Could not load budgets", e);
        }
    }

    // ---------------- Reports ----------------

    private void loadReport() {
        String period = periodBox.getValue();
        if (period == null) {
            return;
        }
        try {
            List<CategorySummary> rows;
            String periodLabel;
            switch (period) {
                case "Yearly": {
                    String year = reportYearBox.getValue();
                    rows = reportService.yearlyReport(year);
                    periodLabel = "Year: " + year;
                    break;
                }
                case "Weekly": {
                    LocalDate today = LocalDate.now();
                    LocalDate weekStart = ReportService.weekStart(today);
                    LocalDate weekEnd = ReportService.weekEnd(today);
                    rows = reportService.weeklyReport(weekStart, weekEnd);
                    periodLabel = "Week: " + weekStart + " to " + weekEnd;
                    break;
                }
                default: {
                    String month = reportMonthBox.getValue();
                    rows = reportService.monthlyReport(month);
                    periodLabel = "Month: " + month;
                    break;
                }
            }
            reportTable.setItems(FXCollections.observableArrayList(rows));
            List<PieChart.Data> slices = new ArrayList<>();
            BigDecimal total = BigDecimal.ZERO;
            for (CategorySummary row : rows) {
                slices.add(new PieChart.Data(row.getCategoryName(), row.getTotal().doubleValue()));
                total = total.add(row.getTotal());
            }
            expenseChart.setData(FXCollections.observableArrayList(slices));
            expenseChart.setTitle("Expenses by category — " + periodLabel);
            reportPeriodLabel.setText(periodLabel);
            reportTotalLabel.setText("Total expenses: " + Money.format(total));
            reportTopLabel.setText(ReportService.topCategoriesText(rows, total));
        } catch (SQLException e) {
            showError("Could not load the report", e);
        }
    }

    private void onExportCsv() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Export transactions");
        chooser.setInitialFileName("transactions.csv");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV files", "*.csv"));
        File file = chooser.showSaveDialog(null);
        if (file == null) {
            return;
        }
        try {
            List<Transaction> rows = transactionService.findAll();
            try (FileWriter writer = new FileWriter(file)) {
                writer.write("id,type,category,amount,date,payment_method,note\n");
                for (Transaction txn : rows) {
                    writer.write(txn.getId() + ","
                            + csv(txn.getType()) + ","
                            + csv(txn.getCategoryName()) + ","
                            + txn.getAmount().toPlainString() + ","
                            + txn.getDate() + ","
                            + csv(txn.getPaymentMethod()) + ","
                            + csv(txn.getNote()) + "\n");
                }
            }
            showInfo("Exported " + rows.size() + " transactions to " + file.getAbsolutePath());
        } catch (SQLException | IOException e) {
            showError("Export failed", e);
        }
    }

    private String csv(String value) {
        if (value == null) {
            return "";
        }
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }

    // ---------------- Savings goals ----------------

    private void onEditGoal() {
        SavingsGoal selected = goalTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showWarning("Select a goal in the table first.");
            return;
        }
        goalNameField.setText(selected.getName());
        goalTargetField.setText(Money.format(selected.getTargetAmount()));
        goalDeadlinePicker.setValue(selected.getDeadline());
        editingGoalId = selected.getId();
        editingGoalSaved = selected.getSavedAmount();
        addGoalButton.setText("Update goal");
        cancelGoalEditButton.setVisible(true);
    }

    private void resetGoalEditState() {
        editingGoalId = -1;
        editingGoalSaved = BigDecimal.ZERO;
        addGoalButton.setText("Add goal");
        cancelGoalEditButton.setVisible(false);
        goalNameField.clear();
        goalTargetField.clear();
        goalDeadlinePicker.setValue(LocalDate.now().plusYears(1));
    }

    private void onAddGoal() {
        try {
            String name = goalNameField.getText().trim();
            BigDecimal target = Money.parse(goalTargetField.getText());
            LocalDate deadline = goalDeadlinePicker.getValue();
            if (editingGoalId != -1) {
                goalService.updateGoal(editingGoalId, name, target, deadline, editingGoalSaved);
                resetGoalEditState();
            } else {
                goalService.createGoal(name, target, deadline);
                goalNameField.clear();
                goalTargetField.clear();
            }
            loadGoals();
        } catch (IllegalArgumentException e) {
            showWarning(e.getMessage());
        } catch (SQLException e) {
            showError("Could not save the goal", e);
        }
    }

    private void onContribute() {
        SavingsGoal selected = goalTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showWarning("Select a goal in the table first.");
            return;
        }
        try {
            BigDecimal amount = Money.parse(contributionAmountField.getText());
            goalService.addContribution(selected.getId(), amount, LocalDate.now());
            contributionAmountField.clear();
            loadGoals();
        } catch (IllegalArgumentException e) {
            showWarning(e.getMessage());
        } catch (SQLException e) {
            showError("Could not save the contribution", e);
        }
    }

    private void onDeleteGoal() {
        SavingsGoal selected = goalTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showWarning("Select a goal in the table first.");
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        styleDialog(confirm.getDialogPane());
        confirm.setTitle("Delete goal");
        confirm.setHeaderText(null);
        confirm.setContentText("Delete the goal '" + selected.getName()
                + "' and all its contributions?");
        Optional<ButtonType> answer = confirm.showAndWait();
        if (answer.isPresent() && answer.get() == ButtonType.OK) {
            try {
                goalService.deleteGoal(selected.getId());
                loadGoals();
            } catch (SQLException e) {
                showError("Could not delete the goal", e);
            }
        }
    }

    private void loadGoals() {
        try {
            goalTable.setItems(FXCollections.observableArrayList(goalService.findAllWithSaved()));
        } catch (SQLException e) {
            showError("Could not load savings goals", e);
        }
    }

    // ---------------- Loans ----------------

    private void onAddLoan() {
        try {
            String person = loanPersonField.getText().trim();
            BigDecimal amount = Money.parse(loanAmountField.getText());
            LocalDate lentDate = loanDatePicker.getValue();
            String note = loanNoteField.getText() == null ? "" : loanNoteField.getText().trim();
            loanService.createLoan(person, amount, lentDate, note);
            loanPersonField.clear();
            loanAmountField.clear();
            loanNoteField.clear();
            loanDatePicker.setValue(LocalDate.now());
            loadLoans();
        } catch (IllegalArgumentException e) {
            showWarning(e.getMessage());
        } catch (SQLException e) {
            showError("Could not save the loan", e);
        }
    }

    private void onRecordRepayment() {
        Loan selected = loanTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showWarning("Select a loan in the table first.");
            return;
        }
        try {
            BigDecimal amount = Money.parse(repaymentAmountField.getText());
            loanService.addRepayment(selected.getId(), amount, selected.getRemaining(), LocalDate.now());
            repaymentAmountField.clear();
            loadLoans();
        } catch (IllegalArgumentException e) {
            showWarning(e.getMessage());
        } catch (SQLException e) {
            showError("Could not save the repayment", e);
        }
    }

    private void onDeleteLoan() {
        Loan selected = loanTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showWarning("Select a loan in the table first.");
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        styleDialog(confirm.getDialogPane());
        confirm.setTitle("Delete loan");
        confirm.setHeaderText(null);
        confirm.setContentText("Delete the loan to '" + selected.getPersonName()
                + "' and all its repayments?");
        Optional<ButtonType> answer = confirm.showAndWait();
        if (answer.isPresent() && answer.get() == ButtonType.OK) {
            try {
                loanService.deleteLoan(selected.getId());
                loadLoans();
            } catch (SQLException e) {
                showError("Could not delete the loan", e);
            }
        }
    }

    private void loadLoans() {
        try {
            List<Loan> loans = loanService.findAllWithReturned();
            loanTable.setItems(FXCollections.observableArrayList(loans));
            BigDecimal outstanding = BigDecimal.ZERO;
            int openCount = 0;
            for (Loan loan : loans) {
                if ("OPEN".equals(loan.getStatus())) {
                    outstanding = outstanding.add(loan.getRemaining());
                    openCount++;
                }
            }
            loanSummaryLabel.setText(loans.isEmpty()
                    ? "No loans yet — add one above."
                    : "Total still owed: " + Money.format(outstanding)
                            + " across " + openCount + " open loan" + (openCount == 1 ? "" : "s") + ".");
        } catch (SQLException e) {
            showError("Could not load loans", e);
        }
    }

    // ---------------- CSV import ----------------

    private void onImportCsv() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Import transactions from CSV");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV files", "*.csv"));
        File file = chooser.showOpenDialog(null);
        if (file == null) {
            return;
        }
        List<String[]> allRows;
        try {
            allRows = CsvParser.parse(Files.readString(file.toPath(), StandardCharsets.UTF_8));
        } catch (IOException e) {
            showError("Could not read the CSV file", e);
            return;
        }
        if (allRows.isEmpty()) {
            showWarning("The CSV file is empty.");
            return;
        }
        List<Category> allCategories;
        Set<String> existingKeys = new HashSet<>();
        try {
            allCategories = transactionService.findAllCategories();
            for (Transaction txn : transactionService.findAll()) {
                existingKeys.add(ImportService.duplicateKey(txn));
            }
        } catch (SQLException e) {
            showError("Could not load existing transactions", e);
            return;
        }

        int columnCount = 0;
        for (String[] row : allRows) {
            columnCount = Math.max(columnCount, row.length);
        }
        List<String> columnChoices = new ArrayList<>();
        columnChoices.add("—");
        for (int i = 1; i <= columnCount; i++) {
            columnChoices.add("Column " + i);
        }

        CheckBox headerBox = new CheckBox("First row is a header");
        headerBox.setSelected(ImportService.isHeaderRow(allRows.get(0)));

        ComboBox<String> dateCombo = new ComboBox<>(FXCollections.observableArrayList(columnChoices));
        ComboBox<String> amountCombo = new ComboBox<>(FXCollections.observableArrayList(columnChoices));
        ComboBox<String> categoryCombo = new ComboBox<>(FXCollections.observableArrayList(columnChoices));
        ComboBox<String> noteCombo = new ComboBox<>(FXCollections.observableArrayList(columnChoices));
        ComboBox<String> typeCombo = new ComboBox<>(FXCollections.observableArrayList(columnChoices));
        ComboBox<String> paymentCombo = new ComboBox<>(FXCollections.observableArrayList(columnChoices));

        ComboBox<Category> defaultExpenseCombo = new ComboBox<>();
        ComboBox<Category> defaultIncomeCombo = new ComboBox<>();
        try {
            List<Category> expenseCats = transactionService.findCategoriesByType("EXPENSE");
            List<Category> incomeCats = transactionService.findCategoriesByType("INCOME");
            defaultExpenseCombo.setItems(FXCollections.observableArrayList(expenseCats));
            defaultIncomeCombo.setItems(FXCollections.observableArrayList(incomeCats));
            defaultExpenseCombo.setValue(pickCategory(expenseCats, "Other"));
            defaultIncomeCombo.setValue(pickCategory(incomeCats, "Other Income"));
        } catch (SQLException e) {
            showError("Could not load categories", e);
            return;
        }

        TableView<Transaction> previewTable = new TableView<>();
        TableColumn<Transaction, LocalDate> previewDateCol = new TableColumn<>("Date");
        previewDateCol.setCellValueFactory(new PropertyValueFactory<>("date"));
        TableColumn<Transaction, String> previewTypeCol = new TableColumn<>("Type");
        previewTypeCol.setCellValueFactory(new PropertyValueFactory<>("type"));
        TableColumn<Transaction, String> previewCategoryCol = new TableColumn<>("Category");
        previewCategoryCol.setCellValueFactory(new PropertyValueFactory<>("categoryName"));
        TableColumn<Transaction, BigDecimal> previewAmountCol = new TableColumn<>("Amount");
        previewAmountCol.setCellValueFactory(new PropertyValueFactory<>("amount"));
        TableColumn<Transaction, String> previewNoteCol = new TableColumn<>("Note");
        previewNoteCol.setCellValueFactory(new PropertyValueFactory<>("note"));
        previewTable.getColumns().addAll(
                previewDateCol, previewTypeCol, previewCategoryCol, previewAmountCol, previewNoteCol);
        previewTable.setPrefHeight(200);

        Label summaryLabel = new Label();
        Label skippedLabel = new Label();
        skippedLabel.setWrapText(true);

        Dialog<ButtonType> dialog = new Dialog<>();
        styleDialog(dialog.getDialogPane());
        dialog.setTitle("Import transactions from CSV");
        dialog.setHeaderText("Map the columns, check the preview, then import. "
                + "Nothing is saved until you press Import.");
        ButtonType importType = new ButtonType("Import");
        dialog.getDialogPane().getButtonTypes().addAll(importType, ButtonType.CANCEL);
        Button importButtonNode = (Button) dialog.getDialogPane().lookupButton(importType);

        final ImportService.ImportResult[] current = new ImportService.ImportResult[1];
        Runnable recompute = () -> {
            ImportService.ColumnMapping mapping = new ImportService.ColumnMapping();
            mapping.dateIdx = columnIndexOf(dateCombo);
            mapping.amountIdx = columnIndexOf(amountCombo);
            mapping.categoryIdx = columnIndexOf(categoryCombo);
            mapping.noteIdx = columnIndexOf(noteCombo);
            mapping.typeIdx = columnIndexOf(typeCombo);
            mapping.paymentIdx = columnIndexOf(paymentCombo);
            List<String[]> dataRows = headerBox.isSelected()
                    ? allRows.subList(1, allRows.size()) : allRows;
            ImportService.ImportResult result = ImportService.resolveRows(dataRows, mapping,
                    allCategories, defaultExpenseCombo.getValue(), defaultIncomeCombo.getValue(),
                    existingKeys);
            current[0] = result;
            previewTable.setItems(FXCollections.observableArrayList(result.toImport));
            summaryLabel.setText(result.toImport.size() + " new · " + result.duplicates.size()
                    + " duplicates · " + result.invalid.size() + " invalid");
            List<String> skipped = new ArrayList<>();
            skipped.addAll(result.duplicates);
            skipped.addAll(result.invalid);
            if (skipped.isEmpty()) {
                skippedLabel.setText("");
            } else if (skipped.size() <= 8) {
                skippedLabel.setText("Skipped:\n" + String.join("\n", skipped));
            } else {
                skippedLabel.setText("Skipped:\n" + String.join("\n", skipped.subList(0, 8))
                        + "\n... and " + (skipped.size() - 8) + " more.");
            }
            importButtonNode.setDisable(result.toImport.isEmpty());
        };

        Runnable applyAutoMapping = () -> {
            if (headerBox.isSelected()) {
                ImportService.ColumnMapping mapping = ImportService.autoMapping(allRows.get(0));
                dateCombo.setValue(choiceFor(mapping.dateIdx));
                amountCombo.setValue(choiceFor(mapping.amountIdx));
                categoryCombo.setValue(choiceFor(mapping.categoryIdx));
                noteCombo.setValue(choiceFor(mapping.noteIdx));
                typeCombo.setValue(choiceFor(mapping.typeIdx));
                paymentCombo.setValue(choiceFor(mapping.paymentIdx));
            } else {
                dateCombo.setValue("—");
                amountCombo.setValue("—");
                categoryCombo.setValue("—");
                noteCombo.setValue("—");
                typeCombo.setValue("—");
                paymentCombo.setValue("—");
            }
        };

        dateCombo.setOnAction(e -> recompute.run());
        amountCombo.setOnAction(e -> recompute.run());
        categoryCombo.setOnAction(e -> recompute.run());
        noteCombo.setOnAction(e -> recompute.run());
        typeCombo.setOnAction(e -> recompute.run());
        paymentCombo.setOnAction(e -> recompute.run());
        defaultExpenseCombo.setOnAction(e -> recompute.run());
        defaultIncomeCombo.setOnAction(e -> recompute.run());
        headerBox.setOnAction(e -> {
            applyAutoMapping.run();
            recompute.run();
        });

        GridPane mappingGrid = new GridPane();
        mappingGrid.setHgap(8);
        mappingGrid.setVgap(6);
        mappingGrid.addRow(0, new Label("Date:"), dateCombo);
        mappingGrid.addRow(1, new Label("Amount:"), amountCombo);
        mappingGrid.addRow(2, new Label("Category:"), categoryCombo);
        mappingGrid.addRow(3, new Label("Note:"), noteCombo);
        mappingGrid.addRow(4, new Label("Type (optional):"), typeCombo);
        mappingGrid.addRow(5, new Label("Payment (optional):"), paymentCombo);
        HBox defaultsRow = new HBox(8,
                new Label("Default expense category:"), defaultExpenseCombo,
                new Label("Default income category:"), defaultIncomeCombo);
        defaultsRow.setAlignment(Pos.CENTER_LEFT);
        VBox content = new VBox(10, headerBox, mappingGrid, defaultsRow,
                summaryLabel, previewTable, skippedLabel);
        content.setPrefWidth(640);
        dialog.getDialogPane().setContent(content);

        applyAutoMapping.run();
        recompute.run();
        Optional<ButtonType> answer = dialog.showAndWait();
        if (answer.isPresent() && answer.get() == importType
                && current[0] != null && !current[0].toImport.isEmpty()) {
            try {
                importService.importRows(current[0].toImport);
                int imported = current[0].toImport.size();
                int dups = current[0].duplicates.size();
                int invalid = current[0].invalid.size();
                showAllTransactions();
                loadDashboard();
                loadBudgets();
                loadReport();
                showInfo("Imported " + imported + " transactions ("
                        + dups + " duplicates skipped, " + invalid + " invalid rows skipped).");
            } catch (SQLException e) {
                showError("Import failed", e);
            }
        }
    }

    /** ComboBox selection to a 0-based column index; -1 for "—". */
    private int columnIndexOf(ComboBox<String> combo) {
        return combo.getSelectionModel().getSelectedIndex() - 1;
    }

    private String choiceFor(int idx) {
        return idx < 0 ? "—" : "Column " + (idx + 1);
    }

    private Category pickCategory(List<Category> cats, String name) {
        for (Category cat : cats) {
            if (cat.getName().equalsIgnoreCase(name)) {
                return cat;
            }
        }
        return cats.isEmpty() ? null : cats.get(0);
    }

    // ---------------- Helpers ----------------

    /** Current month first, then the previous ones, all as "YYYY-MM". */
    /**
     * The month the tracker opens on: November 2026 while it is still upcoming or current,
     * the current month after that. Picking a new month never touches other months' data.
     */
    private static YearMonth defaultMonth() {
        YearMonth november2026 = YearMonth.of(2026, 11);
        return november2026.isBefore(YearMonth.now()) ? YearMonth.now() : november2026;
    }

    /** Months offered in pickers, newest first: one future month, then the current month,
     *  then the 11 before it. */
    private List<String> monthsForPickers() {
        List<String> months = new ArrayList<>();
        YearMonth start = YearMonth.now().plusMonths(1);
        for (int i = 0; i < 13; i++) {
            months.add(start.minusMonths(i).toString());
        }
        return months;
    }

    /** Trailing months ending at the given month, oldest first, for the cash-flow chart. */
    private List<String> trailingMonths(YearMonth end, int count) {
        List<String> months = new ArrayList<>();
        for (int i = count - 1; i >= 0; i--) {
            months.add(end.minusMonths(i).toString());
        }
        return months;
    }

    /** Dialogs and alerts open their own windows, so they do not inherit the
     * main scene's theme. Give each one the same stylesheet explicitly. */
    private static void styleDialog(DialogPane pane) {
        pane.getStylesheets().add(MainController.class.getResource("/css/style.css").toExternalForm());
    }

    private void showWarning(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        styleDialog(alert.getDialogPane());
        alert.setTitle("Personal Finance Tracker");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        styleDialog(alert.getDialogPane());
        alert.setTitle("Personal Finance Tracker");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Database errors never reach the screen as raw messages. The user sees a
     * plain explanation; the technical detail goes to the console only.
     */
    private void showError(String what, Exception e) {
        String detail;
        if (e instanceof SQLException sql) {
            String state = sql.getSQLState();
            if (state != null && state.startsWith("08")) {
                detail = "Could not connect to MySQL. Check that MySQL is running and db.properties is correct.";
            } else if (state != null && state.startsWith("23")) {
                detail = "That conflicts with existing data (for example, a duplicate entry).";
            } else {
                detail = "A database error occurred. Please try again.";
            }
            System.err.println(what + ": " + e.getMessage());
        } else {
            detail = e.getMessage();
        }
        Alert alert = new Alert(Alert.AlertType.ERROR);
        styleDialog(alert.getDialogPane());
        alert.setTitle("Personal Finance Tracker");
        alert.setHeaderText(what);
        alert.setContentText(detail);
        alert.showAndWait();
    }
}
