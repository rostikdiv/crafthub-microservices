output "workload_identity_provider" {
  value       = google_iam_workload_identity_pool_provider.github_provider.name
  description = "The Workload Identity Provider ID for GitHub Actions workflow"
}

output "github_actions_service_account" {
  value       = google_service_account.github_actions_sa.email
  description = "Service Account email for GitHub Actions deployment"
}

output "api_gateway_url" {
  value       = google_cloud_run_v2_service.api_gateway.uri
  description = "Public Endpoint URL of MilHub API Gateway"
}

output "gcs_products_bucket" {
  value       = google_storage_bucket.products_bucket.name
  description = "GCS Products Bucket Name"
}

output "gcs_avatars_bucket" {
  value       = google_storage_bucket.avatars_bucket.name
  description = "GCS Avatars Bucket Name"
}

output "gcs_documents_bucket" {
  value       = google_storage_bucket.documents_bucket.name
  description = "GCS Private Documents Bucket Name"
}
