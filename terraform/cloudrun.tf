# Cloud Run Service Account for MilHub Microservices
resource "google_service_account" "milhub_cloudrun_sa" {
  account_id   = "milhub-cloudrun-sa"
  display_name = "MilHub Cloud Run Microservices Service Account"
}

# Example Cloud Run Service definition for MilHub API Gateway (Publicly Accessible)
resource "google_cloud_run_v2_service" "api_gateway" {
  name     = "milhub-api-gateway"
  location = var.region
  ingress  = "INGRESS_TRAFFIC_ALL"

  template {
    service_account = google_service_account.milhub_cloudrun_sa.email

    scaling {
      min_instance_count = 0
      max_instance_count = 2
    }

    vpc_access {
      network_interfaces {
        network    = data.google_compute_network.parkflow_vpc.name
        subnetwork = data.google_compute_subnetwork.parkflow_subnet.name
      }
      egress = "PRIVATE_RANGES_ONLY"
    }

    containers {
      # Use official Google hello container for initial Terraform bootstrapping
      image = "us-docker.pkg.dev/cloudrun/container/hello"

      resources {
        limits = {
          cpu    = "1000m"
          memory = "1024Mi"
        }
      }

      env {
        name  = "SPRING_PROFILES_ACTIVE"
        value = "cloud"
      }
    }
  }

  lifecycle {
    ignore_changes = [
      template[0].containers[0].image
    ]
  }
}

# Public invoker for API Gateway
resource "google_cloud_run_v2_service_iam_member" "api_gateway_public" {
  project  = google_cloud_run_v2_service.api_gateway.project
  location = google_cloud_run_v2_service.api_gateway.location
  name     = google_cloud_run_v2_service.api_gateway.name
  role     = "roles/run.invoker"
  member   = "allUsers"
}
