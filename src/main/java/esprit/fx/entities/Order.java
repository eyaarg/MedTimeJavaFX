package esprit.fx.entities;

import java.time.LocalDateTime;

public class Order {

    private int id;
    private int userId;
    private double total;
    private String status;
    private LocalDateTime dateOrder;

    public Order() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public double getTotal() { return total; }
    public void setTotal(double total) { this.total = total; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getDateOrder() { return dateOrder; }
    public void setDateOrder(LocalDateTime dateOrder) { this.dateOrder = dateOrder; }

    @Override
    public String toString() {
        return "Order{id=" + id + ", userId=" + userId +
                ", total=" + total + ", status='" + status + "'}";
    }
}
