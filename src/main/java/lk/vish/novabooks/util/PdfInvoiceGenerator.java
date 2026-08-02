package lk.vish.novabooks.util;

import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.Document;
import com.lowagie.text.PageSize;
import com.lowagie.text.Element;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfWriter;

import java.awt.Color;
import lk.vish.novabooks.entity.Orders;
import lk.vish.novabooks.entity.OrderItem;
import lk.vish.novabooks.entity.DeliveryDetails;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.text.SimpleDateFormat;

public class PdfInvoiceGenerator {

    private static final Color PRIMARY_BLUE = new Color(37, 99, 235);    // #2563eb
    private static final Color DARK_TEXT = new Color(15, 23, 42);       // #0f172a
    private static final Color MUTED_TEXT = new Color(100, 116, 139);   // #64748b
    private static final Color BADGE_GREEN = new Color(34, 197, 94);    // #22c55e (PAID)
    private static final Color BADGE_GOLD = new Color(245, 158, 11);    // #f59e0b (PENDING)
    private static final Color BADGE_RED = new Color(239, 68, 68);      // #ef4444 (CANCELLED)
    private static final Color TABLE_BG = new Color(248, 250, 252);     // #f8fafc
    private static final Color BORDER_COLOR = new Color(226, 232, 240); // #e2e8f0

    public static ByteArrayInputStream generateInvoicePdf(Orders order) {
        Document document = new Document(PageSize.A4, 36, 36, 36, 36);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            PdfWriter.getInstance(document, out);
            document.open();

            // Fonts Setup
            Font fontBrand = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 22, PRIMARY_BLUE);
            Font fontInvoice = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, DARK_TEXT);
            Font fontBadge = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.WHITE);
            Font fontLabel = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, MUTED_TEXT);
            Font fontSubText = FontFactory.getFont(FontFactory.HELVETICA, 9, MUTED_TEXT);
            Font fontCustomerName = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, DARK_TEXT);
            Font fontTh = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, DARK_TEXT);
            Font fontTd = FontFactory.getFont(FontFactory.HELVETICA, 10, DARK_TEXT);
            Font fontTdBold = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, DARK_TEXT);
            Font fontTotalLabel = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, DARK_TEXT);
            Font fontTotalVal = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 15, PRIMARY_BLUE);

            PdfPTable headerTable = new PdfPTable(2);
            headerTable.setWidthPercentage(100);
            headerTable.setWidths(new float[]{60, 40});

            PdfPCell leftHeader = new PdfPCell();
            leftHeader.setBorder(Rectangle.NO_BORDER);
            leftHeader.addElement(new Paragraph("NovaBooks", fontBrand));
            leftHeader.addElement(new Paragraph("No. 123, High Level Road, Sri Lanka", fontSubText));
            leftHeader.addElement(new Paragraph("support@novabooks.com | +94 11 234 5678", fontSubText));
            headerTable.addCell(leftHeader);

            PdfPCell rightHeader = new PdfPCell();
            rightHeader.setBorder(Rectangle.NO_BORDER);
            rightHeader.setHorizontalAlignment(Element.ALIGN_RIGHT);

            Paragraph pInvoice = new Paragraph("INVOICE", fontInvoice);
            pInvoice.setAlignment(Element.ALIGN_RIGHT);
            rightHeader.addElement(pInvoice);

            String statusStr = order.getStatus() != null ? order.getStatus().toUpperCase() : "PAID";
            Color badgeBg = BADGE_GREEN;
            if ("PENDING".equals(statusStr)) {
                badgeBg = BADGE_GOLD;
            } else if ("CANCELLED".equals(statusStr)) {
                badgeBg = BADGE_RED;
            }

            PdfPTable badgeTable = new PdfPTable(1);
            badgeTable.setHorizontalAlignment(Element.ALIGN_RIGHT);
            badgeTable.setTotalWidth(80);
            badgeTable.setLockedWidth(true);

            PdfPCell badgeCell = new PdfPCell(new Phrase(statusStr, fontBadge));
            badgeCell.setBackgroundColor(badgeBg);
            badgeCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            badgeCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
            badgeCell.setPaddingTop(4);
            badgeCell.setPaddingBottom(4);
            badgeCell.setBorder(Rectangle.NO_BORDER);
            badgeTable.addCell(badgeCell);

            rightHeader.addElement(badgeTable);
            headerTable.addCell(rightHeader);

            document.add(headerTable);
            document.add(new Paragraph(" ")); // Spacer

            PdfPTable detailsTable = new PdfPTable(2);
            detailsTable.setWidthPercentage(100);
            detailsTable.setWidths(new float[]{50, 50});

            PdfPCell customerCell = new PdfPCell();
            customerCell.setBorder(Rectangle.NO_BORDER);
            customerCell.addElement(new Paragraph("BILLED TO:", fontLabel));

            String customerName = "Valued Customer";
            String customerEmail = "N/A";

            if (order.getUser() != null) {
                String fName = order.getUser().getFirstName() != null ? order.getUser().getFirstName() : "";
                String lName = order.getUser().getLastName() != null ? order.getUser().getLastName() : "";
                String fullName = (fName + " " + lName).trim();

                if (!fullName.isEmpty()) {
                    customerName = fullName;
                } else {
                    customerName = "User #" + order.getUser().getUserId();
                }

                if (order.getUser().getEmail() != null) {
                    customerEmail = order.getUser().getEmail();
                }
            }

            customerCell.addElement(new Paragraph(customerName, fontCustomerName));
            customerCell.addElement(new Paragraph(customerEmail, fontSubText));

            DeliveryDetails delivery = order.getDeliveryDetails();
            if (delivery != null) {
                String phone = delivery.getPhone() != null ? delivery.getPhone() : "N/A";
                String addressStr = "";
                if (delivery.getAddress() != null) addressStr += delivery.getAddress();
                if (delivery.getCity() != null) addressStr += (addressStr.isEmpty() ? "" : ", ") + delivery.getCity();
                if (addressStr.isEmpty()) addressStr = "N/A";

                customerCell.addElement(new Paragraph(phone, fontSubText));
                customerCell.addElement(new Paragraph(addressStr, fontSubText));
            } else {
                customerCell.addElement(new Paragraph("N/A", fontSubText));
            }

            detailsTable.addCell(customerCell);

            PdfPCell invoiceInfoCell = new PdfPCell();
            invoiceInfoCell.setBorder(Rectangle.NO_BORDER);
            invoiceInfoCell.setHorizontalAlignment(Element.ALIGN_RIGHT);

            Paragraph pLabel = new Paragraph("INVOICE DETAILS:", fontLabel);
            pLabel.setAlignment(Element.ALIGN_RIGHT);
            invoiceInfoCell.addElement(pLabel);

            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String dateStr = order.getOrderDate() != null ? dateFormat.format(order.getOrderDate()) : "N/A";

            Paragraph pInvNo = new Paragraph("Invoice No: #INV-" + order.getId(), fontTdBold);
            pInvNo.setAlignment(Element.ALIGN_RIGHT);
            invoiceInfoCell.addElement(pInvNo);

            Paragraph pDate = new Paragraph("Date: " + dateStr, fontSubText);
            pDate.setAlignment(Element.ALIGN_RIGHT);
            invoiceInfoCell.addElement(pDate);

            detailsTable.addCell(invoiceInfoCell);
            document.add(detailsTable);

            document.add(new Paragraph(" ")); // Spacer

            PdfPTable itemTable = new PdfPTable(4);
            itemTable.setWidthPercentage(100);
            itemTable.setWidths(new float[]{45, 20, 12, 23});

            String[] headers = {"Book Description", "Unit Price", "Qty", "Subtotal"};
            int[] alignments = {Element.ALIGN_LEFT, Element.ALIGN_RIGHT, Element.ALIGN_CENTER, Element.ALIGN_RIGHT};

            for (int i = 0; i < headers.length; i++) {
                PdfPCell th = new PdfPCell(new Phrase(headers[i], fontTh));
                th.setBackgroundColor(TABLE_BG);
                th.setBorderColor(BORDER_COLOR);
                th.setPadding(8);
                th.setHorizontalAlignment(alignments[i]);
                itemTable.addCell(th);
            }

            if (order.getOrderItems() != null && !order.getOrderItems().isEmpty()) {
                for (OrderItem item : order.getOrderItems()) {
                    String bookTitle = (item.getBook() != null && item.getBook().getTitle() != null)
                            ? item.getBook().getTitle()
                            : "Unknown Book";

                    double unitPrice = item.getUnitPrice();
                    int qty = item.getQuantity();
                    double subtotal = unitPrice * qty;

                    addCellToTable(itemTable, bookTitle, fontTd, Element.ALIGN_LEFT);
                    addCellToTable(itemTable, String.format("LKR %.2f", unitPrice), fontTd, Element.ALIGN_RIGHT);
                    addCellToTable(itemTable, String.valueOf(qty), fontTd, Element.ALIGN_CENTER);
                    addCellToTable(itemTable, String.format("LKR %.2f", subtotal), fontTdBold, Element.ALIGN_RIGHT);
                }
            } else {
                PdfPCell emptyCell = new PdfPCell(new Phrase("No items found for this order.", fontSubText));
                emptyCell.setColspan(4);
                emptyCell.setPadding(10);
                emptyCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                itemTable.addCell(emptyCell);
            }

            document.add(itemTable);

            PdfPTable totalTable = new PdfPTable(2);
            totalTable.setWidthPercentage(100);
            totalTable.setWidths(new float[]{70, 30});

            PdfPCell totalLabelCell = new PdfPCell(new Phrase("Total Amount:", fontTotalLabel));
            totalLabelCell.setBorder(Rectangle.NO_BORDER);
            totalLabelCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            totalLabelCell.setPaddingRight(10);
            totalLabelCell.setPaddingTop(10);

            PdfPCell totalValCell = new PdfPCell(new Phrase(String.format("LKR %.2f", order.getTotalAmount()), fontTotalVal));
            totalValCell.setBorder(Rectangle.NO_BORDER);
            totalValCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            totalValCell.setPaddingTop(8);

            totalTable.addCell(totalLabelCell);
            totalTable.addCell(totalValCell);
            document.add(totalTable);

            document.add(new Paragraph(" ")); // Spacer

            Paragraph footer1 = new Paragraph("Thank you for your purchase with NovaBooks!", fontTdBold);
            footer1.setAlignment(Element.ALIGN_CENTER);

            Paragraph footer2 = new Paragraph("If you have any questions about this invoice, please contact our support team.", fontSubText);
            footer2.setAlignment(Element.ALIGN_CENTER);

            document.add(footer1);
            document.add(footer2);

            document.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return new ByteArrayInputStream(out.toByteArray());
    }

    private static void addCellToTable(PdfPTable table, String text, Font font, int alignment) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setPadding(8);
        cell.setBorderColor(BORDER_COLOR);
        cell.setHorizontalAlignment(alignment);
        table.addCell(cell);
    }
}