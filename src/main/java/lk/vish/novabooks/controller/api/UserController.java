package lk.vish.novabooks.controller.api;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lk.vish.novabooks.entity.User;
import lk.vish.novabooks.util.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.query.Query;

import java.util.Map;


@Path("/auth")
public class UserController {

    private final Gson gson = new Gson();

    @POST
    @Path("/register")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response registerUser(String jsonInput) {
        JsonObject jsonResponse = new JsonObject();

        try {
            User newUser = gson.fromJson(jsonInput, User.class);

            Session session = HibernateUtil.getSessionFactory().openSession();
            Transaction transaction = session.beginTransaction();

            try {
                Long count = (Long) session.createQuery("SELECT count(u) FROM User u WHERE u.email = :email")
                        .setParameter("email", newUser.getEmail())
                        .uniqueResult();

                if (count > 0) {
                    jsonResponse.addProperty("success", false);
                    jsonResponse.addProperty("message", "Email is already registered.");
                    return Response.status(Response.Status.BAD_REQUEST).entity(jsonResponse.toString()).build();
                } else {
                    newUser.setRole("CUSTOMER");

                    session.save(newUser);
                    transaction.commit();

                    jsonResponse.addProperty("success", true);
                    jsonResponse.addProperty("message", "Registration successful!");
                    return Response.ok(jsonResponse.toString()).build();
                }
            } catch (Exception e) {
                if (transaction != null) transaction.rollback();
                throw e;
            } finally {
                session.close();
            }

        } catch (Exception e) {
            jsonResponse.addProperty("success", false);
            jsonResponse.addProperty("message", "Server error: " + e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(jsonResponse.toString()).build();
        }
    }

    @GET
    @Path("/me")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getCurrentUser(@Context HttpServletRequest request) {
        JsonObject jsonResponse = new JsonObject();
        HttpSession httpSession = request.getSession(false);
        HttpSession session = request.getSession(false);

        if (httpSession != null && httpSession.getAttribute("role") != null) {
            jsonResponse.addProperty("isLoggedIn", true);
            jsonResponse.addProperty("role", (String) httpSession.getAttribute("role"));

            User user = (User) session.getAttribute("user");

            if (user != null) {
                user.setPassword(null);

                jsonResponse.addProperty("firstName", user.getFirstName());
            } else {
                jsonResponse.addProperty("firstName", "Admin");
            }
            jsonResponse.addProperty("user", gson.toJson(user));

            return Response.ok(jsonResponse.toString()).build();
        } else {
            jsonResponse.addProperty("isLoggedIn", false);
            return Response.ok(jsonResponse.toString()).build();
        }

    }

    @POST
    @Path("/login")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response login(String requestBody, @Context HttpServletRequest request) {
        JsonObject jsonResponse = new JsonObject();
        Gson gson = new Gson();

        String ADMIN_EMAIL = "admin@novabooks.lk";
        String ADMIN_PASSWORD = "admin123";

        try {
            JsonObject input = gson.fromJson(requestBody, JsonObject.class);
            String email = input.get("email").getAsString().trim();
            String password = input.get("password").getAsString();
            boolean rememberMe = input.has("rememberMe") && input.get("rememberMe").getAsBoolean();

            if (ADMIN_EMAIL.equalsIgnoreCase(email) && ADMIN_PASSWORD.equals(password)) {
                HttpSession httpSession = request.getSession(true);

                httpSession.setAttribute("admin", true);
                httpSession.setAttribute("adminEmail", email);

                jsonResponse.addProperty("success", true);
                jsonResponse.addProperty("isAdmin", true);
                jsonResponse.addProperty("message", "Admin authenticated successfully.");

                return Response.ok(gson.toJson(jsonResponse)).build();
            }

            User user;
            try (Session session = HibernateUtil.getSessionFactory().openSession()) {
                String hql = "FROM User u WHERE u.email = :email AND u.password = :password";
                Query<User> query = session.createQuery(hql, User.class);
                query.setParameter("email", email);
                query.setParameter("password", password);
                user = query.uniqueResult();
            }

            if (user != null) {
                HttpSession httpSession = request.getSession(true);

                String userRole = user.getRole() != null ? user.getRole().toString() : "USER";
                httpSession.setAttribute("role", userRole);
                httpSession.setAttribute("user", user);

                if (rememberMe) {
                    httpSession.setMaxInactiveInterval(30 * 24 * 60 * 60);
                } else {
                    httpSession.setMaxInactiveInterval(30 * 60);
                }

                jsonResponse.addProperty("status", true);
                jsonResponse.addProperty("message", "Login successful");

                JsonObject userJson = new JsonObject();
                userJson.addProperty("id", user.getUserId());
                userJson.addProperty("firstName", user.getFirstName());
                userJson.addProperty("lastName", user.getLastName());
                userJson.addProperty("email", user.getEmail());
                userJson.addProperty("role", userRole);

                jsonResponse.add("user", userJson);

                return Response.ok(jsonResponse.toString()).build();

            } else {
                // Login Failed Response
                jsonResponse.addProperty("status", false);
                jsonResponse.addProperty("message", "Invalid email or password.");
                return Response.status(Response.Status.UNAUTHORIZED).entity(jsonResponse.toString()).build();
            }

        } catch (Exception e) {
            e.printStackTrace();
            jsonResponse.addProperty("status", false);
            jsonResponse.addProperty("message", "Server error occurred.");
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(jsonResponse.toString()).build();
        }
    }

    @GET
    @Path("/check-session")
    @Produces(MediaType.APPLICATION_JSON)
    public Response checkSession(@Context HttpServletRequest request) {
        HttpSession httpSession = request.getSession(false);

        if (httpSession != null && httpSession.getAttribute("user") != null) {
            User user = (User) httpSession.getAttribute("user");
            return Response.ok(gson.toJson(Map.of("isLoggedIn", true, "user", user))).build();
        }

        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("remember_user".equals(cookie.getName())) {
                    String userEmail = cookie.getValue();

                    try (Session session = HibernateUtil.getSessionFactory().openSession()) {
                        Query<User> query = session.createQuery("FROM User WHERE email = :email", User.class);
                        query.setParameter("email", userEmail);
                        User user = query.uniqueResult();

                        if (user != null) {
                            HttpSession newSession = request.getSession(true);
                            newSession.setAttribute("user", user);
                            return Response.ok(gson.toJson(Map.of("isLoggedIn", true, "user", user))).build();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        return Response.ok(gson.toJson(Map.of("isLoggedIn", false))).build();
    }

    @POST
    @Path("/logout")
    @Produces(MediaType.APPLICATION_JSON)
    public Response logout(@Context HttpServletRequest request, @Context HttpServletResponse response) {

        HttpSession httpSession = request.getSession(false);
        if (httpSession != null) {
            httpSession.invalidate();
        }

        Cookie rememberCookie = new Cookie("remember_user", "");
        rememberCookie.setMaxAge(0);
        rememberCookie.setPath("/");
        response.addCookie(rememberCookie);

        return Response.ok(gson.toJson(Map.of("status", true, "message", "Logged out successfully"))).build();
    }

    @PUT
    @Path("/profile")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateProfile(User updateData, @Context HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("user") == null) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }

        User sessionUser = (User) session.getAttribute("user");

        try (Session hibernateSession = HibernateUtil.getSessionFactory().openSession()) {
            Transaction tx = hibernateSession.beginTransaction();

            User user = hibernateSession.get(User.class, sessionUser.getUserId());

            user.setFirstName(updateData.getFirstName());
            user.setLastName(updateData.getLastName());
            user.setMobile(updateData.getMobile());
            user.setAddressLine1(updateData.getAddressLine1());
            user.setAddressLine2(updateData.getAddressLine2());
            user.setCity(updateData.getCity());

            hibernateSession.update(user);
            tx.commit();

            session.setAttribute("user", user);

            return Response.ok(Map.of("status", true, "message", "Profile updated successfully!")).build();

        } catch (Exception e) {
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("status", false, "message", "Error updating profile")).build();
        }
    }


    @POST
    @Path("/change-password")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response changePassword(Map<String, String> payload, @Context HttpServletRequest request) {
        HttpSession session = request.getSession(false);

        if (session == null || session.getAttribute("user") == null) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }

        User sessionUser = (User) session.getAttribute("user");
        String currentPassword = payload.get("currentPassword");
        String newPassword = payload.get("newPassword");

        if (currentPassword == null || newPassword == null || newPassword.trim().isEmpty()) {
            Map<String, Object> error = Map.of("status", false, "message", "Please fill in all password fields.");
            return Response.ok(gson.toJson(error)).build();
        }

        try (Session hibernateSession = HibernateUtil.getSessionFactory().openSession()) {
            Transaction tx = hibernateSession.beginTransaction();

            User user = hibernateSession.get(User.class, sessionUser.getUserId());

            if (!user.getPassword().equals(currentPassword)) {
                Map<String, Object> responseMap = Map.of("status", false, "message", "Current password is incorrect!");
                return Response.ok(gson.toJson(responseMap)).build();
            }

            user.setPassword(newPassword);
            hibernateSession.update(user);
            tx.commit();

            sessionUser.setPassword(newPassword);
            session.setAttribute("user", sessionUser);

            Map<String, Object> responseMap = Map.of("status", true, "message", "Password updated successfully!");
            return Response.ok(gson.toJson(responseMap)).build();

        } catch (Exception e) {
            e.printStackTrace();
            Map<String, Object> error = Map.of("status", false, "message", "Server error occurred while updating password.");
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(gson.toJson(error)).build();
        }
    }
}