package lk.vish.novabooks.controller.api;

import com.google.gson.*;
import lk.vish.novabooks.entity.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lk.vish.novabooks.mail.InvoiceMail;
import lk.vish.novabooks.provider.MailServiceProvider;
import lk.vish.novabooks.util.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Path("/checkout")
public class ContentController {

    private final Gson gson = new Gson();

    @Path("/user-info")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getUserInfo(@Context HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        Map<String, Object> responseMap = new HashMap<>();

        if (session != null && session.getAttribute("user") != null) {
            User user = (User) session.getAttribute("user");

            responseMap.put("loggedIn", true);
            responseMap.put("firstName", user.getFirstName() != null ? user.getFirstName() : "");
            responseMap.put("lastName", user.getLastName() != null ? user.getLastName() : "");
            responseMap.put("email", user.getEmail() != null ? user.getEmail() : "");

            responseMap.put("phone", user.getMobile() != null ? user.getMobile() : "");

            StringBuilder fullAddress = new StringBuilder();
            if (user.getAddressLine1() != null && !user.getAddressLine1().trim().isEmpty()) {
                fullAddress.append(user.getAddressLine1().trim());
            }
            if (user.getAddressLine2() != null && !user.getAddressLine2().trim().isEmpty()) {
                if (fullAddress.length() > 0) {
                    fullAddress.append(", ");
                }
                fullAddress.append(user.getAddressLine2().trim());
            }
            responseMap.put("address", fullAddress.toString());
            responseMap.put("addressLine1", user.getAddressLine1() != null ? user.getAddressLine1() : "");
            responseMap.put("addressLine2", user.getAddressLine2() != null ? user.getAddressLine2() : "");

            responseMap.put("city", user.getCity() != null ? user.getCity() : "");

            return Response.ok(gson.toJson(responseMap)).build();
        } else {
            responseMap.put("loggedIn", false);
            return Response.ok(gson.toJson(responseMap)).build();
        }
    }

    @POST
    @Path("/place-order")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response placeOrder(String jsonRequest, @Context HttpServletRequest request) {
        Transaction transaction = null;

        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            JsonObject jsonObject = gson.fromJson(jsonRequest, JsonObject.class);

            HttpSession httpSession = request.getSession(false);
            User user = null;
            if (httpSession != null && httpSession.getAttribute("user") != null) {
                user = (User) httpSession.getAttribute("user");
            }

            double totalAmount = jsonObject.has("totalAmount") ? jsonObject.get("totalAmount").getAsDouble() : 0.0;
            String paymentId = (jsonObject.has("payhereOrderId") && !jsonObject.get("payhereOrderId").isJsonNull())
                    ? jsonObject.get("payhereOrderId").getAsString()
                    : null;

            transaction = session.beginTransaction();

            Orders order = new Orders();
            order.setOrderDate(new Date());
            order.setTotalAmount(totalAmount);
            order.setStatus("PENDING");

            if (user != null) {
                User dbUser = session.get(User.class, user.getUserId());
                order.setUser(dbUser);
            }

            session.persist(order);


            if (jsonObject.has("deliveryDetails") && !jsonObject.get("deliveryDetails").isJsonNull()) {

                JsonObject deliveryData = jsonObject.getAsJsonObject("deliveryDetails");

                DeliveryDetails deliveryDetails = new DeliveryDetails();
                deliveryDetails.setFirstName(deliveryData.has("firstName") ? deliveryData.get("firstName").getAsString() : "");
                deliveryDetails.setLastName(deliveryData.has("lastName") ? deliveryData.get("lastName").getAsString() : "");
                deliveryDetails.setEmail(deliveryData.has("email") ? deliveryData.get("email").getAsString() : "");
                deliveryDetails.setPhone(deliveryData.has("phone") ? deliveryData.get("phone").getAsString() : "");
                deliveryDetails.setAddress(deliveryData.has("address") ? deliveryData.get("address").getAsString() : "");
                deliveryDetails.setCity(deliveryData.has("city") ? deliveryData.get("city").getAsString() : "");
                deliveryDetails.setOrder(order);

                session.persist(deliveryDetails);
            }

            System.out.println("Received Cart Items: " + jsonObject.get("cartItems"));

            if (jsonObject.has("cartItems") && !jsonObject.get("cartItems").isJsonNull()) {
                JsonArray cartItems = jsonObject.getAsJsonArray("cartItems");

                for (JsonElement itemElem : cartItems) {
                    JsonObject itemObj = itemElem.getAsJsonObject();

                    OrderItem orderItem = new OrderItem();

                    if (itemObj.has("book_id")) {
                        int bookId = itemObj.get("book_id").getAsInt();
                        Book book = session.get(Book.class, bookId);

                        if (book != null) {
                            orderItem.setOrder(order);
                            orderItem.setBook(book);
                            orderItem.setQuantity(itemObj.get("quantity").getAsInt());
                            orderItem.setUnitPrice(itemObj.get("price").getAsDouble());

                            session.merge(orderItem);
                        } else {
                            System.out.println("Book not found for ID: " + bookId);
                        }

                        if (book.getQuantity() < itemObj.get("quantity").getAsInt()) {
                            transaction.rollback();
                            Map<String, Object> errorMap = new HashMap<>();
                            errorMap.put("success", false);
                            errorMap.put("message", "Sorry, '" + book.getTitle() + "' is out of stock!");
                            return Response.status(Response.Status.BAD_REQUEST)
                                    .entity(new Gson().toJson(errorMap))
                                    .build();
                        }

                        book.setQuantity(book.getQuantity() - itemObj.get("quantity").getAsInt());
                        session.merge(book);


                    }
                }
            }

            transaction.commit();

            Map<String, Object> responseMap = new HashMap<>();
            responseMap.put("success", true);
            responseMap.put("orderId", "ORD" + order.getId());

            if (httpSession != null) {
                httpSession.removeAttribute("cart");
            }

            session.clear();


            Orders fullOrder = session.createQuery(
                            "SELECT DISTINCT o FROM Orders o " +
                                    "LEFT JOIN FETCH o.user " +
                                    "LEFT JOIN FETCH o.deliveryDetails " +
                                    "LEFT JOIN FETCH o.orderItems oi " +
                                    "LEFT JOIN FETCH oi.book " +
                                    "WHERE o.id = :id",
                            Orders.class
                    ).setParameter("id", order.getId())
                    .uniqueResult();

            if (fullOrder != null) {
                MailServiceProvider.getInstance().sendMail(new InvoiceMail(fullOrder));
            }

            responseMap.put("success", true);
            responseMap.put("orderId", order.getId());
            responseMap.put("message", "Order placed successfully!");

            return Response.ok(gson.toJson(responseMap), MediaType.APPLICATION_JSON).build();

        } catch (Exception e) {
            if (transaction != null && transaction.isActive()) {
                try {
                    transaction.rollback();
                } catch (Exception rollbackEx) {
                    System.err.println("Rollback failed: " + rollbackEx.getMessage());
                }
            }

            e.printStackTrace();

            Map<String, Object> errorMap = new HashMap<>();
            errorMap.put("success", false);
            errorMap.put("message", "Failed to place order: " + e.getMessage());

            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new Gson().toJson(errorMap))
                    .build();
        }
    }
}

