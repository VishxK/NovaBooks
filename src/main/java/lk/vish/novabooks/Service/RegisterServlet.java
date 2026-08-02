package lk.vish.novabooks.Service;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import lk.vish.novabooks.entity.User;
import lk.vish.novabooks.util.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.Transaction;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;

@WebServlet("/RegisterServlet")
public class RegisterServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        
        JsonObject jsonResponse = new JsonObject();
        Gson gson = new Gson();

        try {
            BufferedReader reader = request.getReader();
            User newUser = gson.fromJson(reader, User.class);

            Session session = HibernateUtil.getSessionFactory().openSession();
            Transaction transaction = session.beginTransaction();

            try {
                Long count = (Long) session.createQuery("SELECT count(u) FROM User u WHERE u.email = :email")
                                           .setParameter("email", newUser.getEmail())
                                           .uniqueResult();
                
                if (count > 0) {
                    jsonResponse.addProperty("success", false);
                    jsonResponse.addProperty("message", "Email is already registered.");
                } else {
                    session.save(newUser);
                    transaction.commit();
                    
                    jsonResponse.addProperty("success", true);
                    jsonResponse.addProperty("message", "Registration successful!");
                }
            } catch (Exception e) {
                if (transaction != null) transaction.rollback();
                throw e;
            } finally {
                session.close();
            }

        } catch (Exception e) {
            jsonResponse.addProperty("success", false);
            jsonResponse.addProperty("message", "A server error occurred: " + e.getMessage());
        }

        response.getWriter().write(jsonResponse.toString());
    }
}