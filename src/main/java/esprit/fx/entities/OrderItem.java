package esprit.fx.entities;

public class OrderItem {

    private int id;
    private int quantity;
    private double price;
    private int productId;
    private int orderRefId;
    private String productName;
    private String productImage;

    public OrderItem() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }

    public int getProductId() { return productId; }
    public void setProductId(int productId) { this.productId = productId; }

    public int getOrderRefId() { return orderRefId; }
    public void setOrderRefId(int orderRefId) { this.orderRefId = orderRefId; }

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    public String getProductImage() { return productImage; }
    public void setProductImage(String productImage) { this.productImage = productImage; }

    @Override
    public String toString() {
        return "OrderItem{id=" + id + ", productId=" + productId +
                ", quantity=" + quantity + ", price=" + price + '}';
    }
}
