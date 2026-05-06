package report;

import database.DatabaseManager;

import javax.mail.*;
import javax.mail.internet.*;
import java.sql.*;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * Email service using JavaMail with SMTP settings stored in system_settings table.
 */
public class EmailService {

    private static final Logger logger = Logger.getLogger(EmailService.class.getName());

    public static final String KEY_HOST     = "smtp_host";
    public static final String KEY_PORT     = "smtp_port";
    public static final String KEY_USER     = "smtp_username";
    public static final String KEY_PASS     = "smtp_password";
    public static final String KEY_FROM     = "smtp_from_name";
    public static final String KEY_ENABLED  = "smtp_enabled";
    public static final String KEY_TLS      = "smtp_tls";

    public static String send(String toEmail, String subject, String htmlBody) {
        SmtpConfig cfg = loadConfig();
        if (!cfg.enabled) return "Email notifications are disabled in System Settings.";
        if (cfg.host.isEmpty() || cfg.username.isEmpty())
            return "SMTP not configured. Go to System Settings → Email Settings.";

        try {
            Properties props = new Properties();
            props.put("mail.smtp.host", cfg.host);
            props.put("mail.smtp.port", cfg.port);
            props.put("mail.smtp.auth", "true");
            if (cfg.tls) {
                props.put("mail.smtp.starttls.enable", "true");
            } else {
                props.put("mail.smtp.ssl.enable", "true");
            }
            props.put("mail.smtp.connectiontimeout", "10000");
            props.put("mail.smtp.timeout", "10000");

            Session session = Session.getInstance(props, new Authenticator() {
                @Override protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(cfg.username, cfg.password);
                }
            });

            Message msg = new MimeMessage(session);
            msg.setFrom(new InternetAddress(cfg.username,
                cfg.fromName.isEmpty() ? "Electric Billing System" : cfg.fromName));
            msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
            msg.setSubject(subject);
            msg.setContent(htmlBody, "text/html; charset=utf-8");

            Transport.send(msg);
            logger.info("Email sent to: " + toEmail + " | Subject: " + subject);
            return null;

        } catch (Exception e) {
            logger.warning("Email failed: " + e.getMessage());
            return e.getMessage();
        }
    }

    public static void sendBillGenerated(String toEmail, String customerName,
                                          String billId, String month,
                                          double total, String dueDate) {
        String subject = "Your Electricity Bill for " + month + " is Ready";
        String body = buildHtml(
            "New Bill Generated",
            "Dear " + customerName + ",",
            "Your electricity bill for <b>" + month + "</b> has been generated.",
            new String[][]{
                {"Bill Number", String.format("#%06d", Integer.parseInt(billId))},
                {"Billing Month", month},
                {"Total Amount", String.format("$%.2f", total)},
                {"Due Date", dueDate}
            },
            "Please log in to your portal to view and pay your bill.",
            UIColor.ORANGE
        );
        sendAsync(toEmail, subject, body);
    }

    public static void sendPaymentConfirmation(String toEmail, String customerName,
                                                String billId, double amount,
                                                String method, String date) {
        String subject = "Payment Confirmation — $" + String.format("%.2f", amount);
        String body = buildHtml(
            "Payment Confirmed",
            "Dear " + customerName + ",",
            "Your payment has been received successfully.",
            new String[][]{
                {"Bill Number", String.format("#%06d", Integer.parseInt(billId))},
                {"Amount Paid", String.format("$%.2f", amount)},
                {"Payment Method", method},
                {"Payment Date", date}
            },
            "Thank you for your payment. Your bill is now marked as PAID.",
            UIColor.GREEN
        );
        sendAsync(toEmail, subject, body);
    }

    public static void sendDueReminder(String toEmail, String customerName,
                                        String billId, double total, String dueDate) {
        String subject = "Bill Due Reminder — Payment Due on " + dueDate;
        String body = buildHtml(
            "Payment Due Reminder",
            "Dear " + customerName + ",",
            "This is a reminder that your electricity bill is due soon.",
            new String[][]{
                {"Bill Number", String.format("#%06d", Integer.parseInt(billId))},
                {"Amount Due", String.format("$%.2f", total)},
                {"Due Date", dueDate}
            },
            "Please make your payment before the due date to avoid late fees.",
            UIColor.RED
        );
        sendAsync(toEmail, subject, body);
    }

    public static void sendAnomalyAlert(String toEmail, String customerName,
                                         double consumption, String severity) {
        String subject = "Unusual Electricity Usage Detected";
        String body = buildHtml(
            "Anomaly Detected",
            "Dear " + customerName + ",",
            "Our AI system has detected unusual electricity consumption on your account.",
            new String[][]{
                {"Consumption", String.format("%.2f kWh", consumption)},
                {"Severity", severity},
                {"Status", "Under Review"}
            },
            "If you believe this is an error, please contact us immediately.",
            UIColor.RED
        );
        sendAsync(toEmail, subject, body);
    }

    private static void sendAsync(String to, String subject, String body) {
        new Thread(() -> {
            String err = send(to, subject, body);
            if (err != null) {
                SmtpConfig cfg = loadConfig();
                if (cfg.enabled) {
                    logger.warning("Async email failed: " + err);
                } else {
                    logger.fine("Email skipped (SMTP disabled): " + subject);
                }
            }
        }, "EmailThread").start();
    }

    private static class UIColor {
        static final String ORANGE = "#FF8C00";
        static final String GREEN  = "#28a745";
        static final String RED    = "#dc3545";
    }

    private static String buildHtml(String title, String greeting, String intro,
                                     String[][] details, String footer, String accentColor) {
        StringBuilder rows = new StringBuilder();
        for (String[] row : details) {
            rows.append("<tr>")
                .append("<td style='padding:8px 12px;font-weight:bold;color:#555;background:#f8f9fa;border:1px solid #dee2e6;'>")
                .append(row[0]).append("</td>")
                .append("<td style='padding:8px 12px;border:1px solid #dee2e6;'>")
                .append(row[1]).append("</td>")
                .append("</tr>");
        }
        return "<!DOCTYPE html><html><body style='font-family:Segoe UI,Arial,sans-serif;background:#f0f2f5;margin:0;padding:20px;'>"
            + "<div style='max-width:600px;margin:0 auto;background:#fff;border-radius:8px;overflow:hidden;box-shadow:0 2px 8px rgba(0,0,0,0.1);'>"
            + "<div style='background:" + accentColor + ";padding:24px 28px;'>"
            + "<h2 style='color:#fff;margin:0;font-size:20px;'>" + title + "</h2>"
            + "<p style='color:rgba(255,255,255,0.85);margin:4px 0 0;font-size:12px;'>AI-Enhanced Electric Billing System</p>"
            + "</div>"
            + "<div style='padding:24px 28px;'>"
            + "<p style='color:#333;font-size:15px;'>" + greeting + "</p>"
            + "<p style='color:#555;'>" + intro + "</p>"
            + "<table style='width:100%;border-collapse:collapse;margin:16px 0;'>" + rows + "</table>"
            + "<p style='color:#666;font-size:13px;border-top:1px solid #eee;padding-top:12px;'>" + footer + "</p>"
            + "</div>"
            + "<div style='background:#f8f9fa;padding:12px 28px;text-align:center;'>"
            + "<p style='color:#999;font-size:11px;margin:0;'>This is an automated message from AI-Enhanced Electric Billing System. Do not reply.</p>"
            + "</div>"
            + "</div></body></html>";
    }

    public static SmtpConfig loadConfig() {
        SmtpConfig cfg = new SmtpConfig();
        String sql = "SELECT setting_key, setting_value FROM system_settings WHERE setting_key LIKE 'smtp%'";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                String k = rs.getString(1), v = rs.getString(2);
                switch (k) {
                    case KEY_HOST:    cfg.host     = v; break;
                    case KEY_PORT:    cfg.port     = v; break;
                    case KEY_USER:    cfg.username = v; break;
                    case KEY_PASS:    cfg.password = v; break;
                    case KEY_FROM:    cfg.fromName = v; break;
                    case KEY_ENABLED: cfg.enabled  = "true".equals(v); break;
                    case KEY_TLS:     cfg.tls      = "true".equals(v); break;
                }
            }
        } catch (SQLException e) { logger.warning("Could not load SMTP config: " + e.getMessage()); }
        return cfg;
    }

    public static class SmtpConfig {
        public String  host     = "";
        public String  port     = "587";
        public String  username = "";
        public String  password = "";
        public String  fromName = "Electric Billing System";
        public boolean enabled  = false;
        public boolean tls      = true;
    }

    public static String testConnection(String toEmail) {
        return send(toEmail, "Test Email — Electric Billing System",
            buildHtml("Test Email", "Hello,", "This is a test email from your Electric Billing System.",
                new String[][]{{"Status", "Connection Successful"}, {"Time", new java.util.Date().toString()}},
                "If you received this, your SMTP settings are configured correctly.", UIColor.GREEN));
    }
}
