CREATE TABLE iap_receipts (
                              id              BIGINT       NOT NULL AUTO_INCREMENT,
                              user_id         BIGINT       NOT NULL,
                              product_code    VARCHAR(30)  NOT NULL,
                              purchase_token  VARCHAR(512) NOT NULL,
                              order_id        VARCHAR(64)  NULL,
                              status          VARCHAR(20)  NOT NULL,
                              purchased_at    DATETIME(6)  NOT NULL,
                              consumed_at     DATETIME(6)  NULL,
                              created_at      DATETIME(6)  NOT NULL,
                              PRIMARY KEY (id),
                              UNIQUE KEY uk_iap_receipts_token (purchase_token),
                              KEY idx_iap_receipts_status (status, created_at),
                              KEY idx_iap_receipts_user (user_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;