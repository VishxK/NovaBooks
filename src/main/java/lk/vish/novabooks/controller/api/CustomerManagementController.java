package lk.vish.novabooks.controller.api;

import com.google.gson.Gson;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lk.vish.novabooks.entity.User;
import lk.vish.novabooks.util.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Path("/admin/customers")
public class CustomerManagementController {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAllCustomers() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            
            List<User> userList = session.createQuery("FROM User u ORDER BY u.id DESC", User.class).list();
            List<Map<String, Object>> responseList = new ArrayList<>();

            for (User user : userList) {
                Map<String, Object> map = new HashMap<>();
                map.put("id", user.getUserId());
                
                String fName = user.getFirstName() != null ? user.getFirstName() : "";
                String lName = user.getLastName() != null ? user.getLastName() : "";
                map.put("name", (fName + " " + lName).trim());
                
                map.put("email", user.getEmail() != null ? user.getEmail() : "N/A");
                map.put("role", user.getRole() != null ? user.getRole().toString() : "CUSTOMER");
                map.put("status", user.getStatus() != null ? user.getStatus() : "ACTIVE");

                responseList.add(map);
            }

            return Response.ok(new Gson().toJson(responseList), MediaType.APPLICATION_JSON).build();

        } catch (Exception e) {
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"error\":\"" + e.getMessage() + "\"}")
                    .build();
        }
    }

    @POST
    @Path("/toggle-status")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response toggleCustomerStatus(String jsonRequest) {
        Map<String, Object> responseMap = new HashMap<>();
        Transaction transaction = null;

        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Map requestData = new Gson().fromJson(jsonRequest, Map.class);
            
            if (requestData.get("userId") == null || requestData.get("status") == null) {
                responseMap.put("success", false);
                responseMap.put("message", "Invalid Request Data");
                return Response.status(Response.Status.BAD_REQUEST).entity(new Gson().toJson(responseMap)).build();
            }

            int userId = ((Double) requestData.get("userId")).intValue();
            String newStatus = (String) requestData.get("status");

            transaction = session.beginTransaction();
            User user = session.get(User.class, userId);

            if (user != null) {
                user.setStatus(newStatus);
                session.merge(user);
                transaction.commit();

                responseMap.put("success", true);
                responseMap.put("message", "User status updated successfully!");
                return Response.ok(new Gson().toJson(responseMap), MediaType.APPLICATION_JSON).build();
            } else {
                responseMap.put("success", false);
                responseMap.put("message", "User not found!");
                return Response.status(Response.Status.NOT_FOUND).entity(new Gson().toJson(responseMap)).build();
            }

        } catch (Exception e) {
            if (transaction != null) transaction.rollback();
            e.printStackTrace();
            responseMap.put("success", false);
            responseMap.put("message", "Error: " + e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(new Gson().toJson(responseMap)).build();
        }
    }
}