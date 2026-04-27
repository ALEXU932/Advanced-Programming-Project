package utils;

import org.apache.poi.hssf.usermodel.*;
import org.apache.poi.hssf.util.HSSFColor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;

import javax.swing.*;
import javax.swing.table.TableModel;
import java.awt.*;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Exports any JTable to a styled Excel (.xls) file using Apache POI HSSF.
 * Uses only poi-5.2.5.jar — no extra dependencies needed.
 */
public class ExcelExporter {

    public static void export(Component parent, JTable table, String sheetName) {
        if (table.getRowCount() == 0) {
            JOptionPane.showMessageDialog(parent,
                "No data to export.", "Export", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Save Excel File");
        String defaultName = sheetName.replaceAll("[^a-zA-Z0-9_]", "_") + "_"
            + new SimpleDateFormat("yyyyMMdd_HHmm").format(new Date()) + ".xls";
        fc.setSelectedFile(new java.io.File(defaultName));
        fc.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
            "Excel 97-2003 (*.xls)", "xls"));
        if (fc.showSaveDialog(parent) != JFileChooser.APPROVE_OPTION) return;

        String path = fc.getSelectedFile().getAbsolutePath();
        if (!path.toLowerCase().endsWith(".xls")) path += ".xls";

        try (HSSFWorkbook wb = new HSSFWorkbook();
             FileOutputStream fos = new FileOutputStream(path)) {

            String safeName = sheetName.length() > 31 ? sheetName.substring(0, 31) : sheetName;
            HSSFSheet sheet = wb.createSheet(safeName);

            TableModel model = table.getModel();
            int cols = model.getColumnCount();

            // ── Styles ────────────────────────────────────────────────────────

            // Title style — dark blue bg, white bold text
            HSSFCellStyle titleStyle = wb.createCellStyle();
            HSSFFont titleFont = wb.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 14);
            titleFont.setColor(HSSFColor.HSSFColorPredefined.WHITE.getIndex());
            titleStyle.setFont(titleFont);
            titleStyle.setFillForegroundColor(HSSFColor.HSSFColorPredefined.DARK_BLUE.getIndex());
            titleStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            titleStyle.setAlignment(HorizontalAlignment.CENTER);
            titleStyle.setVerticalAlignment(VerticalAlignment.CENTER);

            // Header style — navy bg, white bold
            HSSFCellStyle headerStyle = wb.createCellStyle();
            HSSFFont headerFont = wb.createFont();
            headerFont.setBold(true);
            headerFont.setFontHeightInPoints((short) 11);
            headerFont.setColor(HSSFColor.HSSFColorPredefined.WHITE.getIndex());
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(HSSFColor.HSSFColorPredefined.DARK_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            headerStyle.setBorderBottom(BorderStyle.MEDIUM);
            headerStyle.setBottomBorderColor(HSSFColor.HSSFColorPredefined.ORANGE.getIndex());

            // Even row style — light blue tint
            HSSFCellStyle evenStyle = wb.createCellStyle();
            HSSFFont dataFont = wb.createFont();
            dataFont.setFontHeightInPoints((short) 10);
            evenStyle.setFont(dataFont);
            evenStyle.setFillForegroundColor(HSSFColor.HSSFColorPredefined.LIGHT_CORNFLOWER_BLUE.getIndex());
            evenStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            evenStyle.setBorderBottom(BorderStyle.THIN);
            evenStyle.setBottomBorderColor(HSSFColor.HSSFColorPredefined.GREY_25_PERCENT.getIndex());

            // Odd row style — white
            HSSFCellStyle oddStyle = wb.createCellStyle();
            oddStyle.setFont(dataFont);
            oddStyle.setFillForegroundColor(HSSFColor.HSSFColorPredefined.WHITE.getIndex());
            oddStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            oddStyle.setBorderBottom(BorderStyle.THIN);
            oddStyle.setBottomBorderColor(HSSFColor.HSSFColorPredefined.GREY_25_PERCENT.getIndex());

            // Summary style
            HSSFCellStyle summaryStyle = wb.createCellStyle();
            HSSFFont summaryFont = wb.createFont();
            summaryFont.setBold(true);
            summaryFont.setFontHeightInPoints((short) 10);
            summaryFont.setColor(HSSFColor.HSSFColorPredefined.WHITE.getIndex());
            summaryStyle.setFont(summaryFont);
            summaryStyle.setFillForegroundColor(HSSFColor.HSSFColorPredefined.DARK_BLUE.getIndex());
            summaryStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            // Date style (italic, grey)
            HSSFCellStyle dateStyle = wb.createCellStyle();
            HSSFFont dateFont = wb.createFont();
            dateFont.setItalic(true);
            dateFont.setFontHeightInPoints((short) 9);
            dateFont.setColor(HSSFColor.HSSFColorPredefined.GREY_50_PERCENT.getIndex());
            dateStyle.setFont(dateFont);

            // ── Row 0: Title ──────────────────────────────────────────────────
            Row titleRow = sheet.createRow(0);
            titleRow.setHeightInPoints(26);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("AI-Enhanced Electric Billing System  —  " + sheetName);
            titleCell.setCellStyle(titleStyle);
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, cols - 1));

            // ── Row 1: Generated date ─────────────────────────────────────────
            Row dateRow = sheet.createRow(1);
            Cell dateCell = dateRow.createCell(0);
            dateCell.setCellValue("Generated: " + new SimpleDateFormat("dd MMM yyyy  HH:mm").format(new Date()));
            dateCell.setCellStyle(dateStyle);
            sheet.addMergedRegion(new CellRangeAddress(1, 1, 0, cols - 1));

            // ── Row 2: Column headers ─────────────────────────────────────────
            Row headerRow = sheet.createRow(2);
            headerRow.setHeightInPoints(20);
            for (int c = 0; c < cols; c++) {
                Cell cell = headerRow.createCell(c);
                cell.setCellValue(model.getColumnName(c));
                cell.setCellStyle(headerStyle);
            }

            // ── Rows 3+: Data ─────────────────────────────────────────────────
            for (int r = 0; r < model.getRowCount(); r++) {
                Row row = sheet.createRow(r + 3);
                row.setHeightInPoints(17);
                HSSFCellStyle style = (r % 2 == 0) ? evenStyle : oddStyle;
                for (int c = 0; c < cols; c++) {
                    Cell cell = row.createCell(c);
                    Object val = model.getValueAt(r, c);
                    if (val != null) {
                        String s = val.toString().trim();
                        // Try to write as number (strip $ and ,)
                        try {
                            double num = Double.parseDouble(s.replace("$", "").replace(",", ""));
                            cell.setCellValue(num);
                        } catch (NumberFormatException e) {
                            cell.setCellValue(s);
                        }
                    }
                    cell.setCellStyle(style);
                }
            }

            // ── Summary row ───────────────────────────────────────────────────
            int summaryRowIdx = model.getRowCount() + 4;
            Row summaryRow = sheet.createRow(summaryRowIdx);
            Cell summaryCell = summaryRow.createCell(0);
            summaryCell.setCellValue("Total Records: " + model.getRowCount()
                + "   |   Exported: " + new SimpleDateFormat("dd MMM yyyy HH:mm").format(new Date()));
            summaryCell.setCellStyle(summaryStyle);
            sheet.addMergedRegion(new CellRangeAddress(summaryRowIdx, summaryRowIdx, 0, cols - 1));

            // ── Auto-size columns ─────────────────────────────────────────────
            for (int c = 0; c < cols; c++) {
                sheet.autoSizeColumn(c);
                int w = sheet.getColumnWidth(c);
                // Min 2000, max 12000 units
                sheet.setColumnWidth(c, Math.max(2000, Math.min(w + 512, 12000)));
            }

            // ── Write to file ─────────────────────────────────────────────────
            wb.write(fos);
            fos.flush();

            // Success
            int open = JOptionPane.showConfirmDialog(parent,
                "Excel file exported successfully!\n" + path + "\n\nOpen the file now?",
                "Export Complete", JOptionPane.YES_NO_OPTION, JOptionPane.INFORMATION_MESSAGE);
            if (open == JOptionPane.YES_OPTION) {
                try { java.awt.Desktop.getDesktop().open(new java.io.File(path)); }
                catch (Exception ex) {
                    JOptionPane.showMessageDialog(parent, "Cannot open automatically. File saved at:\n" + path);
                }
            }

        } catch (Exception e) {
            JOptionPane.showMessageDialog(parent,
                "Export failed: " + e.getMessage() + "\n\nMake sure the file is not already open in Excel.",
                "Export Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }
}
