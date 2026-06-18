package in.maheshshelakee.moneymanager.service.admin;

import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.pdf.*;
import in.maheshshelakee.moneymanager.entity.ReportEntity;
import in.maheshshelakee.moneymanager.entity.IncomeEntity;
import in.maheshshelakee.moneymanager.entity.ExpenseEntity;
import in.maheshshelakee.moneymanager.entity.FriendExpense;
import in.maheshshelakee.moneymanager.entity.User;
import in.maheshshelakee.moneymanager.entity.BudgetEntity;
import in.maheshshelakee.moneymanager.repository.ReportRepository;
import in.maheshshelakee.moneymanager.repository.IncomeRepository;
import in.maheshshelakee.moneymanager.repository.ExpenseRepository;
import in.maheshshelakee.moneymanager.repository.FriendExpenseRepository;
import in.maheshshelakee.moneymanager.repository.UserRepository;
import in.maheshshelakee.moneymanager.repository.BudgetRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.awt.Color;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminReportService {

    private final ReportRepository reportRepository;
    private final IncomeRepository incomeRepository;
    private final ExpenseRepository expenseRepository;
    private final FriendExpenseRepository friendExpenseRepository;
    private final UserRepository userRepository;
    private final BudgetRepository budgetRepository;

    private static final String REPORTS_DIR = "reports_archive";

    public List<ReportEntity> getReportHistory() {
        return reportRepository.findAll();
    }

    public Optional<ReportEntity> getReportById(Long id) {
        return reportRepository.findById(id);
    }

    private double calculateSpentAmount(BudgetEntity budget) {
        LocalDate now = LocalDate.now();
        LocalDate startOfPeriod = "WEEKLY".equals(budget.getPeriod()) 
                ? now.minusDays(now.getDayOfWeek().getValue() - 1) 
                : now.withDayOfMonth(1);

        return expenseRepository.findAll().stream()
                .filter(e -> e.getUser().getId().equals(budget.getUser().getId())
                        && e.getCategory().equalsIgnoreCase(budget.getCategory())
                        && !e.getExpenseDate().isBefore(startOfPeriod))
                .mapToDouble(ExpenseEntity::getAmount).sum();
    }

    /**
     * Trigger manual report generation
     */
    @Transactional
    public ReportEntity triggerReportGeneration(String type, String fromDate, String toDate, Long userId, String category, String format, String adminEmail) {
        // 1. Create PENDING report record
        String extension = format.toUpperCase().equals("PDF") ? ".pdf" : ".csv";
        String fileName = type.toLowerCase() + "_" + System.currentTimeMillis() + extension;
        
        ReportEntity report = ReportEntity.builder()
                .reportType(type)
                .dateRangeFrom(fromDate)
                .dateRangeTo(toDate)
                .format(format)
                .fileName(fileName)
                .filePath(REPORTS_DIR + "/" + fileName)
                .status("PENDING")
                .generatedBy(adminEmail != null ? adminEmail : "SYSTEM")
                .build();
        
        report = reportRepository.save(report);

        // 2. Execute actual file compilation
        try {
            ensureReportsDirectoryExists();
            File targetFile = new File(REPORTS_DIR, fileName);
            
            if (format.equalsIgnoreCase("PDF")) {
                generatePdfFile(targetFile, type, fromDate, toDate, userId, category);
            } else {
                generateCsvFile(targetFile, type, fromDate, toDate, userId, category);
            }
            
            report.setStatus("COMPLETED");
        } catch (Exception e) {
            log.error("Failed to generate report", e);
            report.setStatus("FAILED");
        }
        
        return reportRepository.save(report);
    }

    /**
     * Streams CSV content straight to output stream to handle large datasets efficiently.
     */
    public void streamCsvReport(String type, String fromDate, String toDate, Long userId, String category, OutputStream os) throws IOException {
        Writer writer = new BufferedWriter(new OutputStreamWriter(os));
        LocalDate start = LocalDate.parse(fromDate);
        LocalDate end = LocalDate.parse(toDate);

        if (type.equalsIgnoreCase("FINANCIAL_SUMMARY") || type.equalsIgnoreCase("TRANSACTION_AUDIT")) {
            writer.write("ID,Type,User,Amount,Category,Date,Title/Source,Flagged,PaymentMethod,Notes\n");
            
            // Incomes
            List<IncomeEntity> incomes = incomeRepository.findByDateBetweenOrderByDateDesc(start, end);
            for (IncomeEntity inc : incomes) {
                if (userId != null && !inc.getUser().getId().equals(userId)) continue;
                if (category != null && !category.isEmpty() && !inc.getCategory().equalsIgnoreCase(category)) continue;
                
                writer.write(String.format("%d,INFLOW,%s,%.2f,%s,%s,%s,%s,Standard,-\n",
                        inc.getId(), inc.getUser().getEmail(), inc.getAmount(), inc.getCategory(),
                        inc.getDate().toString(), inc.getSource(), inc.getFlagged() ? "YES" : "NO"));
            }

            // Expenses
            List<ExpenseEntity> expenses = expenseRepository.findByExpenseDateBetweenOrderByExpenseDateDesc(start, end);
            for (ExpenseEntity exp : expenses) {
                if (userId != null && !exp.getUser().getId().equals(userId)) continue;
                if (category != null && !category.isEmpty() && !exp.getCategory().equalsIgnoreCase(category)) continue;
                
                writer.write(String.format("%d,OUTFLOW,%s,%.2f,%s,%s,%s,%s,%s,%s\n",
                        exp.getId(), exp.getUser().getEmail(), exp.getAmount(), exp.getCategory(),
                        exp.getExpenseDate().toString(), exp.getTitle(), exp.getFlagged() ? "YES" : "NO",
                        exp.getPaymentMethod(), exp.getNote() != null ? exp.getNote().replace(",", " ") : ""));
            }

            // Friend Expenses
            List<FriendExpense> friendExpenses = friendExpenseRepository.findByExpenseDateBetweenOrderByExpenseDateDesc(start, end);
            for (FriendExpense fe : friendExpenses) {
                if (userId != null && !fe.getUser().getId().equals(userId)) continue;
                if (category != null && !category.isEmpty() && !fe.getCategory().equalsIgnoreCase(category)) continue;
                
                writer.write(String.format("%d,OUTFLOW,%s,%.2f,%s,%s,%s,%s,Friend Outflow,%s\n",
                        fe.getId(), fe.getUser().getEmail(), fe.getAmount(), fe.getCategory(),
                        fe.getExpenseDate().toString(), "Spend on: " + fe.getFriendName(), fe.getAmount() > 50000 ? "YES" : "NO",
                        fe.getDescription() != null ? fe.getDescription().replace(",", " ") : ""));
            }
        } 
        else if (type.equalsIgnoreCase("USER_ACTIVITY")) {
            writer.write("User ID,Full Name,Email,Role,Status,Verified,Join Date\n");
            List<User> users = userRepository.findAll();
            for (User u : users) {
                if (userId != null && !u.getId().equals(userId)) continue;
                writer.write(String.format("%d,%s,%s,%s,%s,%s,%s\n",
                        u.getId(), u.getFullName(), u.getEmail(), u.getRole().name(),
                        u.getStatus().name(), u.getIsVerified() ? "YES" : "NO",
                        u.getCreatedAt() != null ? u.getCreatedAt().toLocalDate().toString() : "N/A"));
            }
        } 
        else if (type.equalsIgnoreCase("BUDGET_COMPLIANCE")) {
            writer.write("Budget ID,User,Category,Allocated Target,Spent Amount,Remaining,Period,Status\n");
            List<BudgetEntity> budgets = budgetRepository.findAll();
            for (BudgetEntity b : budgets) {
                if (userId != null && !b.getUser().getId().equals(userId)) continue;
                if (category != null && !category.isEmpty() && !b.getCategory().equalsIgnoreCase(category)) continue;
                
                double spentAmount = calculateSpentAmount(b);
                double remaining = b.getAmount() - spentAmount;
                String status = spentAmount > b.getAmount() ? "OVER_BUDGET" : (spentAmount > b.getAmount() * 0.9 ? "CRITICAL" : "ON_TRACK");
                writer.write(String.format("%d,%s,%s,%.2f,%.2f,%.2f,%s,%s\n",
                        b.getId(), b.getUser().getEmail(), b.getCategory(), b.getAmount(),
                        spentAmount, remaining, b.getPeriod(), status));
            }
        }
        writer.flush();
    }

    private void ensureReportsDirectoryExists() throws IOException {
        Path path = Paths.get(REPORTS_DIR);
        if (!Files.exists(path)) {
            Files.createDirectories(path);
        }
    }

    private void generateCsvFile(File file, String type, String fromDate, String toDate, Long userId, String category) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(file)) {
            streamCsvReport(type, fromDate, toDate, userId, category, fos);
        }
    }

    /**
     * Generates styled PDF reports using OpenPDF.
     */
    private void generatePdfFile(File file, String type, String fromDate, String toDate, Long userId, String category) throws Exception {
        LocalDate start = LocalDate.parse(fromDate);
        LocalDate end = LocalDate.parse(toDate);
        
        Document document = new Document(PageSize.A4, 36, 36, 54, 36);
        PdfWriter writer = PdfWriter.getInstance(document, new FileOutputStream(file));
        document.open();

        // Title and Header
        Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, Color.decode("#6366f1"));
        Font subFont = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.GRAY);
        Font boldText = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.DARK_GRAY);
        
        Paragraph title = new Paragraph("CREDOWALLET ADMIN: SYSTEM AUDIT", titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        document.add(title);

        Paragraph subtitle = new Paragraph("Report Type: " + type + "  |  Date Range: " + fromDate + " to " + toDate, subFont);
        subtitle.setAlignment(Element.ALIGN_CENTER);
        subtitle.setSpacingAfter(20);
        document.add(subtitle);

        // Core Financial Metrics Box if type is FINANCIAL_SUMMARY
        if (type.equalsIgnoreCase("FINANCIAL_SUMMARY")) {
            List<IncomeEntity> incomes = incomeRepository.findByDateBetweenOrderByDateDesc(start, end);
            List<ExpenseEntity> expenses = expenseRepository.findByExpenseDateBetweenOrderByExpenseDateDesc(start, end);
            List<FriendExpense> friendExpenses = friendExpenseRepository.findByExpenseDateBetweenOrderByExpenseDateDesc(start, end);
            
            double totalIn = incomes.stream()
                    .filter(i -> userId == null || i.getUser().getId().equals(userId))
                    .mapToDouble(IncomeEntity::getAmount).sum();
            double totalOut = expenses.stream()
                    .filter(e -> userId == null || e.getUser().getId().equals(userId))
                    .mapToDouble(ExpenseEntity::getAmount).sum()
                    + friendExpenses.stream()
                    .filter(fe -> userId == null || fe.getUser().getId().equals(userId))
                    .mapToDouble(FriendExpense::getAmount).sum();
            double netSavings = totalIn - totalOut;

            PdfPTable kpiTable = new PdfPTable(3);
            kpiTable.setWidthPercentage(100);
            kpiTable.setSpacingAfter(15);
            
            kpiTable.addCell(createKpiCell("Total Inflow Volume", "₹" + String.format("%,.2f", totalIn), Color.decode("#10b981")));
            kpiTable.addCell(createKpiCell("Total Outflow Volume", "₹" + String.format("%,.2f", totalOut), Color.decode("#f97316")));
            kpiTable.addCell(createKpiCell("Net Balance Cache", "₹" + String.format("%,.2f", netSavings), Color.decode("#6366f1")));
            
            document.add(kpiTable);
        }

        // Main Report Data Table
        PdfPTable dataTable;
        if (type.equalsIgnoreCase("FINANCIAL_SUMMARY") || type.equalsIgnoreCase("TRANSACTION_AUDIT")) {
            dataTable = new PdfPTable(6);
            dataTable.setWidthPercentage(100);
            dataTable.setWidths(new float[]{1.5f, 3.5f, 2.5f, 2.0f, 2.5f, 2.0f});
            
            addTableHeader(dataTable, new String[]{"Type", "Operator", "Category", "Volume", "Logged Date", "Flagged"});
            
            List<IncomeEntity> incomes = incomeRepository.findByDateBetweenOrderByDateDesc(start, end);
            for (IncomeEntity inc : incomes) {
                if (userId != null && !inc.getUser().getId().equals(userId)) continue;
                if (category != null && !category.isEmpty() && !inc.getCategory().equalsIgnoreCase(category)) continue;
                
                dataTable.addCell(createTableCell("Inflow", Color.decode("#10b981")));
                dataTable.addCell(createTableCell(inc.getUser().getEmail()));
                dataTable.addCell(createTableCell(inc.getCategory()));
                dataTable.addCell(createTableCell("₹" + String.format("%,.0f", inc.getAmount())));
                dataTable.addCell(createTableCell(inc.getDate().toString()));
                dataTable.addCell(createTableCell(inc.getFlagged() ? "Anomaly" : "Clear", inc.getFlagged() ? Color.RED : Color.DARK_GRAY));
            }

            List<ExpenseEntity> expenses = expenseRepository.findByExpenseDateBetweenOrderByExpenseDateDesc(start, end);
            for (ExpenseEntity exp : expenses) {
                if (userId != null && !exp.getUser().getId().equals(userId)) continue;
                if (category != null && !category.isEmpty() && !exp.getCategory().equalsIgnoreCase(category)) continue;
                
                dataTable.addCell(createTableCell("Outflow", Color.decode("#f97316")));
                dataTable.addCell(createTableCell(exp.getUser().getEmail()));
                dataTable.addCell(createTableCell(exp.getCategory()));
                dataTable.addCell(createTableCell("₹" + String.format("%,.0f", exp.getAmount())));
                dataTable.addCell(createTableCell(exp.getExpenseDate().toString()));
                dataTable.addCell(createTableCell(exp.getFlagged() ? "Anomaly" : "Clear", exp.getFlagged() ? Color.RED : Color.DARK_GRAY));
            }

            List<FriendExpense> friendExpenses = friendExpenseRepository.findByExpenseDateBetweenOrderByExpenseDateDesc(start, end);
            for (FriendExpense fe : friendExpenses) {
                if (userId != null && !fe.getUser().getId().equals(userId)) continue;
                if (category != null && !category.isEmpty() && !fe.getCategory().equalsIgnoreCase(category)) continue;
                
                dataTable.addCell(createTableCell("Outflow", Color.decode("#f97316")));
                dataTable.addCell(createTableCell(fe.getUser().getEmail()));
                dataTable.addCell(createTableCell(fe.getCategory()));
                dataTable.addCell(createTableCell("₹" + String.format("%,.0f", fe.getAmount())));
                dataTable.addCell(createTableCell(fe.getExpenseDate().toString()));
                dataTable.addCell(createTableCell(fe.getAmount() > 50000 ? "Anomaly" : "Clear", fe.getAmount() > 50000 ? Color.RED : Color.DARK_GRAY));
            }
        } 
        else if (type.equalsIgnoreCase("USER_ACTIVITY")) {
            dataTable = new PdfPTable(5);
            dataTable.setWidthPercentage(100);
            dataTable.setWidths(new float[]{1.5f, 3.5f, 2.0f, 2.0f, 2.5f});
            
            addTableHeader(dataTable, new String[]{"User ID", "Email Address", "Role Tier", "Status", "Joined"});
            
            List<User> users = userRepository.findAll();
            for (User u : users) {
                if (userId != null && !u.getId().equals(userId)) continue;
                dataTable.addCell(createTableCell("#" + u.getId()));
                dataTable.addCell(createTableCell(u.getEmail()));
                dataTable.addCell(createTableCell(u.getRole().name()));
                dataTable.addCell(createTableCell(u.getStatus().name()));
                dataTable.addCell(createTableCell(u.getCreatedAt() != null ? u.getCreatedAt().toLocalDate().toString() : "N/A"));
            }
        } 
        else { // BUDGET_COMPLIANCE
            dataTable = new PdfPTable(6);
            dataTable.setWidthPercentage(100);
            dataTable.setWidths(new float[]{3.0f, 2.5f, 2.5f, 2.5f, 2.5f, 2.5f});
            
            addTableHeader(dataTable, new String[]{"User", "Category Node", "Budget Target", "Consumed", "Remaining", "Compliance"});
            
            List<BudgetEntity> budgets = budgetRepository.findAll();
            for (BudgetEntity b : budgets) {
                if (userId != null && !b.getUser().getId().equals(userId)) continue;
                if (category != null && !category.isEmpty() && !b.getCategory().equalsIgnoreCase(category)) continue;
                
                double spentAmount = calculateSpentAmount(b);
                double remaining = b.getAmount() - spentAmount;
                boolean breached = spentAmount > b.getAmount();
                String statusText = breached ? "OVER BUDGET" : (spentAmount > b.getAmount() * 0.9 ? "CRITICAL" : "ON TRACK");
                Color statusColor = breached ? Color.RED : (statusText.equals("CRITICAL") ? Color.ORANGE : Color.GREEN);
                
                dataTable.addCell(createTableCell(b.getUser().getEmail()));
                dataTable.addCell(createTableCell(b.getCategory()));
                dataTable.addCell(createTableCell("₹" + String.format("%,.0f", b.getAmount())));
                dataTable.addCell(createTableCell("₹" + String.format("%,.0f", spentAmount)));
                dataTable.addCell(createTableCell("₹" + String.format("%,.0f", remaining), remaining < 0 ? Color.RED : Color.DARK_GRAY));
                dataTable.addCell(createTableCell(statusText, statusColor));
            }
        }

        document.add(dataTable);
        
        // Footer signature
        Paragraph footer = new Paragraph("\nReport generated automatically via CredoWallet Core. Authenticated by " + writer.getClass().getSimpleName(), subFont);
        footer.setAlignment(Element.ALIGN_RIGHT);
        document.add(footer);
        
        document.close();
    }

    private PdfPCell createKpiCell(String label, String value, Color color) {
        PdfPCell cell = new PdfPCell();
        cell.setBackgroundColor(Color.decode("#f8fafc"));
        cell.setBorderColor(Color.decode("#e2e8f0"));
        cell.setPadding(8);
        
        Paragraph labelPara = new Paragraph(label, FontFactory.getFont(FontFactory.HELVETICA, 8, Color.GRAY));
        Paragraph valPara = new Paragraph(value, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, color));
        
        cell.addElement(labelPara);
        cell.addElement(valPara);
        return cell;
    }

    private void addTableHeader(PdfPTable table, String[] headers) {
        Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.WHITE);
        for (String header : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(header, headerFont));
            cell.setBackgroundColor(Color.decode("#6366f1"));
            cell.setBorderColor(Color.decode("#4f46e5"));
            cell.setPadding(6);
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            table.addCell(cell);
        }
    }

    private PdfPCell createTableCell(String text) {
        return createTableCell(text, Color.DARK_GRAY);
    }

    private PdfPCell createTableCell(String text, Color color) {
        Font cellFont = FontFactory.getFont(FontFactory.HELVETICA, 8, color);
        PdfPCell cell = new PdfPCell(new Phrase(text, cellFont));
        cell.setBorderColor(Color.decode("#e2e8f0"));
        cell.setPadding(5);
        return cell;
    }

    /**
     * Cron schedule: First day of every month at midnight
     */
    @Scheduled(cron = "0 0 0 1 * ?")
    public void generateMonthlyReports() {
        log.info("System Scheduler: Starting monthly financial summary compilation...");
        LocalDate today = LocalDate.now();
        LocalDate firstOfLastMonth = today.minusMonths(1).withDayOfMonth(1);
        LocalDate lastOfLastMonth = today.minusMonths(1).withDayOfMonth(today.minusMonths(1).lengthOfMonth());

        String fromDate = firstOfLastMonth.toString();
        String toDate = lastOfLastMonth.toString();

        triggerReportGeneration(
                "FINANCIAL_SUMMARY",
                fromDate,
                toDate,
                null,
                null,
                "PDF",
                "SYSTEM"
        );
        log.info("System Scheduler: Monthly report generated successfully.");
    }
}
