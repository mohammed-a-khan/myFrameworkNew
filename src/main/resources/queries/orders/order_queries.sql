-- Order management queries

-- QUERY: findOrderById
SELECT o.id, o.user_id, o.order_date, o.status, o.total_amount, o.shipping_address, o.billing_address
FROM orders o
WHERE o.id = :orderId;

-- QUERY: findOrdersByUser
SELECT o.id, o.user_id, o.order_date, o.status, o.total_amount
FROM orders o
WHERE o.user_id = :userId
ORDER BY o.order_date DESC
LIMIT ${maxResults};

-- QUERY: findOrdersByStatus
SELECT o.id, o.user_id, o.order_date, o.status, o.total_amount
FROM orders o
WHERE o.status = :status
ORDER BY o.order_date DESC
LIMIT ${maxResults};

-- QUERY: findOrdersByDateRange
SELECT o.id, o.user_id, o.order_date, o.status, o.total_amount
FROM orders o
WHERE o.order_date >= :startDate
AND o.order_date <= :endDate
ORDER BY o.order_date DESC
LIMIT ${maxResults};

-- QUERY: updateOrderStatus
UPDATE orders
SET status = :status, modified_date = :modifiedDate
WHERE id = :orderId;

-- QUERY: createOrder
INSERT INTO orders (user_id, order_date, status, total_amount, shipping_address, billing_address, created_date)
VALUES (:userId, :orderDate, :status, :totalAmount, :shippingAddress, :billingAddress, :createdDate);

-- QUERY: getOrderItems
SELECT oi.id, oi.order_id, oi.product_id, oi.quantity, oi.unit_price, oi.total_price,
       p.name as product_name, p.description as product_description
FROM order_items oi
JOIN products p ON oi.product_id = p.id
WHERE oi.order_id = :orderId
ORDER BY oi.id;

-- QUERY: addOrderItem
INSERT INTO order_items (order_id, product_id, quantity, unit_price, total_price)
VALUES (:orderId, :productId, :quantity, :unitPrice, :totalPrice);

-- QUERY: getOrderStats
SELECT 
    COUNT(*) as total_orders,
    SUM(CASE WHEN status = 'PENDING' THEN 1 ELSE 0 END) as pending_orders,
    SUM(CASE WHEN status = 'COMPLETED' THEN 1 ELSE 0 END) as completed_orders,
    SUM(CASE WHEN status = 'CANCELLED' THEN 1 ELSE 0 END) as cancelled_orders,
    SUM(total_amount) as total_revenue,
    AVG(total_amount) as avg_order_value
FROM orders
WHERE order_date >= :startDate
AND order_date <= :endDate;

-- QUERY: getDailySales
SELECT 
    DATE(order_date) as sale_date,
    COUNT(*) as order_count,
    SUM(total_amount) as daily_revenue
FROM orders
WHERE order_date >= :startDate
AND order_date <= :endDate
AND status = 'COMPLETED'
GROUP BY DATE(order_date)
ORDER BY sale_date DESC;