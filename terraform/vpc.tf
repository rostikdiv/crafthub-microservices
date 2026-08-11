# Reference existing ParkFlow VPC resources

data "google_compute_network" "parkflow_vpc" {
  name = "parkflow-vpc"
}

data "google_compute_subnetwork" "parkflow_subnet" {
  name   = "parkflow-subnet"
  region = var.region
}

# Firewall rule to allow Cloud Run Direct VPC Egress to reach Kafka (9092) on the VM
resource "google_compute_firewall" "allow_kafka_internal" {
  name    = "milhub-allow-kafka-internal"
  network = data.google_compute_network.parkflow_vpc.id

  allow {
    protocol = "tcp"
    ports    = ["9092"]
  }

  source_ranges = ["10.0.0.0/8"]
}
