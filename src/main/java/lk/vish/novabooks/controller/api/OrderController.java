package lk.vish.novabooks.controller.api;

import lk.vish.novabooks.entity.Book;
import lk.vish.novabooks.entity.OrderItem;
import lk.vish.novabooks.entity.Orders;
import lk.vish.novabooks.entity.User;
import lk.vish.novabooks.util.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.Transaction;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Path("/orders")
public class OrderController {

    @Path("/list")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAllOrders(@Context HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        User user = (session != null) ? (User) session.getAttribute("user") : null;

        if (user == null) {
            Map<String, Object> error = Map.of("status", false, "message", "Unauthorized access");
            return Response.status(Response.Status.UNAUTHORIZED).entity(error).build();
        }

        try (Session hibernateSession = HibernateUtil.getSessionFactory().openSession()) {
            List<Orders> ordersList = hibernateSession.createQuery(
                            "SELECT DISTINCT o FROM Orders o " +
                                    "LEFT JOIN FETCH o.orderItems oi " +
                                    "LEFT JOIN FETCH oi.book " +
                                    "WHERE o.user.id = :userId ORDER BY o.id DESC", Orders.class)
                    .setParameter("userId", user.getUserId())
                    .list();

            return Response.ok(ordersList).build();
        } catch (Exception e) {
            e.printStackTrace();
            Map<String, Object> error = Map.of("status", false, "message", "Error fetching orders");
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(error).build();
        }
    }

    @Path("/cancel/{orderId}")
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    public Response cancelOrder(@PathParam("orderId") int orderId, @Context HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        User user = (session != null) ? (User) session.getAttribute("user") : null;

        if (user == null) {
            Map<String, Object> error = Map.of("status", false, "message", "Please login first");
            return Response.status(Response.Status.UNAUTHORIZED).entity(error).build();
        }

        Transaction transaction = null;
        try (Session hibernateSession = HibernateUtil.getSessionFactory().openSession()) {
            transaction = hibernateSession.beginTransaction();

            Orders order = hibernateSession.get(Orders.class, orderId);

            if (order != null && order.getUser().getUserId() == user.getUserId() && "PENDING".equalsIgnoreCase(order.getStatus())) {

                order.setStatus("CANCELLED");
                hibernateSession.update(order);

                if (order.getOrderItems() != null) {
                    for (OrderItem item : order.getOrderItems()) {
                        Book book = item.getBook();
                        book.setQuantity(book.getQuantity() + item.getQuantity());
                        hibernateSession.update(book);
                    }
                }

                transaction.commit();

                Map<String, Object> responseMap = new HashMap<>();
                responseMap.put("status", true);
                responseMap.put("message", "Order successfully cancelled and items restocked.");
                return Response.ok(responseMap).build();

            } else {
                Map<String, Object> responseMap = new HashMap<>();
                responseMap.put("status", false);
                responseMap.put("message", "Cannot cancel order. It might already be processed or cancelled.");
                return Response.status(Response.Status.BAD_REQUEST).entity(responseMap).build();
            }

        } catch (Exception e) {
            if (transaction != null) {
                transaction.rollback();
            }
            e.printStackTrace();
            Map<String, Object> errorMap = Map.of("status", false, "message", "Server error while cancelling order");
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(errorMap).build();
        }
    }
}