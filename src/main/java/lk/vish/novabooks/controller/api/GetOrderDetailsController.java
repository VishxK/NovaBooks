package lk.vish.novabooks.controller.api;

import com.google.gson.Gson;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lk.vish.novabooks.entity.DeliveryDetails;
import lk.vish.novabooks.entity.OrderItem;
import lk.vish.novabooks.entity.Orders;
import lk.vish.novabooks.entity.User;
import lk.vish.novabooks.util.HibernateUtil;
import org.hibernate.Session;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Path("/order-details")
public class GetOrderDetailsController {

    @Path("/list")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAllOrders() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {

            List<Orders> ordersList = session.createQuery("SELECT DISTINCT o FROM Orders o LEFT JOIN FETCH o.user ORDER BY o.id DESC", Orders.class).list();

            List<Map<String, Object>> responseList = new ArrayList<>();

            for (Orders order : ordersList) {
                Map<String, Object> map = new HashMap<>();
                map.put("id", order.getId());
                map.put("orderDate", order.getOrderDate() != null ? order.getOrderDate().toString() : "");
                map.put("totalAmount", order.getTotalAmount());
                map.put("status", order.getStatus());


                map.put("customerName", order.getUser().getFirstName() + " " + order.getUser().getLastName());
                map.put("customerEmail", order.getUser().getEmail());


                map.put("itemCount", order.getOrderItems() != null ? order.getOrderItems().size() : 0);

                responseList.add(map);
            }

            String jsonOutput = new Gson().toJson(responseList);
            return Response.ok(jsonOutput, MediaType.APPLICATION_JSON).build();

        } catch (Exception e) {
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"error\": \"" + e.getMessage() + "\"}")
                    .build();
        }
    }


    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getOrderDetails(@QueryParam("id") int id,@Context HttpServletRequest request) {
        Map<String, Object> responseMap = new HashMap<>();

        HttpSession session = request.getSession(false);

        boolean isUser = (session != null && session.getAttribute("user") != null);
        boolean isAdmin = (session != null && session.getAttribute("admin") != null && Boolean.TRUE.equals(session.getAttribute("admin")));

        if (!isUser && !isAdmin) {
            Map<String, Object> error = Map.of("status", false, "message", "Please log in first.");
            return Response.status(Response.Status.UNAUTHORIZED).entity(new Gson().toJson(error)).build();
        }

        if (id <= 0) {
            responseMap.put("success", false);
            responseMap.put("message", "Invalid Order ID");
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new Gson().toJson(responseMap))
                    .build();
        }

        try (Session hibernateSession = HibernateUtil.getSessionFactory().openSession()) {

            Orders order = hibernateSession.createQuery(
                    "SELECT DISTINCT o FROM Orders o " +
                            "LEFT JOIN FETCH o.user " +
                            "LEFT JOIN FETCH o.deliveryDetails " +
                            "LEFT JOIN FETCH o.orderItems oi " +
                            "LEFT JOIN FETCH oi.book " +
                            "WHERE o.id = :id",
                    Orders.class
            ).setParameter("id", id).uniqueResult();

            if (order == null) {
                Map<String, Object> error = Map.of("status", false, "message", "Order not found.");
                return Response.status(Response.Status.NOT_FOUND).entity(new Gson().toJson(error)).build();
            }

            if (!isAdmin) {
                User sessionUser = (User) session.getAttribute("user");
                if (sessionUser == null || order.getUser() == null || order.getUser().getUserId() != sessionUser.getUserId()) {
                    Map<String, Object> error = Map.of(
                            "status", false,
                            "message", "Access denied! You do not have permission to view this order."
                    );
                    return Response.status(Response.Status.FORBIDDEN).entity(new Gson().toJson(error)).build();
                }
            }

            Map<String, Object> orderDto = new HashMap<>();
            orderDto.put("id", order.getId());
            orderDto.put("orderDate", order.getOrderDate() != null ? order.getOrderDate().toString() : "");
            orderDto.put("totalAmount", order.getTotalAmount());
            orderDto.put("status", order.getStatus());

            if (order.getUser() != null) {
                String fName = order.getUser().getFirstName() != null ? order.getUser().getFirstName() : "";
                String lName = order.getUser().getLastName() != null ? order.getUser().getLastName() : "";
                String fullName = (fName + " " + lName).trim();

                orderDto.put("customerName", fullName.isEmpty() ? "User #" + order.getUser().getUserId() : fullName);
                orderDto.put("customerEmail", order.getUser().getEmail() != null ? order.getUser().getEmail() : "N/A");
            } else {
                orderDto.put("customerName", "Guest Customer");
                orderDto.put("customerEmail", "N/A");
            }

            DeliveryDetails delivery = order.getDeliveryDetails();
            if (delivery != null) {
                Map<String, Object> deliveryMap = new HashMap<>();
                deliveryMap.put("phone", delivery.getPhone());
                deliveryMap.put("address", delivery.getAddress());
                deliveryMap.put("city", delivery.getCity());

                responseMap.put("delivery", deliveryMap);
            }

            List<Map<String, Object>> itemsList = new ArrayList<>();
            if (order.getOrderItems() != null) {
                for (OrderItem item : order.getOrderItems()) {
                    Map<String, Object> itemDto = new HashMap<>();
                    itemDto.put("id", item.getId());
                    itemDto.put("quantity", item.getQuantity());
                    itemDto.put("price", item.getUnitPrice());

                    if (item.getBook() != null) {
                        itemDto.put("bookTitle", item.getBook().getTitle());
                    } else {
                        itemDto.put("bookTitle", "Unknown Book");
                    }

                    itemsList.add(itemDto);
                }
            }
            orderDto.put("items", itemsList);

            responseMap.put("success", true);
            responseMap.put("order", orderDto);

            String jsonOutput = new Gson().toJson(responseMap);
            return Response.ok(jsonOutput, MediaType.APPLICATION_JSON).build();

        } catch (Exception e) {
            e.printStackTrace();
            responseMap.put("success", false);
            responseMap.put("message", "Server Error: " + e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new Gson().toJson(responseMap))
                    .build();
        }

    }


    @Path("/summary")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getOrderSummary() {
        Map<String, Object> summaryMap = new HashMap<>();

        try (Session session = HibernateUtil.getSessionFactory().openSession()) {

            Long totalOrders = session.createQuery("SELECT COUNT(o) FROM Orders o", Long.class).uniqueResult();

            Long pendingOrders = session.createQuery("SELECT COUNT(o) FROM Orders o WHERE o.status = 'PENDING'", Long.class).uniqueResult();

            Long completedOrders = session.createQuery("SELECT COUNT(o) FROM Orders o WHERE o.status = 'COMPLETED'", Long.class).uniqueResult();

            Long cancelledOrders = session.createQuery("SELECT COUNT(o) FROM Orders o WHERE o.status = 'CANCELLED'", Long.class).uniqueResult();

            Double totalRevenue = session.createQuery("SELECT SUM(o.totalAmount) FROM Orders o WHERE o.status != 'CANCELLED'", Double.class).uniqueResult();

            summaryMap.put("totalOrders", totalOrders != null ? totalOrders : 0);
            summaryMap.put("pendingOrders", pendingOrders != null ? pendingOrders : 0);
            summaryMap.put("completedOrders", completedOrders != null ? completedOrders : 0);
            summaryMap.put("cancelledOrders", cancelledOrders != null ? cancelledOrders : 0);
            summaryMap.put("totalRevenue", totalRevenue != null ? totalRevenue : 0.00);

            String jsonOutput = new Gson().toJson(summaryMap);
            return Response.ok(jsonOutput, MediaType.APPLICATION_JSON).build();

        } catch (Exception e) {
            e.printStackTrace();
            summaryMap.put("error", "Failed to fetch summary: " + e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new Gson().toJson(summaryMap))
                    .build();
        }
    }
}

