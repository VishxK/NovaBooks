package lk.vish.novabooks.mail;

import lk.vish.novabooks.entity.OrderItem;
import lk.vish.novabooks.entity.Orders;
import lk.vish.novabooks.util.PdfInvoiceGenerator;
import io.rocketbase.mail.EmailTemplateBuilder.EmailTemplateConfigBuilder;
import io.rocketbase.mail.model.HtmlTextEmail;

import jakarta.activation.DataHandler;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Multipart;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMultipart;
import jakarta.mail.util.ByteArrayDataSource;

public class InvoiceMail extends Mailable {

    private final Orders order;

    public InvoiceMail(Orders order) {
        super();
        this.order = order;
    }

    @Override
    public void build(Message message) throws MessagingException {
        try {
            byte[] pdfBytes = PdfInvoiceGenerator.generateInvoicePdf(order).readAllBytes();

            message.setRecipient(Message.RecipientType.TO, new InternetAddress(order.getUser().getEmail()));
            message.setSubject("Order Confirmation & Invoice - #INV-" + order.getId());

            EmailTemplateConfigBuilder builder = getEmailTemplateBuilder();

            builder.header()
                    .logo("https://img.freepik.com/free-vector/book-store-logo-template_23-2149331482.jpg")
                    .logoHeight(50);

            builder.text("Hi " + order.getUser().getFirstName() + ",").and()
                    .text("Thank you for your order! We've received your request and are preparing it for shipment.").and()
                    .text("Order Number: <b>#INV-" + order.getId() + "</b>").and()
                    .text("Total Paid: <b>LKR " + order.getTotalAmount() + "</b>").and();

            builder.button("View Invoice Online", "http://localhost:8080/novabooks/invoice.html?id=" + order.getId())
                    .blue();

            builder.button("My Orders", "http://localhost:8080/novabooks/orders.html")
                    .gray();

            builder.text("Note: A copy of your official invoice is also attached to this email as a PDF document.");
            builder.copyright("NovaBooks Online Store");

            HtmlTextEmail htmlTextEmail = builder.build();

            MimeBodyPart messageBodyPart = new MimeBodyPart();
            messageBodyPart.setContent(htmlTextEmail.getHtml(), "text/html; charset=utf-8");

            MimeBodyPart attachmentPart = new MimeBodyPart();
            ByteArrayDataSource dataSource = new ByteArrayDataSource(pdfBytes, "application/pdf");
            attachmentPart.setDataHandler(new DataHandler(dataSource));
            attachmentPart.setFileName("Invoice_#INV-" + order.getId() + ".pdf");

            Multipart multipart = new MimeMultipart();
            multipart.addBodyPart(messageBodyPart);
            multipart.addBodyPart(attachmentPart);

            message.setContent(multipart);

        } catch (Exception e) {
            throw new MessagingException("Failed to build Invoice Email", e);
        }
    }
}