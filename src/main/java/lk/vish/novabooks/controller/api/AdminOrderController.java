package lk.vish.novabooks.controller.api;

import com.google.gson.Gson;
import lk.vish.novabooks.entity.Orders;
import lk.vish.novabooks.util.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.Transaction;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Path("/admin/orders")
public class AdminOrderController {

    @GET
    @Path("/list")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAllOrders() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            List<Orders> orders = session.createQuery("FROM Orders o ORDER BY o.id DESC", Orders.class).list();
            String json = new Gson().toJson(orders);
            return Response.ok(json, MediaType.APPLICATION_JSON).build();
        } catch (Exception e) {
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                           .entity("Error fetching orders: " + e.getMessage()).build();
        }
    }

    @POST
    @Path("/update-status")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateOrderStatus(@FormParam("orderId") int orderId, @FormParam("status") String status) {
        Transaction transaction = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            transaction = session.beginTransaction();

            Orders order = session.get(Orders.class, orderId);
            if (order != null) {
                order.setStatus(status.toUpperCase());
                session.update(order);
                transaction.commit();

                Map<String, Object> resp = new HashMap<>();
                resp.put("success", true);
                resp.put("message", "Order status updated successfully!");
                return Response.ok(new Gson().toJson(resp)).build();
            } else {
                return Response.status(Response.Status.NOT_FOUND).entity("Order not found").build();
            }
        } catch (Exception e) {
            if (transaction != null) transaction.rollback();
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                           .entity("Error updating status: " + e.getMessage()).build();
        }
    }
}