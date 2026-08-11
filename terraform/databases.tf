# Logical Databases for MilHub Microservices in existing Cloud SQL instance

resource "google_sql_database" "user_service_db" {
  name     = "user_service_db"
  instance = var.cloud_sql_instance_name
}

resource "google_sql_database" "product_service_db" {
  name     = "product_service_db"
  instance = var.cloud_sql_instance_name
}

resource "google_sql_database" "order_service_db" {
  name     = "order_service_db"
  instance = var.cloud_sql_instance_name
}

resource "google_sql_database" "payment_service_db" {
  name     = "payment_service_db"
  instance = var.cloud_sql_instance_name
}

resource "google_sql_database" "delivery_service_db" {
  name     = "delivery_service_db"
  instance = var.cloud_sql_instance_name
}
