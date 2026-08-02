package lk.vish.novabooks.controller.api;

import com.google.gson.Gson;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.Part;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lk.vish.novabooks.entity.Book;
import lk.vish.novabooks.entity.Category;
import lk.vish.novabooks.util.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.io.File;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Path("/admin")
public class AdminBookController {

    private final Gson gson = new Gson();

    @GET
    @Path("/books")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAllBooks() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            List<Book> books = session.createQuery("FROM Book b ORDER BY b.id DESC", Book.class).list();
            return Response.ok(gson.toJson(books)).build();
        } catch (Exception e) {
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"error\": \"Failed to fetch books\"}").build();
        }
    }

    @POST
    @Path("/books")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response addBook(String jsonBody) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Book book = gson.fromJson(jsonBody, Book.class);

            var tx = session.beginTransaction();
            session.persist(book);
            tx.commit();

            return Response.ok("{\"success\": true, \"message\": \"Book added successfully!\"}").build();
        } catch (Exception e) {
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"error\": \"Failed to add book\"}").build();
        }
    }

    @GET
    @Path("/categories")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getCategories() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            List<Category> categories = session.createQuery("FROM Category c ORDER BY c.name ASC", Category.class).list();
            return Response.ok(gson.toJson(categories)).build();
        } catch (Exception e) {
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("[]")
                    .build();
        }

    }

    @POST
    @Path("/books/save")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    public Response saveBook(@Context HttpServletRequest request) {

        HttpSession httpSession = request.getSession(false);
        boolean isAdmin = (httpSession != null && httpSession.getAttribute("admin") != null && Boolean.TRUE.equals(httpSession.getAttribute("admin")));
        if (!isAdmin) {
            Map<String, Object> error = Map.of("success", false, "message", "Unauthorized access!");
            return Response.status(Response.Status.UNAUTHORIZED).entity(gson.toJson(error)).build();
        }

        Transaction tx = null;

        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            tx = session.beginTransaction();

            String idStr = getParamValue(request, "id");
            String title = getParamValue(request, "title");
            String author = getParamValue(request, "author");
            String priceStr = getParamValue(request, "price");
            String qtyStr = getParamValue(request, "quantity");
            String description = getParamValue(request, "description");

            String categoryIdStr = getParamValue(request, "categoryId");
            String newCategoryName = getParamValue(request, "newCategory");

            if (title == null || title.trim().isEmpty()) {
                Map<String, Object> err = Map.of("success", false, "message", "Book title is required");
                return Response.status(Response.Status.BAD_REQUEST).entity(gson.toJson(err)).build();
            }

            double price = 0.0;
            if (priceStr != null && !priceStr.trim().isEmpty()) {
                price = Double.parseDouble(priceStr.trim());
            }

            int quantity = 1;
            if (qtyStr != null && !qtyStr.trim().isEmpty()) {
                quantity = Integer.parseInt(qtyStr.trim());
            }

            Category categoryObj = null;

            if (newCategoryName != null && !newCategoryName.trim().isEmpty()) {
                Category existingCat = session.createQuery("FROM Category c WHERE LOWER(c.name) = :name", Category.class)
                        .setParameter("name", newCategoryName.trim().toLowerCase())
                        .uniqueResult();

                if (existingCat != null) {
                    categoryObj = existingCat;
                } else {
                    Category newCat = new Category();
                    newCat.setName(newCategoryName.trim());
                    session.persist(newCat);
                    session.flush();
                    categoryObj = newCat;
                }
            } else if (categoryIdStr != null && !categoryIdStr.trim().isEmpty()) {
                int categoryId = Integer.parseInt(categoryIdStr.trim());
                categoryObj = session.get(Category.class, categoryId);
            }

            String imagePath = null;
            try {
                Part filePart = request.getPart("imageFile");
                if (filePart != null && filePart.getSize() > 0) {
                    String fileName = Paths.get(filePart.getSubmittedFileName()).getFileName().toString();
                    String uploadPath = request.getServletContext().getRealPath("") + File.separator + "images" + File.separator + "books";

                    File uploadDir = new File(uploadPath);
                    if (!uploadDir.exists()) uploadDir.mkdirs();

                    String uniqueFileName = System.currentTimeMillis() + "_" + fileName;
                    filePart.write(uploadPath + File.separator + uniqueFileName);

                    imagePath = "images/books/" + uniqueFileName;
                }
            } catch (Exception imgEx) {
                System.out.println("Image upload warning: " + imgEx.getMessage());
            }

            Book book;
            boolean isUpdate = (idStr != null && !idStr.trim().isEmpty() && Integer.parseInt(idStr.trim()) > 0);

            if (isUpdate) {
                int bookId = Integer.parseInt(idStr.trim());
                book = session.get(Book.class, bookId);
                if (book == null) {
                    Map<String, Object> err = Map.of("success", false, "message", "Book not found for updating!");
                    return Response.status(Response.Status.NOT_FOUND).entity(gson.toJson(err)).build();
                }
            } else {
                book = new Book();
            }

            book.setTitle(title);
            book.setAuthor(author != null && !author.trim().isEmpty() ? author : "Unknown");
            book.setPrice(price);
            book.setQuantity(quantity);
            book.setDescription(description != null ? description : "");

            if (categoryObj != null) {
                book.setCategoryId(categoryObj);
            }

            if (imagePath != null) {
                book.setImagePath(imagePath);
            } else if (book.getImagePath() == null || book.getImagePath().isEmpty()) {
                book.setImagePath("images/default-book.png");
            }

            session.merge(book);
            tx.commit();

            Map<String, Object> resp = new HashMap<>();
            resp.put("success", true);
            resp.put("message", isUpdate ? "Book updated successfully!" : "Book added successfully!");

            return Response.ok(gson.toJson(resp)).build();

        } catch (Exception e) {
            if (tx != null && tx.isActive()) {
                try { tx.rollback(); } catch (Exception ignored) {}
            }
            e.printStackTrace();

            Map<String, Object> err = new HashMap<>();
            err.put("success", false);
            err.put("error", "Failed to save book: " + e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(gson.toJson(err))
                    .build();
        }
    }


    @GET
    @Path("/books/get")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getBookById(@QueryParam("id") int id) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Book book = session.get(Book.class, id);

            if (book == null) {
                Map<String, Object> err = Map.of("success", false, "message", "Book not found");
                return Response.status(Response.Status.NOT_FOUND).entity(gson.toJson(err)).build();
            }

            Map<String, Object> bookMap = new HashMap<>();
            bookMap.put("id", book.getBookId());
            bookMap.put("title", book.getTitle());
            bookMap.put("author", book.getAuthor());
            bookMap.put("price", book.getPrice());
            bookMap.put("quantity", book.getQuantity());
            bookMap.put("description", book.getDescription());
            bookMap.put("imagePath", book.getImagePath());

            if (book.getCategoryId() != null) {
                bookMap.put("categoryId", book.getCategoryId().getCategoryId());
                bookMap.put("categoryName", book.getCategoryId().getName());
            }

            Map<String, Object> resp = Map.of("success", true, "book", bookMap);
            return Response.ok(gson.toJson(resp)).build();

        } catch (Exception e) {
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"error\": \"" + e.getMessage() + "\"}")
                    .build();
        }
    }



    @DELETE
    @Path("/books/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteBook(@PathParam("id") int id) {
        Transaction tx = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            tx = session.beginTransaction();
            Book book = session.get(Book.class, id);

            if (book != null) {
                session.remove(book);
                tx.commit();
                return Response.ok("{\"success\": true}").build();
            } else {
                return Response.status(Response.Status.NOT_FOUND).entity("{\"error\": \"Book not found\"}").build();
            }
        } catch (Exception e) {
            if (tx != null && tx.isActive()) tx.rollback();
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("{\"error\": \"Delete failed\"}").build();
        }
    }


    private String getParamValue(HttpServletRequest request, String name) {
        try {
            String value = request.getParameter(name);
            if (value != null && !value.trim().isEmpty()) {
                return value;
            }

            Part part = request.getPart(name);
            if (part != null) {
                byte[] bytes = part.getInputStream().readAllBytes();
                String result = new String(bytes, java.nio.charset.StandardCharsets.UTF_8).trim();
                return result.isEmpty() ? null : result;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}