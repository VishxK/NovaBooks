package lk.vish.novabooks.controller.api;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lk.vish.novabooks.entity.Orders;
import lk.vish.novabooks.util.Env;
import lk.vish.novabooks.util.HibernateUtil;
import lk.vish.novabooks.util.PayHereHash;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@Path("/payment")
public class PaymentController {

    private static final String MERCHANT_ID = Env.get("payhere.merchant.id");
    private static final String MERCHANT_SECRET = Env.get("payhere.merchant.secret");
    private static final String CURRENCY = "LKR";

    private final Gson gson = new Gson();

    @POST
    @Path("/initiate")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response initiatePayment(String jsonBody) {
        try {
            JsonObject req = gson.fromJson(jsonBody, JsonObject.class);

            if (req == null || !req.has("amount")) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("{\"error\":\"Amount is required\"}").build();
            }

            double rawAmount = req.get("amount").getAsDouble();

            DecimalFormat df = new DecimalFormat("0.00", DecimalFormatSymbols.getInstance(Locale.US));
            df.setGroupingUsed(false);
            String formattedAmount = df.format(rawAmount);

            String orderId = "ORD" + System.currentTimeMillis();

            String hash = PayHereHash.generateHash(MERCHANT_ID, orderId, formattedAmount, CURRENCY, MERCHANT_SECRET);

            System.out.println("=== PAYHERE HASH DEBUG ===");
            System.out.println("Merchant ID: " + MERCHANT_ID);
            System.out.println("Order ID: " + orderId);
            System.out.println("Formatted Amount: " + formattedAmount);
            System.out.println("Generated Hash: " + hash);
            System.out.println("==========================");

            Map<String, Object> paymentObj = new HashMap<>();
            paymentObj.put("sandbox", true);
            paymentObj.put("merchant_id", MERCHANT_ID);
            paymentObj.put("return_url", "http://localhost:8080/novabooks/checkout.html");
            paymentObj.put("cancel_url", "http://localhost:8080/novabooks/checkout.html");
            paymentObj.put("notify_url", "http://localhost:8080/novabooks/api/payment/notify");
            paymentObj.put("order_id", orderId);
            paymentObj.put("items", req.has("itemsSummary") ? req.get("itemsSummary").getAsString() : "Book Purchase");

            paymentObj.put("amount", formattedAmount);
            paymentObj.put("currency", CURRENCY);
            paymentObj.put("hash", hash);

            paymentObj.put("first_name", req.has("firstName") ? req.get("firstName").getAsString() : "Customer");
            paymentObj.put("last_name", req.has("lastName") ? req.get("lastName").getAsString() : "User");
            paymentObj.put("email", req.has("email") ? req.get("email").getAsString() : "customer@example.com");
            paymentObj.put("phone", req.has("phone") ? req.get("phone").getAsString() : "0771234567");
            paymentObj.put("address", req.has("address") ? req.get("address").getAsString() : "No 123, High Level Rd");
            paymentObj.put("city", req.has("city") ? req.get("city").getAsString() : "Malabe");
            paymentObj.put("country", "Sri Lanka");

            return Response.ok(gson.toJson(paymentObj)).build();

        } catch (Exception e) {
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"error\":\"" + e.getMessage() + "\"}").build();
        }
    }

    @POST
    @Path("/notify")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response handlePaymentNotification(
            @FormParam("merchant_id") String merchantId,
            @FormParam("order_id") String orderId,
            @FormParam("payhere_amount") String payhereAmount,
            @FormParam("payhere_currency") String payhereCurrency,
            @FormParam("status_code") String statusCode,
            @FormParam("md5sig") String md5sig) {

        String localMd5 = PayHereHash.md5(merchantId + orderId + payhereAmount + payhereCurrency + statusCode + PayHereHash.md5(MERCHANT_SECRET));

        if (localMd5.equalsIgnoreCase(md5sig) && "2".equals(statusCode)) {
            Session session = HibernateUtil.getSessionFactory().openSession();
            Transaction tx = null;
            try {
                tx = session.beginTransaction();

                Orders order = session.get(Orders.class, orderId);
                if (order != null) {
                    order.setStatus("PAID");
                    session.update(order);
                }
                tx.commit();
            } catch (Exception e) {
                if (tx != null) tx.rollback();
            } finally {
                session.close();
            }
            return Response.ok("Payment Verified").build();
        } else {
            return Response.status(Response.Status.BAD_REQUEST).entity("Invalid Hash or Failed Payment").build();
        }
    }
}