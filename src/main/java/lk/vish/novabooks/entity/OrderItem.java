package lk.vish.novabooks.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

@Entity
@Table(name = "order_items")
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private int id;

    @ManyToOne
    @JoinColumn(name = "order_id", nullable = false)
    @JsonIgnore
    private Orders order;

    @ManyToOne
    @JoinColumn(name = "book_id")
    private Book book;

    @Column(name = "quantity", nullable = false)
    private int quantity;

    @Column(name = "unit_price", nullable = false)
    private double unitPrice;

    public OrderItem() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    @JsonIgnore
    public Orders getOrder() { return order; }
    public void setOrder(Orders order) { this.order = order; }

    public Book getBook() { return book; }
    public void setBook(Book book) { this.book = book; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public double getUnitPrice() { return unitPrice; }
    public void setUnitPrice(double unitPrice) { this.unitPrice = unitPrice; }
}