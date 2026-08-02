package lk.vish.novabooks.controller.api;

import com.google.gson.Gson;
import lk.vish.novabooks.util.HibernateUtil;
import org.hibernate.Session;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Path("/admin/dashboard")
public class AdminDashboardController {

    @GET
    @Path("/stats")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getDashboardStats() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {

            Long totalBooks = session.createQuery("SELECT COUNT(b) FROM Book b", Long.class).uniqueResult();

            Long totalOrders = session.createQuery("SELECT COUNT(o) FROM Orders o", Long.class).uniqueResult();

            Double totalSales = session.createQuery("SELECT COALESCE(SUM(o.totalAmount), 0.0) FROM Orders o WHERE o.status = 'COMPLETED'", Double.class).uniqueResult();

            Long totalUsers = session.createQuery("SELECT COUNT(u) FROM User u WHERE u.role = 'CUSTOMER'", Long.class).uniqueResult();

            List<Object[]> monthlySalesRaw = session.createQuery(
                            "SELECT date_format(o.orderDate, '%b'), COALESCE(SUM(o.totalAmount), 0.0) " +
                                    "FROM Orders o WHERE o.status = 'COMPLETED' " +
                                    "GROUP BY year(o.orderDate), month(o.orderDate), date_format(o.orderDate, '%b') " +
                                    "ORDER BY year(o.orderDate) ASC, month(o.orderDate) ASC", Object[].class
                    )
                    .setMaxResults(6)
                    .list();

            List<String> monthLabels = new ArrayList<>();
            List<Double> salesData = new ArrayList<>();

            for (Object[] row : monthlySalesRaw) {
                monthLabels.add((String) row[0]);
                salesData.add(((Number) row[1]).doubleValue());
            }

            Map<String, Object> salesChartMap = new HashMap<>();
            salesChartMap.put("labels", monthLabels);
            salesChartMap.put("data", salesData);

            List<Object[]> statusCountsRaw = session.createQuery(
                    "SELECT o.status, COUNT(o) FROM Orders o GROUP BY o.status", Object[].class
            ).list();

            Map<String, Long> statusMap = new HashMap<>();
            statusMap.put("COMPLETED", 0L);
            statusMap.put("PENDING", 0L);
            statusMap.put("CANCELLED", 0L);

            for (Object[] row : statusCountsRaw) {
                String status = (String) row[0];
                Number count = (Number) row[1];
                if (status != null && statusMap.containsKey(status.toUpperCase())) {
                    statusMap.put(status.toUpperCase(), count.longValue());
                }
            }

            Map<String, Object> responseMap = new HashMap<>();
            responseMap.put("totalBooks", totalBooks != null ? totalBooks.longValue() : 0);
            responseMap.put("totalOrders", totalOrders != null ? totalOrders.longValue() : 0);
            responseMap.put("totalSales", totalSales != null ? totalSales.doubleValue() : 0.0);
            responseMap.put("totalUsers", totalUsers != null ? totalUsers.longValue() : 0);
            responseMap.put("salesChart", salesChartMap);
            responseMap.put("orderStatusChart", statusMap);

            String jsonResponse = new Gson().toJson(responseMap);
            return Response.ok(jsonResponse, MediaType.APPLICATION_JSON).build();

        } catch (Exception e) {
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error fetching dashboard stats: " + e.getMessage())
                    .build();
        }
    }
}