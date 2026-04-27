package utils;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import com.itextpdf.text.pdf.draw.LineSeparator;
import models.Bill;
import models.Customer;
import models.Tariff;

import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

public class PDFGenerator {

    // ── Colors ────────────────────────────────────────────────────────────────
    private static final BaseColor C_DARK_BLUE  = new BaseColor(10,  20,  55);
    private static final BaseColor C_ORANGE     = new BaseColor(255, 140, 0);
    private static final BaseColor C_CYAN       = new BaseColor(0,   160, 220);
    private static final BaseColor C_LIGHT_GRAY = new BaseColor(240, 244, 255);
    private static final BaseColor C_MID_GRAY   = new BaseColor(200, 210, 230);
    private static final BaseColor C_WHITE      = BaseColor.WHITE;
    private static final BaseColor C_GREEN      = new BaseColor(30,  160, 70);
    private static final BaseColor C_RED        = new BaseColor(210, 50,  50);
    private static final BaseColor C_HEADER_TXT = new BaseColor(180, 200, 255);

    // ── Fonts ─────────────────────────────────────────────────────────────────
    private static Font fTitle()    { return new Font(Font.FontFamily.HELVETICA, 20, Font.BOLD,   C_WHITE); }
    private static Font fSub()      { return new Font(Font.FontFamily.HELVETICA, 10, Font.NORMAL, C_HEADER_TXT); }
    private static Font fSection()  { return new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD,   C_ORANGE); }
    private static Font fLabel()    { return new Font(Font.FontFamily.HELVETICA,  9, Font.BOLD,   new BaseColor(70, 90, 130)); }
    private static Font fValue()    { return new Font(Font.FontFamily.HELVETICA,  9, Font.NORMAL, C_DARK_BLUE); }
    private static Font fTblHdr()   { return new Font(Font.FontFamily.HELVETICA,  9, Font.BOLD,   C_WHITE); }
    private static Font fTblVal()   { return new Font(Font.FontFamily.HELVETICA,  9, Font.NORMAL, C_DARK_BLUE); }
    private static Font fTotalLbl() { return new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD,   C_WHITE); }
    private static Font fTotalVal() { return new Font(Font.FontFamily.HELVETICA, 14, Font.BOLD,   C_WHITE); }
    private static Font fFooter()   { return new Font(Font.FontFamily.HELVETICA,  8, Font.ITALIC, C_MID_GRAY); }
    private static Font fBillNum()  { return new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD,   C_ORANGE); }
    private static Font fBillDate() { return new Font(Font.FontFamily.HELVETICA,  9, Font.NORMAL, C_HEADER_TXT); }
    private static Font fStatus(String s) {
        BaseColor c = "PAID".equals(s) ? C_GREEN : "OVERDUE".equals(s) ? C_RED : C_ORANGE;
        return new Font(Font.FontFamily.HELVETICA, 11, Font.BOLD, c);
    }

    // ── Public API ────────────────────────────────────────────────────────────

    public static boolean generatePDF(Bill bill, Customer customer, Tariff tariff, String filePath, String profilePicPath) {
        try {
            Document doc = new Document(PageSize.A4, 45, 45, 45, 45);
            PdfWriter writer = PdfWriter.getInstance(doc, new FileOutputStream(filePath));
            doc.open();

            addHeader(doc, writer, bill, customer, profilePicPath);
            addDivider(doc);
            addBillInfo(doc, bill);
            addCustomerInfo(doc, customer);
            addChargesTable(doc, bill, tariff);
            addTotalBox(doc, bill);
            addDivider(doc);
            addFooter(doc);

            doc.close();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // Keep old signature for backward compatibility
    public static boolean generatePDF(Bill bill, Customer customer, Tariff tariff, String filePath) {
        return generatePDF(bill, customer, tariff, filePath, null);
    }

    // ── Sections ──────────────────────────────────────────────────────────────

    private static void addHeader(Document doc, PdfWriter writer, Bill bill, Customer customer, String profilePicPath) throws Exception {
        // Background rectangle
        PdfContentByte cb = writer.getDirectContentUnder();
        float left  = doc.leftMargin();
        float right = doc.right() - doc.rightMargin();
        float top   = doc.top();
        float hdrH  = 90f;

        cb.setColorFill(C_DARK_BLUE);
        cb.rectangle(left, top - hdrH, right - left, hdrH);
        cb.fill();
        cb.setColorFill(C_ORANGE);
        cb.rectangle(left, top - hdrH - 3, right - left, 3);
        cb.fill();

        // Header table: [profile pic] [title] [bill info]
        PdfPTable tbl = new PdfPTable(3);
        tbl.setWidthPercentage(100);
        tbl.setWidths(new float[]{1f, 3f, 1.8f});
        tbl.setSpacingAfter(14);

        // Col 1: Profile picture (circular via clipping)
        PdfPCell picCell = transparentCell();
        picCell.setPadding(10);
        picCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        if (profilePicPath != null && new java.io.File(profilePicPath).exists()) {
            try {
                Image img = Image.getInstance(profilePicPath);
                img.scaleToFit(60, 60);
                // Draw as circle using PdfTemplate
                PdfTemplate tmpl = writer.getDirectContent().createTemplate(64, 64);
                tmpl.setColorFill(C_DARK_BLUE);
                tmpl.circle(32, 32, 32);
                tmpl.fill();
                tmpl.saveState();
                tmpl.circle(32, 32, 30);
                tmpl.clip();
                tmpl.newPath();
                img.setAbsolutePosition(2, 2);
                img.scaleToFit(60, 60);
                tmpl.addImage(img);
                tmpl.restoreState();
                // Orange ring
                tmpl.setColorStroke(C_ORANGE);
                tmpl.setLineWidth(2.5f);
                tmpl.circle(32, 32, 30);
                tmpl.stroke();
                Image tmplImg = Image.getInstance(tmpl);
                tmplImg.scaleToFit(64, 64);
                picCell.addElement(tmplImg);
            } catch (Exception e) {
                picCell.addElement(new Paragraph(nvl(customer.getName()).substring(0,
                    Math.min(2, nvl(customer.getName()).length())).toUpperCase(), fTitle()));
            }
        } else {
            // Initials fallback
            String initials = getInitials(customer.getName());
            Paragraph p = new Paragraph(initials, fTitle());
            p.setAlignment(Element.ALIGN_CENTER);
            picCell.addElement(p);
        }
        tbl.addCell(picCell);

        // Col 2: System title
        PdfPCell leftCell = transparentCell();
        leftCell.setPaddingTop(14); leftCell.setPaddingBottom(14); leftCell.setPaddingLeft(4);
        Paragraph p1 = new Paragraph("ELECTRICITY BILL", fTitle());
        p1.setSpacingAfter(3);
        Paragraph p2 = new Paragraph(utils.SystemSettings.getCompanyName(), fSub());
        Paragraph p3 = new Paragraph(nvl(customer.getName()), new Font(Font.FontFamily.HELVETICA, 11, Font.BOLD, C_ORANGE));        p3.setSpacingBefore(6);
        leftCell.addElement(p1);
        leftCell.addElement(p2);
        leftCell.addElement(p3);
        tbl.addCell(leftCell);

        // Col 3: Bill number + date
        PdfPCell rightCell = transparentCell();
        rightCell.setPaddingTop(14); rightCell.setPaddingBottom(14); rightCell.setPaddingRight(4);
        rightCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        Paragraph pNum  = new Paragraph(String.format("Bill No: #%06d", bill.getBillId()), fBillNum());
        pNum.setAlignment(Element.ALIGN_RIGHT);
        Paragraph pDate = new Paragraph("Generated: " + fmt("dd MMM yyyy", new Date()), fBillDate());
        pDate.setAlignment(Element.ALIGN_RIGHT);
        rightCell.addElement(pNum);
        rightCell.addElement(pDate);
        tbl.addCell(rightCell);

        doc.add(tbl);
    }

    private static String getInitials(String name) {
        if (name == null || name.trim().isEmpty()) return "?";
        String[] parts = name.trim().split("\\s+");
        if (parts.length >= 2) return ("" + parts[0].charAt(0) + parts[1].charAt(0)).toUpperCase();
        return name.substring(0, Math.min(2, name.length())).toUpperCase();
    }

    private static void addBillInfo(Document doc, Bill bill) throws Exception {
        doc.add(sectionLabel("BILL INFORMATION"));

        PdfPTable tbl = infoTable();
        addInfoRow(tbl, "Bill ID",       String.format("#%06d", bill.getBillId()),
                        "Billing Month", nvl(bill.getBillingMonth()));
        addInfoRow(tbl, "Status",        bill.getStatus(),
                        "Due Date",      bill.getDueDate() != null ? fmt("dd MMM yyyy", bill.getDueDate()) : "N/A");
        doc.add(tbl);
    }

    private static void addCustomerInfo(Document doc, Customer customer) throws Exception {
        doc.add(sectionLabel("CUSTOMER DETAILS"));

        PdfPTable tbl = infoTable();
        addInfoRow(tbl, "Customer Name", nvl(customer.getName()),
                        "Meter Number",  nvl(customer.getMeterNumber()));
        addInfoRow(tbl, "Email",         nvl(customer.getEmail()),
                        "Phone",         nvl(customer.getPhone()));
        // Address spans full row
        PdfPCell addrLbl = labelCell("Address");
        PdfPCell addrVal = valueCell(nvl(customer.getAddress()));
        addrVal.setColspan(3);
        tbl.addCell(addrLbl);
        tbl.addCell(addrVal);

        doc.add(tbl);
    }

    private static void addChargesTable(Document doc, Bill bill, Tariff tariff) throws Exception {
        doc.add(sectionLabel("CHARGE BREAKDOWN"));

        PdfPTable tbl = new PdfPTable(4);
        tbl.setWidthPercentage(100);
        tbl.setWidths(new float[]{3.5f, 1.5f, 1.8f, 1.5f});
        tbl.setSpacingAfter(12);

        // Header
        for (String h : new String[]{"Description", "Quantity", "Unit Rate", "Amount"}) {
            PdfPCell c = new PdfPCell(new Phrase(h, fTblHdr()));
            c.setBackgroundColor(C_DARK_BLUE);
            c.setPadding(8);
            c.setBorderColor(C_CYAN);
            c.setHorizontalAlignment(Element.ALIGN_CENTER);
            tbl.addCell(c);
        }

        // Energy row
        chargeRow(tbl,
            "Energy Consumption",
            String.format("%.2f kWh", bill.getConsumptionKwh()),
            String.format("$%.4f / kWh", tariff.getRatePerKwh()),
            String.format("$%.2f", bill.getAmount()),
            false);

        // Fixed charge row
        chargeRow(tbl,
            "Fixed Service Charge",
            "1",
            String.format("$%.2f", bill.getFixedCharge()),
            String.format("$%.2f", bill.getFixedCharge()),
            true);

        doc.add(tbl);
    }

    private static void addTotalBox(Document doc, Bill bill) throws Exception {
        // Right-aligned total table
        PdfPTable tbl = new PdfPTable(2);
        tbl.setWidthPercentage(55);
        tbl.setHorizontalAlignment(Element.ALIGN_RIGHT);
        tbl.setWidths(new float[]{1.8f, 1f});
        tbl.setSpacingBefore(4);
        tbl.setSpacingAfter(14);

        PdfPCell lbl = new PdfPCell(new Phrase("TOTAL AMOUNT DUE", fTotalLbl()));
        lbl.setBackgroundColor(C_DARK_BLUE);
        lbl.setPadding(11);
        lbl.setBorder(Rectangle.NO_BORDER);
        lbl.setHorizontalAlignment(Element.ALIGN_RIGHT);
        lbl.setVerticalAlignment(Element.ALIGN_MIDDLE);

        PdfPCell val = new PdfPCell(new Phrase(String.format("$%.2f", bill.getTotalAmount()), fTotalVal()));
        val.setBackgroundColor(C_ORANGE);
        val.setPadding(11);
        val.setBorder(Rectangle.NO_BORDER);
        val.setHorizontalAlignment(Element.ALIGN_CENTER);
        val.setVerticalAlignment(Element.ALIGN_MIDDLE);

        tbl.addCell(lbl);
        tbl.addCell(val);
        doc.add(tbl);

        // Payment status line
        Paragraph statusLine = new Paragraph("Payment Status:  " + bill.getStatus(), fStatus(bill.getStatus()));
        statusLine.setAlignment(Element.ALIGN_RIGHT);
        statusLine.setSpacingAfter(8);
        doc.add(statusLine);
    }

    private static void addDivider(Document doc) throws Exception {
        LineSeparator ls = new LineSeparator(1f, 100f, C_MID_GRAY, Element.ALIGN_CENTER, -2);
        doc.add(new Chunk(ls));
        doc.add(Chunk.NEWLINE);
    }

    private static void addFooter(Document doc) throws Exception {
        String companyInfo = utils.SystemSettings.getCompanyName()
            + "  |  " + utils.SystemSettings.getCompanyAddress()
            + "  |  " + utils.SystemSettings.getCompanyPhone();
        Paragraph p = new Paragraph(
            companyInfo + "\n" +
            "For inquiries: " + utils.SystemSettings.getCompanyEmail() + "\n" +
            "This is a computer-generated document and does not require a signature.",
            fFooter());
        p.setAlignment(Element.ALIGN_CENTER);
        p.setSpacingBefore(6);
        doc.add(p);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static PdfPCell transparentCell() {
        PdfPCell c = new PdfPCell();
        c.setBorder(Rectangle.NO_BORDER);
        c.setBackgroundColor(new BaseColor(0, 0, 0, 0)); // transparent
        return c;
    }

    private static Paragraph sectionLabel(String text) {
        Paragraph p = new Paragraph(text, fSection());
        p.setSpacingBefore(10);
        p.setSpacingAfter(4);
        return p;
    }

    private static PdfPTable infoTable() throws Exception {
        PdfPTable t = new PdfPTable(4);
        t.setWidthPercentage(100);
        t.setWidths(new float[]{1.3f, 2f, 1.3f, 2f});
        t.setSpacingAfter(10);
        return t;
    }

    private static void addInfoRow(PdfPTable t,
                                   String l1, String v1,
                                   String l2, String v2) {
        t.addCell(labelCell(l1));
        t.addCell(valueCell(v1));
        t.addCell(labelCell(l2));
        t.addCell(valueCell(v2));
    }

    private static PdfPCell labelCell(String text) {
        PdfPCell c = new PdfPCell(new Phrase(text, fLabel()));
        c.setBackgroundColor(C_LIGHT_GRAY);
        c.setPadding(7);
        c.setBorderColor(C_MID_GRAY);
        return c;
    }

    private static PdfPCell valueCell(String text) {
        PdfPCell c = new PdfPCell(new Phrase(text, fValue()));
        c.setBackgroundColor(C_WHITE);
        c.setPadding(7);
        c.setBorderColor(C_MID_GRAY);
        return c;
    }

    private static void chargeRow(PdfPTable t,
                                  String desc, String qty,
                                  String rate, String amount,
                                  boolean shaded) {
        BaseColor bg = shaded ? C_LIGHT_GRAY : C_WHITE;
        String[] vals   = {desc, qty, rate, amount};
        int[]    aligns = {Element.ALIGN_LEFT, Element.ALIGN_CENTER,
                           Element.ALIGN_CENTER, Element.ALIGN_RIGHT};
        for (int i = 0; i < 4; i++) {
            PdfPCell c = new PdfPCell(new Phrase(vals[i], fTblVal()));
            c.setBackgroundColor(bg);
            c.setPadding(8);
            c.setBorderColor(C_MID_GRAY);
            c.setHorizontalAlignment(aligns[i]);
            t.addCell(c);
        }
    }

    private static String nvl(String s) {
        return (s != null && !s.trim().isEmpty()) ? s.trim() : "-";
    }

    private static String fmt(String pattern, java.util.Date d) {
        return new SimpleDateFormat(pattern).format(d);
    }

    // ── Legacy text fallback ──────────────────────────────────────────────────

    public static String generateBillText(Bill bill, Customer customer, Tariff tariff) {
        StringBuilder sb = new StringBuilder();
        sb.append("=======================================================\n");
        sb.append("       AI-ENHANCED ELECTRIC BILLING SYSTEM             \n");
        sb.append("=======================================================\n");
        sb.append(String.format("Bill ID      : #%06d%n", bill.getBillId()));
        sb.append("Billing Month: ").append(bill.getBillingMonth()).append("\n");
        sb.append("Generated    : ").append(fmt("yyyy-MM-dd", new Date())).append("\n");
        sb.append("-------------------------------------------------------\n");
        sb.append("Name         : ").append(nvl(customer.getName())).append("\n");
        sb.append("Meter No.    : ").append(nvl(customer.getMeterNumber())).append("\n");
        sb.append("-------------------------------------------------------\n");
        sb.append(String.format("Consumption  : %.2f kWh%n", bill.getConsumptionKwh()));
        sb.append(String.format("Rate         : $%.4f/kWh%n", tariff.getRatePerKwh()));
        sb.append(String.format("Energy Charge: $%.2f%n", bill.getAmount()));
        sb.append(String.format("Fixed Charge : $%.2f%n", bill.getFixedCharge()));
        sb.append("-------------------------------------------------------\n");
        sb.append(String.format("TOTAL AMOUNT : $%.2f%n", bill.getTotalAmount()));
        sb.append("Status       : ").append(bill.getStatus()).append("\n");
        sb.append("=======================================================\n");
        return sb.toString();
    }

    public static boolean saveToFile(String content, String filePath) {
        try (java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.FileWriter(filePath))) {
            pw.print(content);
            return true;
        } catch (java.io.IOException e) { return false; }
    }
}
