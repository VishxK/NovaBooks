package lk.vish.novabooks.controller.api;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import jakarta.ws.rs.*;
import lk.vish.novabooks.entity.Book;
import lk.vish.novabooks.util.HibernateUtil;
import org.hibernate.Session;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.hibernate.Transaction;
import org.hibernate.query.Query;

import java.util.List;

@Path("/books")
public class BookController {

    private final Gson gson = new Gson();


    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getBookById(@PathParam("id") int id) {
        Session session = HibernateUtil.getSessionFactory().openSession();
        try {
            Book book = session.get(Book.class, id);
            if (book == null) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity("{\"error\": \"Book not found\"}").build();
            }
            return Response.ok(gson.toJson(book)).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"error\": \"" + e.getMessage() + "\"}").build();
        } finally {
            session.close();
        }
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getBooks(
            @QueryParam("q") String query,
            @QueryParam("minPrice") Double minPrice,
            @QueryParam("maxPrice") Double maxPrice,
            @QueryParam("page") @DefaultValue("1") int page,
            @QueryParam("limit") @DefaultValue("8") int limit
    ) {
        JsonObject jsonResponse = new JsonObject();

        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            StringBuilder hql = new StringBuilder("FROM Book b WHERE 1=1");
            StringBuilder countHql = new StringBuilder("SELECT COUNT(b) FROM Book b WHERE 1=1");

            if (query != null && !query.trim().isEmpty()) {
                String filter = " AND (LOWER(b.title) LIKE :query OR LOWER(b.author) LIKE :query)";
                hql.append(filter);
                countHql.append(filter);
            }
            if (minPrice != null) {
                String filter = " AND b.price >= :minPrice";
                hql.append(filter);
                countHql.append(filter);
            }
            if (maxPrice != null) {
                String filter = " AND b.price <= :maxPrice";
                hql.append(filter);
                countHql.append(filter);
            }

            hql.append(" ORDER BY b.id DESC");

            Query<Book> bookQuery = session.createQuery(hql.toString(), Book.class);
            Query<Long> countQuery = session.createQuery(countHql.toString(), Long.class);

            if (query != null && !query.trim().isEmpty()) {
                String searchPattern = "%" + query.trim().toLowerCase() + "%";
                bookQuery.setParameter("query", searchPattern);
                countQuery.setParameter("query", searchPattern);
            }
            if (minPrice != null) {
                bookQuery.setParameter("minPrice", minPrice);
                countQuery.setParameter("minPrice", minPrice);
            }
            if (maxPrice != null) {
                bookQuery.setParameter("maxPrice", maxPrice);
                countQuery.setParameter("maxPrice", maxPrice);
            }

            long totalBooks = countQuery.uniqueResult();
            int totalPages = (int) Math.ceil((double) totalBooks / limit);
            if (totalPages == 0) totalPages = 1;

            int firstResult = (page - 1) * limit;
            bookQuery.setFirstResult(firstResult);
            bookQuery.setMaxResults(limit);

            List<Book> books = bookQuery.list();

            jsonResponse.add("books", gson.toJsonTree(books));
            jsonResponse.addProperty("currentPage", page);
            jsonResponse.addProperty("totalPages", totalPages);
            jsonResponse.addProperty("totalBooks", totalBooks);

            return Response.ok(gson.toJson(jsonResponse)).build();

        } catch (Exception e) {
            jsonResponse.addProperty("error", "Failed to retrieve catalog: " + e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(jsonResponse.toString()).build();
        }
    }


    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response addBook(String jsonInput) {
        JsonObject jsonResponse = new JsonObject();
        Transaction transaction = null;

        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            transaction = session.beginTransaction();

            Book book = gson.fromJson(jsonInput, Book.class);

            session.persist(book);
            transaction.commit();

            jsonResponse.addProperty("success", true);
            jsonResponse.addProperty("message", "Book added successfully!");
            return Response.status(Response.Status.CREATED).entity(jsonResponse.toString()).build();

        } catch (Exception e) {
            if (transaction != null) transaction.rollback();
            jsonResponse.addProperty("success", false);
            jsonResponse.addProperty("message", "Error adding book: " + e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(jsonResponse.toString()).build();
        }
    }
}