variable "project_id" {
  type        = string
  description = "GCP Project ID"
  default     = "parkflow-cloud"
}

variable "region" {
  type        = string
  description = "Default GCP Region"
  default     = "us-central1"
}

variable "zone" {
  type        = string
  description = "Default GCP Zone"
  default     = "us-central1-a"
}

variable "gar_repository_name" {
  type        = string
  description = "GCP Artifact Registry Repository Name"
  default     = "milhub-repo"
}

variable "cloud_sql_instance_name" {
  type        = string
  description = "Existing Cloud SQL Instance Name"
  default     = "parkflow-db-instance"
}

variable "vm_instance_name" {
  type        = string
  description = "Existing Compute Engine VM Name"
  default     = "parkflow-services-vm"
}
